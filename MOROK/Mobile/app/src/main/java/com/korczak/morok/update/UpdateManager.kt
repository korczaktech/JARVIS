package com.korczak.morok.update

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.korczak.morok.BuildConfig
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.Executors

class UpdateManager(private val activity: Activity) {
    companion object {
        private const val REPOSITORY = "korczaktechnology-tech/JARVIS"
        private const val DEBUG_TAG = "morok-debug"
        private const val DEBUG_APK = "morok-debug.apk"
        private const val DEBUG_SHA = "morok-debug.sha256"
        private const val BUILD_SHA = "build-sha.txt"
        private const val MOROK_PACKAGE = "com.korczak.morok"
    }

    private val executor = Executors.newSingleThreadExecutor()
    private val releaseUrl = "https://api.github.com/repos/$REPOSITORY/releases/tags/$DEBUG_TAG"

    fun check(onResult: (String) -> Unit) {
        executor.execute {
            runCatching {
                val release = JSONObject(request(releaseUrl))

                if (release.optString("tag_name").trim() != DEBUG_TAG) {
                    throw IllegalStateException("Fonte de atualização inválida")
                }
                if (!release.optBoolean("prerelease", false)) {
                    throw IllegalStateException("A atualização não é do canal Debug do Morok")
                }

                val assets = release.optJSONArray("assets")
                    ?: throw IllegalStateException("Release Debug sem assets")

                var apkUrl = ""
                var shaUrl = ""
                var buildShaUrl = ""

                for (i in 0 until assets.length()) {
                    val asset = assets.optJSONObject(i) ?: continue
                    when (asset.optString("name").trim()) {
                        DEBUG_APK -> apkUrl = asset.optString("browser_download_url").trim()
                        DEBUG_SHA -> shaUrl = asset.optString("browser_download_url").trim()
                        BUILD_SHA -> buildShaUrl = asset.optString("browser_download_url").trim()
                    }
                }

                if (apkUrl.isBlank() || shaUrl.isBlank()) {
                    throw IllegalStateException("Atualização Debug sem APK ou checksum")
                }

                val publishedSha = if (buildShaUrl.isNotBlank()) {
                    request(buildShaUrl).trim()
                } else {
                    release.optString("target_commitish").trim()
                }

                if (publishedSha.isBlank() || publishedSha == BuildConfig.MOROK_BUILD_SHA) {
                    return@runCatching
                }

                val expectedSha = request(shaUrl).trim()
                    .split(Regex("\\s+"))
                    .firstOrNull()
                    .orEmpty()

                if (!Regex("^[a-fA-F0-9]{64}$").matches(expectedSha)) {
                    throw IllegalStateException("Checksum do APK Debug inválido")
                }

                val apk = downloadApk(apkUrl, expectedSha)
                verifySha256(apk, expectedSha)
                validateMorokDebugApk(apk)

                val version = release.optString("name").ifBlank { "Morok Debug App" }
                activity.runOnUiThread {
                    onResult("READY:" + version + ":" + apk.absolutePath)
                }
            }.onFailure { e ->
                activity.runOnUiThread {
                    onResult("ERRO:" + (e.message ?: "falha na atualização"))
                }
            }
        }
    }

    private fun request(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.requestMethod = "GET"
        c.connectTimeout = 15000
        c.readTimeout = 30000
        c.setRequestProperty("Accept", "application/vnd.github+json")
        c.setRequestProperty("Cache-Control", "no-cache")
        c.inputStream.use { return it.bufferedReader().use { reader -> reader.readText() } }
    }

    private fun downloadApk(url: String, sha: String): File {
        val dir = File(activity.filesDir, "updates").apply { mkdirs() }
        val file = File(dir, "morok-debug-$sha.apk")
        val c = URL(url).openConnection() as HttpURLConnection
        c.requestMethod = "GET"
        c.connectTimeout = 15000
        c.readTimeout = 120000
        c.setRequestProperty("Cache-Control", "no-cache")
        c.inputStream.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        }
        return file
    }

    private fun validateMorokDebugApk(file: File) {
        val info = activity.packageManager.getPackageArchiveInfo(
            file.absolutePath,
            PackageManager.GET_META_DATA or PackageManager.GET_SIGNING_CERTIFICATES
        ) ?: throw IllegalStateException("O arquivo baixado não é um APK Android válido")

        if (info.packageName != MOROK_PACKAGE) {
            throw IllegalStateException("APK rejeitado: pacote incorreto (${info.packageName})")
        }

        val label = info.applicationInfo?.loadLabel(activity.packageManager)?.toString()?.trim()
        if (label != "Morok") {
            throw IllegalStateException("APK rejeitado: aplicativo incorreto")
        }

        val versionName = info.versionName.orEmpty()
        if (!versionName.endsWith("-debug")) {
            throw IllegalStateException("APK rejeitado: não é uma versão Debug do Morok")
        }

        if (Build.VERSION.SDK_INT >= 28) {
            val currentInfo = activity.packageManager.getPackageInfo(
                activity.packageName,
                PackageManager.GET_SIGNING_CERTIFICATES
            )
            val currentSigners = currentInfo.signingInfo?.apkContentsSigners?.map { it.toCharsString() }?.toSet().orEmpty()
            val newSigners = info.signingInfo?.apkContentsSigners?.map { it.toCharsString() }?.toSet().orEmpty()
            if (currentSigners.isNotEmpty() && newSigners.isNotEmpty() && currentSigners != newSigners) {
                throw IllegalStateException("APK rejeitado: assinatura diferente do Morok instalado")
            }
        }

        if (info.longVersionCode <= BuildConfig.VERSION_CODE) {
            throw IllegalStateException("APK rejeitado: versão não é mais nova que a instalada")
        }
    }

    private fun verifySha256(file: File, expected: String) {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val n = input.read(buffer)
                if (n <= 0) break
                digest.update(buffer, 0, n)
            }
        }
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        if (!actual.equals(expected, true)) {
            throw IllegalStateException("Checksum do APK Debug inválido")
        }
    }

    fun install(path: String) = install(File(path))

    private fun install(file: File) {
        if (Build.VERSION.SDK_INT >= 26 && !activity.packageManager.canRequestPackageInstalls()) {
            activity.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:" + activity.packageName)
                )
            )
            return
        }

        val uri = FileProvider.getUriForFile(
            activity,
            activity.packageName + ".fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        activity.startActivity(intent)
    }
}

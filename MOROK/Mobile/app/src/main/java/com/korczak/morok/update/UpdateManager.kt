package com.korczak.morok.update

import android.app.Activity
import android.content.Intent
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
    private val executor = Executors.newSingleThreadExecutor()
    private val manifestUrl = "https://korczaktechnology-tech.github.io/JARVIS/morok-update.json"

    fun check(onResult: (String) -> Unit) {
        executor.execute {
            runCatching {
                val json = JSONObject(request(manifestUrl))
                val sha = json.optString("sha").trim()
                val version = json.optString("version").ifBlank { "nova" }
                val apkUrl = json.optString("apkUrl").trim()
                if (sha.isBlank() || apkUrl.isBlank() || sha == BuildConfig.MOROK_BUILD_SHA) return@runCatching
                val apk = downloadApk(apkUrl, sha)
                verifySha256(apk, sha)
                activity.runOnUiThread { onResult("READY:$version:${apk.absolutePath}") }
            }.onFailure { e ->
                activity.runOnUiThread { onResult("ERRO:" + (e.message ?: "falha na atualização")) }
            }
        }
    }

    private fun request(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.requestMethod = "GET"
        c.connectTimeout = 15000
        c.readTimeout = 30000
        c.setRequestProperty("Cache-Control", "no-cache")
        return c.inputStream.bufferedReader().use { it.readText() }
    }

    private fun downloadApk(url: String, sha: String): File {
        val dir = File(activity.filesDir, "updates").apply { mkdirs() }
        val file = File(dir, "morok-debug-${sha}.apk")
        val c = URL(url).openConnection() as HttpURLConnection
        c.requestMethod = "GET"
        c.connectTimeout = 15000
        c.readTimeout = 120000
        c.inputStream.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
        return file
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
        if (!actual.equals(expected, true)) throw IllegalStateException("Checksum do APK Debug inválido")
    }

    fun install(path: String) = install(File(path))
    private fun install(file: File) {
        if (Build.VERSION.SDK_INT >= 26 && !activity.packageManager.canRequestPackageInstalls()) {
            activity.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + activity.packageName)))
            return
        }
        val uri = FileProvider.getUriForFile(activity, activity.packageName + ".fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        activity.startActivity(intent)
    }
}

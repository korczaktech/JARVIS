package com.korczak.morok.update

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.korczak.morok.BuildConfig
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream
import java.util.concurrent.Executors

class UpdateManager(private val activity: Activity) {
    private val executor = Executors.newSingleThreadExecutor()
    private val api = "https://api.github.com/repos/korczaktechnology-tech/JARVIS"

    fun check(onResult: (String) -> Unit) {
        executor.execute {
            runCatching {
                val runs = request("$api/actions/workflows/morok-mobile.yml/runs?branch=main&status=success&per_page=10")
                val run = Regex("""\{[^{}]*"id"\s*:\s*(\d+)[^{}]*"head_sha"\s*:\s*"([0-9a-f]+)"[^{}]*\}""").findAll(runs).map { it.groupValues[1].toLong() to it.groupValues[2] }.firstOrNull { it.second != BuildConfig.MOROK_BUILD_SHA } ?: return@runCatching
                val artifacts = request("$api/actions/runs/${run.first}/artifacts?per_page=20")
                val artifact = Regex("""\{[^{}]*"id"\s*:\s*(\d+)[^{}]*"name"\s*:\s*"morok-mobile-debug"[^{}]*"archive_download_url"\s*:\s*"([^"]+)"[^{}]*\}""").find(artifacts) ?: return@runCatching
                val zip = downloadZip(artifact.groupValues[2], run.first)
                val apk = unzipApk(zip, run.second)
                activity.runOnUiThread { onResult("READY:${run.second}:${apk.absolutePath}") }
            }.onFailure { e -> activity.runOnUiThread { onResult("ERRO:" + (e.message ?: "falha na atualização")) } }
        }
    }

    private fun request(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.requestMethod = "GET"
        c.setRequestProperty("Accept", "application/vnd.github+json")
        c.connectTimeout = 15000
        c.readTimeout = 30000
        return c.inputStream.bufferedReader().use { it.readText() }
    }

    private fun downloadZip(url: String, runId: Long): File {
        val dir = File(activity.filesDir, "updates").apply { mkdirs() }
        val file = File(dir, "morok-debug-$runId.zip")
        val c = URL(url).openConnection() as HttpURLConnection
        c.requestMethod = "GET"
        c.setRequestProperty("Accept", "application/vnd.github+json")
        c.connectTimeout = 15000
        c.readTimeout = 120000
        c.inputStream.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
        return file
    }

    private fun unzipApk(zip: File, sha: String): File {
        val out = File(activity.filesDir, "updates/morok-debug-$sha.apk")
        ZipInputStream(zip.inputStream().buffered()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && entry.name.endsWith(".apk")) { out.outputStream().use { output -> zis.copyTo(output) }; return out }
                entry = zis.nextEntry
            }
        }
        throw IllegalStateException("APK Debug não encontrado no artifact")
    }

    fun install(path: String) = install(File(path))
    private fun install(file: File) {
        if (Build.VERSION.SDK_INT >= 26 && !activity.packageManager.canRequestPackageInstalls()) { activity.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + activity.packageName))); return }
        val uri = FileProvider.getUriForFile(activity, activity.packageName + ".fileprovider", file)
        val i = Intent(Intent.ACTION_VIEW).apply { setDataAndType(uri, "application/vnd.android.package-archive"); addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION) }
        activity.startActivity(i)
    }
}
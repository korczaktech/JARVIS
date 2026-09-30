package com.korczak.morok.update

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.Executors

class UpdateManager(private val activity: Activity) {
    private val executor = Executors.newSingleThreadExecutor()
    private val api = "https://api.github.com/repos/korczaktechnology-tech/JARVIS/releases/latest"

    fun check(onResult: (String) -> Unit) {
        executor.execute {
            runCatching {
                val json = request(api)
                val tag = Regex("\"tag_name\"\\s*:\\s*\"([^\"]+)\"").find(json)?.groupValues?.get(1) ?: return@runCatching
                val version = tag.removePrefix("v")
                val current = activity.packageManager.getPackageInfo(activity.packageName, 0).versionName ?: "0.0.0"
                if (compare(version, current) <= 0) return@runCatching
                val assets = Regex("\"browser_download_url\"\\s*:\\s*\"([^\"]+)\"").findAll(json).map { it.groupValues[1] }.toList()
                val apk = assets.firstOrNull { it.endsWith(".apk") } ?: return@runCatching
                val hashUrl = assets.firstOrNull { it.endsWith(".sha256") }
                val file = download(apk, version)
                if (hashUrl != null) {
                    val expected = downloadText(hashUrl).trim().split(Regex("\\s+")).first()
                    require(expected.equals(sha256(file), true)) { "integridade da atualização inválida" }
                }
                activity.runOnUiThread { onResult(version); install(file) }
            }.onFailure { e ->
                activity.runOnUiThread { onResult("ERRO:" + (e.message ?: "falha na atualização")) }
            }
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

    private fun download(url: String, version: String): File {
        val dir = File(activity.filesDir, "updates").apply { mkdirs() }
        val file = File(dir, "morok-$version.apk")
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15000
        c.readTimeout = 120000
        c.inputStream.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
        return file
    }

    private fun downloadText(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 15000
        c.readTimeout = 30000
        return c.inputStream.bufferedReader().use { it.readText() }
    }

    private fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val b = ByteArray(8192)
            var n = input.read(b)
            while (n > 0) { md.update(b, 0, n); n = input.read(b) }
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private fun install(file: File) {
        if (Build.VERSION.SDK_INT >= 26 && !activity.packageManager.canRequestPackageInstalls()) {
            activity.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + activity.packageName)))
            return
        }
        val uri = FileProvider.getUriForFile(activity, activity.packageName + ".fileprovider", file)
        val i = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        activity.startActivity(i)
    }

    private fun compare(a: String, b: String): Int {
        val x = a.split(".").map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
        val y = b.split(".").map { it.filter(Char::isDigit).toIntOrNull() ?: 0 }
        for (i in 0..2) {
            val d = x.getOrElse(i) { 0 }.compareTo(y.getOrElse(i) { 0 })
            if (d != 0) return d
        }
        return 0
    }
}

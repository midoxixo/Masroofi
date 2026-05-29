package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

data class UpdateInfo(
    val versionCode: Int,
    val versionName: String,
    val isForceUpdate: Boolean,
    val releaseNotesEn: String,
    val releaseNotesAr: String,
    val downloadUrl: String
)

object UpdateManager {

    // TODO: Replace this URL with the actual raw JSON URL where you host your update.json
    // Example: "https://raw.githubusercontent.com/username/repo/main/update.json"
    private const val UPDATE_JSON_URL = "https://raw.githubusercontent.com/midoxixo/Masroofi/main/update.json"
    private val client = OkHttpClient()

    /**
     * Fetches the remote update.json and parses the data.
     * Throws an exception if no internet or invalid response.
     */
    suspend fun checkUpdate(): UpdateInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val urlWithTimestamp = "$UPDATE_JSON_URL?t=${System.currentTimeMillis()}"
                val request = Request.Builder()
                    .url(urlWithTimestamp)
                    .cacheControl(okhttp3.CacheControl.FORCE_NETWORK)
                    .build()
                try {
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        return@withContext parseJson(responseBody)
                    } else {
                        // Return null if there's no update file on GitHub
                        return@withContext null
                    }
                } catch (e: java.io.IOException) {
                    throw e
                }
            } catch (e: Exception) {
                throw e
            }
        }
    }

    private fun parseJson(body: String?): UpdateInfo? {
        if (body.isNullOrEmpty()) return null
        return try {
            val json = JSONObject(body)
            UpdateInfo(
                versionCode = json.getInt("versionCode"),
                versionName = json.getString("versionName"),
                isForceUpdate = json.optBoolean("isForceUpdate", false),
                releaseNotesEn = json.optString("releaseNotesEn", "Added new features and bug fixes."),
                releaseNotesAr = json.optString("releaseNotesAr", "تم إضافة ميزات جديدة وإصلاح بعض الأخطاء."),
                downloadUrl = json.getString("downloadUrl")
            )
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Resolves the release page URL to extract a direct APK link if it's GitHub releases.
     */
    private suspend fun resolveDirectApkUrl(url: String): String {
        return withContext(Dispatchers.IO) {
            try {
                if (url.contains("github.com") && url.contains("/releases")) {
                    val parts = url.split("github.com/")[1].split("/")
                    if (parts.size >= 2) {
                        val owner = parts[0]
                        val repo = parts[1]
                        val apiUrl = "https://api.github.com/repos/$owner/$repo/releases/latest"
                        val request = Request.Builder()
                            .url(apiUrl)
                            .header("Accept", "application/vnd.github.v3+json")
                            .build()
                        val response = client.newCall(request).execute()
                        if (response.isSuccessful) {
                            val jsonStr = response.body?.string() ?: ""
                            val jsonObj = JSONObject(jsonStr)
                            val assets = jsonObj.optJSONArray("assets")
                            if (assets != null) {
                                for (i in 0 until assets.length()) {
                                    val asset = assets.getJSONObject(i)
                                    val assetName = asset.optString("name", "")
                                    if (assetName.endsWith(".apk")) {
                                        return@withContext asset.getString("browser_download_url")
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return@withContext url
        }
    }

    /**
     * Downloads the APK file from downloadUrl to temporary cache, with progress notification,
     * and automatic launch of Android Package Installer.
     */
    suspend fun downloadAndInstallApk(
        context: Context,
        downloadUrl: String,
        onProgress: (Float) -> Unit,
        onSuccess: () -> Unit,
        onError: (Throwable) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                val directUrl = resolveDirectApkUrl(downloadUrl)
                val request = Request.Builder().url(directUrl).build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    throw java.io.IOException("Unexpected HTTP code: ${response.code}")
                }

                val body = response.body ?: throw java.io.IOException("Empty response body")
                val totalBytes = body.contentLength()

                val apkFile = File(context.cacheDir, "Masroofi_Update.apk")
                if (apkFile.exists()) {
                    apkFile.delete()
                }

                body.byteStream().use { inputStream ->
                    FileOutputStream(apkFile).use { outputStream ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Long = 0
                        var read = inputStream.read(buffer)
                        while (read != -1) {
                            outputStream.write(buffer, 0, read)
                            bytesRead += read
                            if (totalBytes > 0) {
                                val progress = bytesRead.toFloat() / totalBytes
                                onProgress(progress)
                            }
                            read = inputStream.read(buffer)
                        }
                    }
                }

                // Ensure UI is updated to 100%
                onProgress(1.0f)
                onSuccess()

                // Install the APK
                installApk(context, apkFile)
            } catch (e: Throwable) {
                onError(e)
            }
        }
    }

    private fun installApk(context: Context, apkFile: File) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val apkUri: Uri = FileProvider.getUriForFile(context, authority, apkFile)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback
            try {
                val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
                    setDataAndType(Uri.fromFile(apkFile), "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}

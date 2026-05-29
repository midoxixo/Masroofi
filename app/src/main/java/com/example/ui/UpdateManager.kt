package com.example.ui

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

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
}

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
    private const val UPDATE_JSON_URL = "https://gist.githubusercontent.com/dummy/update.json"
    private val client = OkHttpClient()

    /**
     * Fetches the remote update.json and parses the data.
     * Throws an exception if no internet or invalid response.
     */
    suspend fun checkUpdate(): UpdateInfo? {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(UPDATE_JSON_URL).build()
                try {
                    val response = client.newCall(request).execute()
                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        return@withContext parseJson(responseBody)
                    } else {
                        // Return mock info for demonstration if the dummy URL is 404
                        return@withContext getMockUpdateInfo()
                    }
                } catch (e: java.io.IOException) {
                    // No internet or timeout
                    throw e
                }
            } catch (e: Exception) {
                // Throw to let the UI know we couldn't connect
                throw e
            }
        }
    }

    private fun parseJson(body: String?): UpdateInfo? {
        if (body.isNullOrEmpty()) return null
        return try {
            val json = JSONObject(body)
            UpdateInfo(
                versionCode = json.optInt("versionCode", 4), // Fallback to 4 for testing
                versionName = json.optString("versionName", "1.3"),
                isForceUpdate = json.optBoolean("isForceUpdate", false),
                releaseNotesEn = json.optString("releaseNotesEn", "Added new features and bug fixes."),
                releaseNotesAr = json.optString("releaseNotesAr", "تم إضافة ميزات جديدة وإصلاح بعض الأخطاء."),
                downloadUrl = json.optString("downloadUrl", "https://play.google.com/store/apps/details?id=com.aistudio.masroofy.zkwldn")
            )
        } catch (e: Exception) {
            null
        }
    }

    // A mock method to supply update info for testing since we don't have a real update.json
    fun getMockUpdateInfo(): UpdateInfo {
        return UpdateInfo(
            versionCode = 4,
            versionName = "1.3",
            isForceUpdate = false,
            releaseNotesEn = "Added new features and bug fixes.\n- Performance improvements\n- UI enhancements",
            releaseNotesAr = "تمت إضافة ميزات جديدة وإصلاح الأخطاء.\n- تحسينات الأداء\n- تحديثات الواجهة",
            downloadUrl = "https://play.google.com/store/apps/details?id=com.aistudio.masroofy.zkwldn"
        )
    }
}

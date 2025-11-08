package com.smartsystems.chatapp.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * Manages app updates from GitHub releases.
 * Checks for new versions and downloads/installs APKs automatically.
 */
class UpdateManager(private val context: Context) {

    companion object {
        private const val GITHUB_API_URL = "https://api.github.com/repos/alonsobasauri/smartsystems-chat-android/releases/latest"
        private const val UPDATE_CHECK_INTERVAL = 6 * 60 * 60 * 1000L // 6 hours
        private const val PREF_NAME = "app_updates"
        private const val PREF_LAST_CHECK = "last_check"
        private const val PREF_DOWNLOAD_ID = "download_id"
    }

    private val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private var downloadId: Long = -1

    /**
     * Check if a new version is available on GitHub releases.
     * Returns update info if available, null otherwise.
     */
    suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val currentVersion = getCurrentVersion()
            val latestRelease = fetchLatestRelease() ?: return@withContext null

            val latestVersion = latestRelease.versionName
            val latestVersionCode = latestRelease.versionCode

            // Compare version codes
            if (latestVersionCode > currentVersion.versionCode) {
                // Update last check time
                prefs.edit().putLong(PREF_LAST_CHECK, System.currentTimeMillis()).apply()
                return@withContext latestRelease
            }

            null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Download and install the update.
     */
    fun downloadAndInstall(updateInfo: UpdateInfo) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

        val request = DownloadManager.Request(Uri.parse(updateInfo.downloadUrl))
            .setTitle("SmartSystems Chat Update")
            .setDescription("Descargando versión ${updateInfo.versionName}")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "smartsystems-chat-${updateInfo.versionName}.apk"
            )
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        downloadId = downloadManager.enqueue(request)
        prefs.edit().putLong(PREF_DOWNLOAD_ID, downloadId).apply()

        // Register receiver for download completion
        registerDownloadReceiver()
    }

    private fun registerDownloadReceiver() {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id == downloadId) {
                    // Download completed, install APK
                    installApk()
                    context?.unregisterReceiver(this)
                }
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                receiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            context.registerReceiver(
                receiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            )
        }
    }

    private fun installApk() {
        try {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val uri = downloadManager.getUriForDownloadedFile(downloadId)

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun getCurrentVersion(): VersionInfo {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        return VersionInfo(
            versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            },
            versionName = packageInfo.versionName ?: "1.0.0"
        )
    }

    private fun fetchLatestRelease(): UpdateInfo? {
        return try {
            val url = URL(GITHUB_API_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.connectTimeout = 10000
            connection.readTimeout = 10000

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = connection.inputStream.bufferedReader().use { it.readText() }
                parseReleaseJson(response)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun parseReleaseJson(json: String): UpdateInfo? {
        return try {
            val jsonObject = JSONObject(json)
            val tagName = jsonObject.getString("tag_name") // e.g., "v1.0.0"
            val versionName = tagName.removePrefix("v")
            val body = jsonObject.optString("body", "")

            // Extract version code from release notes or parse from tag
            val versionCode = extractVersionCode(versionName)

            // Find APK asset
            val assets = jsonObject.getJSONArray("assets")
            var apkUrl: String? = null

            for (i in 0 until assets.length()) {
                val asset = assets.getJSONObject(i)
                val name = asset.getString("name")
                if (name.endsWith(".apk")) {
                    apkUrl = asset.getString("browser_download_url")
                    break
                }
            }

            if (apkUrl != null) {
                UpdateInfo(
                    versionName = versionName,
                    versionCode = versionCode,
                    downloadUrl = apkUrl,
                    releaseNotes = body
                )
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun extractVersionCode(versionName: String): Int {
        // Convert semantic version to integer code
        // e.g., "1.2.3" -> 10203
        return try {
            val parts = versionName.split(".")
            val major = parts.getOrNull(0)?.toIntOrNull() ?: 1
            val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
            major * 10000 + minor * 100 + patch
        } catch (e: Exception) {
            1
        }
    }

    fun shouldCheckForUpdate(): Boolean {
        val lastCheck = prefs.getLong(PREF_LAST_CHECK, 0)
        return System.currentTimeMillis() - lastCheck > UPDATE_CHECK_INTERVAL
    }
}

data class VersionInfo(
    val versionCode: Int,
    val versionName: String
)

data class UpdateInfo(
    val versionName: String,
    val versionCode: Int,
    val downloadUrl: String,
    val releaseNotes: String
)

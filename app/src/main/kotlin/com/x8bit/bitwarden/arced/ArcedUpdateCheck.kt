package com.x8bit.bitwarden.arced

import android.content.Intent
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withStarted
import com.x8bit.bitwarden.BuildConfig
import com.x8bit.bitwarden.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val TIMEOUT_MS = 10_000

/**
 * ARCED: the app is installed from vault.arced.foundation/get, not a store, so nothing updates it.
 * Once per app start, fetch the published latest.json and offer the newer APK if there is one.
 * Any network or parsing error is ignored; the check simply runs again on the next start.
 */
object ArcedUpdateCheck {
    // Once per app process, so rotating the screen or restoring the activity doesn't ask again.
    private var hasRun = false

    fun run(activity: AppCompatActivity) {
        if (hasRun || BuildConfig.ARCED_UPDATE_URL.isEmpty()) return
        hasRun = true
        activity.lifecycleScope.launch {
            val latest = withContext(Dispatchers.IO) { runCatching { fetch() }.getOrNull() }
                ?: return@launch
            if (latest.versionCode <= BuildConfig.VERSION_CODE) return@launch
            activity.lifecycle.withStarted {
                AlertDialog.Builder(activity, R.style.ArcedUpdateDialog)
                    .setTitle(R.string.arced_update_available)
                    .setMessage(activity.getString(R.string.arced_update_message, latest.version))
                    .setPositiveButton(R.string.arced_update_download) { _, _ ->
                        activity.startActivity(Intent(Intent.ACTION_VIEW, latest.apkUrl.toUri()))
                    }
                    .setNegativeButton(R.string.arced_update_later, null)
                    .show()
            }
        }
    }

    private fun fetch(): Latest {
        val url = URL(BuildConfig.ARCED_UPDATE_URL)
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        val json = try {
            JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        } finally {
            connection.disconnect()
        }
        return Latest(
            version = json.getString("version"),
            versionCode = json.getInt("versionCode"),
            apkUrl = URL(url, json.getString("file")).toString(),
        )
    }

    private data class Latest(val version: String, val versionCode: Int, val apkUrl: String)
}

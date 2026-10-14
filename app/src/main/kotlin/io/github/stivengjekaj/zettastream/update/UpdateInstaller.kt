package io.github.stivengjekaj.zettastream.update

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.provider.Settings
import androidx.core.net.toUri
import io.github.stivengjekaj.zettastream.diag.Diagnostics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

/** The steps of an update inside the app. */
sealed interface UpdateState {
    data object Idle : UpdateState
    data class Downloading(val percent: Int) : UpdateState
    data object Installing : UpdateState
    data object NeedsPermission : UpdateState
    data class Failed(val reason: String) : UpdateState
}

/**
 * Downloads the newest APK and gives it to the package installer of Android,
 * which asks the viewer to confirm. Android installs it only when the
 * signature is the same as the signature of the installed app.
 */
class UpdateInstaller(private val context: Context, private val http: OkHttpClient) {
    private val state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updates: StateFlow<UpdateState> = state.asStateFlow()

    suspend fun install() {
        if (state.value is UpdateState.Downloading || state.value is UpdateState.Installing) return
        // Android asks one time if this app can install apps.
        if (!context.packageManager.canRequestPackageInstalls()) {
            state.value = UpdateState.NeedsPermission
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, "package:${context.packageName}".toUri())
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            return
        }
        runCatching {
            val apk = download()
            state.value = UpdateState.Installing
            withContext(Dispatchers.IO) { commit(apk) }
        }.onFailure {
            Diagnostics.log("Update", "failed", it)
            state.value = UpdateState.Failed(it.message ?: it.javaClass.simpleName)
        }
    }

    private suspend fun download(): File = withContext(Dispatchers.IO) {
        val target = File(context.cacheDir, "update.apk")
        state.value = UpdateState.Downloading(0)
        http.newCall(Request.Builder().url(UpdateChecker.DOWNLOAD_URL).build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val total = response.body.contentLength()
            response.body.byteStream().use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var done = 0L
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        done += read
                        if (total > 0) state.value = UpdateState.Downloading((done * 100 / total).toInt())
                    }
                }
            }
        }
        Diagnostics.log("Update", "downloaded ${target.length() / 1024} KB")
        target
    }

    private fun commit(apk: File) {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        params.setAppPackageName(context.packageName)
        val id = installer.createSession(params)
        installer.openSession(id).use { session ->
            session.openWrite("ZettaStream.apk", 0, apk.length()).use { out ->
                apk.inputStream().use { it.copyTo(out) }
                session.fsync(out)
            }
            val intent = Intent(context, UpdateReceiver::class.java)
            // The installer adds the result to the intent, so it must be mutable.
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
            session.commit(PendingIntent.getBroadcast(context, id, intent, flags).intentSender)
        }
    }

    internal fun finished(reason: String?) {
        state.value = if (reason == null) UpdateState.Idle else UpdateState.Failed(reason)
    }
}

/** Gets the result of the package installer. Android first asks the viewer to confirm. */
class UpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val installer = (context.applicationContext as? io.github.stivengjekaj.zettastream.ZettaStreamApp)?.container?.installer
        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                @Suppress("DEPRECATION")
                val confirm = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT) ?: return
                context.startActivity(confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
            PackageInstaller.STATUS_SUCCESS -> installer?.finished(null)
            else -> {
                val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "status $status"
                Diagnostics.log("Update", "installer: $message")
                installer?.finished(message)
            }
        }
    }
}

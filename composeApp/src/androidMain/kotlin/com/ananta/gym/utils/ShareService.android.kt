package `in`.gym.trak.studio.utils

import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.FileProvider
import `in`.gym.trak.studio.appContext
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder

private const val WHATSAPP_PACKAGE = "com.whatsapp"
private const val FILE_PROVIDER_AUTHORITY = "in.gym.trak.studio.fileprovider"

actual fun platformShare(payload: SharePayload) {
    val context = appContext ?: return
    val text = payload.text.trim()
    if (text.isBlank()) return

    when (payload.channel) {
        ShareChannel.WhatsApp -> {
            val whatsAppIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                payload.subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
                setPackage(WHATSAPP_PACKAGE)
            }
            if (whatsAppIntent.resolveActivity(context.packageManager) != null) {
                whatsAppIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(whatsAppIntent)
                return
            }
            val waLink = "https://wa.me/?text=${URLEncoder.encode(text, Charsets.UTF_8.name())}"
            val linkIntent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(waLink)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (linkIntent.resolveActivity(context.packageManager) != null) {
                context.startActivity(linkIntent)
                return
            }
        }
        ShareChannel.System -> Unit
    }

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        payload.subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val chooser = Intent.createChooser(shareIntent, "Share via").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
}

actual fun platformShareImage(payload: ShareImagePayload) {
    val context = appContext ?: return
    if (payload.pngBytes.isEmpty()) return

    val cacheDir = File(context.cacheDir, "shared_images").apply { mkdirs() }
    val imageFile = File(cacheDir, payload.fileName)
    FileOutputStream(imageFile).use { it.write(payload.pngBytes) }

    val uri = FileProvider.getUriForFile(context, FILE_PROVIDER_AUTHORITY, imageFile)
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newRawUri("workout_share", uri)
        payload.subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (payload.channel == ShareChannel.WhatsApp) {
            setPackage(WHATSAPP_PACKAGE)
        }
    }

    if (payload.channel == ShareChannel.WhatsApp &&
        shareIntent.resolveActivity(context.packageManager) != null
    ) {
        context.startActivity(shareIntent)
        return
    }

    if (payload.channel == ShareChannel.WhatsApp) {
        shareIntent.setPackage(null)
    }

    val chooser = Intent.createChooser(shareIntent, "Share via").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newRawUri("workout_share", uri)
    }
    val resolvedActivities = context.packageManager.queryIntentActivities(
        shareIntent,
        PackageManager.MATCH_DEFAULT_ONLY,
    )
    for (resolveInfo in resolvedActivities) {
        val packageName = resolveInfo.activityInfo.packageName
        context.grantUriPermission(
            packageName,
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION,
        )
    }
    context.startActivity(chooser)
}

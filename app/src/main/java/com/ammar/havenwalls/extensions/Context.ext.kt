package com.ammar.havenwalls.extensions

import android.app.Activity
import android.app.WallpaperManager
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapRegionDecoder
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.Display
import android.view.Surface
import android.view.WindowManager
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toAndroidRectF
import androidx.compose.ui.unit.IntSize
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.toRect
import androidx.core.hardware.display.DisplayManagerCompat
import androidx.core.net.toUri
import androidx.work.WorkManager
import com.ammar.havenwalls.FILE_PROVIDER_AUTHORITY
import com.ammar.havenwalls.model.WallpaperTarget
import com.ammar.havenwalls.model.toWhichInt
import com.ammar.havenwalls.ui.common.permissions.checkSetWallpaperPermission
import com.ammar.havenwalls.utils.getDecodeSampledBitmapOptions
import com.ammar.havenwalls.utils.isExternalStorageWritable
import com.ammar.havenwalls.utils.withMLModelsDir
import com.ammar.havenwalls.utils.withTempDir
import java.io.File
import java.io.InputStream

fun Context.openUrl(url: String) {
    var tempUrl = url
    if (!tempUrl.startsWith("https://") && !tempUrl.startsWith("http://")) {
        tempUrl = "http://$tempUrl"
    }
    val intent = Intent(Intent.ACTION_VIEW, tempUrl.toUri())
    if (intent.resolveActivity(packageManager) == null) return
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        toast("No Browser found!")
    } catch (e: Exception) {
        Log.e(TAG, "openUrl", e)
    }
}

fun parseMimeType(url: String) = parseMimeType(File(url))

fun parseMimeType(file: File): String {
    val ext = MimeTypeMap.getFileExtensionFromUrl(file.name)
    var type = MimeTypeMap
        .getSingleton()
        .getMimeTypeFromExtension(ext)
    type = type ?: "*/*"
    return type
}

fun Context.toast(message: String) = Toast.makeText(this, message, Toast.LENGTH_LONG).show()

fun Context.setWallpaper(
    uri: Uri,
    cropRect: Rect,
    targets: Set<WallpaperTarget> = setOf(WallpaperTarget.HOME, WallpaperTarget.LOCKSCREEN),
) = this.contentResolver.openInputStream(uri).use {
    val screenResolution = this.getScreenResolution(true)
    if (it == null) return@use false
    val decoder = getBitmapRegionDecoder(it) ?: return@use false
    val (opts, _) = getDecodeSampledBitmapOptions(
        context = this,
        uri = uri,
        reqWidth = screenResolution.width,
        reqHeight = screenResolution.height,
    ) ?: return@use false
    val bitmap = decoder.decodeRegion(
        cropRect.toAndroidRectF().toRect(),
        opts
    ) ?: return@use false
    return this.setWallpaper(bitmap, targets)
}

private fun getBitmapRegionDecoder(`is`: InputStream) =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        BitmapRegionDecoder.newInstance(`is`)
    } else {
        @Suppress("DEPRECATION")
        BitmapRegionDecoder.newInstance(`is`, false)
    }

fun Context.setWallpaper(
    bitmap: Bitmap,
    targets: Set<WallpaperTarget> = setOf(WallpaperTarget.HOME, WallpaperTarget.LOCKSCREEN),
): Boolean {
    if (!this.checkSetWallpaperPermission()) return false
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        wallpaperManager.setBitmap(
            bitmap,
            null,
            true,
            targets.toWhichInt(),
        )
        return true
    }
    wallpaperManager.setBitmap(bitmap)
    return true
}

fun Context.share(
    text: String,
    type: String = "text/plain",
) {
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        this.type = type
    }
    val intent = Intent.createChooser(sendIntent, null)
    startActivity(intent)
}

fun Context.share(
    uri: Uri,
    type: String,
    title: String? = null,
    grantTempPermission: Boolean = false,
) = startActivity(getShareChooserIntent(uri, type, title, grantTempPermission))

fun Context.getShareChooserIntent(
    file: File,
    grantTempPermission: Boolean,
): Intent = getShareChooserIntent(
    uri = getUriForFile(file),
    type = parseMimeType(file),
    title = file.name,
    grantTempPermission = grantTempPermission,
)

fun getShareChooserIntent(
    uri: Uri,
    type: String,
    title: String?,
    grantTempPermission: Boolean,
): Intent = Intent.createChooser(
    Intent().apply {
        action = Intent.ACTION_SEND
        if (grantTempPermission) {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        clipData = ClipData.newRawUri(title, uri)
        putExtra(Intent.EXTRA_STREAM, uri)
        this.type = type
    },
    title,
)

val Context.externalFilesDir
    get() = getExternalFilesDir(null)

fun Context.getTempFile(fileName: String) = getAppDir(withTempDir(fileName))
fun Context.getTempDir() = getAppDir(withTempDir())

fun Context.getMLModelsFile(fileName: String) = getAppDir(withMLModelsDir(fileName))
fun Context.getMLModelsDir() = getAppDir(withMLModelsDir())

private fun Context.getAppDir(subPath: String): File {
    val parentFile = if (isExternalStorageWritable()) externalFilesDir else filesDir
    return File(parentFile, subPath)
}

fun Context.getTempFileIfExists(fileName: String): File? {
    val file = getTempFile(fileName)
    return if (file.exists()) file else null
}

fun Context.getMLModelsFileIfExists(fileName: String): File? {
    val file = getMLModelsFile(fileName)
    return if (file.exists()) file else null
}

val Context.displayManager
    get() = DisplayManagerCompat.getInstance(this)

val Context.windowManager
    get() = getSystemService(Context.WINDOW_SERVICE) as WindowManager

fun Context.getScreenResolution(
    inDefaultOrientation: Boolean = false,
): IntSize {
    var changeOrientation = false
    if (inDefaultOrientation) {
        val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY) ?: return IntSize.Zero
        val rotation = display.rotation
        // if current rotation is 90 or 270, device is rotated, so we will swap width and height
        changeOrientation = rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270
    }
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val bounds = windowManager.currentWindowMetrics.bounds
        IntSize(
            width = if (changeOrientation) bounds.height() else bounds.width(),
            height = if (changeOrientation) bounds.width() else bounds.height(),
        )
    } else {
        val displayMetrics = resources.displayMetrics
        IntSize(
            width = if (changeOrientation) displayMetrics.heightPixels else displayMetrics.widthPixels,
            height = if (changeOrientation) displayMetrics.widthPixels else displayMetrics.heightPixels,
        )
    }
}

fun Context.getUriForFile(file: File): Uri = FileProvider.getUriForFile(
    this,
    FILE_PROVIDER_AUTHORITY,
    file,
)

val Context.wallpaperManager: WallpaperManager
    get() = WallpaperManager.getInstance(this)

val WallpaperManager.isWallpaperSupportedCompat
    get() = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) this.isWallpaperSupported else true)

val WallpaperManager.isSetWallpaperAllowedCompat
    get() = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) this.isSetWallpaperAllowed else true)

fun Context.findActivity(): Activity {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    throw IllegalStateException("Permissions should be called in the context of an Activity")
}

val Context.workManager
    get() = WorkManager.getInstance(this)

val Context.notificationManager
    get() = NotificationManagerCompat.from(this)

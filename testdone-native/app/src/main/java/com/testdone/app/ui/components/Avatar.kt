package com.testdone.app.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/**
 * v2.3.16 — profile photos.
 *
 * Users can set a photo from the gallery; it is compressed to a small square
 * JPEG, stored as a data-URI in the local profile AND in Firestore
 * (users/{uid}.avatarUrl), so it survives logout/login and follows the
 * account to new devices. No extra dependency: plain BitmapFactory + Base64.
 *
 * Supported avatarUrl forms:
 *  • "data:image/jpeg;base64,...."  (what we write)
 *  • raw base64 (defensive)
 *  • https://… (downloaded via the shared OkHttp client)
 */
object Avatars {

    /** Decode any supported avatar spec into a Bitmap (IO thread, ~512px). */
    suspend fun load(spec: String?): Bitmap? = withContext(Dispatchers.IO) {
        if (spec.isNullOrBlank()) return@withContext null
        try {
            val bytes = when {
                spec.startsWith("data:", ignoreCase = true) -> {
                    val b64 = spec.substringAfter(',', "").trim()
                    if (b64.isEmpty()) null else Base64.decode(b64, Base64.DEFAULT)
                }
                spec.startsWith("http", ignoreCase = true) -> download(spec)
                else -> runCatching { Base64.decode(spec.trim(), Base64.DEFAULT) }.getOrNull()
            } ?: return@withContext null
            decodeDownsampled(bytes, maxDim = 512)
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun download(url: String): ByteArray? = withContext(Dispatchers.IO) {
        try {
            val client = okhttp3.OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(20, java.util.concurrent.TimeUnit.SECONDS)
                .build()
            client.newCall(okhttp3.Request.Builder().url(url).build()).execute().use { resp ->
                if (resp.isSuccessful) resp.body?.bytes() else null
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun decodeDownsampled(bytes: ByteArray, maxDim: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= maxDim && bounds.outHeight / (sample * 2) >= maxDim) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
    }

    /**
     * Center-crop the picked gallery image to a square, scale to
     * [side] px and re-encode as a compact data-URI (~40–90 KB).
     */
    suspend fun toCompactDataUri(context: android.content.Context, uri: Uri, side: Int = 512): String? =
        withContext(Dispatchers.IO) {
            try {
                val src = context.contentResolver.openInputStream(uri)?.use { ins ->
                    decodeDownsampled(ins.readBytes(), maxDim = side * 2)
                } ?: return@withContext null
                val square = cropCenterSquare(src, side)
                val out = ByteArrayOutputStream()
                square.compress(Bitmap.CompressFormat.JPEG, 82, out)
                val b64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                "data:image/jpeg;base64,$b64"
            } catch (e: Exception) {
                null
            }
        }

    private fun cropCenterSquare(src: Bitmap, side: Int): Bitmap {
        val min = minOf(src.width, src.height)
        val x = (src.width - min) / 2
        val y = (src.height - min) / 2
        val cropped = Bitmap.createBitmap(src, x, y, min, min)
        return if (min == side) cropped else Bitmap.createScaledBitmap(cropped, side, side, true)
    }

    /** Matrix-rotated variant kept for future camera capture support. */
    fun rotate(bitmap: Bitmap, degrees: Float): Bitmap {
        if (degrees == 0f) return bitmap
        val m = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, m, true)
    }
}

/** Reactive avatar bitmap — recomputes when the spec changes; null = use letter. */
@Composable
fun rememberAvatarBitmap(spec: String?): State<Bitmap?> = produceState<Bitmap?>(initialValue = null, key1 = spec) {
    value = Avatars.load(spec)
}

/**
 * Gallery picker launcher (system document picker — no storage permission
 * needed on any API level; works from API 26 up).
 */
@Composable
fun rememberGalleryAvatarPicker(onPicked: (android.net.Uri) -> Unit) =
    rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) onPicked(uri)
    }

/**
 * The avatar everywhere: user photo when available, otherwise the branded
 * gradient squircle with the initial. [shape] lets callers go squircle
 * (drawer letter fallback) or circle. v2.3.17: photos are ALWAYS clipped to
 * a perfect circle ("circular fit ekdum") — the square source bitmap used to
 * draw straight over the rounded background and read as a square photo.
 */
@Composable
fun TdAvatar(
    name: String,
    avatarUrl: String?,
    size: Dp,
    shape: RoundedCornerShape = RoundedCornerShape(18.dp),
    brush: Brush? = null,
    fontSizeScale: Float = 0.42f,
) {
    val bitmap by rememberAvatarBitmap(avatarUrl)
    val resolvedBrush = brush ?: Brush.linearGradient(listOf(Color(0xFF6366F1), Color(0xFF8B5CF6)))
    // photo → always a circle; letter fallback keeps the caller's shape
    val photoShape: RoundedCornerShape = CircleShape
    val bgShape = if (bitmap != null) photoShape else shape
    Box(
        Modifier
            .size(size)
            .background(resolvedBrush, bgShape),
        contentAlignment = Alignment.Center,
    ) {
        val bmp = bitmap
        if (bmp != null) {
            Image(
                bitmap = bmp.asImageBitmap(),
                contentDescription = "Profile photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clip(photoShape),
            )
        } else {
            val textSize = size * fontSizeScale
            Text(
                name.trim().firstOrNull()?.uppercase() ?: "T",
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = androidx.compose.ui.unit.TextUnit(textSize.value, androidx.compose.ui.unit.TextUnitType.Sp),
            )
        }
    }
}

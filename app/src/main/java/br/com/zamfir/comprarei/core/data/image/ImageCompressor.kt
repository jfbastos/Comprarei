package br.com.zamfir.comprarei.core.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.core.graphics.scale
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/** Lê uma imagem escolhida pelo usuário e reduz para upload (lado maior ≤ [MAX_SIZE], JPEG [QUALITY]%). */
class ImageCompressor(private val context: Context, private val dispatcher: CoroutineDispatcher) {

    suspend fun compress(uri: Uri): ByteArray = withContext(dispatcher) {
        val bitmap = decode(uri)
        ByteArrayOutputStream().use { output ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, output)
            bitmap.recycle()
            output.toByteArray()
        }
    }

    private fun decode(uri: Uri): Bitmap =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // ImageDecoder já aplica a rotação do EXIF (fotos da câmera).
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                val (width, height) = info.size.width to info.size.height
                val scale = MAX_SIZE.toFloat() / max(width, height)
                if (scale < 1f) decoder.setTargetSize((width * scale).roundToInt(), (height * scale).roundToInt())
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            decodeLegacy(uri)
        }

    private fun decodeLegacy(uri: Uri): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }

        var sampleSize = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sampleSize * 2) >= MAX_SIZE) sampleSize *= 2

        val decoded = context.contentResolver.openInputStream(uri).use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sampleSize })
        } ?: error("Não foi possível ler a imagem $uri")

        val scale = MAX_SIZE.toFloat() / max(decoded.width, decoded.height)
        if (scale >= 1f) return decoded
        return decoded.scale((decoded.width * scale).roundToInt(), (decoded.height * scale).roundToInt())
            .also { if (it !== decoded) decoded.recycle() }
    }

    private companion object {
        const val MAX_SIZE = 1024
        const val QUALITY = 85
    }
}

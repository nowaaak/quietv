package io.github.nowaaak.quietv.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.security.MessageDigest
import kotlin.math.ceil

class WallpaperStore(context: Context) {

    private val dir = context.applicationContext.filesDir
    val file = File(dir, FILE)
    private val part = File(dir, PART)

    fun exists(): Boolean = file.isFile && file.length() > 0

    fun version(): Long = if (exists()) file.lastModified() else 0L

    fun writeChunk(offset: Long, data: ByteArray, last: Boolean, sha256: String?): String? {
        if (offset == 0L) part.delete()
        if (part.length() != offset) return "chunk at $offset does not follow ${part.length()} bytes"
        if (offset + data.size > MAX_BYTES) {
            part.delete()
            return "image is larger than ${MAX_BYTES / 1024 / 1024} MB"
        }
        part.appendBytes(data)
        if (!last) return null

        val digest = MessageDigest.getInstance("SHA-256").digest(part.readBytes()).joinToString("") { "%02x".format(it) }
        if (sha256 == null || !digest.equals(sha256, ignoreCase = true)) {
            part.delete()
            return "checksum mismatch"
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(part.path, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            part.delete()
            return "not an image Android can read"
        }
        file.delete()
        if (!part.renameTo(file)) return "could not store the image"
        return null
    }

    fun clear() {
        file.delete()
        part.delete()
    }

    companion object {
        const val MAX_BYTES = 8L * 1024 * 1024
        private const val FILE = "wallpaper"
        private const val PART = "wallpaper.part"
        private const val SAMPLE_COLUMNS = 16
        private const val SAMPLE_ROWS = 9

        fun decode(file: File, width: Int, height: Int): Bitmap? {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= width && bounds.outHeight / (sample * 2) >= height) sample *= 2
            val sampledWidth = bounds.outWidth / sample
            val factor = maxOf(width.toFloat() / sampledWidth, height.toFloat() / (bounds.outHeight / sample))
            val options = BitmapFactory.Options().apply {
                inSampleSize = sample
                if (factor < 1f) {
                    inScaled = true
                    inDensity = sampledWidth
                    inTargetDensity = ceil(sampledWidth * factor).toInt()
                }
            }
            return BitmapFactory.decodeFile(file.path, options)
        }

        fun samples(bitmap: Bitmap): IntArray {
            val small = Bitmap.createScaledBitmap(bitmap, SAMPLE_COLUMNS, SAMPLE_ROWS, true)
            val pixels = IntArray(SAMPLE_COLUMNS * SAMPLE_ROWS)
            small.getPixels(pixels, 0, SAMPLE_COLUMNS, 0, 0, SAMPLE_COLUMNS, SAMPLE_ROWS)
            if (small !== bitmap) small.recycle()
            return pixels
        }
    }
}

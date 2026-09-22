package com.usefocus.app.data.website

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.usefocus.app.domain.DomainNormalizer
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class WebsiteIconStore(context: Context) {
    private val directory = File(context.cacheDir, "website-icons")
    private val locks = ConcurrentHashMap<String, Mutex>()

    suspend fun load(domain: String): Bitmap? = withContext(Dispatchers.IO) {
        val normalized = runCatching { DomainNormalizer.normalize(domain) }.getOrNull() ?: return@withContext null
        directory.mkdirs()
        val key = normalized.sha256()
        val image = File(directory, "$key.png")
        val miss = File(directory, "$key.miss")
        decode(image)?.let { return@withContext it }
        if (miss.exists() && System.currentTimeMillis() - miss.lastModified() < MISS_TTL_MS)
            return@withContext null

        locks.getOrPut(key) { Mutex() }.withLock {
            decode(image)?.let { return@withLock it }
            val bytes =
                FaviconFetcher.fetch(normalized) {
                    imageDimensions(it)?.let { (width, height) ->
                        width in 1..MAX_SOURCE_EDGE && height in 1..MAX_SOURCE_EDGE
                    } == true
                }
            val bitmap = bytes?.let(::decodeBytes)
            if (bitmap == null) {
                miss.writeText("")
                null
            } else {
                image.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                miss.delete()
                bitmap
            }
        }.also { locks.remove(key) }
    }

    private fun decode(file: File): Bitmap? =
        if (file.isFile) BitmapFactory.decodeFile(file.absolutePath) else null

    private fun imageDimensions(bytes: ByteArray): Pair<Int, Int>? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        return if (options.outWidth > 0 && options.outHeight > 0)
            options.outWidth to options.outHeight
        else null
    }

    private fun decodeBytes(bytes: ByteArray): Bitmap? {
        val (width, height) = imageDimensions(bytes) ?: return null
        var sample = 1
        while (width / sample > CACHE_EDGE || height / sample > CACHE_EDGE) sample *= 2
        return BitmapFactory.decodeByteArray(
            bytes,
            0,
            bytes.size,
            BitmapFactory.Options().apply { inSampleSize = sample },
        )
    }

    private fun String.sha256(): String =
        MessageDigest.getInstance("SHA-256")
            .digest(toByteArray())
            .joinToString("") { "%02x".format(it) }

    private companion object {
        const val MISS_TTL_MS = 24 * 60 * 60 * 1000L
        const val MAX_SOURCE_EDGE = 4_096
        const val CACHE_EDGE = 512
    }
}

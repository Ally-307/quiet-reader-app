package com.quietreader.data

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.roundToInt

class PdfDocumentSession private constructor(
    private val descriptor: ParcelFileDescriptor,
    private val renderer: PdfRenderer,
    val pageCount: Int,
    val defaultPageAspectRatio: Float,
) : Closeable {
    private val rendererLock = ReentrantLock()
    private val cache = object : LruCache<String, Bitmap>(CACHE_SIZE_KIB) {
        override fun sizeOf(key: String, value: Bitmap): Int =
            (value.allocationByteCount / 1024).coerceAtLeast(1)
    }
    private var closed = false

    suspend fun renderPage(pageIndex: Int, requestedWidthPx: Int): Bitmap = withContext(Dispatchers.IO) {
        require(pageIndex in 0 until pageCount) { "PDF 页码超出范围" }
        val targetWidth = requestedWidthPx.coerceIn(MIN_RENDER_WIDTH_PX, MAX_RENDER_WIDTH_PX)
        val cacheKey = "$pageIndex:$targetWidth"

        rendererLock.withLock {
            check(!closed) { "PDF 已关闭" }
            cache.get(cacheKey)?.let { return@withLock it }

            val page = renderer.openPage(pageIndex)
            try {
                if (page.width <= 0 || page.height <= 0) throw IllegalArgumentException("PDF 页面尺寸不可用")
                var width = targetWidth
                var height = (page.height * (width / page.width.toFloat())).roundToInt().coerceAtLeast(1)
                if (height > MAX_RENDER_HEIGHT_PX) {
                    height = MAX_RENDER_HEIGHT_PX
                    width = (page.width * (height / page.height.toFloat())).roundToInt().coerceAtLeast(1)
                }

                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                try {
                    bitmap.eraseColor(Color.WHITE)
                    val transform = Matrix().apply {
                        postScale(width / page.width.toFloat(), height / page.height.toFloat())
                    }
                    page.render(bitmap, null, transform, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    cache.put(cacheKey, bitmap)
                    bitmap
                } catch (error: Exception) {
                    bitmap.recycle()
                    throw error
                }
            } finally {
                page.close()
            }
        }
    }

    override fun close() {
        rendererLock.withLock {
            if (closed) return
            closed = true
            cache.evictAll()
            renderer.close()
            descriptor.close()
        }
    }

    companion object {
        fun open(file: File): PdfDocumentSession {
            if (!file.isFile) throw IllegalArgumentException("PDF 文件不存在")
            val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            return try {
                val renderer = PdfRenderer(descriptor)
                try {
                    if (renderer.pageCount <= 0) throw IllegalArgumentException("PDF 没有可阅读页面")
                    val firstPage = renderer.openPage(0)
                    val aspectRatio = try {
                        if (firstPage.width > 0 && firstPage.height > 0) {
                            firstPage.width / firstPage.height.toFloat()
                        } else {
                            DEFAULT_PAGE_ASPECT_RATIO
                        }
                    } finally {
                        firstPage.close()
                    }
                    PdfDocumentSession(
                        descriptor = descriptor,
                        renderer = renderer,
                        pageCount = renderer.pageCount,
                        defaultPageAspectRatio = aspectRatio.coerceIn(MIN_PAGE_ASPECT_RATIO, MAX_PAGE_ASPECT_RATIO),
                    )
                } catch (error: Exception) {
                    renderer.close()
                    throw error
                }
            } catch (error: Exception) {
                descriptor.close()
                throw error
            }
        }

        private const val CACHE_SIZE_KIB = 48 * 1024
        private const val MIN_RENDER_WIDTH_PX = 320
        private const val MAX_RENDER_WIDTH_PX = 1800
        private const val MAX_RENDER_HEIGHT_PX = 2600
        private const val DEFAULT_PAGE_ASPECT_RATIO = 0.707f
        private const val MIN_PAGE_ASPECT_RATIO = 0.35f
        private const val MAX_PAGE_ASPECT_RATIO = 2.5f
    }
}

package com.configcat.intellij.plugin.webview.cef

import com.intellij.testFramework.LightPlatformTestCase
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStream

class CefStreamResourceHandlerTest : LightPlatformTestCase() {

    fun testProcessRequestInternal_continuesAndReturnsTrue() {
        val handler = CefStreamResourceHandler(ByteArrayInputStream("hello".toByteArray()), "text/plain")
        var continued = false

        val processed = handler.processRequestInternal { continued = true }

        assertTrue(processed)
        assertTrue(continued)
    }

    fun testApplyHeaders_setsMimeTypeStatusAndAllHeaders() {
        val handler = CefStreamResourceHandler(
            ByteArrayInputStream("content".toByteArray()),
            "text/html",
            mapOf("Cache-Control" to "no-cache", "X-Test" to "true"),
        )
        var mimeType = ""
        var status = 0
        val headerMap = mutableMapOf<String, String>()

        handler.applyHeaders(
            setMimeType = { mimeType = it },
            setStatus = { status = it },
            setHeader = { key, value -> headerMap[key] = value },
        )

        assertEquals("text/html", mimeType)
        assertEquals(200, status)
        assertEquals("no-cache", headerMap["Cache-Control"])
        assertEquals("true", headerMap["X-Test"])
    }

    fun testReadResponseInternal_readsBytesAndReturnsTrue() {
        val handler = CefStreamResourceHandler(ByteArrayInputStream("hello".toByteArray()), "text/plain")
        val buffer = ByteArray(5)
        var canceled = false

        val result = handler.readResponseInternal(buffer, buffer.size) { canceled = true }

        assertTrue(result.first)
        assertEquals(5, result.second)
        assertEquals("hello", String(buffer, 0, result.second))
        assertFalse(canceled)
    }

    fun testReadResponseInternal_onEndOfStream_returnsFalseAndDisposesStream() {
        val input = CloseTrackingInputStream(byteArrayOf())
        val handler = CefStreamResourceHandler(input, "text/plain")
        val buffer = ByteArray(4)

        val result = handler.readResponseInternal(buffer, buffer.size) { fail("cancel must not be called on EOF") }

        assertFalse(result.first)
        assertEquals(0, result.second)
        assertTrue(input.closed)
    }

    fun testReadResponseInternal_onIOException_cancelsAndReturnsFalse() {
        val input = ReadThrowingInputStream()
        val handler = CefStreamResourceHandler(input, "text/plain")
        var canceled = false

        val result = handler.readResponseInternal(ByteArray(8), 8) { canceled = true }

        assertFalse(result.first)
        assertEquals(0, result.second)
        assertTrue(canceled)
        assertTrue(input.closed)
    }

    fun testCancel_disposesAndClosesStream() {
        val input = CloseTrackingInputStream("data".toByteArray())
        val handler = CefStreamResourceHandler(input, "text/plain")

        handler.cancel()

        assertTrue(input.closed)
    }

    fun testDispose_swallowsCloseIOException() {
        val handler = CefStreamResourceHandler(CloseThrowingInputStream(), "text/plain")

        handler.dispose()

        assertTrue(true)
    }

    private class CloseTrackingInputStream(bytes: ByteArray) : ByteArrayInputStream(bytes) {
        var closed = false

        override fun close() {
            closed = true
            super.close()
        }
    }

    private class ReadThrowingInputStream : InputStream() {
        var closed = false

        override fun read(): Int {
            throw IOException("read failed")
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            throw IOException("read failed")
        }

        override fun close() {
            closed = true
        }
    }

    private class CloseThrowingInputStream : InputStream() {
        override fun read(): Int = -1

        override fun close() {
            throw IOException("close failed")
        }
    }
}



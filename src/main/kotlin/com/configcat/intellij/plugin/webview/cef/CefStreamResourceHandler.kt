package com.configcat.intellij.plugin.webview.cef

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Disposer
import org.cef.callback.CefCallback
import org.cef.handler.CefResourceHandler
import org.cef.misc.IntRef
import org.cef.misc.StringRef
import org.cef.network.CefRequest
import org.cef.network.CefResponse
import java.io.IOException
import java.io.InputStream

class CefStreamResourceHandler(
    private val myStream: InputStream,
    private val myMimeType: String,
    private val headers: Map<String, String> = mapOf(),
) : CefResourceHandler, Disposable {

    internal fun processRequestInternal(onContinue: () -> Unit): Boolean {
        onContinue()
        return true
    }

    internal fun applyHeaders(
        setMimeType: (String) -> Unit,
        setStatus: (Int) -> Unit,
        setHeader: (String, String) -> Unit,
    ) {
        setMimeType(myMimeType)
        setStatus(200)
        for (header in headers) {
            setHeader(header.key, header.value)
        }
    }

    internal fun readResponseInternal(
        dataOut: ByteArray,
        bytesToRead: Int,
        onCancel: () -> Unit,
    ): Pair<Boolean, Int> {
        return try {
            val readCount = myStream.read(dataOut, 0, bytesToRead)
            if (readCount != -1) {
                true to readCount
            } else {
                Disposer.dispose(this)
                false to 0
            }
        } catch (e: IOException) {
            onCancel()
            Disposer.dispose(this)
            false to 0
        }
    }

    override fun processRequest(
        request: CefRequest,
        callback: CefCallback,
    ): Boolean {
        return processRequestInternal { callback.Continue() }
    }

    override fun getResponseHeaders(
        response: CefResponse,
        responseLength: IntRef,
        redirectUrl: StringRef,
    ) {
        applyHeaders(
            setMimeType = { response.mimeType = it },
            setStatus = { response.status = it },
            setHeader = { key, value -> response.setHeaderByName(key, value, true) },
        )
    }

    override fun readResponse(
        dataOut: ByteArray,
        bytesToRead: Int,
        bytesRead: IntRef,
        callback: CefCallback,
    ): Boolean {
        val (hasMoreData, readCount) = readResponseInternal(dataOut, bytesToRead) { callback.cancel() }
        bytesRead.set(readCount)
        return hasMoreData
    }

    override fun cancel() {
        Disposer.dispose(this)
    }

    override fun dispose() {
        try {
            myStream.close()
        } catch (e: IOException) {
            Logger.getInstance(CefStreamResourceHandler::class.java).warn("Failed to close the stream", e)
        }
    }
}

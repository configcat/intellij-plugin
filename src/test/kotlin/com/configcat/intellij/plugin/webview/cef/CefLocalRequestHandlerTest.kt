package com.configcat.intellij.plugin.webview.cef

import com.intellij.ide.BrowserUtil
import com.intellij.testFramework.LightPlatformTestCase
import io.mockk.every
import io.mockk.just
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import org.cef.handler.CefResourceHandlerAdapter
import org.cef.network.CefRequest

class CefLocalRequestHandlerTest : LightPlatformTestCase() {

    override fun setUp() {
        super.setUp()
        mockkStatic(BrowserUtil::class)
        every { BrowserUtil.open(any<String>()) } just runs
    }

    override fun tearDown() {
        unmockkAll()
        super.tearDown()
    }

    fun testIsDistUrl_returnsTrueForDistPrefix_caseInsensitive() {
        val handler = CefLocalRequestHandler()

        assertTrue(handler.isDistUrl("http://dist/index.html"))
        assertTrue(handler.isDistUrl("HTTP://DIST/assets/main.js"))
    }

    fun testIsDistUrl_returnsFalseForNullOrNonDistUrl() {
        val handler = CefLocalRequestHandler()

        assertFalse(handler.isDistUrl(null))
        assertFalse(handler.isDistUrl("https://configcat.com/index.html"))
    }

    fun testRegisteredResource_isReturnedByFileName() {
        val handler = CefLocalRequestHandler()
        val expectedResourceHandler = object : CefResourceHandlerAdapter() {}

        handler.addResource("index.html") { expectedResourceHandler }
        val actualHandler = handler.resolveResourceHandler("http://dist/assets/index.html")

        assertSame(expectedResourceHandler, actualHandler)
    }

    fun testMissingOrInvalidResourceUrl_returnsRejectingHandler() {
        val handler = CefLocalRequestHandler()
        val missingHandler = handler.resolveResourceHandler("http://dist/missing.js")
        val malformedHandler = handler.resolveResourceHandler("::not-a-valid-url")

        assertSame("missing and malformed URLs should resolve to the same rejecting handler", missingHandler, malformedHandler)
        assertTrue("missing resource URL must map to the dedicated rejecting handler", handler.isRejectingHandler(missingHandler))
        assertTrue("malformed resource URL must map to the dedicated rejecting handler", handler.isRejectingHandler(malformedHandler))

        var missingCanceled = false
        var malformedCanceled = false

        val missingProcessed = handler.rejectRequest { missingCanceled = true }
        val malformedProcessed = handler.rejectRequest { malformedCanceled = true }

        assertFalse("rejectRequest must reject requests", missingProcessed)
        assertFalse("rejectRequest must reject requests", malformedProcessed)
        assertTrue("rejectRequest must invoke cancel callback", missingCanceled)
        assertTrue("rejectRequest must invoke cancel callback", malformedCanceled)
    }

    fun testShouldConsumeNavigation_returnsTrueForUserGestureAndLinkTransitions() {
        val handler = CefLocalRequestHandler()

        assertTrue(handler.shouldConsumeNavigation(true, CefRequest.TransitionType.TT_EXPLICIT))
        assertTrue(handler.shouldConsumeNavigation(false, CefRequest.TransitionType.TT_LINK))
    }

    fun testShouldConsumeNavigation_returnsFalseForNonUserNonLinkTransition() {
        val handler = CefLocalRequestHandler()

        assertFalse(handler.shouldConsumeNavigation(false, CefRequest.TransitionType.TT_EXPLICIT))
    }

    fun testOnBeforeBrowse_userGestureWithNullRequest_fallsBackToDefaultBehavior() {
        val handler = CefLocalRequestHandler()

        val consumed = handler.onBeforeBrowse(null, null, null, true, false)

        assertFalse(consumed)
        verify(exactly = 0) { BrowserUtil.open(any<String>()) }
    }

    fun testOnBeforeBrowse_nonUserGestureNonLinkFallsBackToDefaultBehavior() {
        val handler = CefLocalRequestHandler()

        val consumed = handler.onBeforeBrowse(null, null, null, false, false)

        assertFalse("non-user non-link requests should fall back to default behavior", consumed)
    }

    fun testOnBeforeBrowse_userGestureOpensExternalBrowserAndConsumesNavigation() {
        val handler = CefLocalRequestHandler()
        val request = io.mockk.mockk<CefRequest>()
        every { request.url } returns "https://configcat.com/docs"
        every { request.transitionType } returns CefRequest.TransitionType.TT_EXPLICIT

        val consumed = handler.onBeforeBrowse(null, null, request, true, false)

        assertTrue("user gesture navigation should be consumed", consumed)
        verify(exactly = 1) { BrowserUtil.open("https://configcat.com/docs") }
    }

    fun testOnBeforeBrowse_linkTransitionWithoutUserGestureConsumesWithoutOpeningBrowser() {
        val handler = CefLocalRequestHandler()
        val request = io.mockk.mockk<CefRequest>()
        every { request.url } returns "https://configcat.com/other"
        every { request.transitionType } returns CefRequest.TransitionType.TT_LINK

        val consumed = handler.onBeforeBrowse(null, null, request, false, false)

        assertTrue("link transition should be consumed", consumed)
        verify(exactly = 0) { BrowserUtil.open(any<String>()) }
    }

    fun testOnBeforeBrowse_userGestureWithRedirectStillOpensExternalBrowser() {
        val handler = CefLocalRequestHandler()
        val request = io.mockk.mockk<CefRequest>()
        every { request.url } returns "https://configcat.com/redirect"
        every { request.transitionType } returns CefRequest.TransitionType.TT_EXPLICIT

        val consumed = handler.onBeforeBrowse(null, null, request, true, true)

        assertTrue("user gesture with redirect should still be consumed", consumed)
        verify(exactly = 1) { BrowserUtil.open("https://configcat.com/redirect") }
    }

}







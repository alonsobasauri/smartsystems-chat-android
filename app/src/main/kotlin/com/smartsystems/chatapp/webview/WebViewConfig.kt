package com.smartsystems.chatapp.webview

import android.webkit.WebSettings
import android.webkit.WebView
import com.smartsystems.chatapp.BuildConfig

/**
 * WebView configuration for optimal PWA support.
 * Configures settings for JavaScript, storage, caching, and security.
 */
object WebViewConfig {

    fun configure(webView: WebView) {
        with(webView.settings) {
            // Enable JavaScript (required for PWA)
            javaScriptEnabled = true

            // Enable storage APIs for PWA
            domStorageEnabled = true
            databaseEnabled = true

            // Cache settings for offline support
            cacheMode = WebSettings.LOAD_DEFAULT
            // Note: setAppCacheEnabled is deprecated and removed in API 33+
            // PWA caching now handled by Service Workers

            // PWA support
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW

            // User agent
            userAgentString = "$userAgentString SmartSystems/1.0"

            // Security settings
            allowFileAccess = false
            allowContentAccess = true

            // Zoom controls
            builtInZoomControls = false
            displayZoomControls = false
            setSupportZoom(false)

            // View settings
            useWideViewPort = true
            loadWithOverviewMode = true
            layoutAlgorithm = WebSettings.LayoutAlgorithm.TEXT_AUTOSIZING

            // Media
            mediaPlaybackRequiresUserGesture = false

            // Geolocation
            setGeolocationEnabled(false)

            // Safe browsing
            safeBrowsingEnabled = true
        }

        // Enable debugging in debug builds
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        // Enable hardware acceleration
        webView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
    }
}

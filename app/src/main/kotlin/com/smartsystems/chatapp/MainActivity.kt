package com.smartsystems.chatapp

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.smartsystems.chatapp.network.NetworkMonitor
import com.smartsystems.chatapp.webview.WebViewConfig
import com.smartsystems.chatapp.update.UpdateManager
import kotlinx.coroutines.launch
import android.app.AlertDialog

/**
 * Main activity that displays the chatbot PWA in a full-screen WebView.
 * Automatically detects network connectivity and shows offline page when needed.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var offlineContainer: FrameLayout
    private lateinit var networkMonitor: NetworkMonitor
    private lateinit var updateManager: UpdateManager
    private var isOffline = false

    companion object {
        private const val PWA_URL = "https://chat.smartsystems.work/"
        private const val OFFLINE_PAGE = "file:///android_asset/offline.html"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize views
        webView = findViewById(R.id.webView)
        progressBar = findViewById(R.id.progressBar)
        offlineContainer = findViewById(R.id.offlineContainer)

        // Initialize network monitor
        networkMonitor = NetworkMonitor(this)

        // Initialize update manager
        updateManager = UpdateManager(this)

        // Configure WebView
        WebViewConfig.configure(webView)

        // Set up WebView client with offline handling
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(
                view: WebView?,
                url: String?,
                favicon: android.graphics.Bitmap?
            ) {
                if (url != OFFLINE_PAGE) {
                    progressBar.visibility = View.VISIBLE
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                progressBar.visibility = View.GONE
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                // Show offline page if main frame fails and no network
                if (request?.isForMainFrame == true && !networkMonitor.isConnected) {
                    showOfflinePage()
                }
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                // Allow only smartsystems.work domain
                val url = request?.url?.toString() ?: return false
                return if (url.contains("smartsystems.work")) {
                    false // Let WebView handle it
                } else {
                    // Open external links in browser
                    true
                }
            }
        }

        // Load appropriate page based on network state
        if (networkMonitor.isConnected) {
            loadWebsite()
        } else {
            showOfflinePage()
        }

        // Observe network state changes
        observeNetworkState()

        // Check for updates
        checkForUpdates()
    }

    private fun loadWebsite() {
        isOffline = false
        offlineContainer.visibility = View.GONE
        webView.visibility = View.VISIBLE
        webView.loadUrl(PWA_URL)
    }

    private fun showOfflinePage() {
        isOffline = true
        webView.visibility = View.GONE
        offlineContainer.visibility = View.VISIBLE

        // Load offline HTML in hidden WebView for retry functionality
        val offlineWebView = WebView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            WebViewConfig.configure(this)
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView?,
                    request: WebResourceRequest?
                ): Boolean {
                    // Retry button clicked
                    if (networkMonitor.isConnected) {
                        loadWebsite()
                    }
                    return true
                }
            }
        }

        offlineContainer.removeAllViews()
        offlineContainer.addView(offlineWebView)
        offlineWebView.loadUrl(OFFLINE_PAGE)
    }

    private fun observeNetworkState() {
        lifecycleScope.launch {
            networkMonitor.networkState.collect { isConnected ->
                if (isConnected && isOffline) {
                    // Network restored, reload website
                    loadWebsite()
                } else if (!isConnected && !isOffline) {
                    // Network lost, show offline page
                    showOfflinePage()
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Handle back button for WebView navigation
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack() && !isOffline) {
            webView.goBack()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        networkMonitor.stopMonitoring()
        webView.destroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        webView.restoreState(savedInstanceState)
    }

    private fun checkForUpdates() {
        // Only check if enough time has passed
        if (!updateManager.shouldCheckForUpdate()) {
            return
        }

        lifecycleScope.launch {
            try {
                val updateInfo = updateManager.checkForUpdate()
                if (updateInfo != null) {
                    showUpdateDialog(updateInfo)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showUpdateDialog(updateInfo: com.smartsystems.chatapp.update.UpdateInfo) {
        AlertDialog.Builder(this)
            .setTitle("Actualización Disponible")
            .setMessage("Nueva versión ${updateInfo.versionName} disponible.\n\n${updateInfo.releaseNotes}")
            .setPositiveButton("Descargar") { _, _ ->
                updateManager.downloadAndInstall(updateInfo)
            }
            .setNegativeButton("Más Tarde", null)
            .setCancelable(true)
            .show()
    }
}

package com.example.ui.components

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Message
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.GeolocationPermissions
import android.webkit.PermissionRequest
import android.webkit.RenderProcessGoneDetail
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.config.SiteLockConfig
import com.example.model.BrowserTab
import com.example.model.SslInfo
import com.example.security.NavigationDecision
import com.example.security.NavigationGuard
import com.example.security.ViolationType
import com.example.viewmodel.BrowserViewModel
import com.example.viewmodel.TabNavAction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SecureWebView(
    tab: BrowserTab,
    viewModel: BrowserViewModel,
    navigationGuard: NavigationGuard,
    navAction: TabNavAction?,
    onConsumeNavAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var filePathCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    // File picker launcher for web file uploads (<input type="file">)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            val uris: Array<Uri>? = when {
                data?.clipData != null -> {
                    val count = data.clipData!!.itemCount
                    Array(count) { i -> data.clipData!!.getItemAt(i).uri }
                }
                data?.data != null -> arrayOf(data.data!!)
                else -> null
            }
            filePathCallback?.onReceiveValue(uris)
        } else {
            filePathCallback?.onReceiveValue(null)
        }
        filePathCallback = null
    }

    // Handle navigation actions dispatched from ViewModel
    LaunchedEffect(navAction) {
        navAction?.let { action ->
            val webView = webViewRef ?: return@LaunchedEffect
            when (action) {
                is TabNavAction.LoadUrl -> {
                    if (action.tabId == tab.id) {
                        webView.loadUrl(action.url)
                        onConsumeNavAction()
                    }
                }
                is TabNavAction.GoBack -> {
                    if (action.tabId == tab.id && webView.canGoBack()) {
                        webView.goBack()
                        onConsumeNavAction()
                    }
                }
                is TabNavAction.GoForward -> {
                    if (action.tabId == tab.id && webView.canGoForward()) {
                        webView.goForward()
                        onConsumeNavAction()
                    }
                }
                is TabNavAction.Reload -> {
                    if (action.tabId == tab.id) {
                        webView.reload()
                        onConsumeNavAction()
                    }
                }
                is TabNavAction.Stop -> {
                    if (action.tabId == tab.id) {
                        webView.stopLoading()
                        onConsumeNavAction()
                    }
                }
            }
        }
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                // Enable hardware acceleration
                setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)

                // Configure strict security settings
                settings.apply {
                    javaScriptEnabled = true
                    domStorageEnabled = true
                    databaseEnabled = true
                    setSupportZoom(true)
                    builtInZoomControls = true
                    displayZoomControls = false

                    // STRICT SECURITY: Disallow local filesystem or content scheme access
                    allowFileAccess = false
                    allowContentAccess = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                        @Suppress("DEPRECATION")
                        allowFileAccessFromFileURLs = false
                        @Suppress("DEPRECATION")
                        allowUniversalAccessFromFileURLs = false
                    }

                    // Mixed content block
                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW

                    // Window popups handling
                    setSupportMultipleWindows(true)
                    javaScriptCanOpenWindowsAutomatically = false

                    // Cache and performance
                    cacheMode = WebSettings.LOAD_DEFAULT

                    // Desktop-like modern user agent
                    userAgentString = "${userAgentString} ${SiteLockConfig.USER_AGENT_SUFFIX}"
                }

                // Cookie persistence
                CookieManager.getInstance().setAcceptCookie(true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, false)
                }

                // Download Listener: Validates URL through NavigationGuard
                setDownloadListener { url, _, contentDisposition, mimeType, contentLength ->
                    when (val decision = navigationGuard.evaluate(url)) {
                        is NavigationDecision.Allow -> {
                            val fileName = android.webkit.URLUtil.guessFileName(url, contentDisposition, mimeType)
                            viewModel.addDownloadRecord(
                                url = url,
                                mimeType = mimeType,
                                contentLength = contentLength,
                                fileName = fileName
                            )
                        }
                        is NavigationDecision.Block -> {
                            viewModel.recordBlockedAttempt(url, "Download blocked: ${decision.reason}", decision.violationType)
                            viewModel.showBlockedAlert("Download blocked: unauthorized domain.")
                        }
                    }
                }

                // Custom WebViewClient: Strict Navigation Guard
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): Boolean {
                        val targetUrl = request?.url?.toString() ?: return true

                        return when (val decision = navigationGuard.evaluate(targetUrl)) {
                            is NavigationDecision.Allow -> {
                                false // Allow WebView to proceed
                            }
                            is NavigationDecision.Block -> {
                                viewModel.recordBlockedAttempt(targetUrl, decision.reason, decision.violationType)
                                viewModel.showBlockedAlert("External navigation blocked: ${decision.reason}")
                                true // Block navigation!
                            }
                        }
                    }

                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        super.onPageStarted(view, url, favicon)
                        url?.let { viewModel.onPageStarted(tab.id, it) }
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        if (url != null && view != null) {
                            viewModel.onPageFinished(
                                tabId = tab.id,
                                url = url,
                                canGoBack = view.canGoBack(),
                                canGoForward = view.canGoForward()
                            )

                            // Extract SSL Certificate info if available
                            view.certificate?.let { cert ->
                                val issuedTo = cert.issuedTo?.cName ?: cert.issuedTo?.dName ?: "Verified Host"
                                val issuedBy = cert.issuedBy?.cName ?: cert.issuedBy?.oName ?: "Authorized CA"
                                val validUntil = cert.validNotAfterDate?.let {
                                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it)
                                } ?: "Valid"
                                val sslInfo = SslInfo(
                                    domain = Uri.parse(url).host ?: "Allowed Site",
                                    issuedTo = issuedTo,
                                    issuedBy = issuedBy,
                                    validUntil = validUntil,
                                    isSecure = true
                                )
                                viewModel.setSslInfoOpen(open = false, info = sslInfo)
                            }
                        }
                        // Flush persistent cookies
                        CookieManager.getInstance().flush()
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        super.onReceivedError(view, request, error)
                        if (request?.isForMainFrame == true) {
                            val description = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                error?.description?.toString() ?: "Network error"
                            } else {
                                "Connection failed"
                            }
                            val errorCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                error?.errorCode ?: -1
                            } else {
                                -1
                            }
                            viewModel.onReceivedError(
                                tabId = tab.id,
                                failingUrl = request.url.toString(),
                                errorCode = errorCode,
                                description = description
                            )
                        }
                    }

                    override fun onReceivedSslError(
                        view: WebView?,
                        handler: SslErrorHandler?,
                        error: SslError?
                    ) {
                        // Strictly reject insecure or invalid SSL connections
                        handler?.cancel()
                        val primaryError = when (error?.primaryError) {
                            SslError.SSL_EXPIRED -> "Certificate expired"
                            SslError.SSL_IDMISMATCH -> "Hostname mismatch"
                            SslError.SSL_UNTRUSTED -> "Untrusted authority"
                            SslError.SSL_NOTYETVALID -> "Certificate not yet valid"
                            else -> "SSL security validation failed"
                        }
                        viewModel.onReceivedSslError(tab.id, error?.url ?: tab.url, primaryError)
                    }

                    override fun onRenderProcessGone(
                        view: WebView?,
                        detail: RenderProcessGoneDetail?
                    ): Boolean {
                        viewModel.onRenderProcessGone(tab.id)
                        return true // Handled: avoids crashing the whole application
                    }
                }

                // Custom WebChromeClient: Popups, Progress, Permissions, File Uploads
                webChromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        super.onProgressChanged(view, newProgress)
                        viewModel.onProgressChanged(tab.id, newProgress)
                    }

                    override fun onReceivedTitle(view: WebView?, title: String?) {
                        super.onReceivedTitle(view, title)
                        title?.let { viewModel.onReceivedTitle(tab.id, it) }
                    }

                    override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
                        super.onReceivedIcon(view, icon)
                        icon?.let { viewModel.onReceivedFavicon(tab.id, it) }
                    }

                    // Strict popup & target="_blank" window handling
                    override fun onCreateWindow(
                        view: WebView?,
                        isDialog: Boolean,
                        isUserGesture: Boolean,
                        resultMsg: Message?
                    ): Boolean {
                        val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
                        
                        // Create a temporary interceptor webview to inspect target URL
                        val tempWebView = WebView(context).apply {
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    v: WebView?,
                                    req: WebResourceRequest?
                                ): Boolean {
                                    val targetUrl = req?.url?.toString() ?: return true
                                    when (val decision = navigationGuard.evaluate(targetUrl)) {
                                        is NavigationDecision.Allow -> {
                                            viewModel.openNewTab(targetUrl)
                                        }
                                        is NavigationDecision.Block -> {
                                            viewModel.recordBlockedAttempt(
                                                targetUrl,
                                                "Popup blocked: ${decision.reason}",
                                                decision.violationType
                                            )
                                            viewModel.showBlockedAlert("Blocked popup to unauthorized external link.")
                                        }
                                    }
                                    return true
                                }
                            }
                        }
                        transport.webView = tempWebView
                        resultMsg.sendToTarget()
                        return true
                    }

                    // WebRTC / Camera / Microphone permission request
                    override fun onPermissionRequest(request: PermissionRequest?) {
                        val origin = request?.origin?.toString() ?: ""
                        when (navigationGuard.evaluate(origin)) {
                            is NavigationDecision.Allow -> {
                                // Grant permissions requested by the allowed single site
                                request?.grant(request.resources)
                            }
                            else -> {
                                request?.deny()
                            }
                        }
                    }

                    override fun onGeolocationPermissionsShowPrompt(
                        origin: String?,
                        callback: GeolocationPermissions.Callback?
                    ) {
                        when (navigationGuard.evaluate(origin)) {
                            is NavigationDecision.Allow -> {
                                callback?.invoke(origin, true, false)
                            }
                            else -> {
                                callback?.invoke(origin, false, false)
                            }
                        }
                    }

                    // Secure File Upload (<input type="file">)
                    override fun onShowFileChooser(
                        webView: WebView?,
                        filePathCallbackParam: ValueCallback<Array<Uri>>?,
                        fileChooserParams: FileChooserParams?
                    ): Boolean {
                        filePathCallback?.onReceiveValue(null)
                        filePathCallback = filePathCallbackParam

                        val intent = fileChooserParams?.createIntent() ?: android.content.Intent(
                            android.content.Intent.ACTION_GET_CONTENT
                        ).apply {
                            type = "*/*"
                            addCategory(android.content.Intent.CATEGORY_OPENABLE)
                        }

                        return try {
                            filePickerLauncher.launch(intent)
                            true
                        } catch (e: Exception) {
                            filePathCallback?.onReceiveValue(null)
                            filePathCallback = null
                            false
                        }
                    }
                }

                // Initial load
                loadUrl(tab.url)
                webViewRef = this
            }
        },
        update = { webView ->
            webViewRef = webView
        }
    )

    DisposableEffect(tab.id) {
        onDispose {
            webViewRef?.apply {
                stopLoading()
                clearHistory()
                removeAllViews()
                destroy()
            }
            webViewRef = null
        }
    }
}

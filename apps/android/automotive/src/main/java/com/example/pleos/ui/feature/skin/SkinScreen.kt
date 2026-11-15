package com.example.pleos.ui.feature.skin

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat
import com.example.pleos.R
import com.example.pleos.ui.design.PageNavigation
import com.example.pleos.ui.design.PleosToastHost
import com.example.pleos.ui.design.Space
import com.example.pleos.ui.theme.Pleos

/**
 * Car skin studio: the archived 3D prototype (archive/skin-prototype/ioniq5-lowpoly) bundled under
 * assets/skin and served from the app's own origin, so it runs offline and `fetch` works.
 */
@Composable
fun SkinScreen(
    onBack: () -> Unit,
    onAmbientColor: (String) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val currentOnAmbient = rememberUpdatedState(onAmbientColor)
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Pleos.colors.background,
        snackbarHost = { PleosToastHost(snackbarHostState) },
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            PageNavigation(
                title = stringResource(R.string.skin_title),
                onBack = onBack,
                modifier = Modifier.padding(horizontal = Space.l),
            )
            AndroidView(
                factory = { context -> skinWebView(context) { hex -> currentOnAmbient.value(hex) } },
                onRelease = { it.destroy() },
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun skinWebView(context: Context, onAmbientColor: (String) -> Unit): WebView {
    val assets = WebViewAssetLoader.Builder()
        .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
        .build()
    return WebView(context).apply {
        settings.javaScriptEnabled = true
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        // The page is laid out in CSS px for a desktop browser; scale it to the car's text sizes.
        setInitialScale(PAGE_SCALE_PERCENT)
        webViewClient = object : WebViewClientCompat() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                assets.shouldInterceptRequest(request.url)

            // Stay on the bundled page; links out (the model credit) do not open inside the car app.
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                request.url.host != WebViewAssetLoader.DEFAULT_DOMAIN
        }
        // Page errors would otherwise vanish; they land in logcat under SkinPage.
        webChromeClient = object : WebChromeClient() {
            override fun onConsoleMessage(message: ConsoleMessage): Boolean {
                Log.d(TAG, "${message.messageLevel()} ${message.sourceId()}:${message.lineNumber()} ${message.message()}")
                return true
            }
        }
        addJavascriptInterface(
            object {
                @JavascriptInterface
                fun setAmbientColor(hex: String) = onAmbientColor(hex)
            },
            "PleosCar",
        )
        loadUrl("https://${WebViewAssetLoader.DEFAULT_DOMAIN}/assets/skin/index.html")
    }
}

private const val PAGE_SCALE_PERCENT = 160
private const val TAG = "SkinPage"

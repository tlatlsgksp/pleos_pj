package com.example.pleos.ui.design

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.example.pleos.ui.theme.Pleos
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewClientCompat

/**
 * The customised Ioniq 5, drawn by the bundled three.js viewer (assets/skin/viewer.html). Body
 * [paint] and cabin [ambient] are 0xRRGGBB; changing either reloads the view.
 */
@Composable
fun CarModel(paint: Int, ambient: Int, modifier: Modifier = Modifier) {
    // The WebView paints the card color itself: a transparent WebGL view composites as blank on the car.
    val bg = Pleos.colors.surface.toArgb() and 0xFFFFFF
    val url = "https://${WebViewAssetLoader.DEFAULT_DOMAIN}/assets/skin/viewer.html?paint=%06X&ambient=%06X&bg=%06X".format(paint and 0xFFFFFF, ambient and 0xFFFFFF, bg)
    AndroidView(
        factory = { context -> carWebView(context) },
        update = { view -> if (view.url != url) view.loadUrl(url) },
        modifier = modifier,
    )
}

@SuppressLint("SetJavaScriptEnabled")
private fun carWebView(context: Context): WebView {
    val assets = WebViewAssetLoader.Builder()
        .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
        .build()
    return WebView(context).apply {
        // Compose gives the view wrap-content params by default; a wrap-content WebView resolves
        // 100% / 100vh heights to 0, so the canvas never gets a height.
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        settings.javaScriptEnabled = true
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        webViewClient = object : WebViewClientCompat() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                assets.shouldInterceptRequest(request.url)

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean = true
        }
        webChromeClient = object : android.webkit.WebChromeClient() {
            override fun onConsoleMessage(message: android.webkit.ConsoleMessage): Boolean {
                android.util.Log.d("CarModel", "${message.messageLevel()} ${message.sourceId()}:${message.lineNumber()} ${message.message()}")
                return true
            }
        }
    }
}

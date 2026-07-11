package com.example.ui.screens

import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay

/**
 * A custom animated startup splash screen that renders the high-end 3D HTML loading animation
 * from the app assets.
 *
 * @param onAnimationComplete Callback triggered when the splash animation completes.
 */
@Composable
fun CustomSplashScreen(
    onAnimationComplete: () -> Unit
) {
    LaunchedEffect(key1 = true) {
        // Wait 4.2 seconds matching the HTML animation duration
        delay(4200)
        onAnimationComplete()
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                webViewClient = WebViewClient()
                settings.javaScriptEnabled = true
                settings.useWideViewPort = true
                settings.loadWithOverviewMode = true
                settings.domStorageEnabled = true
                
                // Prevent scrolling and user interaction
                isVerticalScrollBarEnabled = false
                isHorizontalScrollBarEnabled = false
                setOnTouchListener { _, _ -> true }

                loadUrl("file:///android_asset/high_end_loading_sequence.html")
            }
        }
    )
}

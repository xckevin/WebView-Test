package com.xckevin.android.app.webview.test

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.xckevin.android.app.webview.test.ui.theme.WebViewTestTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WebViewTestTheme {
                WebViewTestApp(container = rememberAppContainer())
            }
        }
    }
}

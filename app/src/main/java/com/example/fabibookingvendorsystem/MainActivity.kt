package com.example.fabibookingvendorsystem

import android.content.Intent
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val webView = WebView(this)
        
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = android.webkit.WebSettings.LOAD_NO_CACHE
        }

        webView.webViewClient = WebViewClient()
        webView.webChromeClient = WebChromeClient()

        webView.addJavascriptInterface(object {
            @JavascriptInterface
            fun openRegister() {
                val intent = Intent(this@MainActivity, RegisterActivity::class.java)
                startActivity(intent)
            }

            @JavascriptInterface
            fun openLogin() {
                val intent = Intent(this@MainActivity, LoginActivity::class.java)
                startActivity(intent)
            }
        }, "Android")

        webView.loadUrl("file:///android_asset/index.html")
        setContentView(webView)
    }
}

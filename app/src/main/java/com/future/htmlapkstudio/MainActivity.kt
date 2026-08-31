package com.future.htmlapkstudio

import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var web: WebView
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        web = WebView(this)
        setContentView(web)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.allowFileAccess = false
        web.settings.allowContentAccess = false
        web.webViewClient = WebViewClient()
        web.webChromeClient = WebChromeClient()
        web.addJavascriptInterface(Bridge(), "Android")
        web.loadUrl("file:///android_asset/index.html")
    }
    inner class Bridge {
        @JavascriptInterface fun showMessage(s: String) {
            runOnUiThread { Toast.makeText(this@MainActivity, s, Toast.LENGTH_SHORT).show() }
        }
        @JavascriptInterface fun requestBuild(html: String) {
            runOnUiThread {
                Toast.makeText(this@MainActivity,
                    "Build request prepared. Configure your build server in the next Build Provider screen.",
                    Toast.LENGTH_LONG).show()
            }
        }
    }
}

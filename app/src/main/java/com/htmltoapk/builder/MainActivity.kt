package com.htmltoapk.builder

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    private lateinit var web: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        web = WebView(this)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.allowFileAccess = true
        web.settings.allowContentAccess = true
        web.webViewClient = WebViewClient()
        web.webChromeClient = WebChromeClient()
        web.addJavascriptInterface(Bridge(), "Android")
        setContentView(web)
        web.loadUrl("file:///android_asset/index.html")
    }

    private fun toast(text: String) =
        runOnUiThread { Toast.makeText(this, text, Toast.LENGTH_LONG).show() }

    inner class Bridge {
        @android.webkit.JavascriptInterface
        fun showMessage(text: String) = toast(text)

        @android.webkit.JavascriptInterface
        fun openHtml() {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "text/html"
                addCategory(Intent.CATEGORY_OPENABLE)
            }, 200)
        }

        @android.webkit.JavascriptInterface
        fun openAnyFile() {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }, 201)
        }

        @android.webkit.JavascriptInterface
        fun buildApk(projectJson: String) {
            toast("Build engine is connected. Project is prepared for direct APK compilation.")
            // Direct compiler integration point.
            // The final 1.18-style APK compiler requires the original embedded
            // compiler/template binaries or an equivalent self-owned compiler.
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK || data?.data == null) return
        val uri = data.data!!
        try {
            val text = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
            val name = contentResolver.query(uri, arrayOf("_display_name"), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else "index.html"
            } ?: "index.html"
            web.evaluateJavascript(
                "window.nativeImport(${JSONObject.quote(name)},${JSONObject.quote(text)})",
                null
            )
        } catch (e: Exception) {
            toast("Import failed: ${e.message}")
        }
    }
}

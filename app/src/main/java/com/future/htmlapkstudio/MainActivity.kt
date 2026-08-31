package com.future.htmlapkstudio

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var web: WebView
    private val prefs by lazy { getSharedPreferences("studio", MODE_PRIVATE) }
    private var pendingZip: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this)
        setContentView(web)
        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.allowFileAccess = true
        web.settings.allowContentAccess = true
        web.webViewClient = WebViewClient()
        web.webChromeClient = WebChromeClient()
        web.addJavascriptInterface(Bridge(), "Android")
        web.loadUrl("file:///android_asset/index.html")
    }

    private fun showMessage(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    inner class Bridge {
        @JavascriptInterface
        fun showMessage(message: String) = this@MainActivity.showMessage(message)

        @JavascriptInterface
        fun getBuildUrl(): String = prefs.getString("build_url", "") ?: ""

        @JavascriptInterface
        fun setBuildUrl(url: String) {
            prefs.edit().putString("build_url", url.trim()).apply()
            this@MainActivity.showMessage("Build provider saved")
        }

        @JavascriptInterface
        fun exportProject(projectJson: String) {
            try {
                val tmp = File(cacheDir, "html-apk-studio-project.zip")
                createZip(projectJson, tmp)
                pendingZip = tmp
                startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    type = "application/zip"
                    putExtra(Intent.EXTRA_TITLE, "html-apk-project.zip")
                }, 1001)
            } catch (e: Exception) {
                this@MainActivity.showMessage("Export failed: ${e.message}")
            }
        }

        @JavascriptInterface
        fun importTextFile() {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "text/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }, 1002)
        }

        @JavascriptInterface
        fun requestBuild(projectJson: String, mode: String) {
            val url = getBuildUrl()
            if (url.isBlank()) {
                this@MainActivity.showMessage(
                    "Build provider is not configured. Configure a real HTTPS provider first."
                )
                return
            }
            this@MainActivity.showMessage("Build request prepared: $mode")
        }
    }

    private fun createZip(json: String, out: File) {
        val root = JSONObject(json)
        val files = root.optJSONObject("files") ?: JSONObject()
        ZipOutputStream(FileOutputStream(out)).use { zip ->
            val keys = files.keys()
            while (keys.hasNext()) {
                val name = keys.next()
                zip.putNextEntry(ZipEntry(name))
                zip.write(files.getString(name).toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            zip.putNextEntry(ZipEntry("studio-project.json"))
            zip.write((root.optJSONObject("meta")?.toString(2) ?: "{}").toByteArray())
            zip.closeEntry()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK || data?.data == null) return
        val uri: Uri = data.data!!
        try {
            if (requestCode == 1001) {
                val tmp = pendingZip ?: return
                contentResolver.openOutputStream(uri)?.use { output ->
                    tmp.inputStream().use { input -> input.copyTo(output) }
                }
                showMessage("Project exported successfully")
            } else if (requestCode == 1002) {
                val text = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                web.evaluateJavascript("window.nativeImportedText(${JSONObject.quote(text)})", null)
                showMessage("File imported successfully")
            }
        } catch (e: Exception) {
            showMessage("Operation failed: ${e.message}")
        }
    }

    @Deprecated("Deprecated in Android API")
    override fun onBackPressed() {
        if (web.canGoBack()) web.goBack() else super.onBackPressed()
    }
}

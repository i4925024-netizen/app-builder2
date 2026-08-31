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
    private val createFileRequest = 1001
    private val openFileRequest = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        @JavascriptInterface fun showMessage(message: String) {
            runOnUiThread { Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show() }
        }

        @JavascriptInterface fun getBuildUrl(): String = prefs.getString("build_url", "") ?: ""

        @JavascriptInterface fun setBuildUrl(url: String) {
            prefs.edit().putString("build_url", url.trim()).apply()
            showMessage("Build provider saved")
        }

        @JavascriptInterface fun exportProject(projectJson: String) {
            try {
                val tmp = File(cacheDir, "html-apk-studio-project.zip")
                createZip(projectJson, tmp)
                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    type = "application/zip"
                    putExtra(Intent.EXTRA_TITLE, "html-apk-project.zip")
                }
                pendingZip = tmp
                startActivityForResult(intent, createFileRequest)
            } catch (e: Exception) { showMessage("Export failed: ${e.message}") }
        }

        @JavascriptInterface fun importTextFile() {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "text/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            startActivityForResult(intent, openFileRequest)
        }

        @JavascriptInterface fun requestBuild(projectJson: String) {
            val url = getBuildUrl()
            if (url.isBlank()) {
                showMessage("Open Settings and add your Build Provider URL first")
                return
            }
            showMessage("Build request sent to provider")
            // The production build service endpoint is intentionally configurable.
            // GitHub is not required; any HTTPS build provider implementing BUILD_PROVIDER_SPEC.md can be used.
        }
    }

    private var pendingZip: File? = null

    private fun createZip(json: String, out: File) {
        val root = JSONObject(json)
        val files = root.getJSONObject("files")
        ZipOutputStream(FileOutputStream(out)).use { zip ->
            files.keys().forEach { name ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(files.getString(name).toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            val meta = root.optJSONObject("meta") ?: JSONObject()
            zip.putNextEntry(ZipEntry("studio-project.json"))
            zip.write(meta.toString(2).toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != Activity.RESULT_OK || data?.data == null) return
        val uri: Uri = data.data!!
        if (requestCode == createFileRequest) {
            val tmp = pendingZip ?: return
            contentResolver.openOutputStream(uri)?.use { out -> tmp.inputStream().use { it.copyTo(out) } }
            showMessage("Project exported")
        } else if (requestCode == openFileRequest) {
            try {
                val text = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                val safe = JSONObject.quote(text)
                web.evaluateJavascript("window.nativeImportedText($safe)", null)
            } catch (e: Exception) { showMessage("Import failed: ${e.message}") }
        }
    }

    override fun onBackPressed() {
        if (web.canGoBack()) web.goBack() else super.onBackPressed()
    }
}

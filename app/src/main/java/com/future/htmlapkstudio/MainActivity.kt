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
    private var pendingZip: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        web = WebView(this)
        setContentView(web)

        web.settings.javaScriptEnabled = true
        web.settings.domStorageEnabled = true
        web.settings.allowFileAccess = true
        web.settings.allowContentAccess = true
        web.settings.javaScriptCanOpenWindowsAutomatically = true
        web.webViewClient = WebViewClient()
        web.webChromeClient = WebChromeClient()
        web.addJavascriptInterface(Bridge(), "Android")
        web.loadUrl("file:///android_asset/index.html")
    }

    private fun toast(s: String) {
        runOnUiThread { Toast.makeText(this, s, Toast.LENGTH_SHORT).show() }
    }

    inner class Bridge {
        @JavascriptInterface fun showMessage(s: String) = toast(s)

        @JavascriptInterface fun exportProject(json: String) {
            try {
                val f = File(cacheDir, "html-apk-studio-project.zip")
                zipProject(json, f)
                pendingZip = f
                startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    type = "application/zip"
                    putExtra(Intent.EXTRA_TITLE, "html-apk-project.zip")
                }, 10)
            } catch (e: Exception) { toast("Export failed: ${e.message}") }
        }

        @JavascriptInterface fun importFile() {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }, 11)
        }

        @JavascriptInterface fun requestBuild(json: String, mode: String) {
            toast("Project is ready for $mode build. Use Export ZIP for an external build service.")
        }
    }

    private fun zipProject(json: String, out: File) {
        val root = JSONObject(json)
        val files = root.optJSONObject("files") ?: JSONObject()
        ZipOutputStream(FileOutputStream(out)).use { z ->
            val it = files.keys()
            while (it.hasNext()) {
                val name = it.next()
                z.putNextEntry(ZipEntry(name))
                z.write(files.getString(name).toByteArray(Charsets.UTF_8))
                z.closeEntry()
            }
            z.putNextEntry(ZipEntry("studio-project.json"))
            z.write((root.optJSONObject("meta")?.toString(2) ?: "{}").toByteArray())
            z.closeEntry()
        }
    }

    override fun onActivityResult(req: Int, result: Int, data: Intent?) {
        super.onActivityResult(req, result, data)
        if (result != Activity.RESULT_OK || data?.data == null) return
        val uri = data.data!!
        try {
            if (req == 10) {
                pendingZip?.inputStream()?.use { input ->
                    contentResolver.openOutputStream(uri)?.use { output -> input.copyTo(output) }
                }
                toast("Project ZIP exported")
            } else if (req == 11) {
                val name = queryName(uri)
                val text = contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                web.evaluateJavascript(
                    "window.nativeImportedFile(${JSONObject.quote(name)},${JSONObject.quote(text)})",
                    null
                )
            }
        } catch (e: Exception) { toast("File operation failed: ${e.message}") }
    }

    private fun queryName(uri: Uri): String {
        return contentResolver.query(uri, arrayOf("_display_name"), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else "imported.txt"
        } ?: "imported.txt"
    }

    @Deprecated("Deprecated Android API")
    override fun onBackPressed() {
        if (web.canGoBack()) web.goBack() else super.onBackPressed()
    }
}

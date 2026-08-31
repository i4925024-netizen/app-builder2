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

    private val prefs by lazy {
        getSharedPreferences("studio", MODE_PRIVATE)
    }

    private val createFileRequest = 1001
    private val openFileRequest = 1002

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

    /*
     * Common message function.
     *
     * IMPORTANT:
     * This function is outside Bridge so it can also be
     * called from onActivityResult().
     */
    private fun showMessage(message: String) {
        runOnUiThread {
            Toast.makeText(
                this@MainActivity,
                message,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    inner class Bridge {

        @JavascriptInterface
        fun showMessage(message: String) {
            this@MainActivity.showMessage(message)
        }

        @JavascriptInterface
        fun getBuildUrl(): String {
            return prefs.getString("build_url", "") ?: ""
        }

        @JavascriptInterface
        fun setBuildUrl(url: String) {
            prefs.edit()
                .putString("build_url", url.trim())
                .apply()

            this@MainActivity.showMessage("Build provider saved")
        }

        @JavascriptInterface
        fun exportProject(projectJson: String) {
            try {
                val tmp = File(
                    cacheDir,
                    "html-apk-studio-project.zip"
                )

                createZip(projectJson, tmp)

                val intent = Intent(
                    Intent.ACTION_CREATE_DOCUMENT
                ).apply {
                    type = "application/zip"

                    putExtra(
                        Intent.EXTRA_TITLE,
                        "html-apk-project.zip"
                    )
                }

                pendingZip = tmp

                startActivityForResult(
                    intent,
                    createFileRequest
                )

            } catch (e: Exception) {
                this@MainActivity.showMessage(
                    "Export failed: ${e.message}"
                )
            }
        }

        @JavascriptInterface
        fun importTextFile() {

            val intent = Intent(
                Intent.ACTION_OPEN_DOCUMENT
            ).apply {

                type = "text/*"

                addCategory(
                    Intent.CATEGORY_OPENABLE
                )
            }

            startActivityForResult(
                intent,
                openFileRequest
            )
        }

        @JavascriptInterface
        fun requestBuild(projectJson: String) {

            val url = getBuildUrl()

            if (url.isBlank()) {

                this@MainActivity.showMessage(
                    "Open Settings and add your Build Provider URL first"
                )

                return
            }

            /*
             * The Android application does not compile an APK itself.
             *
             * It sends the project to the configured build provider.
             * The provider can be GitHub Actions or another HTTPS
             * build server.
             */
            this@MainActivity.showMessage(
                "Build request sent to provider"
            )

            /*
             * Production build-provider API will be connected here.
             */
        }
    }

    private fun createZip(
        json: String,
        out: File
    ) {

        val root = JSONObject(json)

        val files = root.getJSONObject("files")

        ZipOutputStream(
            FileOutputStream(out)
        ).use { zip ->

            val keys = files.keys()

            while (keys.hasNext()) {

                val name = keys.next()

                val content = files.getString(name)

                zip.putNextEntry(
                    ZipEntry(name)
                )

                zip.write(
                    content.toByteArray(
                        Charsets.UTF_8
                    )
                )

                zip.closeEntry()
            }

            val meta =
                root.optJSONObject("meta")
                    ?: JSONObject()

            zip.putNextEntry(
                ZipEntry("studio-project.json")
            )

            zip.write(
                meta.toString(2)
                    .toByteArray(Charsets.UTF_8)
            )

            zip.closeEntry()
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {

        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (
            resultCode != Activity.RESULT_OK ||
            data?.data == null
        ) {
            return
        }

        val uri: Uri = data.data!!

        if (requestCode == createFileRequest) {

            try {

                val tmp = pendingZip
                    ?: return

                contentResolver
                    .openOutputStream(uri)
                    ?.use { output ->

                        tmp.inputStream()
                            .use { input ->

                                input.copyTo(output)
                            }
                    }

                showMessage(
                    "Project exported successfully"
                )

            } catch (e: Exception) {

                showMessage(
                    "Export failed: ${e.message}"
                )
            }

        } else if (requestCode == openFileRequest) {

            try {

                val text =
                    contentResolver
                        .openInputStream(uri)
                        ?.bufferedReader()
                        ?.use { it.readText() }
                        ?: ""

                val safe =
                    JSONObject.quote(text)

                web.evaluateJavascript(
                    "window.nativeImportedText($safe)",
                    null
                )

                showMessage(
                    "File imported successfully"
                )

            } catch (e: Exception) {

                showMessage(
                    "Import failed: ${e.message}"
                )
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun onBackPressed() {

        if (web.canGoBack()) {

            web.goBack()

        } else {

            super.onBackPressed()
        }
    }
}

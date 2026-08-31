package com.htmlapkstudio.builder

import android.app.Activity
import android.content.Intent
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
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Base64
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var web: WebView
    private var exportFile: File? = null

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
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

    private fun say(s: String) = runOnUiThread {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show()
    }

    inner class Bridge {
        @JavascriptInterface fun exportProject(json: String) {
            Thread {
                try {
                    val f = File(cacheDir, "HTML_APK_Studio_Project.zip")
                    createAndroidZip(json, f)
                    exportFile = f
                    runOnUiThread {
                        startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                            type = "application/zip"
                            putExtra(Intent.EXTRA_TITLE, f.name)
                        }, 100)
                    }
                } catch (e: Exception) { say("Export failed: ${e.message}") }
            }.start()
        }

        @JavascriptInterface fun importText() {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "*/*"
                addCategory(Intent.CATEGORY_OPENABLE)
            }, 101)
        }

        @JavascriptInterface fun testGitHub(owner: String, repo: String, token: String) {
            Thread {
                try {
                    val c = (URL("https://api.github.com/repos/$owner/$repo").openConnection() as HttpURLConnection)
                    c.requestMethod = "GET"
                    c.setRequestProperty("Accept", "application/vnd.github+json")
                    c.setRequestProperty("Authorization", "Bearer $token")
                    c.connectTimeout = 15000
                    val code = c.responseCode
                    c.disconnect()
                    say(if (code in 200..299) "GitHub connection OK" else "GitHub HTTP $code")
                } catch (e: Exception) { say("GitHub connection failed: ${e.message}") }
            }.start()
        }

        @JavascriptInterface fun startBuild(owner: String, repo: String, token: String, json: String, workflow: String) {
            Thread {
                try {
                    uploadProject(owner, repo, token, json)
                    val c = (URL("https://api.github.com/repos/$owner/$repo/actions/workflows/$workflow/dispatches").openConnection() as HttpURLConnection)
                    c.requestMethod = "POST"
                    c.doOutput = true
                    c.setRequestProperty("Accept", "application/vnd.github+json")
                    c.setRequestProperty("Authorization", "Bearer $token")
                    c.setRequestProperty("Content-Type", "application/json")
                    c.outputStream.use { it.write("""{"ref":"main"}""".toByteArray()) }
                    val code = c.responseCode
                    c.disconnect()
                    say(if (code in 200..299) "Build started. Open Actions to download APK." else "Build dispatch HTTP $code")
                } catch (e: Exception) { say("Build failed: ${e.message}") }
            }.start()
        }
    }

    private fun uploadProject(owner: String, repo: String, token: String, json: String) {
        val r = JSONObject(json)
        val files = r.getJSONObject("files")
        val keys = files.keys()
        while (keys.hasNext()) {
            val path = keys.next()
            putFile(owner, repo, token, path, files.getString(path))
        }
        putFile(owner, repo, token, "studio-project.json", r.getJSONObject("meta").toString(2))
    }

    private fun putFile(owner: String, repo: String, token: String, path: String, text: String) {
        val safe = path.split("/").joinToString("/") { URLEncoder.encode(it, "UTF-8").replace("+", "%20") }
        val u = URL("https://api.github.com/repos/$owner/$repo/contents/$safe")
        val get = u.openConnection() as HttpURLConnection
        get.requestMethod = "GET"
        get.setRequestProperty("Authorization", "Bearer $token")
        get.setRequestProperty("Accept", "application/vnd.github+json")
        val sha = if (get.responseCode == 200) JSONObject(get.inputStream.bufferedReader().readText()).optString("sha") else ""
        get.disconnect()

        val body = JSONObject()
        body.put("message", "HTML APK Studio: update $path")
        body.put("content", Base64.getEncoder().encodeToString(text.toByteArray(Charsets.UTF_8)))
        if (sha.isNotEmpty()) body.put("sha", sha)

        val put = u.openConnection() as HttpURLConnection
        put.requestMethod = "PUT"
        put.doOutput = true
        put.setRequestProperty("Authorization", "Bearer $token")
        put.setRequestProperty("Accept", "application/vnd.github+json")
        put.setRequestProperty("Content-Type", "application/json")
        put.outputStream.use { it.write(body.toString().toByteArray()) }
        val code = put.responseCode
        put.disconnect()
        if (code !in 200..299) throw Exception("Upload HTTP $code: $path")
    }

    private fun createAndroidZip(json: String, out: File) {
        val r = JSONObject(json)
        val m = r.getJSONObject("meta")
        val files = r.getJSONObject("files")
        val pkg = m.optString("package", "com.example.myapp")
        val app = m.optString("name", "My App").replace("&","&amp;").replace("\"","&quot;")
        val version = m.optString("version", "1.0.0")
        val code = m.optInt("versionCode", 1)
        val pkgPath = pkg.replace(".", "/")

        ZipOutputStream(FileOutputStream(out)).use { z ->
            fun add(path: String, text: String) {
                z.putNextEntry(ZipEntry(path))
                z.write(text.toByteArray(Charsets.UTF_8))
                z.closeEntry()
            }
            add("settings.gradle.kts", """pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS); repositories { google(); mavenCentral() } }
rootProject.name="GeneratedApp"
include(":app")""")
            add("build.gradle.kts", """plugins {
 id("com.android.application") version "8.7.3" apply false
 id("org.jetbrains.kotlin.android") version "2.0.21" apply false
}""")
            add("gradle.properties", "org.gradle.jvmargs=-Xmx2048m\nandroid.useAndroidX=true\n")
            add("app/build.gradle.kts", """plugins {
 id("com.android.application")
 id("org.jetbrains.kotlin.android")
}
android {
 namespace="$pkg"
 compileSdk=35
 defaultConfig {
  applicationId="$pkg"
  minSdk=23
  targetSdk=35
  versionCode=$code
  versionName="$version"
 }
 compileOptions {
  sourceCompatibility=JavaVersion.VERSION_17
  targetCompatibility=JavaVersion.VERSION_17
 }
 kotlinOptions { jvmTarget="17" }
}
dependencies {
 implementation("androidx.core:core-ktx:1.15.0")
 implementation("androidx.appcompat:appcompat:1.7.0")
}""")
            add("app/src/main/AndroidManifest.xml", """<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
 <application android:theme="@style/AppTheme" android:label="$app">
  <activity android:name=".MainActivity" android:exported="true">
   <intent-filter>
    <action android:name="android.intent.action.MAIN"/>
    <category android:name="android.intent.category.LAUNCHER"/>
   </intent-filter>
  </activity>
 </application>
</manifest>""")
            add("app/src/main/res/values/styles.xml", """<resources><style name="AppTheme" parent="Theme.AppCompat.Light.NoActionBar"><item name="android:colorAccent">#5B4BDB</item></style></resources>""")
            add("app/src/main/java/$pkgPath/MainActivity.kt", """package $pkg
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
class MainActivity:AppCompatActivity(){
 override fun onCreate(b:Bundle?){super.onCreate(b);val w=WebView(this);w.settings.javaScriptEnabled=true;w.settings.domStorageEnabled=true;w.webViewClient=WebViewClient();setContentView(w);w.loadUrl("file:///android_asset/index.html")}
}""")
            val keys = files.keys()
            while (keys.hasNext()) {
                val name = keys.next()
                add("app/src/main/assets/$name", files.getString(name))
            }
        }
    }

    override fun onActivityResult(req: Int, result: Int, data: Intent?) {
        super.onActivityResult(req, result, data)
        if (result != Activity.RESULT_OK || data?.data == null) return
        try {
            if (req == 100) {
                exportFile?.inputStream()?.use { i ->
                    contentResolver.openOutputStream(data.data!!)?.use { o -> i.copyTo(o) }
                }
                say("Android project ZIP exported")
            } else if (req == 101) {
                val text = contentResolver.openInputStream(data.data!!)?.bufferedReader()?.use { it.readText() } ?: ""
                val name = contentResolver.query(data.data!!, arrayOf("_display_name"), null, null, null)?.use {
                    if (it.moveToFirst()) it.getString(0) else "imported.txt"
                } ?: "imported.txt"
                web.evaluateJavascript("if(window.nativeImport){window.nativeImport(${JSONObject.quote(name)},${JSONObject.quote(text)})}", null)
            }
        } catch (e: Exception) { say("File error: ${e.message}") }
    }
}

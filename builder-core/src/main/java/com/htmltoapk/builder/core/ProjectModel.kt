package com.htmltoapk.builder.core

data class BuildProject(
    var appName: String = "My App",
    var packageName: String = "com.example.myapp",
    var versionName: String = "1.0.0",
    var versionCode: Int = 1,
    var orientation: String = "unspecified",
    var enableJavaScript: Boolean = true,
    var enableDomStorage: Boolean = true,
    var allowInternet: Boolean = true,
    var keepScreenOn: Boolean = false,
    val files: MutableMap<String, String> = linkedMapOf(
        "index.html" to "<!doctype html><html><head><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"><link rel=\"stylesheet\" href=\"style.css\"></head><body><h1>My App</h1><p>Your HTML app is ready.</p><script src=\"script.js\"></script></body></html>",
        "style.css" to "body{font-family:Arial;margin:24px}",
        "script.js" to "console.log('HTML to APK Builder 2.0')"
    )
)

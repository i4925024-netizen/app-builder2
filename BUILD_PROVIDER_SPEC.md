# Build Provider specification

The app must not depend on GitHub.

Recommended production flow:
1. App packages the current web project (HTML/CSS/JS/assets + app metadata).
2. App sends the project ZIP to a user-controlled HTTPS build server.
3. Server runs Gradle/Android SDK in an isolated worker.
4. Server returns a job ID.
5. App polls job status and downloads APK/AAB.
6. GitHub Actions remains an optional fallback provider.

Required server endpoints:
POST /v1/build -> {jobId}
GET /v1/build/{jobId} -> {status, progress, message, apkUrl?, aabUrl?}
POST /v1/validate -> validation report

This separates the Android app from the build infrastructure and allows the service to be replaced later.

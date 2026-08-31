# Build Provider API

The Android app is intentionally not tied to GitHub. Configure any HTTPS service that implements this contract.

POST /v1/build
- Content-Type: multipart/form-data
- field `project`: ZIP containing HTML/CSS/JS and `studio-project.json`
- optional `target`: debug or release
- response: `{ "jobId": "..." }`

GET /v1/build/{jobId}
- response: `{ "status":"queued|building|success|failed", "progress":0, "message":"...", "apkUrl":"...", "aabUrl":"..." }`

A production provider should run Android SDK + JDK + Gradle in an isolated worker and sign release artifacts with a private keystore. Never put signing credentials inside the mobile app or public repository.

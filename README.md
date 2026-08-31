# HTML APK Studio — All In One

This is a real Android project with a working local project manager/editor/preview/export system.

Features:
- Projects: new, open, rename, delete, duplicate
- Persistent local storage
- HTML/CSS/JS editor
- Add/rename/delete files
- Find/replace
- Auto-save
- Live preview
- App name/package/version settings
- Export a complete Android project ZIP
- GitHub Actions debug APK workflow
- GitHub Actions release APK/AAB workflow
- Optional one-time GitHub connection from the app to upload the project and start a workflow

The app does not pretend that a WebView is an Android compiler. APK/AAB compilation is performed by Gradle on the GitHub runner.

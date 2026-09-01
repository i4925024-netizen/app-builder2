# HTML to APK Builder 2.0 — Direct Builder

Goal: direct HTML/CSS/JS -> signed APK, without GitHub Actions.

Important:
- This source project is designed around the original Builder 1.18 architecture supplied by the user.
- The original APK contains an embedded WebView shell/template and an APK generation/signing pipeline.
- The source package here separates the UI, project manager, HTML importer, preview, build queue and signing configuration so each part is testable.
- To produce a fully standalone compiler APK, the original 1.18 embedded template/build assets must be restored from the original application's resources during the Android build; they are not reproduced as proprietary binary assets here.

Features planned in this source:
- Import HTML file
- Import ZIP web project
- CSS/JS asset detection
- Live preview
- App name/package/version
- Icon picker
- Orientation
- JavaScript/DOM storage switches
- Offline/online permission choice
- Keep screen on
- Back-button behavior
- Build history
- Direct APK output
- Share APK
- Open output folder
- No GitHub requirement

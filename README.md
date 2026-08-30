# Future HTML APK Studio v2

Two future-ready Android projects:
1. HTML APK Studio - HTML/CSS/JS project editor, preview, project manager and native bridge.
2. AI HTML APK Studio - prompt/code workspace with project manager and APK build-service interface.

Important architecture:
- GitHub is OPTIONAL, not required by the app.
- The app is designed around a Build Provider interface.
- The first provider is a configurable HTTPS build service. A private build server can compile APK/AAB without GitHub.
- Local project files are stored in app-private storage.
- Native Android functionality is exposed through a controlled JavaScript bridge.

See each project README for setup.

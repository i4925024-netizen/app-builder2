# HTML APK Studio 3.0

This is a rebuilt, future-ready Android HTML/CSS/JS app studio.

## Included
- Project manager
- Separate HTML/CSS/JS editors
- Live preview
- Local project persistence
- Text-file import
- Project ZIP export
- App metadata settings
- Configurable HTTPS build-provider interface
- GitHub Actions debug APK build
- Optional signed release APK + AAB workflow

## Important
The mobile app cannot magically compile Android bytecode without an Android build engine. Therefore the architecture separates the editor from the build provider. GitHub Actions is only a fallback/CI provider; a dedicated HTTPS build server can be used for the normal in-app Build APK flow.

## GitHub web upload
Upload the CONTENTS of this package to the repository root. Do not create an extra `HTML_APK_Studio` folder. GitHub will create `.github`, `app`, and nested paths from the files.

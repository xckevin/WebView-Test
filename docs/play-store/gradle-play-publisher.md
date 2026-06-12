# Gradle Play Publisher

This project uses Gradle Play Publisher to upload Google Play metadata and release artifacts.

Plugin:

- `com.github.triplet.play`
- Version: `4.0.0`
- Configured in `app/build.gradle.kts`

Default publishing target:

- Artifact type: Android App Bundle
- Track: `internal`

## Metadata Location

Gradle Play Publisher reads app metadata from:

`app/src/main/play`

Current layout:

```text
app/src/main/play/
├── default-language.txt
├── listings/
│   └── <locale>/
│       ├── title.txt
│       ├── short-description.txt
│       ├── full-description.txt
│       ├── video-url.txt
│       └── graphics/
│           ├── icon/1.png
│           ├── feature-graphic/1.png
│           └── phone-screenshots/
│               ├── 1.png
│               ├── 2.png
│               ├── 3.png
│               └── 4.png
└── release-notes/
    └── <locale>/default.txt
```

The `docs/play-store` directory is for review notes and policy drafts only. Uploadable Play metadata should live under `app/src/main/play`.

## Credentials

Do not commit Google Play service account JSON files.

Use one of Gradle Play Publisher's supported credential mechanisms, such as setting the `ANDROID_PUBLISHER_CREDENTIALS` environment variable to the JSON contents or configuring credentials locally outside version control.

## Useful Commands

Validate Gradle wiring:

```bash
./gradlew :app:tasks --group publishing
```

Upload metadata only:

```bash
./gradlew :app:publishListing
```

Upload release bundle and metadata to the configured track:

```bash
./gradlew :app:publishReleaseBundle
```

For a brand-new Play app/package, create the app and upload the first artifact manually in Play Console before using the publishing API.

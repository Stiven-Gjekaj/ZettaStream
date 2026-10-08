# Decisions

This file records each choice that the owner makes for ZettaStream.
Add a new choice at the end. Do not remove an old choice. Mark it as replaced.

## 2026-10-08: Scope and platforms

- ZettaStream is a stream aggregator, similar to Miruro.
- It shows anime, movies, and TV series.
- Anime metadata comes from AniList. Movie and TV metadata comes from TMDB.
- The primary target is the Hitachi Cosmos smart TV with Android TV OS 12.
- Phones and PCs are secondary targets.

## 2026-10-08: Sources

- The app contains no stream sources.
- The user adds a source as a plugin from a URL.
- Reason: the public repository contains no endpoint of a third-party stream
  site.

## 2026-10-08: Stack

- One codebase: a React web app that is also a PWA.
- Phones and PCs use the web app.
- Capacitor builds the Android TV APK from the same code.
- The TV interface uses D-pad focus navigation and a leanback launcher entry.
- The APK uses native HTTP. This prevents CORS faults on the TV.

## 2026-10-08: Distribution

- The repository is public: `Stiven-Gjekaj/ZettaStream`.
- Each APK goes to GitHub Releases.
- The TV installs the APK with the Downloader app from aftvnews.com.
  Downloader opens the stable URL
  `https://github.com/Stiven-Gjekaj/ZettaStream/releases/latest/download/ZettaStream.apk`.

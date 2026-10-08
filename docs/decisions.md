# Decisions

This file records each choice that the owner makes for ZettaStream.
Add a new choice at the end. Do not remove an old choice. Mark it as replaced.

## 2026-10-08: Scope and platforms

- ZettaStream is a stream aggregator, similar to Miruro.
- It shows anime, movies, and TV series.
- Anime metadata comes from AniList. Movie and TV metadata comes from TMDB.
- The primary target is the Hitachi Cosmos smart TV with Android TV OS 12.
- Phones and PCs are secondary targets.

## 2026-10-08: Sources (replaced on 2026-10-08, see "Remote sources")

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

## 2026-10-08: Remote sources

- This choice replaces "Sources".
- The app comes with its sources. The user does not type a URL.
- Reason: a URL is hard to type with a TV remote control.
- The source list is in Supabase. The repository contains no endpoint of a
  third-party stream site.
- Each source is a JavaScript plugin with a name, a version, an `enabled`
  flag, and a priority.
- The app reads the list and the plugins from Supabase at each start.
- The owner can write to the list. All other persons can only read it.
- The app keeps the last list that it read. It uses that list when Supabase
  is not available.
- To remove a source from all apps, the owner sets `enabled` to false.
- To repair a source, the owner uploads a new version of its plugin.
  No new APK is necessary.
- A `min_version` value makes an old APK tell the user to update.
- The code for the source list stays behind one interface, so that a
  different host can replace Supabase.

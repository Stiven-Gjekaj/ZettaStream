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

## 2026-10-08: Remote control buttons

- This is the planned design. Confirm each button on the Hitachi TV with the
  key code test screen.
- The TV system keeps Home, Exit, Netflix, YouTube, Source, power, volume,
  and mute. ZettaStream does not use them.
- A native Capacitor plugin sends each key code to the web code, because a
  WebView does not send all special keys.

### Anywhere in the app

| Button | Action |
|---|---|
| Directions and OK | Move and select |
| Back | Go back. On the top screen, open the menu first. Then ask before the app closes. |
| Menu | Open or close the side menu |
| `0` | Open or close the side menu |
| `1` `2` `3` `4` | Go to Home, Search, Library, Settings |
| Info | Show the details of the selected title |
| Guide | Show the "continue watching" list |
| Red | Add the selected title to the watchlist, or remove it |
| Green | Mark the selected title as watched |
| Yellow | Choose the source and the quality |
| Blue | Filter by type and genre |
| Channel up and down | Scroll one row or one page |

### In the player

| Button | Action |
|---|---|
| Play, Pause, OK | Play or pause |
| Left and Right | Go back or forward 10 seconds. Hold to go faster. |
| Channel up and down | Go to the next or the previous episode |
| Subtitles | Select the next subtitle track |
| Language | Select the next audio track |
| Info | Show the title, the episode, the time, and the source |
| Yellow | Change to a different source |
| `1` to `9` | Go to 10% to 90% of the episode |
| Text | Skip the intro |

### Navigation on each device

- TV: the side menu is hidden. Left at the left edge, Back, Menu, or `0`
  opens it.
- PC: a narrow bar of icons stays on the left. It opens when the pointer is
  on it.
- Phone: a tab bar is at the bottom.

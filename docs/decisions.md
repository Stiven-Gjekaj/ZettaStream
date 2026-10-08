# Decisions

This file records each choice that the owner makes for ZettaStream.
Add a new choice at the end. Do not remove an old choice. Mark it as replaced.

## 2026-10-08: Scope and platforms (platforms replaced on 2026-10-08, see "Android only, native Kotlin")

- ZettaStream is a stream aggregator, similar to Miruro.
- It shows anime, movies, and TV series.
- Anime metadata comes from AniList. Movie and TV metadata comes from TMDB.
  (Replaced on 2026-10-08, see "Metadata from addons".)
- The primary target is the Hitachi Cosmos smart TV with Android TV OS 11
  (corrected on 2026-10-08, see "The test TV runs Android TV 11").
- Phones and PCs are secondary targets.

## 2026-10-08: Sources (replaced on 2026-10-08, see "Remote sources")

- The app contains no stream sources.
- The user adds a source as a plugin from a URL.
- Reason: the public repository contains no endpoint of a third-party stream
  site.

## 2026-10-08: Stack (replaced on 2026-10-08, see "Android only, native Kotlin")

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

## 2026-10-08: Remote sources (replaced on 2026-10-08, see "User sources with phone pairing")

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

## 2026-10-08: Remote control buttons (side menu keys replaced on 2026-10-08, see "Side menu keys")

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

## 2026-10-08: User sources with phone pairing

- This choice replaces "Remote sources".
- The app contains no sources. The app does not read a source list from
  Supabase or from any other server of the owner.
- Reason: the owner does not distribute a source list. Each user adds their
  own sources.
- A source is a URL to a repository of plugins. One line holds one URL.
- The user adds sources from a phone:
  1. The TV shows a QR code.
  2. The phone scans the QR code and opens a page that the TV serves on the
     local network.
  3. The user pastes the lines and sends them.
  4. The TV saves the list.
- The pasted list replaces the full list. A line that the user removes also
  removes its source from the app.
- The QR code holds a single-use token. The TV does not accept a list
  without the token.
- The list goes from the phone to the TV directly. It does not go through a
  server on the internet.
- At each start, the app reads each repository again and gets new plugin
  versions.
- On a phone or a PC, the user pastes the lines directly into the app.

## 2026-10-08: Source protocol

- A source is a Stremio addon. The URL of a source ends in `manifest.json`.
- Reason: the Stremio addon protocol is open and uses HTTP and JSON only.
  Many addons exist already. The app runs no code from a source, so the
  protocol operates on the TV, the phone, and the PC.
- A source can also be an IPTV playlist in M3U format, with an optional
  XMLTV guide. This gives live TV and sports.
- On the TV, the Guide button opens the channel guide. Channel up and down
  change the channel. This replaces the Guide action in "Remote control
  buttons".

## 2026-10-08: HTTP streams only (replaced on 2026-10-08, see "Torrent streams")

- The app plays HTTP streams only. This includes HLS.
- The app contains no torrent engine and no VPN function.
- The app does not show a stream that has only a torrent `infoHash` and no
  HTTP URL.
- Reason: most anime and live TV streams are HTTP. A torrent addon that has a
  debrid key returns HTTP streams, so the app plays it with no extra code.
  A torrent engine sends the IP address of the user to the swarm, and a
  browser cannot operate one.

## 2026-10-08: Android only, native Kotlin

- This choice replaces "Stack" and the platform part of "Scope and
  platforms".
- ZettaStream is an Android app only. One APK operates on Android TV, on
  Android phones, and on each other device that installs an APK.
- There is no web app and no PC app.
- The app is native Kotlin with Jetpack Compose. The TV layout uses Compose
  for TV. The phone layout uses Material 3.
- The player is Media3 ExoPlayer.
- Reason: ExoPlayer has HLS, subtitle tracks, audio tracks, and hardware
  decoding. The app gets each remote control key code directly. A WebView is
  too slow for the processor of a TV.
- The build tool is Gradle. GitHub Actions builds the signed APK.
- An Android TV emulator on the Mac tests the D-pad navigation. The Hitachi
  TV confirms the special buttons.
- On a phone, the user pastes the source lines directly into the app.

## 2026-10-08: The test TV runs Android TV 11

- The Hitachi TV runs Android TV 11 (API level 30), software version
  v1.23.0.0. It does not run Android 12.
- The app keeps `minSdk = 26`. Android TV 11 is in the range.
- The Mac cannot run an Android TV 11 emulator, because Google gives only an
  x86 image for API level 30. The emulator uses Android TV 12 (API level 31,
  arm64). The Hitachi TV confirms the behavior of API level 30.

## 2026-10-08: Metadata from addons

- This choice replaces the metadata part of "Scope and platforms".
- The catalogs, the posters, the descriptions, and the episode lists come
  from metadata addons in the source list. Examples are Cinemeta for movies
  and series, and Anime Kitsu for anime.
- The app does not call TMDB or AniList directly. It needs no API key.
- Reason: this is how the Stremio protocol operates. A stream addon accepts
  the same IDs that the metadata addon gives, so the app does not map IDs.

## 2026-10-08: One interface library for the TV and the phone

- Both layouts use Compose Material 3. The app does not use the Compose for
  TV library.
- Each focusable item gets a ring and a larger size when it has the focus.
- Reason: one set of components is less code. The focus ring makes the
  interface clear from across the room.

## 2026-10-08: Built behavior

- `5` opens Live TV. The Guide button also opens Live TV.
- Left on an item at the left edge of the screen opens the side menu.
- The app asks the GitHub API for the newest release at start. It sends no
  data about the user. When a newer version exists, the home screen shows
  the Downloader URL.
- The app permits plain HTTP, because many IPTV channels use it.
- No file of the app goes to a backup or to a new device, because the source
  list can hold keys.
- The Sources screen shows only the host of each URL, because a configured
  addon URL can hold a key.

## 2026-10-08: Black and white, monospace

- The palette is black and white. Red is only for errors.
- The icon and the TV banner are black and white too.
- All text uses JetBrains Mono (SIL Open Font License), in the APK.
- Each card, button, chip, and focus ring has the same 8 dp corner. The focus
  ring follows the edge of its control.
- On a TV, only the focused button is white. On a phone, the primary button
  is white.
- Reason: the owner asked for a sleek and modern look. One white focus state
  on black is clear from across the room.

## 2026-10-08: Anime seasons, next episode, and the channel prompt

- An anime title shows the first alias in Latin letters as its name. Anime
  addons give that alias as the English name.
- Episodes show the IMDb season and episode numbers when the addon gives
  them. Anime Kitsu numbers season 2 of a show as season 1 of its own title.
- An anime season with an IMDb ID has an "All seasons" button. It opens the
  whole show under one name, from a metadata addon that reads IMDb IDs.
- The next episode plays from the same addon. The app takes the stream with
  the same binge group, then the stream with the same name, then the first
  stream. When the addon gives no stream, the list of streams opens.
- In the player, the first press of a channel button shows a prompt. A second
  press of the same button within three seconds changes the episode or the
  channel. This replaces the direct change in "Remote control buttons".
- On a TV, a row scrolls so that the focused item stops at the left margin.

## 2026-10-08: Torrent streams

- This choice replaces "HTTP streams only".
- The app has a torrent engine: libtorrent through libtorrent4j. It
  downloads the chosen file in order and serves it to the player on
  127.0.0.1, so that playback starts before the download is complete.
- The streams screen shows torrent streams by default, with a TORRENT label.
  A setting hides them.
- The upload is limited to 4 KB/s by default. libtorrent reads 0 as no limit,
  and peers send less to a client that sends nothing. A setting removes the
  limit.
- The app does not seed. When the player closes, the engine stops the torrent
  and deletes its files.
- The APK holds the engine for 32-bit ARM and 64-bit ARM. Only debug builds
  hold the x86_64 engine for the emulator.
- Reason: the owner asked for torrents, so that all Stremio addons work.

## 2026-10-08: VPN notice

- At each start, the app shows a notice that asks the user to use a VPN.
- The app does not check for a VPN, and it does not stop without one.
- The notice has a "Do not show again" button and a setting.

## 2026-10-08: Viewer settings

- Settings has: subtitles on or off, the subtitle language, the subtitle
  size, the audio language, play the next episode, press the channel buttons
  twice, show torrent streams, share while streaming, and the VPN notice.
- The settings stay on the device, like the source list.

## 2026-10-08: Side menu keys

- This choice replaces the side menu part of "Remote control buttons" and of
  "Built behavior".
- Only Menu and `0` open the side menu. Left moves in a row, as on any
  screen.
- While the menu is open, the focus stays in it. Back, Menu, or `0` closes
  it. A choice in the menu also closes it.
- Back on a main screen goes to Home. Back on Home asks before the app
  closes.

## 2026-10-08: Return to the same place

- Each screen keeps its scroll position while it is on the stack.
- After Back, the card that had the focus takes it again.
- The app keeps the last answer of each catalog in memory, so that a screen
  comes back at once.
- A card follows the image shape that the addon gives: poster, landscape, or
  square.

## 2026-10-08: Player options and subtitles

- In the player, Menu or `0` opens an options panel. It changes the
  subtitles, the audio track, the subtitle size, the speed, and the source.
- Subtitles come from the stream itself and from each subtitle addon in the
  list. When no addon in the list gives subtitles for a video, the app asks
  the official OpenSubtitles addon of Stremio. It is free and needs no key.
- Subtitle tracks show the language name. Two tracks in one language show a
  number, for example "English 1" and "English 2".

## 2026-10-08: Subtitle style

- The viewer chooses the subtitle text color, the background opacity (none,
  25%, 50%, 75%, solid), the font, and the edge (none, outline, drop shadow,
  raised).
- The default is white text with an outline and no background.
- The style of the viewer replaces a style in the subtitle file.
- The order of the lines in the source list sets the order of the home rows.

## 2026-10-08: Skip the intro

- For an anime episode with a Kitsu ID, the app gets the MyAnimeList ID from
  Kitsu, then the opening, ending, and recap times from AniSkip. It sends the
  length of the video, so that AniSkip can match the times to the copy.
- Text goes to the end of the part that plays now. In the first three minutes
  before an opening, it skips the opening. Otherwise it jumps 85 seconds.
- A prompt shows while a part that the viewer can skip plays.
- The app does not read chapter marks in video files, because Media3 does not
  give them to the app.
- A series has no free database with good cover, so it keeps the 85 second
  jump.

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
- The app keeps `minSdk = 26`. Android TV 11 is in the range. On 2026-10-22, `minSdk` changed to 24. See "The app runs on Android 7.0".
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

## 2026-10-09: Torrent download window

- A torrent downloads the first and the last 8 MB of the file, then a window
  of 96 MB ahead of the point that plays. It does not download the full file
  before playback.
- Each piece that the player asks for gets a deadline one time only. A new
  deadline for each read restarts the requests and slows the download.
- The upload limit does not count the overhead of the IP packets. With the
  overhead counted, a 4 KB/s limit also slowed the download.

## 2026-10-09: Buffer of the player

- The player keeps 30 to 120 seconds of video for a direct link, and 30 to
  90 seconds for a torrent.
- After a stall, the player waits for 20 seconds of video (12 for a torrent)
  before it plays again. A short wait made the player stop every few seconds
  near the end of an episode.

## 2026-10-09: The list of streams

- The app reads the seeders, the resolution, HDR, the codec, the source, dual
  audio, the size, and the release group from the text of each stream. Each
  addon writes them in its own format, so the parser accepts the common
  forms.
- Torrents sort by seeders. Direct links keep the order of the addon.
- A badge shows each value that the app finds.
- "Reload streams" or Blue asks each addon again.
- A setting puts direct links above torrents. It is off by default.

## 2026-10-09: The next episode

- The next episode uses the addon of the current stream. In the answer of
  that addon, the app compares the binge group, the name, the resolution,
  the codec, the source, and the release group, and plays the closest
  stream.
- A countdown of 10 seconds shows at the start of the ending, or 20 seconds
  before the end. OK plays the next episode at once. Back stays.
- A setting skips intros by itself. It is off by default.
- When the viewer skips the ending, the episode is marked as watched.

## 2026-10-09: Text that is too long

- A name that does not fit scrolls while its card or row has the focus.
- A card has 12 dp of space under it, so that the focused card does not
  cover its title.

## 2026-10-09: Universal remote keys

- The app maps the keys of game controllers and keyboards to the remote
  keys. A or Enter is OK. B or Escape is Back. Start, X, or Tab is Menu. Y is
  Info. L1 and R1 are Channel down and up.
- A TV screen at the top of the app has a Menu button. Up reaches it. Down or
  Left goes back to the screen.
- A long press on OK in the player opens the options. OK also acts on a skip
  prompt.
- The player options have a row that changes the episode or the channel, for
  a remote with no channel keys.

## 2026-10-09: Home rows

- New, latest, trending, airing, and season rows come first. Popular and top
  rows come after them. Other rows keep the order of the source list.
- A catalog that needs an option, such as a genre or a year, shows a row for
  its first option. A catalog that needs a search text shows no row.
- A row that repeats 70% or more of a row above it does not show.
- Settings, Home rows turns each row on or off.

## 2026-10-09: Network data

- The app keeps a playlist for one hour and a TV guide for six hours in its
  cache, when the server gives no time. A guide can be many megabytes.
- Images go only into the image cache. The HTTP cache keeps only the answers
  of the addons, so that it does not hold a second copy of each image.
- A link that gives a web page shows a clear message in the player, and not
  only an error code.

## 2026-10-09: Play the best anime stream at once

- When the viewer opens an anime episode, the app plays the best stream when
  the addons answer, or after 10 seconds. A key press shows the list instead.
- The best stream is 1080p. With no 1080p stream, the nearest lower
  resolution comes next, then a higher one. At the same resolution, the
  setting "Direct links first" chooses the kind: on prefers direct links, off
  prefers torrents. A torrent with fewer than 3 seeders comes last.
- An anime has the type "anime" or an ID from Kitsu, MyAnimeList, AniList, or
  AniDB.
- After a stream that played at once, Back goes to the episodes. After a
  stream that the viewer chose, Back goes to the same row of the list.
- The list of streams of a video stays in memory for 15 minutes, so that Back
  does not ask the addons again.

## 2026-10-09: All the text of a stream

- Each row shows the title, the description, and the file name from the
  addon. On a TV, this text fills the right side of the row. The focused row
  shows all of it.

## 2026-10-09: Streams that drop

- A network error reconnects at the same position, up to 3 times in a row.
- Each part of a stream gets 6 tries. When a part of an HLS stream fails two
  times, the player takes it from a lower quality for one minute.
- The player reads video with no HTTP cache, so that video data does not push
  the answers of the addons out of the cache.
- When a stream still fails, the message shows the reason from the server.

## 2026-10-09: Play starts a stream at once

- This choice replaces "Play the best anime stream at once" for the way that
  a stream starts.
- Play on any movie or episode opens the player at once. The app asks each
  addon and plays the best stream: an ideal stream after 2.5 seconds, any
  stream after 8 seconds, or the best one when all addons answered.
- The player shows a short name of the stream at the top left for 8
  seconds, and each time the overlay shows: the name, the resolution, and the
  size.
- When the stream fails before its first frame, the player tries the next
  best stream, up to 5 of them.
- Yellow on the episodes, or while the app finds a stream, shows the list of
  streams.
- The player reads the first bytes of a link that does not show its kind. An
  HLS link that does not end in .m3u8 then plays as HLS.
- The list of streams is made one time for each answer of an addon. Before,
  each key press made it again, which took about 60 ms for 3000 streams on a
  computer, and more on a TV.

## 2026-10-12: Torrents that stream

- When the app chooses a torrent, a file that is too large to stream comes
  after a smaller one of the same resolution. The limit is 6 GB for a film
  and 2.5 GB for an episode, about 7 Mbit/s. A larger file needs more speed
  than most peers give, and the player stops again and again.
- The 4 MB after the read position download first. The rest of the window,
  and the head and the tail of the file, download only with spare speed.
  Before, about 135 pieces had the same priority, and the next piece came
  late.
- The window ahead is about 1/30 of the file, from 24 MB to 96 MB.
- One torrent gets the information and then the data, so the peers are found
  one time.
- The player shows the downloaded size of the file, such as "180 MB of
  1.5 GB". Before, the percent counted only the pieces that the engine asked
  for, not the file.

## 2026-10-12: Subtitles and links

- Each subtitle addon gets 4 seconds. A slow addon no longer hides the
  subtitles of the others.
- The player names the languages in the codes of OpenSubtitles, such as
  "pob" and "alb". The language of the viewer comes first, with at most 3
  tracks of each language.
- The player plays DASH links. A link that the player cannot read moves to
  the next stream, and does not stop the app.

## 2026-10-14: Resume, failed streams, and the next episode

- A saved position that is past the end of the source that plays now, less
  15 seconds, starts the video from the beginning. A stream that never played
  does not save a position, so its end does not mark the video as watched.
- A stream that does not start is remembered for one day. The app chooses
  it last, and the list marks it "DID NOT START". An addon with 3 such
  streams in one day comes after the other addons of the same resolution. A
  stream that plays clears the marks of its addon. The marks stay on the
  device.
- About one minute before the next episode countdown, the app finds the next
  episode from the same source. When the countdown ends, it plays at once.

## 2026-10-14: Subtitles for anime

- Subtitle addons such as OpenSubtitles know only IMDb IDs. For a title from
  Kitsu, the app also asks with the IMDb form of the ID, such as
  `tt2560140:1:1`, from the IMDb ID and the IMDb episode numbers of the title.

## 2026-10-14: Diagnostics

- The app keeps the last 500 events in memory: the answers of the addons and
  their times, the choice of auto play, the torrent details, slow torrent
  pieces, the player states, and the errors with their reasons.
- The phone page of Settings, Sources has a link to these events. The page
  needs the token of the QR code, like the source list.
- Nothing goes to a server, and nothing goes to a file.

## 2026-10-14: Updates inside the app

- When a newer release is out, Home and Settings show "Install the update".
  The app downloads the APK of the newest release and gives it to the
  package installer of Android, which asks the viewer to confirm.
- Android asks one time if ZettaStream can install apps. Android installs an
  update only when its signature is the same as the signature of the app.
- The Downloader code stays as a second way.

## 2026-10-14: Play next on the TV home screen

- The titles that the viewer did not finish show in the "Play next" row of
  the home screen of the TV, at most 10, newest first.
- A `zettastream://title` link opens the title in the app.
- When the viewer removes a title from the row, the app does not add it
  again until the viewer plays it again.

## 2026-10-14: Voice search

- The search screen has a Voice button. On that screen, the Search key of the
  remote control also starts voice search. The speech service of the device
  turns the words into text.

## 2026-10-14: Frame rate

- On a TV, the player switches the display to a mode of the same size with
  the lowest refresh rate that is a whole multiple of the frame rate of the
  video, such as 24 Hz for a film or 50 Hz for 25 fps. The mode goes back
  after playback.
- A video that does not tell its frame rate gets it from the times of its
  first 48 frames.
- A setting turns this off. The screen can go black for a second when the
  mode changes.

## 2026-10-14: Live TV favourites and guide grid

- Red on a channel adds it to the favourites, or removes it. A "Favourites"
  chip shows only them, and they come first in "All". They stay in the
  library.
- "Guide view" shows the guide as a time grid: three hours from the last
  half hour, one row for each channel. OK on a programme plays the channel.
- A playlist and a guide can write one channel ID in two forms, such as
  `News24.al@SD` and `News.24.al`. The app compares the letters and digits
  before the `@`.

## 2026-10-14: Backup

- The phone page has "Download a backup": one JSON file with the library,
  the favourite channels, the settings, and the source list.
- "Restore" sends a backup file to the TV, which replaces its data. A file
  that is not a ZettaStream backup changes nothing.
- The page needs the token of the QR code, like the source list.

## 2026-10-14: Interface overhaul after Miruro 2.0

- The interface follows the layout of Miruro 2.0 (the "Miruro 2.0" link of
  miruro.com): a slim icon bar on the left, a large banner on Home, and a
  title page with the art behind it.
- The colors stay black and white.
- All text uses Karla, the font of Miruro 2.0 (SIL Open Font License), in
  the APK. This replaces JetBrains Mono. The phone page keeps a monospace
  font only for the source list and the diagnostics.
- The icon bar is always on a TV screen. Left on the first item of a row
  goes into it, but only with a new press: a Left that moved inside the row
  less than 0.4 seconds before, or a held Left, stops at the edge. Right goes
  back to the item that had the focus. Right, Back, Menu, and `0` leave the
  bar. The bar does not keep the focus.
- A number key opens its screen and gives the focus to the screen, as a
  choice in the icon bar does.
- On a TV, the first button of the Home banner has the focus when the app
  starts. Back returns the focus to the button or the card that had it.
- Each card shows the kind and the year under the name, such as
  "Movie · 2026", and the IMDb rating on the art when the addon gives one.
- The title page shows the art at the top, the poster, the logo or the name,
  facts in small frames, the buttons, a grid of episode cards with an "EP"
  mark, and an About table. Down from the buttons goes to the episode that
  Watch starts.
- A chosen chip is white. Only the focused button or row is white on a TV.

## 2026-10-14: Test versions

- A test release has a label after the version, such as `1.0.0-rc1`. It comes
  before `1.0.0` and after `0.11.2`. `rc10` comes after `rc2`.
- GitHub does not give a pre-release as the latest release, so the app does
  not offer a test version as an update.

## 2026-10-14: The favourite key of a channel

- A favourite channel is kept by its guide ID, because the ID stays when the
  playlist changes the link.
- When two channels in a playlist have the same ID, such as an HD and an SD
  copy, each one is kept by its link. Then each copy is a favourite alone.

## 2026-10-14: Version numbers

- 1.0.0 is the first stable release.
- From 1.0.0, a fix raises the patch number, such as 1.0.1.
- A new function raises the minor number, such as 1.1.0.
- A change that an older version cannot read, such as a new form of the
  backup or of the source list, raises the major number.
- A test release has a label, such as `1.1.0-rc1`. See "Test versions".

## 2026-10-14: Home after 1.0.0

- Home has a "Your watchlist" row, newest first, under Continue watching. The
  type filter applies to it.
- Home shows a spinner until the art of the banner is there, at most for 6
  seconds, so the screen does not jump.
- When a button of the banner, or of the top of a title page, has the focus,
  the screen scrolls to the top.

## 2026-10-14: Performance

- The app has a startup profile. The `baselineprofile` module records it on
  an emulator with Android 13 or newer: a start, a move through the rows of
  Home, and a title page. The benchmark build starts with Cinemeta as its
  only source, so that Home has rows to record.
- A TV emulator with Android 12 cannot record it, because that needs root.
  The profile comes from a phone emulator. Most of the code is the same.
- On emulators, the start was about 4% faster with the profile, which is
  inside the noise. The real test is the first days after an install on the
  TV.
- The focus ring is one modifier node, not a composed modifier.
- A card asks metahub for the small poster (300 x 450), which is less than
  half the download of the medium poster. The banner keeps its size, because
  it fills the screen.

## 2026-10-15: Profiles

- The app starts on "Who is watching?", as on Netflix: round pictures with
  the names. The profile of the last time has the focus, so OK starts it.
- Each profile has its own library (history, watchlist, watched marks,
  favourite channels) and its own settings, in `files/profiles/<id>/`. The
  sources are the same for all profiles.
- On the first start of this version, the library and the settings move into
  a first profile named "Main". Nothing is lost.
- At most 5 profiles. The last profile cannot be deleted. A profile has no
  PIN.
- The picture comes from AniList (anime characters) or TVmaze (TV characters
  and actors). Both are free and need no key. A search finds a character or
  an actor by name. With no picture, the circle shows the first letter.
- The picture at the top of the icon bar, and Settings, Switch profile, open
  the profiles again.

## 2026-10-15: A stream that stops in the middle

- A stream that played and then stops with an error: the player reconnects
  at the same position. Damaged data makes the second try jump 5 seconds
  ahead. Then the next best source goes on at the same position.
- The count of tries starts again only after 30 seconds of good playback past
  the fault, so a reconnect that fails at once does not repeat with no end.
- A manual choice from the list has no next source, so it shows the error.

## 2026-10-15: Explore more, and the number keys

- The last card of each Home row, and of "More like this", is "Explore
  more". It opens all the titles of that catalog in a grid, as in Search.
  Near the end of the grid, the next page loads with the `skip` value of the
  addon, until a page gives no new title.
- "More like this" is under the About table.
- This replaces the number keys of "Remote control buttons" and "Live TV":
  `4` opens Live TV and `5` opens Settings, in the order of the icon bar,
  where Settings is at the bottom. The phone tab bar has the same order.

## 2026-10-15: Search results

- Each addon answers in its own list. The app makes one list in the order of
  the source list, so the order does not depend on which addon is fast.
- Two entries are one title when they have the same ID, the same IMDb ID, or
  the same name, kind (film, or series and anime), and first year. The entry
  with an IMDb ID stays, because most stream addons know it. The facts that
  only the other entry has, such as a poster, go into it.
- An entry with no year is never joined by its name only, because two
  different titles can have one name.
- The best match comes first: the same name, then a name that starts with
  the search, then a name with all the words, then a name that contains the
  search. Accents, case, and signs do not count. A title that more addons
  give comes first among equal matches.
- The same search within 10 minutes answers from memory.

## 2026-10-15: Side bars

- A video of another shape than the screen, such as an old 4:3 anime, has
  bars at the sides. The setting "Side bars" chooses what fills them:
  "Ambient blur" (the default), "Black", or "Stretch to fill". Stretch
  shows all of the picture and changes its shape. "Zoom to fill" was removed
  on 2026-10-15. A saved choice that no longer exists reads as the default,
  and the other settings stay. It is in the
  player options and in Settings.
- Ambient blur copies the frame at 32 x 18 pixels two times a second,
  blurs the copy, and stretches it behind the video, with a dark layer. The
  video itself goes from the decoder to the screen as before. A blur of each
  frame on the graphics chip looks better, but it costs too much on a TV.
- The fill changes only when the colours change clearly, and with no fade,
  because each draw covers the full screen. On the emulator it costs about
  the same as black bars.
- A protected stream that does not let the app copy its frames keeps black
  bars.

## 2026-10-15: A shorter options panel

- The player options have no rows for the episode, the speed, or the
  source. Channel up and down change the episode, and Yellow chooses another
  source. The speed is always normal. This replaces the episode row of
  "Universal remote keys". Live TV keeps its channel row.

## 2026-10-15: Video filters

- The player options have a "Filter" row: None (the default), CRT, VHS,
  Vintage film, and Black and white. It is saved for each profile.
- The scanlines, the dark corners, and the grain are drawn over the video,
  as the subtitles are. They cost almost nothing. The grain moves 8 times a
  second.
- VHS, Vintage film, and Black and white also change the colours with one
  colour matrix on the graphics chip, through the effects of Media3. The
  row says "(more power)" for them. On the emulator they used about 40
  percent more of the processor. With None or CRT, the player gets no
  effects, and the video goes from the decoder to the screen as before.
- The video pipeline takes new effects only when it starts. A change during
  playback starts it again at the same position, with a short pause.
- With a colour filter, the player gives no video size, so the ambient side
  bars read the size of the video track.
- CRT draws one scanline for each 1/270 of the screen height, so the lines
  show from a sofa on a large TV. It also has strong dark corners, a round
  black tube edge, and a soft shine at the top.
- Once the effects ran, the video goes through them until the player
  closes, and they draw the picture in its own shape. So "Stretch to fill"
  is also an effect then, which stretches the picture to the shape of the
  screen. A change of the filter or of the side bars waits 0.8 seconds, so
  only the last choice restarts the video.


## 2026-10-22: The app runs on Android 7.0

- An abod Android TV runs Android 7.0 (API level 24). The app did not
  install on it, because `minSdk` was 26.
- `minSdk` is 24 now.
- Core library desugaring adds `java.time` to the app. Android 7 does not
  have it.
- The adaptive launcher icon is in `mipmap-anydpi-v26`. Android 7 uses the
  PNG icons in the other `mipmap` folders.
- Android 7.0 does not have the ISRG roots of Let's Encrypt. The network
  security config adds ISRG Root X1 and X2 from `res/raw`, next to the system
  roots. Without them, HTTPS fails to hosts such as `iptv-org.github.io`.
  Cinemeta and the other strem.io hosts use Google Trust Services, which
  Android 7.0 trusts.
- Android 7 ignores the weight of a variable font. Below API level 26 the font
  family has only the regular Karla, so Compose makes bold text itself.
- Picture-in-picture starts only on API levels 26 to 30. Android 7 has no
  small window.
- On Android 7 the update installer opens the security settings for unknown
  sources. It does not ask per app.
- libtorrent4j stays at 2.1.0-38. Version 2.1.0-39 needs API level 28, and
  its native library does not load on Android 7.
- The emulator for these checks is `android-24;default;arm64-v8a`. Google
  gives the Android TV 7 image only for x86, which does not run on the Mac.

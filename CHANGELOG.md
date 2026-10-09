<div align="center">
  <a href="README.md"><img src="assets/brand/icon-full.svg" alt="ZettaStream" height="44"></a>
</div>

# Changelog

Every release is written here, newest first. A version is `MAJOR.MINOR.PATCH`.
While the major number is 0, a change that a user can see raises the minor
number.

A tag such as `v0.1.0` builds the signed APK and attaches it to a release on
GitHub as `ZettaStream.apk`. The Downloader URL in the readme always gives the
newest one.

## 0.6.0 (2026-10-09)

- Play on a movie or an episode starts the best stream at once: 1080p first,
  direct links or torrents as "Direct links first" sets. The top left shows
  which source plays: its name, resolution, and size.
- When a stream fails at the start, the next best stream starts by itself.
- HLS links that do not end in .m3u8, such as many CNCVerse links, play. They
  gave "None of the available extractors" before.
- The list of streams no longer lags with addons that give many torrents.
- Yellow shows the list of streams, on the episodes and while a stream
  starts.

## 0.5.0 (2026-10-09)

- An anime episode plays the best stream at once: 1080p first, then the
  nearest quality. "Direct links first" in Settings chooses direct links or
  torrents. Press a key while the addons answer to choose from the list.
- Back after a stream that played at once goes to the episodes. Back after a
  stream that you chose goes to the same row of the list, and the list does
  not load again.
- Each stream shows all its text from the addon. On a TV, the text fills the
  right side of the row.
- A stream that drops reconnects at the same position. A part that fails
  comes from a lower quality. If the stream still fails, the message shows
  the reason.
- A long title no longer covers its second line.
- A screen that you open again starts fresh.

## 0.4.0 (2026-10-09)

### Streams

- Each stream shows badges for its resolution, HDR, codec, source, dual
  audio, size, and seeders.
- Torrents sort by seeders. A setting puts direct links above torrents.
- "Reload streams" or Blue asks each addon again.
- A link that gives a web page, or that the server refuses, shows a clear
  message in the player.

### Watching

- A countdown to the next episode shows at the start of the ending. OK plays
  the next episode at once.
- The next episode plays from the same source, with the same resolution and
  release group when the addon has it.
- A setting skips intros by itself. Skipping the ending marks the episode as
  watched.
- A stream near the end of an episode no longer stops every few seconds.

### Torrents

- A torrent downloads only a window ahead of playback, not the full file.
- A second torrent in the same session starts. Before, only the first one
  started.
- A torrent that the viewer leaves while it starts is removed.

### Interface

- New and trending rows come first on Home. A row that repeats a row above
  it does not show. Settings, Home rows turns each row on or off.
- A long name scrolls while its card has the focus.
- A Menu button at the top of the TV screen, a long press on OK for the
  player options, and an episode row in the options. A remote with no Menu
  key and no number keys can do each action.
- Game controllers and keyboards work.

### Fixes

- A bad request from a device on the network no longer stops the app while
  the Sources screen shows.
- Two saves at the same time no longer risk the library or the settings.
- Playlists and TV guides stay in the cache for some hours, and images are
  kept only once.

## 0.3.0 (2026-10-08)

- Text skips to the exact end of the opening of an anime episode. The times
  come from AniSkip, a free database that viewers make.
- A "Skip intro", "Skip ending", or "Skip recap" prompt shows while that part
  plays. On a phone, tap the prompt.
- With no known times, for example for most series, Text still jumps 85
  seconds.

## 0.2.3 (2026-10-08)

- The player options scroll, so that Speed and Source come into view on a TV.

## 0.2.2 (2026-10-08)

- A torrent that holds a full season plays the correct episode. The engine
  finds the file by the name that the addon gives, then by the episode number
  in the file names.

## 0.2.1 (2026-10-08)

- Subtitles have a style: text color, background opacity from none to solid,
  font, and edge. Change it in Settings or in the player options.
- The streams screen says that torrents are hidden only when the setting
  hides them.

## 0.2.0 (2026-10-08)

### Torrents

- Plays torrent streams. A libtorrent engine downloads the file in order and
  plays it while it downloads.
- Shows torrent streams by default, with a TORRENT label.
- Limits the upload to 4 KB/s by default, does not seed, and deletes each
  torrent when the player closes.
- Shows the peers, the speed, and the progress while a torrent loads.
- Asks the user to use a VPN at start. The app does not check for one.

### Player

- Menu or `0` in the player opens an options panel: subtitles, audio,
  subtitle size, speed, and source.
- Subtitle tracks show the language name, such as "English", and not a code.
- When no subtitle addon is in the list, the app gets subtitles from
  OpenSubtitles.

### Settings

- Adds viewer settings: subtitles on or off, the subtitle language, the
  subtitle size, the audio language, play the next episode, press the
  channel buttons twice, show torrent streams, share while streaming, and the
  VPN notice.

### Fixes

- Only Menu and `0` open the side menu. Left moves in a row.
- The side menu stays open while you move in it. Back, Menu, or `0` closes it.
- Back returns to the same scroll position and the same focused card.
- The type filter shows each kind one time, for example one "Sports".
- A landscape or square image gets a card of that shape.

## 0.1.2 (2026-10-08)

- Shows the English name of an anime when the addon gives one.
- Numbers the episodes of an anime season correctly: season 2 of a show
  shows as S2, not as S1.
- Adds an "All seasons" button to an anime season, which opens the whole
  show under one name.
- Shows a title name on up to two lines.
- The next episode plays from the same source, with no stop at the list of
  streams.
- The channel buttons in the player show a small prompt first. A second
  press changes the episode or the channel.
- Each focused item in a row stops at the same place, so rows do not wobble.
- The search field has one border, and Down moves from it to the results.

## 0.1.1 (2026-10-08)

- A selected chip is grey now. Only the focused control is white, so the
  focus is clear on a TV.
- The streams screen writes "1 stream" and "1 addon" correctly.

## 0.1.0 (2026-10-08)

The first release. It is tested on an Android TV 12 emulator and an Android 15
phone emulator. The Hitachi TV with Android TV 11 is the next test.

### Sources

- Reads a list of URLs, one on each line: Stremio addons, M3U playlists, and
  XMLTV guides.
- On a TV, takes the list from a phone through a QR code and a page on the
  local network. Each code works one time.
- The new list replaces the old list.
- Shows the state of each source, and only the host of each URL.
- Tries the addons again when the app comes back after a start with no
  network.

### Watching

- Shows a row of posters for each catalog of each addon, with a filter by
  type.
- Shows a title with its seasons and episodes, and the episode to play next.
- Asks all addons for streams at the same time, and hides streams that are
  only torrents.
- Plays HTTP and HLS streams with the headers that the addon gives.
- Loads subtitles from the stream and from subtitle addons.
- Keeps continue watching, a watchlist, and watched marks on the device.
- Plays live channels, with now and next from the guide.
- Searches each catalog that supports a search.

### The remote control

- Opens a hidden side menu with Left at the edge, Back, Menu, or `0`.
- Gives each number, colour, channel, info, subtitle, language, and text
  button a job. See the readme.
- Has a test screen that shows each button that reaches the app.

### The look

- Black and white, with JetBrains Mono, and a focus ring that follows the
  edge of each control.
- A black and white icon and TV banner.

### Updates

- Asks GitHub for the newest release at start, and shows the Downloader URL
  when a newer one exists.

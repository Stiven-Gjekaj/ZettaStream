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

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

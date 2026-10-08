<div align="center">

<img src="assets/brand/icon-full.svg" alt="ZettaStream" width="160">

### Anime, movies, series, and live TV in one app for your Android TV and phone

_Plays your Stremio addons and IPTV playlists, made for a remote control_

<p align="center">
  <img src="https://img.shields.io/badge/Android-8.0%2B-000000?style=for-the-badge&logo=android&logoColor=white" alt="Android 8.0 or newer"/>
  <img src="https://img.shields.io/badge/Android_TV-ready-000000?style=for-the-badge&logo=androidtv&logoColor=white" alt="Ready for Android TV"/>
  <img src="https://img.shields.io/badge/Kotlin-Compose-000000?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin and Jetpack Compose"/>
</p>

<p align="center">
  <a href="https://github.com/Stiven-Gjekaj/ZettaStream/releases/latest"><img src="https://img.shields.io/github/v/release/Stiven-Gjekaj/ZettaStream?style=for-the-badge&color=ffffff&labelColor=000000&label=Download" alt="Download the newest release"/></a>
  <a href="https://ko-fi.com/stivengjekaj"><img src="https://img.shields.io/badge/Ko--fi-Support_this_project-000000?style=for-the-badge&logo=ko-fi&logoColor=white" alt="Support this project on Ko-fi"/></a>
</p>

<p align="center">
  <a href="https://github.com/Stiven-Gjekaj/ZettaStream/actions/workflows/ci.yml"><img src="https://github.com/Stiven-Gjekaj/ZettaStream/actions/workflows/ci.yml/badge.svg" alt="CI"/></a>
  <img src="https://img.shields.io/badge/license-MIT-black?style=flat-square" alt="MIT License"/>
</p>

<p align="center">
  <a href="#install-on-a-tv"><b>Install</b></a> |
  <a href="#add-your-sources"><b>Sources</b></a> |
  <a href="#the-remote-control"><b>Remote control</b></a> |
  <a href="#how-it-works"><b>How it works</b></a> |
  <a href="#documentation"><b>Documentation</b></a>
</p>

</div>

---

## Overview

**ZettaStream** is a stream aggregator for Android TV and Android phones. It
shows the catalogs of your Stremio addons as rows of posters, finds the
streams for a title in all your addons at the same time, and plays them. It
also plays IPTV playlists as live TV, with a guide.

The app comes with no sources. You add your own list one time, and on a TV you
do not type it with the remote control: you scan a QR code with your phone and
paste the list there.

It is made for a remote control first. A side menu stays hidden until you need
it, and each button on the remote has a job. On a phone, the same app has a
tab bar and touch controls.

## Features

<table>
<tr>
<td width="50%" valign="top">

### Watching

- Rows of posters from the catalogs of each addon
- Streams from all addons at the same time, with badges for quality, HDR,
  and dual audio, and torrents sorted by seeders
- Torrent streams, played while they download, with no seeding after
- Seasons and episodes, with the next episode to watch
- A countdown to the next episode, from the same source and quality
- Continue watching, a watchlist, and watched marks, on the device only
- Subtitles from the stream and from subtitle addons
- Change the subtitle and the audio track with one button
- Viewer settings: subtitles, languages, subtitle size, next episode
- An options panel in the player: Menu, `0`, or hold OK
- Skip the intro of an anime episode at the exact second, from AniSkip
- Subtitles from OpenSubtitles when no subtitle addon is in your list
- Live TV from M3U playlists, with now and next from an XMLTV guide

</td>
<td width="50%" valign="top">

### Made for a TV

- A side menu that stays hidden until you press Menu or `0`, or select the
  Menu button at the top
- Works with a game controller or a keyboard
- Turn each home row on or off, with new releases first
- Number buttons go straight to each screen
- Colour buttons for the watchlist, watched, sources, and filters
- Channel buttons for the next episode or the next channel
- A white ring that follows the edge of the focused item
- Add sources from a phone with a QR code
- A test screen that shows each button of your remote

</td>
</tr>
</table>

## Install on a TV

ZettaStream is not in the Play Store. Install it with the free
[Downloader](https://www.aftvnews.com/downloader/) app by AFTVnews.

1. On the TV, install **Downloader** from the Play Store.
2. In the settings of the TV, permit Downloader to install unknown apps.
3. Open Downloader, and enter the code **`8764108`**.
4. Downloader shows the real URL for a moment. Make sure that it is
   `github.com/Stiven-Gjekaj/ZettaStream`.
5. Install the APK, then open ZettaStream from the list of apps.

The code opens this URL, which you can also type:

```
https://github.com/Stiven-Gjekaj/ZettaStream/releases/latest/download/ZettaStream.apk
```

The code always gives the newest release. To update, enter the same code
again and install over the old version.
The home screen tells you when a newer version is available.

## Install on a phone

Open the [newest release](https://github.com/Stiven-Gjekaj/ZettaStream/releases/latest)
on the phone, download `ZettaStream.apk`, and open it. Android asks you to
permit the browser to install apps.

## Add your sources

ZettaStream reads three kinds of line. Put one URL on each line:

| Line | Example | Gives |
| ---- | ------- | ----- |
| A Stremio addon | `https://.../manifest.json` | Catalogs, titles, streams, subtitles |
| An IPTV playlist | `https://.../playlist.m3u` | Live channels |
| A TV guide | `https://.../guide.xml.gz` | What is on now and next |

**On a TV:**

1. Open the menu, then Settings, then Sources. The TV shows a QR code.
2. Scan the code with your phone. The phone must be on the same Wi-Fi.
3. Paste your list, and press Send.

**On a phone:** open Settings, then Sources, paste the list, and press Save.

The new list replaces the old list. To remove a source, remove its line and
send the list again. Each QR code works one time.

The order of the lines sets the order of the rows on the home screen. Put the
addon whose catalogs you like most first.

A metadata addon such as Cinemeta gives the rows on the home screen. A stream
addon gives the streams. ZettaStream plays HTTP streams and torrent streams.

## Torrents and your IP address

A torrent stream connects your TV to other people's devices. They can see your
IP address. ZettaStream limits the upload to 4 KB/s, does not seed, and deletes
each torrent when you stop watching, but it cannot hide your IP address. Use a
VPN that permits P2P. The app reminds you at start; it does not check.

To avoid torrents, turn off Settings, Show torrent streams. A debrid key in a
torrent addon's settings also changes its torrents into HTTP links.

## The remote control

| Button | Anywhere | In the player |
| ------ | -------- | ------------- |
| Directions, OK | Move, select | Left and Right: back or forward 10 s. OK: pause |
| Back | Go back. Close the menu | Leave the player |
| Menu, `0` | Open or close the menu | Open or close the options |
| Hold OK | | Open the options |
| `1` to `5` | Home, Search, Library, Settings, Live TV | `1` to `9`: go to 10% to 90% |
| Info | Open the focused title | Show the title and the time |
| Guide | Live TV | |
| Red | Add to the watchlist, or remove | |
| Green | Mark as watched | |
| Yellow | Choose a source | Choose another source |
| Blue | Filter the home screen | |
| Channel up and down | Scroll | Press twice: next or previous episode or channel |
| Subtitles | | Next subtitle track |
| Language | | Next audio track |
| Text | | Skip the intro, ending, or recap (anime). Otherwise jump 85 s |
| OK at a prompt | | Skip the part, or play the next episode now |

A remote with no Menu key and no number keys can still do each action. Press
Up to reach the Menu button at the top of the screen. In the player, hold OK
for the options, which also change the episode or the channel.

A game controller and a keyboard also work. A or Enter is OK. B or Escape is
Back. Start, X, or Tab is Menu. Y is Info. L1 and R1 are Channel down and up.

Some TVs keep a button for themselves. Settings, Remote control test shows
each button that reaches the app.

## How it works

```
your list -> addons -> catalogs -> a title -> streams from each addon -> the player
```

ZettaStream speaks the open Stremio addon protocol. An addon is a small web
server that answers requests in JSON. When you open an episode, the app asks
each of your addons for streams at the same time, and shows each answer as it
arrives. When an addon repairs a broken site, your TV gets the repair at once,
because the work happens on the addon's side.

The app runs no code from a source. It sends HTTP requests and plays what
comes back. For a torrent, a libtorrent engine downloads the file in order and
serves it to the player on `127.0.0.1`.

See [docs/architecture.md](docs/architecture.md) for the whole picture, and
[docs/decisions.md](docs/decisions.md) for the reason behind each choice.

## Project structure

```
app/src/main/kotlin/.../zettastream/
  addon/      the Stremio addon protocol
  iptv/       M3U playlists and XMLTV guides
  source/     the source list
  library/    the watchlist and the watch history
  pairing/    the page that a phone opens to send the list
  remote/     what each remote control button does
  torrent/    the torrent engine and its local server
  settings/   the viewer settings
  ui/         the screens, the theme, and the components
app/src/test/ the unit tests
assets/brand/ the icon and the TV banner
docs/         how the parts fit together, and why
```

## Documentation

<table>
<tr>
<td align="center" width="16%" valign="top">
<h3>Build</h3>
<p>How the parts<br/>fit together</p>
<a href="docs/architecture.md"><b>Architecture</b></a>
</td>
<td align="center" width="16%" valign="top">
<h3>Decide</h3>
<p>Each choice<br/>and its reason</p>
<a href="docs/decisions.md"><b>Decisions</b></a>
</td>
<td align="center" width="16%" valign="top">
<h3>Help</h3>
<p>When something<br/>does not play</p>
<a href="SUPPORT.md"><b>Support</b></a>
</td>
<td align="center" width="16%" valign="top">
<h3>Join in</h3>
<p>How to work<br/>on this</p>
<a href="CONTRIBUTING.md"><b>Contributing</b></a>
</td>
<td align="center" width="16%" valign="top">
<h3>Follow</h3>
<p>What changed,<br/>and when</p>
<a href="CHANGELOG.md"><b>Changelog</b></a>
</td>
<td align="center" width="16%" valign="top">
<h3>Terms</h3>
<p>What you agree<br/>to by using it</p>
<a href="TERMS.md"><b>Terms</b></a>
</td>
</tr>
</table>

## Building

You need JDK 21 and the Android SDK.

```
git clone https://github.com/Stiven-Gjekaj/ZettaStream
cd ZettaStream
./gradlew assembleDebug
```

The APK is in `app/build/outputs/apk/debug/`. A tag such as `v0.1.0` makes
the workflow build a signed APK and attach it to a release.

## Testing

```
./gradlew testDebugUnitTest lintDebug
```

The unit tests cover the addon protocol, the playlists and the guides, the
source list, the library, the remote control keys, the pairing page, the
torrent helpers, and the settings. The
tests of the addon client talk to a local mock server, not to a real addon.
Lint must report no error and no warning. The workflow runs both on each push.

The interface is tested by hand on an Android TV emulator with a D-pad, and
on a phone emulator. See [CONTRIBUTING.md](CONTRIBUTING.md).

## Contributing

Contributions are welcome. Start with [CONTRIBUTING.md](CONTRIBUTING.md),
follow the [Code of Conduct](CODE_OF_CONDUCT.md), and see
[SUPPORT.md](SUPPORT.md) if you need help. A contribution that adds a source,
or a link to a source, is refused.

## A note on rights

ZettaStream is a player. It contains no media and no sources, and the project
does not recommend any. You choose your sources, and you are responsible for
them and for what you watch. See [TERMS.md](TERMS.md).

## License

MIT. See [LICENSE](LICENSE) for the text, and [TERMS.md](TERMS.md) for the terms
of the project. JetBrains Mono is under the SIL Open Font License, in
[licenses/](licenses/JetBrainsMono-OFL.txt).

<div align="center">
<sub>Made for one Hitachi TV and one remote control. Press <code>0</code> for the menu.</sub>
</div>

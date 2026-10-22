<div align="center">
  <a href="README.md"><img src="assets/brand/icon-full.svg" alt="ZettaStream" height="44"></a>
</div>

# Changelog

Every release is written here, newest first. A version is `MAJOR.MINOR.PATCH`.
From 1.0.0, a fix raises the patch number, a new function raises the minor
number, and a change that breaks the backup or the source list raises the
major number. See "Version numbers" in the decisions.

A tag such as `v0.1.0` builds the signed APK and attaches it to a release on
GitHub as `ZettaStream.apk`. The Downloader URL in the readme always gives the
newest one.

## 1.6.0 (2026-10-22)

- ZettaStream runs on Android 7.0 and newer. Before, it needed Android 8.0.
- HTTPS works on Android 7.0 to hosts with Let's Encrypt certificates, for
  example many IPTV playlists.
- Torrent streams work on Android 7.

## 1.5.1 (2026-10-15)

- The CRT filter is much stronger: large scanlines, dark corners, and the
  round edge of a tube screen.
- Fixes "Stretch to fill", which stopped working after a colour filter.
- A quick change through many filters restarts the video one time only.

## 1.5.0 (2026-10-15)

- Video filters in the player options: CRT, VHS, Vintage film, and Black and
  white. VHS, Vintage film, and Black and white use more power.
- Side bars has no "Zoom to fill" now. A saved Zoom changes to Ambient blur.

## 1.4.2 (2026-10-15)

- The player options are shorter: no rows for the episode, the speed, or
  the source. Channel up and down change the episode, and Yellow chooses
  another source.

## 1.4.1 (2026-10-15)

- Side bars has a new choice, "Stretch to fill": the full picture fills the
  screen, wider than its true shape.

## 1.4.0 (2026-10-15)

- Side bars: the bars beside a 4:3 video, such as an old anime, take a soft
  blur of the colours of the picture, as in the ambient mode of YouTube.
  The player options and Settings can change it to black bars or a zoom.

## 1.3.3 (2026-10-15)

- Fixes a freeze and a crash of search with a long source list. The merge of
  the results was too slow for a TV, and it ran on the main thread. It is
  now about 17 times faster and runs in the background.
- The answers of the addons are read in the background, so Home and the
  stream list also stay smooth while they load.

## 1.3.2 (2026-10-15)

- Profile pictures show the whole face in the circle. Before, the top of the
  picture showed, and some faces were cut at the chin or the eyes.

## 1.3.1 (2026-10-15)

- Search shows the same title from many addons one time, and the best match
  comes first. A repeated search answers at once.

## 1.3.0 (2026-10-15)

- "Explore more" at the end of each row opens all the titles of the row in a
  grid, and more titles load as you scroll.
- "More like this" is under About, and it also ends with "Explore more".
- `4` now opens Live TV and `5` opens Settings, in the order of the icon bar.

## 1.2.0 (2026-10-15)

- Profiles: the app starts on "Who is watching?". Each profile has its own
  picture, history, watchlist, and settings. Pictures of anime characters
  come from AniList, and of TV characters and actors from TVmaze.
- Your library and settings move into a first profile named "Main".
- The picture at the top of the icon bar opens the profiles.
- On Android 12 and newer phones, the small window opens by itself.

## 1.1.3 (2026-10-15)

- A stream that stops in the middle of a video no longer ends playback. The
  player reconnects, jumps past damaged data, and then goes on with the next
  best source at the same position.
- A reconnect that fails at the same place no longer repeats with no end.
- The player options have a subtitle delay and an audio delay.
- Home has a "New episodes" row for the shows that you follow.
- Search shows your recent searches. Home rows can move up and down.
- A title page has a Trailer button, "More like this", the director, and
  the cast.
- On a phone, the video goes on in a small window when you leave the app.
  On a TV, the sound stops when another app comes in front.

## 1.1.2 (2026-10-14)

- The facts of a title have a dark fill, so they are easy to read on bright
  art.
- A screen that opens from the icon bar starts at its first item.
- On the search screen, a number key of the remote opens its screen and does
  not type a digit.

## 1.1.1 (2026-10-14)

- A startup profile: Android compiles the code for the start and for Home
  ahead of time.
- Cards download a smaller poster, and the focus ring does less work.

## 1.1.0 (2026-10-14)

- Home has a "Your watchlist" row under Continue watching.
- Home waits for the art of the banner, and the whole banner shows when one
  of its buttons has the focus. Red and Info act on the banner title.
- On a title page, Up from the episodes shows the title again.
- The icon bar no longer opens for a moment when you open a title.
- The search icon has space from the frame of the field.

## 1.0.0 (2026-10-14)

The first stable release. It holds all the work of 0.1.0 to 0.11.1.

- Stremio addons: rows of posters, search, title pages with seasons and
  episodes, and streams from all addons at the same time.
- Play starts the best 1080p stream at once and shows which source plays.
  When a stream fails, the next best one starts. Yellow shows the list.
- Torrents play while they download, in order, with no seeding after.
- Subtitles from the stream, from subtitle addons, and from OpenSubtitles.
  Viewer settings for the size, the color, the background, and the font.
- The next episode, from the same source, with a countdown. Skip the intro of
  an anime episode from AniSkip.
- Live TV from M3U playlists, with now and next, a guide grid, and favourite
  channels.
- Continue watching, a watchlist, watched marks, and the Play next row of the
  TV. A backup and a restore from the phone page.
- Made for a remote control: an icon bar, number keys, colour keys, and a
  remote control test. A game controller and a keyboard also work.
- Add sources from a phone with a QR code. A diagnostics page on the phone.
- Updates install from inside the app. Voice search. The TV switches to the
  frame rate of the video.
- The interface follows Miruro 2.0, in black and white, with the Karla font.

## 0.11.1 (2026-10-14)

- On a phone, the facts and the buttons of a title wrap to a new line.
- Live TV takes the focus after its channels load, also when a number key
  opens it.
- An HD and an SD copy of a channel are each a favourite alone.

## 0.11.0 (2026-10-14)

- A new interface after Miruro 2.0, in black and white, with the Karla font.
- A slim icon bar at the left of the TV screen. A fast run of Left presses
  stops at the edge of a row. A new Left press goes into the bar.
- Home starts with a large banner of featured titles. Each card shows the
  kind, the year, and the rating.
- The title page shows the art, the facts, a grid of episode cards, and an
  About table.
- One style for the chips, the settings rows, the channel rows, and the
  stream rows. Search has a hint and a count of the results.
- Fixes: the focus after a number key, after Back from a Continue card, and
  after the start. Up and Down in the icon bar follow the screen.
- The update keeps running when you leave the screen, and a TV that cannot
  allow installs shows the Downloader code.
- A file that is not a backup no longer clears the library.
- Two feeds of one channel, such as East and West, keep their own guide.
- The phone page accepts a large body only with the token of the QR code.

## 0.10.0 (2026-10-14)

- Live TV: Red marks a favourite channel, a "Favourites" chip shows them,
  and "Guide view" shows the guide as a time grid.
- More channels get their guide: a playlist and a guide can write a channel
  ID in two forms, and the app matches both.
- The phone page can download a backup of the library, the favourites, the
  settings, and the sources, and restore it on any TV.

## 0.9.0 (2026-10-14)

- Install updates from inside the app: Home and Settings show "Install the
  update" when a new version is out. Android asks one time to allow it.
- Unfinished titles show in the Play next row of the TV home screen.
- Voice search: the Voice button, or the Search key of the remote on the
  search screen.
- The TV switches to the frame rate of the video, such as 24 Hz for a film.
  Turn it off in Settings.

## 0.8.0 (2026-10-14)

- OpenSubtitles gives subtitles for anime from Kitsu. The app asks with the
  IMDb form of the episode.
- A stream that did not start is chosen last for one day, and the list marks
  it "DID NOT START".
- The next episode is found a minute before the countdown, so it starts at
  once.
- A saved position past the end of a shorter source starts from the
  beginning, and the video is not marked as watched.
- The phone page in Settings, Sources has a diagnostics link with the last
  events of the TV, for a fault report.

## 0.7.0 (2026-10-12)

### Torrents

- The app prefers a torrent that is small enough to stream: at most 6 GB for
  a film and 2.5 GB for an episode. Very large files were the main cause of
  the stops.
- The next pieces download first. The rest of the window downloads with
  spare speed, and the window follows the file size.
- The torrent information and the data come from one torrent, so the peers
  are found one time.
- The player shows the downloaded size of the file, not a percent of the
  wanted pieces.

### Subtitles

- OpenSubtitles works again. A slow subtitle addon, such as CNCVerse, held
  back all subtitles for up to 23 seconds.
- Languages such as "pob" and "alb" show their names. Your language comes
  first, with at most 3 tracks of each language.

### Fixes

- A DASH link no longer closes the app. DASH links play.

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

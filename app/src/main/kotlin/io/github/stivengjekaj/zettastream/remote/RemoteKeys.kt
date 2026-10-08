package io.github.stivengjekaj.zettastream.remote

import android.view.KeyEvent

/** The actions that the remote control buttons do. See docs/decisions.md. */
enum class RemoteAction {
    ToggleMenu, Home, Search, Library, Settings, LiveTv,
    Info, Guide, Watchlist, Watched, Sources, Filter,
    PageUp, PageDown,
    PlayPause, Play, Pause, Next, Previous,
    Subtitles, AudioTrack, SkipIntro,
    Number1, Number2, Number3, Number4, Number5, Number6, Number7, Number8, Number9,
}

object RemoteKeys {
    /** The action of a key anywhere in the app, outside the player. */
    fun global(keyCode: Int): RemoteAction? = when (keyCode) {
        KeyEvent.KEYCODE_MENU, KeyEvent.KEYCODE_0, KeyEvent.KEYCODE_NUMPAD_0 -> RemoteAction.ToggleMenu
        KeyEvent.KEYCODE_1, KeyEvent.KEYCODE_NUMPAD_1 -> RemoteAction.Home
        KeyEvent.KEYCODE_2, KeyEvent.KEYCODE_NUMPAD_2 -> RemoteAction.Search
        KeyEvent.KEYCODE_3, KeyEvent.KEYCODE_NUMPAD_3 -> RemoteAction.Library
        KeyEvent.KEYCODE_4, KeyEvent.KEYCODE_NUMPAD_4 -> RemoteAction.Settings
        KeyEvent.KEYCODE_5, KeyEvent.KEYCODE_NUMPAD_5 -> RemoteAction.LiveTv
        KeyEvent.KEYCODE_INFO -> RemoteAction.Info
        KeyEvent.KEYCODE_GUIDE, KeyEvent.KEYCODE_TV_DATA_SERVICE -> RemoteAction.Guide
        KeyEvent.KEYCODE_PROG_RED -> RemoteAction.Watchlist
        KeyEvent.KEYCODE_PROG_GREEN -> RemoteAction.Watched
        KeyEvent.KEYCODE_PROG_YELLOW -> RemoteAction.Sources
        KeyEvent.KEYCODE_PROG_BLUE -> RemoteAction.Filter
        KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_PAGE_UP -> RemoteAction.PageUp
        KeyEvent.KEYCODE_CHANNEL_DOWN, KeyEvent.KEYCODE_PAGE_DOWN -> RemoteAction.PageDown
        else -> null
    }

    /** The action of a key in the player. */
    fun player(keyCode: Int): RemoteAction? = when (keyCode) {
        KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, KeyEvent.KEYCODE_HEADSETHOOK, KeyEvent.KEYCODE_SPACE -> RemoteAction.PlayPause
        KeyEvent.KEYCODE_MEDIA_PLAY -> RemoteAction.Play
        KeyEvent.KEYCODE_MEDIA_PAUSE -> RemoteAction.Pause
        KeyEvent.KEYCODE_CHANNEL_UP, KeyEvent.KEYCODE_MEDIA_NEXT -> RemoteAction.Next
        KeyEvent.KEYCODE_CHANNEL_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS -> RemoteAction.Previous
        KeyEvent.KEYCODE_CAPTIONS -> RemoteAction.Subtitles
        KeyEvent.KEYCODE_MEDIA_AUDIO_TRACK, KeyEvent.KEYCODE_LANGUAGE_SWITCH -> RemoteAction.AudioTrack
        KeyEvent.KEYCODE_TV_TELETEXT -> RemoteAction.SkipIntro
        KeyEvent.KEYCODE_INFO -> RemoteAction.Info
        KeyEvent.KEYCODE_PROG_YELLOW -> RemoteAction.Sources
        in KeyEvent.KEYCODE_1..KeyEvent.KEYCODE_9 -> RemoteAction.entries[RemoteAction.Number1.ordinal + keyCode - KeyEvent.KEYCODE_1]
        in KeyEvent.KEYCODE_NUMPAD_1..KeyEvent.KEYCODE_NUMPAD_9 ->
            RemoteAction.entries[RemoteAction.Number1.ordinal + keyCode - KeyEvent.KEYCODE_NUMPAD_1]
        else -> null
    }

    /** The percent of the video that a number key goes to: 1 is 10 percent. */
    fun percentOf(action: RemoteAction): Int? =
        if (action.ordinal in RemoteAction.Number1.ordinal..RemoteAction.Number9.ordinal)
            (action.ordinal - RemoteAction.Number1.ordinal + 1) * 10 else null
}

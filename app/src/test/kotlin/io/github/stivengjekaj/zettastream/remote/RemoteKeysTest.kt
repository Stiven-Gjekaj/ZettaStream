package io.github.stivengjekaj.zettastream.remote

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RemoteKeysTest {
    @Test
    fun theMenuButtonAndZeroOpenTheMenu() {
        assertEquals(RemoteAction.ToggleMenu, RemoteKeys.global(KeyEvent.KEYCODE_MENU))
        assertEquals(RemoteAction.ToggleMenu, RemoteKeys.global(KeyEvent.KEYCODE_0))
    }

    @Test
    fun theNumberKeysGoToTheScreens() {
        assertEquals(
            listOf(RemoteAction.Home, RemoteAction.Search, RemoteAction.Library, RemoteAction.Settings, RemoteAction.LiveTv),
            (KeyEvent.KEYCODE_1..KeyEvent.KEYCODE_5).map { RemoteKeys.global(it) },
        )
    }

    @Test
    fun theColourButtonsHaveTheirActions() {
        assertEquals(
            listOf(RemoteAction.Watchlist, RemoteAction.Watched, RemoteAction.Sources, RemoteAction.Filter),
            listOf(KeyEvent.KEYCODE_PROG_RED, KeyEvent.KEYCODE_PROG_GREEN, KeyEvent.KEYCODE_PROG_YELLOW, KeyEvent.KEYCODE_PROG_BLUE)
                .map { RemoteKeys.global(it) },
        )
    }

    @Test
    fun inThePlayerTheNumberKeysGoToAPercent() {
        val five = RemoteKeys.player(KeyEvent.KEYCODE_5)!!
        assertEquals(50, RemoteKeys.percentOf(five))
        assertEquals(90, RemoteKeys.percentOf(RemoteKeys.player(KeyEvent.KEYCODE_NUMPAD_9)!!))
        assertNull(RemoteKeys.percentOf(RemoteAction.PlayPause))
    }

    @Test
    fun inThePlayerTheSpecialKeysHaveTheirActions() {
        assertEquals(RemoteAction.Subtitles, RemoteKeys.player(KeyEvent.KEYCODE_CAPTIONS))
        assertEquals(RemoteAction.AudioTrack, RemoteKeys.player(KeyEvent.KEYCODE_MEDIA_AUDIO_TRACK))
        assertEquals(RemoteAction.AudioTrack, RemoteKeys.player(KeyEvent.KEYCODE_LANGUAGE_SWITCH))
        assertEquals(RemoteAction.SkipIntro, RemoteKeys.player(KeyEvent.KEYCODE_TV_TELETEXT))
        assertEquals(RemoteAction.Next, RemoteKeys.player(KeyEvent.KEYCODE_CHANNEL_UP))
    }

    @Test
    fun theDirectionKeysHaveNoActionHere() {
        assertNull(RemoteKeys.global(KeyEvent.KEYCODE_DPAD_LEFT))
        assertNull(RemoteKeys.player(KeyEvent.KEYCODE_DPAD_CENTER))
    }

    @Test
    fun controllerAndKeyboardKeysBecomeRemoteKeys() {
        assertEquals(KeyEvent.KEYCODE_DPAD_CENTER, RemoteKeys.translate(KeyEvent.KEYCODE_BUTTON_A))
        assertEquals(KeyEvent.KEYCODE_BACK, RemoteKeys.translate(KeyEvent.KEYCODE_BUTTON_B))
        assertEquals(KeyEvent.KEYCODE_BACK, RemoteKeys.translate(KeyEvent.KEYCODE_ESCAPE))
        assertEquals(KeyEvent.KEYCODE_MENU, RemoteKeys.translate(KeyEvent.KEYCODE_BUTTON_START))
        assertEquals(KeyEvent.KEYCODE_CHANNEL_UP, RemoteKeys.translate(KeyEvent.KEYCODE_BUTTON_R1))
        assertNull(RemoteKeys.translate(KeyEvent.KEYCODE_DPAD_UP))
    }
}

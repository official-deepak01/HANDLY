package com.example.data

import com.example.model.ActionType
import com.example.model.GestureProfile
import com.example.model.GestureType
import com.example.model.ProfileMapping

object DefaultProfiles {

    fun getDefaultMappings(): Map<GestureType, ProfileMapping> {
        return mapOf(
            GestureType.OPEN_PALM to ProfileMapping(GestureType.OPEN_PALM, ActionType.NONE, isEnabled = true),
            GestureType.FIST to ProfileMapping(GestureType.FIST, ActionType.BACK, isEnabled = true),
            GestureType.SWIPE_UP to ProfileMapping(GestureType.SWIPE_UP, ActionType.HOME, isEnabled = true),
            GestureType.SWIPE_DOWN to ProfileMapping(GestureType.SWIPE_DOWN, ActionType.RECENTS, isEnabled = true),
            GestureType.SWIPE_LEFT to ProfileMapping(GestureType.SWIPE_LEFT, ActionType.BACK, isEnabled = true),
            GestureType.SWIPE_RIGHT to ProfileMapping(GestureType.SWIPE_RIGHT, ActionType.SCROLL_FORWARD, isEnabled = true),
            GestureType.THUMBS_UP to ProfileMapping(GestureType.THUMBS_UP, ActionType.VOLUME_UP, isEnabled = true),
            GestureType.THUMBS_DOWN to ProfileMapping(GestureType.THUMBS_DOWN, ActionType.VOLUME_DOWN, isEnabled = true),
            GestureType.INDEX_POINT to ProfileMapping(GestureType.INDEX_POINT, ActionType.TAP, isEnabled = true),
            GestureType.TWO_FINGERS to ProfileMapping(GestureType.TWO_FINGERS, ActionType.NEXT_TRACK, isEnabled = true),
            GestureType.PINCH to ProfileMapping(GestureType.PINCH, ActionType.TAP, isEnabled = true),
            GestureType.OK_SIGN to ProfileMapping(GestureType.OK_SIGN, ActionType.NOTIFICATIONS, isEnabled = true)
        )
    }

    val GENERAL_PHONE = GestureProfile(
        id = "profile_general",
        name = "General Phone",
        description = "Standard system navigation and reading gestures for daily use.",
        isBuiltIn = true,
        packageName = null,
        mappings = getDefaultMappings()
    )

    val YOUTUBE = GestureProfile(
        id = "profile_youtube",
        name = "YouTube",
        description = "Optimized for full-screen video playback, scrolling recommendations, and volume.",
        isBuiltIn = true,
        packageName = "com.google.android.youtube",
        mappings = getDefaultMappings().toMutableMap().apply {
            put(GestureType.THUMBS_UP, ProfileMapping(GestureType.THUMBS_UP, ActionType.PLAY_PAUSE, true))
            put(GestureType.TWO_FINGERS, ProfileMapping(GestureType.TWO_FINGERS, ActionType.NEXT_TRACK, true))
            put(GestureType.INDEX_POINT, ProfileMapping(GestureType.INDEX_POINT, ActionType.VOLUME_UP, true))
            put(GestureType.THUMBS_DOWN, ProfileMapping(GestureType.THUMBS_DOWN, ActionType.VOLUME_DOWN, true))
            put(GestureType.SWIPE_UP, ProfileMapping(GestureType.SWIPE_UP, ActionType.SCROLL_FORWARD, true))
            put(GestureType.SWIPE_DOWN, ProfileMapping(GestureType.SWIPE_DOWN, ActionType.SCROLL_BACKWARD, true))
        }
    )

    val INSTAGRAM = GestureProfile(
        id = "profile_instagram",
        name = "Instagram",
        description = "Seamless scrolling for Reels, Feeds, and Stories hands-free.",
        isBuiltIn = true,
        packageName = "com.instagram.android",
        mappings = getDefaultMappings().toMutableMap().apply {
            put(GestureType.SWIPE_UP, ProfileMapping(GestureType.SWIPE_UP, ActionType.TOUCH_SWIPE_UP, true))
            put(GestureType.SWIPE_DOWN, ProfileMapping(GestureType.SWIPE_DOWN, ActionType.TOUCH_SWIPE_DOWN, true))
            put(GestureType.PINCH, ProfileMapping(GestureType.PINCH, ActionType.TAP, true))
            put(GestureType.THUMBS_UP, ProfileMapping(GestureType.THUMBS_UP, ActionType.TAP, true))
        }
    )

    val TIKTOK = GestureProfile(
        id = "profile_tiktok",
        name = "TikTok",
        description = "Effortless hands-free video swiping and playback control.",
        isBuiltIn = true,
        packageName = "com.zhiliaoapp.musically",
        mappings = getDefaultMappings().toMutableMap().apply {
            put(GestureType.SWIPE_UP, ProfileMapping(GestureType.SWIPE_UP, ActionType.TOUCH_SWIPE_UP, true))
            put(GestureType.SWIPE_DOWN, ProfileMapping(GestureType.SWIPE_DOWN, ActionType.TOUCH_SWIPE_DOWN, true))
            put(GestureType.THUMBS_UP, ProfileMapping(GestureType.THUMBS_UP, ActionType.PLAY_PAUSE, true))
            put(GestureType.PINCH, ProfileMapping(GestureType.PINCH, ActionType.TAP, true))
        }
    )

    val SNAPCHAT = GestureProfile(
        id = "profile_snapchat",
        name = "Snapchat",
        description = "Navigate stories and spotlight with quick swipes and taps.",
        isBuiltIn = true,
        packageName = "com.snapchat.android",
        mappings = getDefaultMappings().toMutableMap().apply {
            put(GestureType.SWIPE_RIGHT, ProfileMapping(GestureType.SWIPE_RIGHT, ActionType.TOUCH_SWIPE_RIGHT, true))
            put(GestureType.SWIPE_LEFT, ProfileMapping(GestureType.SWIPE_LEFT, ActionType.TOUCH_SWIPE_LEFT, true))
            put(GestureType.PINCH, ProfileMapping(GestureType.PINCH, ActionType.TAP, true))
        }
    )

    val MEDIA = GestureProfile(
        id = "profile_media",
        name = "Media & Music",
        description = "Dedicated music and podcast playback and volume adjustments.",
        isBuiltIn = true,
        packageName = null,
        mappings = getDefaultMappings().toMutableMap().apply {
            put(GestureType.THUMBS_UP, ProfileMapping(GestureType.THUMBS_UP, ActionType.PLAY_PAUSE, true))
            put(GestureType.TWO_FINGERS, ProfileMapping(GestureType.TWO_FINGERS, ActionType.NEXT_TRACK, true))
            put(GestureType.INDEX_POINT, ProfileMapping(GestureType.INDEX_POINT, ActionType.PREV_TRACK, true))
            put(GestureType.SWIPE_UP, ProfileMapping(GestureType.SWIPE_UP, ActionType.VOLUME_UP, true))
            put(GestureType.SWIPE_DOWN, ProfileMapping(GestureType.SWIPE_DOWN, ActionType.VOLUME_DOWN, true))
        }
    )

    val CAMERA = GestureProfile(
        id = "profile_camera",
        name = "Camera",
        description = "Trigger photo captures via standard hardware volume shutter.",
        isBuiltIn = true,
        packageName = "com.android.camera",
        mappings = getDefaultMappings().toMutableMap().apply {
            put(GestureType.TWO_FINGERS, ProfileMapping(GestureType.TWO_FINGERS, ActionType.CAMERA_SHUTTER, true))
            put(GestureType.PINCH, ProfileMapping(GestureType.PINCH, ActionType.CAMERA_SHUTTER, true))
            put(GestureType.THUMBS_UP, ProfileMapping(GestureType.THUMBS_UP, ActionType.CAMERA_SHUTTER, true))
        }
    )

    val CHROME = GestureProfile(
        id = "profile_chrome",
        name = "Chrome / Browser",
        description = "Smooth reading scroll, page navigation, and link clicks hands-free.",
        isBuiltIn = true,
        packageName = "com.android.chrome",
        mappings = getDefaultMappings().toMutableMap().apply {
            put(GestureType.SWIPE_UP, ProfileMapping(GestureType.SWIPE_UP, ActionType.SCROLL_FORWARD, true))
            put(GestureType.SWIPE_DOWN, ProfileMapping(GestureType.SWIPE_DOWN, ActionType.SCROLL_BACKWARD, true))
            put(GestureType.SWIPE_LEFT, ProfileMapping(GestureType.SWIPE_LEFT, ActionType.BACK, true))
            put(GestureType.PINCH, ProfileMapping(GestureType.PINCH, ActionType.TAP, true))
            put(GestureType.INDEX_POINT, ProfileMapping(GestureType.INDEX_POINT, ActionType.SCROLL_FORWARD, true))
            put(GestureType.TWO_FINGERS, ProfileMapping(GestureType.TWO_FINGERS, ActionType.SCROLL_BACKWARD, true))
        }
    )

    val WHATSAPP = GestureProfile(
        id = "profile_whatsapp",
        name = "WhatsApp",
        description = "Read messages, scroll chats, and manage audio/calls without touching screen.",
        isBuiltIn = true,
        packageName = "com.whatsapp",
        mappings = getDefaultMappings().toMutableMap().apply {
            put(GestureType.SWIPE_UP, ProfileMapping(GestureType.SWIPE_UP, ActionType.SCROLL_FORWARD, true))
            put(GestureType.SWIPE_DOWN, ProfileMapping(GestureType.SWIPE_DOWN, ActionType.SCROLL_BACKWARD, true))
            put(GestureType.PINCH, ProfileMapping(GestureType.PINCH, ActionType.TAP, true))
            put(GestureType.THUMBS_UP, ProfileMapping(GestureType.THUMBS_UP, ActionType.PLAY_PAUSE, true))
            put(GestureType.FIST, ProfileMapping(GestureType.FIST, ActionType.BACK, true))
        }
    )

    val GALLERY = GestureProfile(
        id = "profile_gallery",
        name = "Gallery / Photos",
        description = "Flip through photo collections, zoom/tap pictures hands-free.",
        isBuiltIn = true,
        packageName = "com.google.android.apps.photos",
        mappings = getDefaultMappings().toMutableMap().apply {
            put(GestureType.SWIPE_LEFT, ProfileMapping(GestureType.SWIPE_LEFT, ActionType.TOUCH_SWIPE_LEFT, true))
            put(GestureType.SWIPE_RIGHT, ProfileMapping(GestureType.SWIPE_RIGHT, ActionType.TOUCH_SWIPE_RIGHT, true))
            put(GestureType.PINCH, ProfileMapping(GestureType.PINCH, ActionType.TAP, true))
            put(GestureType.TWO_FINGERS, ProfileMapping(GestureType.TWO_FINGERS, ActionType.DOUBLE_TAP, true))
        }
    )

    fun getAllBuiltInProfiles(): List<GestureProfile> {
        return listOf(
            GENERAL_PHONE,
            CHROME,
            WHATSAPP,
            GALLERY,
            YOUTUBE,
            INSTAGRAM,
            TIKTOK,
            SNAPCHAT,
            MEDIA,
            CAMERA
        )
    }
}

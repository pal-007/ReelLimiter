package com.reellimiter.app

import android.view.accessibility.AccessibilityNodeInfo

/**
 * Decides whether the foreground screen of Instagram / YouTube is currently a Reels / Shorts player.
 *
 * IMPORTANT: Instagram and YouTube rename their internal view IDs between app versions. If counting
 * stops working after an app update, inspect the screen with Android Studio's Layout Inspector or
 * `uiautomatorviewer`, find the id of the vertical Reels/Shorts pager, and update the lists below.
 */
object ReelDetector {

    private val INSTAGRAM_IDS = listOf(
        "com.instagram.android:id/clips_viewer_view_pager",
        "com.instagram.android:id/clips_viewer_root",
        "com.instagram.android:id/clips_swipe_refresh_container"
    )

    private val YOUTUBE_IDS = listOf(
        "com.google.android.youtube:id/reel_recycler",
        "com.google.android.youtube:id/reel_player_page_container",
        "com.google.android.youtube:id/shorts_player_container"
    )

    fun isReelScreen(app: TrackedApp, root: AccessibilityNodeInfo): Boolean {
        val ids = when (app) {
            TrackedApp.INSTAGRAM -> INSTAGRAM_IDS
            TrackedApp.YOUTUBE -> YOUTUBE_IDS
        }
        if (ids.any { root.findAccessibilityNodeInfosByViewId(it).isNotEmpty() }) return true

        // Fallback for Instagram: the "Reels" tab in the bottom bar is selected.
        if (app == TrackedApp.INSTAGRAM) {
            return root.findAccessibilityNodeInfosByText("Reels").any { node ->
                node.isSelected || node.parent?.isSelected == true
            }
        }
        return false
    }
}

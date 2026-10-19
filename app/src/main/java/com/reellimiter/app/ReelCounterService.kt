package com.reellimiter.app

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent

/**
 * Watches Instagram and YouTube. Each time a new Reel / Short is shown it bumps the counter.
 * Once the daily limit is hit, the next swipe (or reopening Reels/Shorts) sends the user home
 * and shows [BlockActivity].
 */
class ReelCounterService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())

    private var inReels: TrackedApp? = null
    private var pendingCount: Runnable? = null
    private var lastCheckAt = 0L
    private var lastBlockAt = 0L

    /** Polls while a reel session is active so we notice when the user leaves, even without events. */
    private val heartbeat = object : Runnable {
        override fun run() {
            val app = inReels ?: return
            val root = rootInActiveWindow
            if (root != null &&
                (root.packageName?.toString() != app.pkg || !ReelDetector.isReelScreen(app, root))
            ) {
                inReels = null
                return
            }
            handler.postDelayed(this, HEARTBEAT_MS)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val app = TrackedApp.fromPackage(event.packageName?.toString() ?: return) ?: return
        if (!LimitStore.isEnabled(this)) return

        val isScroll = event.eventType == AccessibilityEvent.TYPE_VIEW_SCROLLED
        val now = SystemClock.uptimeMillis()
        if (!isScroll && now - lastCheckAt < CHECK_THROTTLE_MS) return
        lastCheckAt = now

        val root = rootInActiveWindow ?: return
        if (root.packageName?.toString() != app.pkg || !ReelDetector.isReelScreen(app, root)) {
            if (inReels == app) inReels = null
            return
        }

        val newSession = inReels != app

        // Limit already hit: let the current reel finish, but stop on the next swipe or re-entry.
        if (LimitStore.isLimitReached(this, app) && (newSession || isScroll)) {
            block(app)
            return
        }

        if (newSession) {
            inReels = app
            handler.removeCallbacks(heartbeat)
            handler.postDelayed(heartbeat, HEARTBEAT_MS)
            if (!isScroll) registerReel(app) // the first reel you land on counts
        }
        if (isScroll && !newSession) scheduleCount(app)
    }

    /** A swipe fires many scroll events; wait until it settles and count it once. */
    private fun scheduleCount(app: TrackedApp) {
        pendingCount?.let { handler.removeCallbacks(it) }
        val r = Runnable {
            val root = rootInActiveWindow
            if (root != null &&
                root.packageName?.toString() == app.pkg &&
                ReelDetector.isReelScreen(app, root) &&
                !LimitStore.isLimitReached(this, app)
            ) {
                registerReel(app)
            }
        }
        pendingCount = r
        handler.postDelayed(r, SCROLL_SETTLE_MS)
    }

    private fun registerReel(app: TrackedApp) {
        LimitStore.increment(this, app)
    }

    private fun block(app: TrackedApp) {
        val now = SystemClock.uptimeMillis()
        if (now - lastBlockAt < BLOCK_THROTTLE_MS) return
        lastBlockAt = now

        pendingCount?.let { handler.removeCallbacks(it) }
        handler.removeCallbacks(heartbeat)
        inReels = null

        performGlobalAction(GLOBAL_ACTION_HOME)
        handler.postDelayed({
            val intent = Intent(this, BlockActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(BlockActivity.EXTRA_APP, app.name)
            startActivity(intent)
        }, 150)
    }

    override fun onInterrupt() {
        handler.removeCallbacksAndMessages(null)
        inReels = null
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    private companion object {
        const val CHECK_THROTTLE_MS = 250L
        const val SCROLL_SETTLE_MS = 600L
        const val HEARTBEAT_MS = 1500L
        const val BLOCK_THROTTLE_MS = 2000L
    }
}

package com.reellimiter.app

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class TrackedApp(
    val pkg: String,
    val label: String,
    val unit: String,
    val key: String
) {
    INSTAGRAM("com.instagram.android", "Instagram", "Reels", "ig"),
    YOUTUBE("com.google.android.youtube", "YouTube", "Shorts", "yt");

    companion object {
        fun fromPackage(pkg: String): TrackedApp? = values().firstOrNull { it.pkg == pkg }
    }
}

/** Stores limits and today's counts in SharedPreferences. Counts roll over at midnight. */
object LimitStore {
    private const val PREFS = "reel_limiter"
    private const val DEFAULT_LIMIT = 30

    private fun prefs(c: Context) = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private fun rollOverIfNeeded(c: Context) {
        val p = prefs(c)
        if (p.getString("day", null) != today()) {
            val e = p.edit().putString("day", today())
            TrackedApp.values().forEach { e.putInt("count_${it.key}", 0) }
            e.apply()
        }
    }

    fun isEnabled(c: Context): Boolean = prefs(c).getBoolean("enabled", true)

    fun setEnabled(c: Context, enabled: Boolean) {
        prefs(c).edit().putBoolean("enabled", enabled).apply()
    }

    fun getLimit(c: Context, app: TrackedApp): Int =
        prefs(c).getInt("limit_${app.key}", DEFAULT_LIMIT)

    fun setLimit(c: Context, app: TrackedApp, limit: Int) {
        prefs(c).edit().putInt("limit_${app.key}", limit).apply()
    }

    fun getCount(c: Context, app: TrackedApp): Int {
        rollOverIfNeeded(c)
        return prefs(c).getInt("count_${app.key}", 0)
    }

    fun increment(c: Context, app: TrackedApp): Int {
        val next = getCount(c, app) + 1
        prefs(c).edit().putInt("count_${app.key}", next).apply()
        return next
    }

    fun resetToday(c: Context) {
        val e = prefs(c).edit().putString("day", today())
        TrackedApp.values().forEach { e.putInt("count_${it.key}", 0) }
        e.apply()
    }

    fun isLimitReached(c: Context, app: TrackedApp): Boolean =
        getCount(c, app) >= getLimit(c, app)
}

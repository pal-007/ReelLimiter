package com.reellimiter.app

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.View
import android.view.accessibility.AccessibilityManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.slider.Slider

class MainActivity : AppCompatActivity() {

    private class Card(
        val count: TextView,
        val progress: LinearProgressIndicator,
        val limitLabel: TextView,
        val slider: Slider
    )

    private val cards = mutableMapOf<TrackedApp, Card>()
    private val handler = Handler(Looper.getMainLooper())
    private val ticker = object : Runnable {
        override fun run() {
            refresh()
            handler.postDelayed(this, 1000)
        }
    }

    private lateinit var statusText: TextView
    private lateinit var enableBtn: Button
    private lateinit var masterSwitch: MaterialSwitch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText = findViewById(R.id.statusText)
        enableBtn = findViewById(R.id.enableServiceBtn)
        masterSwitch = findViewById(R.id.masterSwitch)

        enableBtn.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        masterSwitch.isChecked = LimitStore.isEnabled(this)
        masterSwitch.setOnCheckedChangeListener { _, checked ->
            LimitStore.setEnabled(this, checked)
        }

        bindCard(TrackedApp.INSTAGRAM, findViewById(R.id.cardInstagram))
        bindCard(TrackedApp.YOUTUBE, findViewById(R.id.cardYoutube))

        findViewById<Button>(R.id.resetBtn).setOnClickListener {
            LimitStore.resetToday(this)
            refresh()
        }
    }

    private fun bindCard(app: TrackedApp, root: View) {
        root.findViewById<TextView>(R.id.title).text = "${app.label} ${app.unit}"
        val card = Card(
            count = root.findViewById(R.id.countText),
            progress = root.findViewById(R.id.progress),
            limitLabel = root.findViewById(R.id.limitLabel),
            slider = root.findViewById(R.id.limitSlider)
        )
        card.slider.value = LimitStore.getLimit(this, app).toFloat()
        card.slider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                LimitStore.setLimit(this, app, value.toInt())
                refresh()
            }
        }
        cards[app] = card
    }

    private fun refresh() {
        val enabled = isServiceEnabled()
        statusText.text = if (enabled) {
            "Accessibility service is ON. ReelLimiter is counting."
        } else {
            "Accessibility service is OFF. Turn on ReelLimiter in Accessibility settings so it can count your Reels and Shorts."
        }
        enableBtn.visibility = if (enabled) View.GONE else View.VISIBLE

        cards.forEach { (app, card) ->
            val count = LimitStore.getCount(this, app)
            val limit = LimitStore.getLimit(this, app)
            card.count.text = if (count >= limit) {
                "$count / $limit today - limit reached"
            } else {
                "$count / $limit today"
            }
            card.progress.max = limit
            card.progress.progress = minOf(count, limit)
            card.limitLabel.text = "Daily limit: $limit ${app.unit}"
        }
    }

    private fun isServiceEnabled(): Boolean {
        val am = getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager
        return am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            .any { it.resolveInfo.serviceInfo.packageName == packageName }
    }

    override fun onResume() {
        super.onResume()
        masterSwitch.isChecked = LimitStore.isEnabled(this)
        handler.post(ticker)
    }

    override fun onPause() {
        handler.removeCallbacks(ticker)
        super.onPause()
    }
}

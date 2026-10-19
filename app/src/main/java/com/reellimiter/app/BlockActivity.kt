package com.reellimiter.app

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/** Full-screen "you're done for today" message shown when a limit is reached. */
class BlockActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_block)

        val app = runCatching { TrackedApp.valueOf(intent.getStringExtra(EXTRA_APP) ?: "") }
            .getOrDefault(TrackedApp.INSTAGRAM)
        val limit = LimitStore.getLimit(this, app)

        findViewById<TextView>(R.id.blockMessage).text =
            "You've watched your $limit ${app.label} ${app.unit} for today.\n" +
                "Your count resets at midnight."

        findViewById<Button>(R.id.closeBtn).setOnClickListener { finishAndRemoveTask() }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        finishAndRemoveTask()
    }

    companion object {
        const val EXTRA_APP = "extra_app"
    }
}

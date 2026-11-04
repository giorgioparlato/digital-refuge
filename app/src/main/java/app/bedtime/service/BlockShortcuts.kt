package app.bedtime.service

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import app.bedtime.R
import app.bedtime.data.Repository
import app.bedtime.data.Schedule
import app.bedtime.ui.formatMinutes
import app.bedtime.ui.formatTime
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Your blocks as shortcuts on digital refuge's icon: a long press on it, in any launcher, offers them,
 * and one tap starts one for its usual length.
 */
object BlockShortcuts {
    private const val MAX = 4
    const val ACTION = "app.bedtime.START_BLOCK"
    const val EXTRA_ID = "block"

    /** Your blocks, and with [voice] (voice notes set up) a voice note first. */
    fun update(context: Context, schedules: List<Schedule>, voice: Boolean = false) {
        val manager = context.getSystemService(ShortcutManager::class.java) ?: return
        val record = if (!voice) emptyList() else listOf(
            ShortcutInfo.Builder(context, "voice")
                .setShortLabel("Voice note")
                .setLongLabel("Record a voice note")
                .setIcon(Icon.createWithResource(context, R.mipmap.ic_launcher))
                .setIntent(app.bedtime.voice.RecordActivity.intent(context))
                .setRank(0)
                .build(),
        )
        val shortcuts = record + schedules.filter { it.isBlock && it.enabled }.take(MAX - record.size).mapIndexed { i, block ->
            ShortcutInfo.Builder(context, "block:${block.id}")
                .setShortLabel(block.name.ifBlank { "A block" })
                .setLongLabel("Start ${block.name.ifBlank { "a block" }} · ${formatMinutes(block.durationMinutes.toLong())}")
                .setIcon(Icon.createWithResource(context, R.mipmap.ic_launcher))
                .setIntent(Intent(context, StartBlockActivity::class.java).setAction(ACTION).putExtra(EXTRA_ID, block.id))
                .setRank(i + record.size)
                .build()
        }
        runCatching { manager.dynamicShortcuts = shortcuts }
    }
}

/** Starts the block a shortcut names, says until when, and goes, with nothing on screen. */
class StartBlockActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getStringExtra(BlockShortcuts.EXTRA_ID)
        lifecycleScope.launch {
            val repo = Repository.get(this@StartBlockActivity)
            val block = repo.schedules.first().firstOrNull { it.id == id && it.isBlock }
            if (block == null) {
                Toast.makeText(this@StartBlockActivity, "that block is gone", Toast.LENGTH_SHORT).show()
            } else {
                repo.startBlock(block, block.durationMinutes)
                val until = formatTime(this@StartBlockActivity, System.currentTimeMillis() + block.durationMinutes * 60_000L)
                Toast.makeText(this@StartBlockActivity, "${block.name.ifBlank { "a block" }} until $until".lowercase(), Toast.LENGTH_SHORT).show()
            }
            finish()
        }
    }
}

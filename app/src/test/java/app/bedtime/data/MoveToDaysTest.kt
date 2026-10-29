package app.bedtime.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class MoveToDaysTest {
    @get:Rule
    val folder = TemporaryFolder()

    private val usage = stringPreferencesKey("usage")
    private val notes = stringPreferencesKey("notes")
    private val settings = stringPreferencesKey("settings")

    @Test
    fun dayToDayKeysMoveAndTheRestStays() = runBlocking {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val old = PreferenceDataStoreFactory.create(scope = scope) { folder.newFile("bedtime.preferences_pb").also { it.delete() } }
        old.edit {
            it[usage] = "[1]"
            it[notes] = "[2]"
            it[settings] = "{3}"
        }
        val days = PreferenceDataStoreFactory.create(migrations = listOf(MoveToDays(old)), scope = scope) {
            folder.newFile("bedtime_days.preferences_pb").also { it.delete() }
        }
        val moved = days.data.first()
        assertEquals("[1]", moved[usage])
        assertEquals("[2]", moved[notes])
        assertNull(moved[settings])
        val left = old.data.first()
        assertNull(left[usage])
        assertNull(left[notes])
        assertEquals("{3}", left[settings])
        scope.cancel()
    }

    @Test
    fun nothingMovesTwice() = runBlocking {
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val old = PreferenceDataStoreFactory.create(scope = scope) { folder.newFile("a.preferences_pb").also { it.delete() } }
        val days = PreferenceDataStoreFactory.create(migrations = listOf(MoveToDays(old)), scope = scope) {
            folder.newFile("b.preferences_pb").also { it.delete() }
        }
        days.edit { it[usage] = "[new]" }
        // An old value appearing later (it can't, but if it did) never overwrites what's there.
        old.edit { it[usage] = "[old]" }
        assertEquals("[new]", days.data.first()[usage])
        scope.cancel()
    }
}

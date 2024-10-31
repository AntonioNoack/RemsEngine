package me.anno.tests.engine

import me.anno.ecs.prefab.ChangeHistory
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.change.Path
import me.anno.ecs.prefab.change.PrefabChanges
import me.anno.engine.ECSRegistry
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertFalse
import me.anno.utils.assertions.assertTrue
import org.junit.jupiter.api.Test

class ChangeHistoryTests {

    @Test
    fun testChangeHistory() {
        ECSRegistry.init()
        val prefab = Prefab("Entity")
        val history = ChangeHistory()
        val propertyName = "name"
        prefab.history = history
        history.prefab = prefab
        put(history, prefab) // put initial state without name
        prefab[Path.ROOT_PATH, propertyName] = "Original Name"
        assertEquals("Original Name", prefab[Path.ROOT_PATH, propertyName])
        put(history, prefab)
        prefab[Path.ROOT_PATH, propertyName] = "New Name"
        assertEquals("New Name", prefab[Path.ROOT_PATH, propertyName])
        put(history, prefab)
        assertEquals("New Name", prefab[Path.ROOT_PATH, propertyName])
        assertTrue(history.undo())
        assertEquals("Original Name", prefab[Path.ROOT_PATH, propertyName])
        assertTrue(history.redo())
        assertFalse(history.redo()) // nothing left to redo
        assertEquals("New Name", prefab[Path.ROOT_PATH, propertyName])
        assertTrue(history.undo(2))
        assertEquals(null, prefab[Path.ROOT_PATH, propertyName])
        assertFalse(history.undo()) // nothing left to undo
    }

    private fun put(history: ChangeHistory, prefab: Prefab) {
        history.put(JsonStringWriter.toText(PrefabChanges(prefab), InvalidRef))
    }
}
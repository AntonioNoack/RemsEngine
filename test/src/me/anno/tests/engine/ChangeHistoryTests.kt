package me.anno.tests.engine

import me.anno.ecs.Entity
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
import org.joml.Vector3d
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ChangeHistoryTests {

    @BeforeEach
    fun init() {
        ECSRegistry.init()
    }

    @Test
    fun testChangeHistoryProperty() {
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
        assertFalse(history.redo()) // still nothing left to redo
        assertEquals("New Name", prefab[Path.ROOT_PATH, propertyName])
        assertTrue(history.undo(2))
        assertEquals(null, prefab[Path.ROOT_PATH, propertyName])
        assertFalse(history.undo()) // nothing left to undo
        assertFalse(history.undo()) // still nothing left to undo
        assertEquals(null, prefab[Path.ROOT_PATH, propertyName])
    }

    @Test
    fun testChangeHistoryChild() {
        ECSRegistry.init()
        val prefab = Prefab("Entity")
        val history = ChangeHistory()

        prefab.history = history
        history.prefab = prefab
        put(history, prefab) // put initial state without name
        put(history, prefab)
        val childPath = prefab.add(Path.ROOT_PATH, 'c', "MeshComponent", "Comp")
        assertTrue(hasChild(prefab, childPath))
        put(history, prefab)
        assertTrue(hasChild(prefab, childPath))
        assertTrue(history.undo())
        assertFalse(hasChild(prefab, childPath))
        assertTrue(history.redo())
        assertFalse(history.redo()) // nothing left to redo
        assertFalse(history.redo()) // still nothing left to redo
        assertTrue(hasChild(prefab, childPath))
        assertTrue(history.undo(2))
        assertFalse(history.undo()) // nothing left to undo
        assertFalse(history.undo()) // still nothing left to undo
        assertFalse(hasChild(prefab, childPath))
    }

    @Test
    fun testChangeHistorySampleInstance() {
        ECSRegistry.init()
        val prefab = Prefab("Entity")
        val history = ChangeHistory()
        val propertyName = "position"
        prefab.history = history
        history.prefab = prefab
        put(history, prefab) // put initial state without name
        checkPosition(Vector3d(), prefab)
        prefab[Path.ROOT_PATH, propertyName] = Vector3d(1.0, 2.0, 3.0)
        checkPosition(Vector3d(1.0, 2.0, 3.0), prefab)
        put(history, prefab)
        prefab[Path.ROOT_PATH, propertyName] = Vector3d(3.0, 4.0, 5.0)
        checkPosition(Vector3d(3.0, 4.0, 5.0), prefab)
        put(history, prefab)
        checkPosition(Vector3d(3.0, 4.0, 5.0), prefab)
        assertTrue(history.undo())
        checkPosition(Vector3d(1.0, 2.0, 3.0), prefab)
        assertTrue(history.redo())
        assertFalse(history.redo()) // nothing left to redo
        assertFalse(history.redo()) // still nothing left to redo
        checkPosition(Vector3d(3.0, 4.0, 5.0), prefab)
        assertTrue(history.undo(2))
        checkPosition(Vector3d(), prefab)
        assertFalse(history.undo()) // nothing left to undo
        assertFalse(history.undo()) // still nothing left to undo
        checkPosition(Vector3d(), prefab)
    }

    private fun checkPosition(expected: Vector3d, prefab: Prefab) {
        assertEquals(expected, (prefab.getSampleInstance() as Entity).position)
    }

    private fun hasChild(prefab: Prefab, path: Path): Boolean {
        return prefab.findCAdd(path) != null
    }

    private fun put(history: ChangeHistory, prefab: Prefab) {
        history.put(JsonStringWriter.toText(PrefabChanges(prefab), InvalidRef))
    }
}
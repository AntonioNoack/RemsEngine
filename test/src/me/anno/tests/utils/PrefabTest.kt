package me.anno.tests.utils

import me.anno.bullet.bodies.DynamicBody
import me.anno.ecs.Entity
import me.anno.ecs.EntityStats.totalNumComponents
import me.anno.ecs.EntityStats.totalNumEntities
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.prefab.Hierarchy
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.change.CAdd
import me.anno.ecs.prefab.change.CSet
import me.anno.ecs.prefab.change.Path
import me.anno.io.files.InvalidRef
import me.anno.io.files.inner.temporary.InnerTmpPrefabFile
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.io.saveable.Saveable.Companion.registerCustomClass
import me.anno.tests.FlakyTest
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertFalse
import me.anno.utils.assertions.assertNotEquals
import me.anno.utils.assertions.assertTrue
import org.apache.logging.log4j.LogManager
import org.joml.Vector3d
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class PrefabTest {
    companion object {
        private val LOGGER = LogManager.getLogger("PrefabTest")
    }

    @BeforeEach
    fun init() {
        registerCustomClass(Prefab())
        registerCustomClass(CAdd())
        registerCustomClass(CSet())
        registerCustomClass(Path())
        registerCustomClass(Entity())
        registerCustomClass(DynamicBody())
        registerCustomClass(MeshComponent())
    }

    @Test
    @FlakyTest
    fun testAddingAppendingAndPropertySetting() {

        // test adding, appending, setting of properties
        // todo test with and without prefabs

        val basePrefab = Prefab("Entity")

        basePrefab["name"] = "Gustav"
        assertEquals(basePrefab.getSampleInstance().name, "Gustav")

        basePrefab["isCollapsed"] = false
        assertEquals(basePrefab.getSampleInstance().isCollapsed, false)

        basePrefab.add(Path.ROOT_PATH, 'c', "MeshComponent", "MC")

        val basePrefabFile = InnerTmpPrefabFile(basePrefab)

        // add
        val prefab = Prefab("Entity", basePrefabFile)
        assertEquals(prefab.getSampleInstance().name, "Gustav")
        assertEquals(prefab.getSampleInstance().isCollapsed, false)

        // remove
        prefab["name"] = "Herbert"
        assertEquals(prefab.getSampleInstance().name, "Herbert")

        val child = prefab.add(Path.ROOT_PATH, 'e', "Entity", "SomeChild", basePrefabFile)
        val rigidbody = prefab.add(child, 'c', "DynamicBody", "RB")
        prefab[rigidbody, "overrideGravity"] = true
        prefab[rigidbody, "gravity"] = Vector3d()

        LOGGER.info(prefab.getSampleInstance()) // shall have two mesh components

        // todo make this an automated test

        val text = JsonStringWriter.toText(prefab, InvalidRef)
        LOGGER.info(text)

        val copied = JsonStringReader.readFirst(text, InvalidRef, Prefab::class)
        LOGGER.info(copied.getSampleInstance())
    }

    @Test
    fun testRemovingLowestChild() {
        val prefab = Prefab("Entity")
        val child = prefab.add(Path.ROOT_PATH, 'e', "Entity", "E")
        prefab[child, "name"] = "Test"
        val rigid = prefab.add(child, 'c', "DynamicBody", "RB")
        prefab[rigid, "overrideGravity"] = true
        prefab[rigid, "gravity"] = Vector3d()

        assertEquals(2, prefab.numAdds)
        assertEquals(1, prefab.sets.count { path, _, _ -> path == child })
        assertEquals(2, prefab.sets.count { path, _, _ -> path == rigid })

        val sample0 = prefab.getSampleInstance() as Entity
        assertEquals(0, sample0.depthInHierarchy)
        assertEquals(2, sample0.totalNumEntities)
        assertEquals(1, sample0.totalNumComponents)

        assertTrue(prefab.remove(rigid))

        assertEquals(1, prefab.numAdds)
        assertEquals(1, prefab.sets.count { path, _, _ -> path == child })
        assertEquals(0, prefab.sets.count { path, _, _ -> path == rigid })

        val sample1 = prefab.getSampleInstance() as Entity
        assertEquals(0, sample1.depthInHierarchy)
        assertEquals(2, sample1.totalNumEntities)
        assertEquals(0, sample1.totalNumComponents)
    }

    @Test
    fun testRemovingMiddleChild() {
        val prefab = Prefab("Entity")
        val child = prefab.add(Path.ROOT_PATH, 'e', "Entity", "E")
        prefab[child, "name"] = "Test"
        val rigid = prefab.add(child, 'c', "DynamicBody", "RB")
        prefab[rigid, "overrideGravity"] = true
        prefab[rigid, "gravity"] = Vector3d()

        assertEquals(2, prefab.numAdds)
        assertEquals(3, prefab.sets.size)
        assertEquals(0, prefab.sets.count { path, _, _ -> path == Path.ROOT_PATH })
        assertEquals(1, prefab.sets.count { path, _, _ -> path == child })
        assertEquals(2, prefab.sets.count { path, _, _ -> path == rigid })

        val sample0 = prefab.getSampleInstance() as Entity
        assertEquals(0, sample0.depthInHierarchy)
        assertEquals(2, sample0.totalNumEntities)
        assertEquals(1, sample0.totalNumComponents)

        assertTrue(prefab.remove(child))

        assertEquals(0, prefab.numAdds)
        assertEquals(0, prefab.sets.size)

        val sample1 = prefab.getSampleInstance() as Entity
        assertEquals(0, sample1.depthInHierarchy)
        assertEquals(1, sample1.totalNumEntities)
        assertEquals(0, sample1.totalNumComponents)
    }

    @Test
    @FlakyTest
    fun testRemovingLowestChildWithInheritance() {
        val original = Entity()
        val childE = Entity(original)
        val rigidE = DynamicBody()
        childE.add(rigidE)

        val prefab0 = original.ref
        val child = childE.prefabPath
        assertNotEquals(Path.ROOT_PATH, child)
        val rigid = rigidE.prefabPath
        assertNotEquals(Path.ROOT_PATH, rigid)
        assertNotEquals(child, rigid)
        assertEquals(0, original.prefabPath.depth)
        assertEquals(1, child.depth)
        assertEquals(2, rigid.depth)

        val prefab = Prefab("Entity")
        prefab.parentPrefabFile = prefab0
        prefab[rigid, "gravity"] = Vector3d()

        assertEquals(0, prefab.numAdds)
        assertEquals(1, prefab.sets.size)

        val sample0 = prefab.getSampleInstance() as Entity
        assertEquals(0, sample0.depthInHierarchy)
        assertEquals(2, sample0.totalNumEntities)
        assertEquals(1, sample0.totalNumComponents)

        assertFalse(prefab.remove(rigid))
        assertEquals(false, prefab[rigid, "isEnabled"])

        assertEquals(0, prefab.numAdds)
        assertEquals(1, prefab.sets.size)

        val sample1 = prefab.getSampleInstance() as Entity
        assertEquals(0, sample1.depthInHierarchy)
        assertEquals(2, sample1.totalNumEntities)
        assertEquals(1, sample1.totalNumComponents)
        val rigid1 = Hierarchy.getInstanceAt(sample1, rigid)
        assertEquals(rigidE::class, rigid1!!::class)
        assertFalse(rigid1.isEnabled)
    }

    private fun newPrefab(prefab0: Prefab = Prefab("Entity")): Prefab {
        assertTrue(prefab0.wasModified)
        val prefab = JsonStringReader.clone(prefab0)
        assertTrue(prefab0.wasModified)
        assertFalse(prefab.wasModified)
        return prefab
    }

    @Test
    fun testModifiedSet() {
        val tested = newPrefab()
        tested["name"] = "Modified"
        assertTrue(tested.wasModified)
    }

    @Test
    fun testModifiedSetUnsafe() {
        val tested = newPrefab()
        tested.setUnsafe("name", "Modified")
        assertTrue(tested.wasModified)
    }

    @Test
    fun testModifiedAdd() {
        val tested = newPrefab()
        tested.add(Path.ROOT_PATH, 'e', "Entity", "test")
        assertTrue(tested.wasModified)
    }

    @Test
    fun testModifiedRemove() {
        val withChild = Prefab()
        val path = withChild.add(Path.ROOT_PATH, 'e', "Entity", "test")
        val tested = newPrefab(withChild)
        tested.remove(path)
        assertTrue(tested.wasModified)
    }
}
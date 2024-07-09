package me.anno.tests.engine.prefab

import me.anno.Engine
import me.anno.ecs.Entity
import me.anno.ecs.EntityStats.sizeOfHierarchy
import me.anno.ecs.components.light.PointLight
import me.anno.ecs.prefab.Hierarchy
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabCache
import me.anno.ecs.prefab.change.Path
import me.anno.engine.ECSRegistry
import me.anno.engine.OfficialExtensions
import me.anno.engine.ScenePrefab
import me.anno.extensions.ExtensionLoader
import me.anno.io.files.Reference.getReference
import me.anno.io.files.inner.temporary.InnerTmpTextFile
import me.anno.io.json.generic.JsonFormatter
import me.anno.sdf.modifiers.SDFHalfSpace
import me.anno.sdf.shapes.SDFBox
import me.anno.utils.OS
import me.anno.utils.structures.lists.Lists.createArrayList
import org.joml.Planef
import org.joml.Vector3d
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.math.PI
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HierarchyTests {

    @BeforeEach
    fun init() {
        OfficialExtensions.register()
        ExtensionLoader.load()
        ECSRegistry.init()
    }

    @Test
    fun testAdd() {
        // test
        val prefab = Prefab("Entity", ScenePrefab)
        prefab.source = InnerTmpTextFile("")
        val sample0 = prefab.createInstance() as Entity
        val size0 = sample0.sizeOfHierarchy
        val added = Hierarchy.add(prefab, Path.ROOT_PATH, prefab, Path.ROOT_PATH, 'e')!!
        val sample1 = prefab.createInstance() as Entity
        val size1 = sample1.sizeOfHierarchy
        if (size0 * 2 != size1) {
            println(prefab.adds)
            println(prefab.sets)
            println(sample0)
            println(sample1)
            throw RuntimeException("Sizes don't match: $size0*2 vs $size1")
        }
        Hierarchy.removePathFromPrefab(prefab, added, "Entity")
        val sample2 = prefab.createInstance() as Entity
        val size2 = sample2.sizeOfHierarchy
        if (size0 != size2) {
            println(sample0)
            println(sample2)
            throw RuntimeException("Removal failed: $size0 vs $size2")
        }
    }

    @Test
    fun testRenumberRemove() {
        val prefab = Prefab("Entity")
        val elementA = prefab.add(Path.ROOT_PATH, 'e', "Entity", "A")
        val elementB = prefab.add(Path.ROOT_PATH, 'e', "Entity", "B")
        val elementC = prefab.add(elementB, 'e', "Entity", "C")
        prefab[elementC, "position"] = Vector3d()
        // Root
        // - A
        // - B
        // - - C
        val sample0 = prefab.getSampleInstance() as Entity
        val numElements1 = sample0.sizeOfHierarchy
        if (numElements1 != 4) throw IllegalStateException("incorrect number of elements: $numElements1")
        Hierarchy.removePathFromPrefab(prefab, elementA, "Entity")
        val sample1 = prefab.getSampleInstance() as Entity
        val numElements2 = sample1.sizeOfHierarchy
        if (numElements2 != 3) throw IllegalStateException("incorrect number of elements: $numElements2")
        // renumbering is currently disabled, because it only is a hint
        /*if (prefab.adds.any { it.path.isNotEmpty() && it.path.firstIndex() > 0 }) {
            LOGGER.warn(JsonFormatter.format(sample1.toString()))
            for (add in prefab.adds) {
                LOGGER.warn(add)
            }
            LOGGER.warn(prefab.sets)
            throw IllegalStateException("there still is adds, which are non-empty and the firstIndex > 0")
        }*/
    }

    @Test
    fun testRemoval2() {
        val prefab = Prefab("Entity")
        val clazz = "PointLight"
        val n = 10
        val names = createArrayList(n) { "child$it" }
        for (i in 0 until n) {
            val child = prefab.add(Path.ROOT_PATH, 'c', clazz, names[i])
            prefab[child, "description"] = "desc$i"
            prefab[child, "lightSize"] = i.toDouble()
        }
        assertEquals(prefab.adds.values.sumOf { it.size }, n)
        assertEquals(prefab.sets.size, 2 * n)
        val tested = intArrayOf(1, 2, 3, 5, 7)
        for (i in tested.sortedDescending()) {
            val sample = prefab.getSampleInstance() as Entity
            Hierarchy.removePathFromPrefab(prefab, sample.components[i])
        }
        // test prefab
        assertEquals(prefab.adds.values.sumOf { it.size }, n - tested.size)
        assertEquals(prefab.sets.size, 2 * (n - tested.size))
        // test result
        val sample = prefab.getSampleInstance() as Entity
        assertEquals(sample.components.size, n - tested.size)
        for (i in 0 until n) {
            // test, that exactly those still exist, that we didn't remove
            assertEquals(sample.components.count { it.name == names[i] }, if (i in tested) 0 else 1)
        }
        Engine.requestShutdown()
    }

    @Test
    fun testJsonFormatter() {
        val ref = OS.documents.getChild("RemsEngine/SampleProject/Scene.json")
        val prefab = PrefabCache[ref]
        println(JsonFormatter.format(prefab.toString()))
    }

    @Test
    fun testPrefab() {
        val prefab = Prefab("Entity")
        val sample1 = prefab.getSampleInstance()
        assertTrue(sample1 is Entity)
        val child = prefab.add(Path.ROOT_PATH, 'c', "PointLight", "PL")
        prefab[child, "lightSize"] = PI
        val sample2 = prefab.getSampleInstance()
        assertTrue(sample2 is Entity)
        assertEquals(sample2.components.count { it is PointLight }, 1)
        val light1 = Hierarchy.getInstanceAt(sample2, child)
        println("found ${light1?.prefabPath} at $child")
        assertTrue(light1 is PointLight)
        assertEquals(light1.lightSize, PI)
    }

    @Test
    fun testMultiAdd() {
        val prefab = Prefab("SDFBox")
        val count = 3
        for (i in 0 until count) {
            val child = Prefab("SDFHalfSpace")
            child[Path.ROOT_PATH, "plane"] = Planef(0f, 1f, 0f, i.toFloat())
            Hierarchy.add(child, Path.ROOT_PATH, prefab, Path.ROOT_PATH, 'd')
        }
        println(prefab.adds)
        println(prefab.sets)
        val inst = prefab.getSampleInstance() as SDFBox
        println(inst.distanceMappers)
        for (i in 0 until count) {
            val dist = inst.distanceMappers[i] as SDFHalfSpace
            assertEquals(i.toFloat(), dist.plane.distance)
        }
    }

    @Test
    fun testReordering() {
        val prefab = Prefab("SDFBox")
        val count = 12
        val random = Random(1234L)
        val order = ArrayList<Int>()
        for (i in 0 until count) {
            val insertIndex = random.nextInt(i + 1)
            order.add(insertIndex, i)
            val child = Prefab("SDFHalfSpace")
            child[Path.ROOT_PATH, "plane"] = Planef(0f, 1f, 0f, i.toFloat())
            Hierarchy.add(child, Path.ROOT_PATH, prefab, Path.ROOT_PATH, 'd', insertIndex)
        }
        val inst = prefab.getSampleInstance() as SDFBox
        for (i in 0 until count) {
            val dist = inst.distanceMappers[i] as SDFHalfSpace
            assertEquals(order[i].toFloat(), dist.plane.distance)
        }
    }

    @Test
    fun testAddSimpleChild() {
        OfficialExtensions.register()
        ExtensionLoader.load()
        val scene = Prefab("Entity")
        val added = PrefabCache[getReference("res://meshes/CuteGhost.fbx")]!!
        val ca = scene.adds.values.sumOf { it.size }
        val cs = scene.sets.size
        Hierarchy.add(added, Path.ROOT_PATH, scene, Path.ROOT_PATH, 'e')
        val nca = scene.adds.values.sumOf { it.size }
        val ncs = scene.sets.size
        assertEquals(nca, ca + 1)
        assertEquals(ncs, cs)
        scene.createInstance()
        // to do check there were no warnings
    }
}

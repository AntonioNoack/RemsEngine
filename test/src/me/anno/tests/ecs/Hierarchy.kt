package me.anno.tests.ecs

import me.anno.Engine
import me.anno.ecs.Entity
import me.anno.ecs.components.light.PointLight
import me.anno.ecs.components.mesh.sdf.modifiers.SDFHalfSpace
import me.anno.ecs.components.mesh.sdf.shapes.SDFBox
import me.anno.ecs.prefab.Hierarchy
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabCache
import me.anno.ecs.prefab.change.Path
import me.anno.engine.ECSRegistry
import me.anno.engine.scene.ScenePrefab
import me.anno.io.files.FileReference
import me.anno.io.json.JsonFormatter
import me.anno.io.zip.InnerTmpFile
import me.anno.utils.OS
import org.joml.Planef
import org.joml.Vector3d
import java.util.*


private fun testAdd() {
    // test
    val scene = Prefab("Entity", ScenePrefab)
    scene.source = InnerTmpFile.InnerTmpPrefabFile(scene)
    val sample0 = scene.getSampleInstance() as Entity
    val size0 = sample0.sizeOfHierarchy
    val added = Hierarchy.add(scene, Path.ROOT_PATH, scene, Path.ROOT_PATH)!!
    val sample1 = scene.getSampleInstance() as Entity
    val size1 = sample1.sizeOfHierarchy
    if (size0 * 2 != size1) {
        println(sample0)
        println(sample1)
        throw RuntimeException("Sizes don't match: $size0*2 vs $size1")
    }
    Hierarchy.removePathFromPrefab(scene, added, "Entity")
    val sample2 = scene.getSampleInstance() as Entity
    val size2 = sample2.sizeOfHierarchy
    if (size0 != size2) {
        println(sample0)
        println(sample2)
        throw RuntimeException("Removal failed: $size0 vs $size2")
    }
}

private fun testRenumberRemove() {
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

fun testRemoval2() {
    val prefab = Prefab("Entity")
    val clazz = "PointLight"
    val n = 10
    val names = Array(n) { "child$it" }
    for (i in 0 until n) {
        val child = prefab.add(Path.ROOT_PATH, 'c', clazz, names[i])
        prefab.setProperty(child, "description", "desc$i")
        prefab.setProperty(child, "lightSize", i.toDouble())
    }
    Hierarchy.assert(prefab.adds.size, n)
    Hierarchy.assert(prefab.sets.size, 2 * n)
    val tested = intArrayOf(1, 2, 3, 5, 7)
    for (i in tested.sortedDescending()) {
        val sample = prefab.getSampleInstance() as Entity
        Hierarchy.removePathFromPrefab(prefab, sample.components[i])
    }
    // test prefab
    Hierarchy.assert(prefab.adds.size, n - tested.size)
    Hierarchy.assert(prefab.sets.size, 2 * (n - tested.size))
    // test result
    val sample = prefab.getSampleInstance() as Entity
    Hierarchy.assert(sample.components.size, n - tested.size)
    for (i in 0 until n) {
        // test that exactly those still exist, that we didn't remove
        Hierarchy.assert(sample.components.count { it.name == names[i] }, if (i in tested) 0 else 1)
    }
    Engine.requestShutdown()
}

private fun testJsonFormatter() {
    val ref = FileReference.getReference(OS.documents, "RemsEngine/SampleProject/Scene.json")
    val prefab = PrefabCache[ref]
    println(JsonFormatter.format(prefab.toString()))
}

private fun testPrefab() {
    val prefab = Prefab("Entity")
    val sample1 = prefab.getSampleInstance()
    Hierarchy.assert(sample1 is Entity)
    val child = prefab.add(Path.ROOT_PATH, 'c', "PointLight", "PL")
    prefab[child, "lightSize"] = Math.PI
    val sample2 = prefab.getSampleInstance()
    Hierarchy.assert(sample2 is Entity)
    sample2 as Entity
    Hierarchy.assert(sample2.components.count { it is PointLight }, 1)
    val light1 = Hierarchy.getInstanceAt(sample2, child)
    println("found ${light1?.prefabPath} at $child")
    Hierarchy.assert(light1 is PointLight)
    Hierarchy.assert((light1 as PointLight).lightSize == Math.PI)
}

private fun testMultiAdd() {
    val prefab = Prefab("SDFBox")
    val count = 3
    for (i in 0 until count) {
        val child = Prefab("SDFHalfSpace")
        child[Path.ROOT_PATH, "plane"] = Planef(0f, 1f, 0f, i.toFloat())
        Hierarchy.add(child, Path.ROOT_PATH, prefab, Path.ROOT_PATH)
    }
    println(prefab.adds)
    println(prefab.sets)
    val inst = prefab.getSampleInstance() as SDFBox
    for (i in 0 until count) {
        val dist = inst.distanceMappers[i] as SDFHalfSpace
        Hierarchy.assert(dist.plane.d, i.toFloat())
    }
}

private fun testReordering() {
    val prefab = Prefab("SDFBox")
    val count = 12
    val random = Random(1234L)
    val order = ArrayList<Int>()
    for (i in 0 until count) {
        val insertIndex = random.nextInt(i + 1)
        order.add(insertIndex, i)
        val child = Prefab("SDFHalfSpace")
        child[Path.ROOT_PATH, "plane"] = Planef(0f, 1f, 0f, i.toFloat())
        Hierarchy.add(child, Path.ROOT_PATH, prefab, Path.ROOT_PATH, insertIndex)
    }
    println(prefab.adds)
    println(prefab.sets)
    val inst = prefab.getSampleInstance() as SDFBox
    for (i in 0 until count) {
        val dist = inst.distanceMappers[i] as SDFHalfSpace
        Hierarchy.assert(dist.plane.d, order[i].toFloat())
    }
}

fun testAddSimpleChild() {
    val scene = Prefab("Entity")
    val added = PrefabCache[FileReference.getReference(OS.documents, "CuteGhost.fbx")]!!
    val ca = scene.adds.size
    val cs = scene.sets.size
    Hierarchy.add(added, Path.ROOT_PATH, scene, Path.ROOT_PATH, -1)
    val nca = scene.adds.size
    val ncs = scene.sets.size
    Hierarchy.assert(nca, ca + 1)
    Hierarchy.assert(ncs, cs)
    scene.createInstance()
    // to do check there were no warnings
}

fun main() {
    ECSRegistry.init()
    println("----------------------")
    testMultiAdd()
    println("----------------------")
    testPrefab()
    println("----------------------")
    testRemoval2()
    println("----------------------")
    testAdd()
    println("----------------------")
    testReordering()
    println("----------------------")
    testRenumberRemove()
    println("----------------------")
    testAddSimpleChild()
    println("----------------------")
    // testJsonFormatter()
    println("----------------------")
    Engine.requestShutdown()
}
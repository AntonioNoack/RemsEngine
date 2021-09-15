package me.anno.ecs.prefab

import me.anno.ecs.prefab.change.CAdd
import me.anno.ecs.prefab.change.Path
import me.anno.engine.ECSRegistry
import me.anno.io.zip.InnerTmpFile


fun main() {

    ECSRegistry.initNoGFX()

    fun <V> assert(v1: V, v2: V) {
        if (v1 != v2) throw RuntimeException("$v1 != $v2")
    }

    fun assert(b: Boolean) {
        if (!b) throw RuntimeException()
    }

    // todo test adding, appending, setting of properties
    // todo test removing, deleting
    // todo test with and without prefabs...

    val basePrefab = Prefab("Entity")

    basePrefab.setProperty("name", "Gustav")
    assert(basePrefab.getSampleInstance().name, "Gustav")

    basePrefab.setProperty("isCollapsed", false)
    assert(basePrefab.getSampleInstance().isCollapsed, false)

    basePrefab.add(CAdd(Path.ROOT_PATH, 'c', "MeshComponent"))

    val basePrefabFile = InnerTmpFile.InnerTmpPrefabFile(basePrefab)

    // add
    val prefab = Prefab("Entity", basePrefabFile)
    assert(prefab.getSampleInstance().name, "Gustav")
    assert(prefab.getSampleInstance().isCollapsed, false)

    // remove
    prefab.setProperty("name", "Herbert")
    assert(prefab.getSampleInstance().name, "Herbert")

    prefab.add(CAdd(Path.ROOT_PATH, 'e', "Entity", "SomeChild", basePrefabFile))
    println(prefab.getSampleInstance()) // shall have two mesh components


}
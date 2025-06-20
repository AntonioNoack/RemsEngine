package me.anno.tests.engine.prefab

import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabCache
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.prefab.change.CAdd
import me.anno.ecs.prefab.change.Path
import me.anno.engine.ECSRegistry
import me.anno.engine.EngineBase.Companion.workspace
import me.anno.io.files.FileReference
import me.anno.io.files.inner.temporary.InnerTmpPrefabFile
import me.anno.utils.OS.documents
import me.anno.utils.assertions.assertEquals

fun printTree(file: FileReference) {
    val prefab = PrefabCache[file].waitFor()!!
    val instance = prefab.newInstance(PrefabSaveable::class)!!
    printTree(file, instance, 0)
}

fun printTree(file: FileReference, instance: PrefabSaveable, depth: Int) {
    assertEquals(file, instance.prefab!!.sourceFile)
    println("${"  ".repeat(depth)}${instance.className}: '${instance.name}'")
    println("${"  ".repeat(depth + 1)}Path: ${instance.prefabPath}")
    for (ct in instance.listChildTypes()) {
        for (ch in instance.getChildListByType(ct)) {
            printTree(file, ch, depth + 1)
        }
    }
}

fun main() {
    ECSRegistry.init()
    workspace = documents.getChild("RemsEngine/YandereSim")
    val meshFile = workspace.getChild("Outside/SM_Prop_Cafe_Chair_01.json")
    printTree(meshFile)
    val child = Prefab("Entity")
    child.add(CAdd(Path.ROOT_PATH, 'e', "Entity", "NameID", meshFile), 0)
    child.newInstance()
    printTree(InnerTmpPrefabFile(child))
}
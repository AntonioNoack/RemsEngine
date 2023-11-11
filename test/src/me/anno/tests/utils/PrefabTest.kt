package me.anno.tests.utils

import me.anno.config.DefaultConfig
import me.anno.ecs.Entity
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabInspector
import me.anno.ecs.prefab.change.Path
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.EditorState
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.io.files.InvalidRef
import me.anno.io.json.JsonFormatter
import me.anno.io.text.TextReader
import me.anno.io.text.TextWriter
import me.anno.io.zip.InnerTmpFile
import me.anno.studio.StudioBase
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.ui.editor.PropertyInspector
import org.apache.logging.log4j.LogManager
import org.joml.Vector3d

fun main() {

    ECSRegistry.init()

    test1()
    test2()

    disableRenderDoc()

    val sample = Entity()

    // broken text input
    // testSceneWithUI(sample)
    testUI("PrefabTest") {
        StudioBase.instance?.enableVSync = true
        sample.prefabPath = Path.ROOT_PATH
        EditorState.prefabSource = sample.ref
        PrefabInspector.currentInspector = PrefabInspector(sample.ref)
        EditorState.select(sample)
        PropertyInspector({ EditorState.selection }, DefaultConfig.style)
    }
    /*testUI { // works
        val list = PanelListY(style)
        sample.prefabPath = Path.ROOT_PATH
        PrefabInspector(sample.ref).inspect(sample, list, style)
        list
    }*/
}

fun test1() {

    val logger = LogManager.getLogger("PrefabTest")

    fun <V> assert(v1: V, v2: V) {
        if (v1 != v2) throw RuntimeException("$v1 != $v2")
    }

    // test adding, appending, setting of properties
    // todo test with and without prefabs

    val basePrefab = Prefab("Entity")

    basePrefab["name"] = "Gustav"
    assert(basePrefab.getSampleInstance().name, "Gustav")

    basePrefab["isCollapsed"] = false
    assert(basePrefab.getSampleInstance().isCollapsed, false)

    basePrefab.add(Path.ROOT_PATH, 'c', "MeshComponent", "MC")

    val basePrefabFile = InnerTmpFile.InnerTmpPrefabFile(basePrefab)

    // add
    val prefab = Prefab("Entity", basePrefabFile)
    assert(prefab.getSampleInstance().name, "Gustav")
    assert(prefab.getSampleInstance().isCollapsed, false)

    // remove
    prefab["name"] = "Herbert"
    assert(prefab.getSampleInstance().name, "Herbert")

    val child = prefab.add(Path.ROOT_PATH, 'e', "Entity", "SomeChild", basePrefabFile)
    val rigidbody = prefab.add(child, 'c', "Rigidbody", "RB")
    prefab[rigidbody, "overrideGravity"] = true
    prefab[rigidbody, "gravity"] = Vector3d()

    logger.info(prefab.getSampleInstance()) // shall have two mesh components

    val text = TextWriter.toText(prefab, InvalidRef)
    logger.info(text)

    val copied = TextReader.read(text, InvalidRef, true).first() as Prefab
    logger.info(copied.getSampleInstance())
}

fun test2() {

    val logger = LogManager.getLogger("PrefabTest")

    // test removing, deleting

    val prefab = Prefab("Entity")
    val child = prefab.add(Path.ROOT_PATH, 'e', "Entity", "E")
    val rigid = prefab.add(child, 'c', "Rigidbody", "RB")
    prefab[rigid, "overrideGravity"] = true
    prefab[rigid, "gravity"] = Vector3d()

    val text = TextWriter.toText(prefab, InvalidRef)
    logger.info(JsonFormatter.format(text))

    val copied = TextReader.read(text, InvalidRef, false).first() as Prefab
    logger.info(copied.getSampleInstance())
}
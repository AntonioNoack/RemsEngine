package me.anno.tests.utils

import me.anno.bullet.Rigidbody
import me.anno.config.DefaultConfig
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabInspector
import me.anno.ecs.prefab.change.CAdd
import me.anno.ecs.prefab.change.CSet
import me.anno.ecs.prefab.change.Path
import me.anno.engine.ECSRegistry
import me.anno.engine.EngineBase
import me.anno.engine.ui.EditorState
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.io.files.InvalidRef
import me.anno.io.files.inner.temporary.InnerTmpPrefabFile
import me.anno.io.json.generic.JsonFormatter
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.io.saveable.Saveable.Companion.registerCustomClass
import me.anno.ui.debug.TestEngine.Companion.testUI
import me.anno.ui.editor.PropertyInspector
import me.anno.utils.assertions.assertEquals
import org.apache.logging.log4j.LogManager
import org.joml.Vector3d
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

fun main() {

    ECSRegistry.init()

    disableRenderDoc()

    val sample = Entity()

    // broken text input
    // testSceneWithUI(sample)
    testUI("PrefabTest") {
        EngineBase.enableVSync = true
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

class PrefabTest {

    @BeforeEach
    fun init() {
        registerCustomClass(Prefab())
        registerCustomClass(CAdd())
        registerCustomClass(CSet())
        registerCustomClass(Path())
        registerCustomClass(Entity())
        registerCustomClass(Rigidbody())
        registerCustomClass(MeshComponent())
    }

    @Test
    fun test1() {

        val logger = LogManager.getLogger("PrefabTest")

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
        val rigidbody = prefab.add(child, 'c', "Rigidbody", "RB")
        prefab[rigidbody, "overrideGravity"] = true
        prefab[rigidbody, "gravity"] = Vector3d()

        logger.info(prefab.getSampleInstance()) // shall have two mesh components

        val text = JsonStringWriter.toText(prefab, InvalidRef)
        logger.info(text)

        val copied = JsonStringReader.readFirst(text, InvalidRef, Prefab::class)
        logger.info(copied.getSampleInstance())
    }

    @Test
    fun test2() {

        val logger = LogManager.getLogger("PrefabTest")

        // test removing, deleting

        val prefab = Prefab("Entity")
        val child = prefab.add(Path.ROOT_PATH, 'e', "Entity", "E")
        val rigid = prefab.add(child, 'c', "Rigidbody", "RB")
        prefab[rigid, "overrideGravity"] = true
        prefab[rigid, "gravity"] = Vector3d()

        val text = JsonStringWriter.toText(prefab, InvalidRef)
        logger.info(JsonFormatter.format(text))

        val copied = JsonStringReader.readFirst(text, InvalidRef, Prefab::class)
        logger.info(copied.getSampleInstance())
    }
}
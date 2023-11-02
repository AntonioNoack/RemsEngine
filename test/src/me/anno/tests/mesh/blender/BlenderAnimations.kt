package me.anno.tests.mesh.blender

import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.mesh.blender.BlenderReader
import me.anno.utils.OS.documents
import org.apache.logging.log4j.LogManager

fun main() {

    ECSRegistry.initPrefabs()
    ECSRegistry.initMeshes()

    // todo load and display animated Blender files correctly
    //  - pose
    //  - armature
    //  - bones
    //  - vertex weights

    // todo uvs are also broken :(
    LogManager.logAll()
    val source = documents.getChild("Blender/AnimTest.blend")
    BlenderReader.readAsFolder(source) { folder, _ ->
        if (false) testSceneWithUI("Blender Animations", folder!!.getChild("Scene.json")) {
            it.renderer.renderMode = RenderMode.BONE_INDICES
        }
    }
}
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
    //  - extract frame-rate from project somehow...
    // done:
    //  - pose
    //  - armature
    //  - bones
    //  - vertex weights
    // todo bones or their animations aren't correct yet

    LogManager.logAll()
    LogManager.disableLogger("BlenderMaterialConverter")

    val source = documents.getChild("Blender/AnimTest.blend")
    BlenderReader.readAsFolder(source) { folder, exc ->
        if (folder != null) {
            testSceneWithUI("Blender Animations", folder.getChild("Scene.json")) {
                it.renderer.renderMode = RenderMode.BONE_INDICES
            }
        } else exc?.printStackTrace()
    }
}
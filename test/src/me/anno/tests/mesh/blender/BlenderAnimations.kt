package me.anno.tests.mesh.blender

import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.mesh.blender.BlenderReader
import me.anno.utils.OS.documents
import org.apache.logging.log4j.LogManager

fun main() {

    ECSRegistry.init()

    // load and display animated Blender files correctly:
    // done:
    //  - pose
    //  - armature
    //  - bones
    //  - vertex weights

    LogManager.logAll()
    val source = documents.getChild("Blender/AnimTest2.blend")
    BlenderReader.readAsFolder(source) { folder, exc ->
        if (folder != null) {
            testSceneWithUI(
                "Blender Animations",
                folder.getChild("Scene.json"),
                RenderMode.BONE_INDICES
            )
        } else exc?.printStackTrace()
    }
}
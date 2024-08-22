package me.anno.tests.sdf

import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.sdf.shapes.SDFMesh
import me.anno.utils.OS.res

fun main() {
    // todo why doesn't this work on ShaderToy?
    testSceneWithUI("SDFMesh", SDFMesh().apply {
        technique = SDFMesh.SDFMeshTechnique.CONST_ARRAY
        meshFile = res.getChild("icon-lowpoly.obj")
        smoothness = 0.01f
    })
}
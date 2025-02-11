package me.anno.tests.mesh.gltf

import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS.downloads

fun main() {
    // todo this is crashing Assimp... why???
    //  BLEND: BLENDER magic bytes are missing, couldn't find GZIP header either
    OfficialExtensions.initForTests()
    testSceneWithUI("Classroom", downloads.getChild("3d/ClassRoom/classroom.glb"))
}
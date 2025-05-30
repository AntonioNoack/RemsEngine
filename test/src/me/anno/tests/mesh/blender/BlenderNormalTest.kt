package me.anno.tests.mesh.blender

import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.io.files.Reference.getReference
import me.anno.mesh.blender.BlenderMeshConverter
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.LogManager

fun main() {
    // todo ensure normals are loaded correctly...
    //  they look incorrect in many places, but that might be because it's an indexed mesh when it should be flat-shaded
    OfficialExtensions.initForTests()
    LogManager.define(BlenderMeshConverter::class.simpleName, Level.DEBUG)
    val src = getReference("E:/Assets/Quaternius/Gun Blends/Pistol_5.blend")
    testSceneWithUI("Blender Normals", src)
}
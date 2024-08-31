package me.anno.tests.engine.ui

import me.anno.ecs.Entity
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.sdf.SDFGroup
import me.anno.sdf.shapes.SDFBox
import me.anno.sdf.shapes.SDFSphere

/**
 * check, that Gizmos are present, and usable for SDF shapes
 * */
fun main() {
    val group = SDFGroup()
        .add(SDFSphere())
        .add(SDFBox())
    val scene = Entity("Scene")
        .add(group)
    testSceneWithUI("SDF Gizmos", scene)
}
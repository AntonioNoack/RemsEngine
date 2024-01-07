package me.anno.tests.engine.ui

import me.anno.ecs.Entity
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.sdf.SDFGroup
import me.anno.sdf.shapes.SDFBox
import me.anno.sdf.shapes.SDFSphere

fun main() {
    // check, that Gizmos are present, and usable for SDF shapes
    val group = SDFGroup()
    group.addChild(SDFSphere())
    group.addChild(SDFBox())
    val entity = Entity()
    entity.add(group)
    testSceneWithUI("SDF Gizmos", entity)
}
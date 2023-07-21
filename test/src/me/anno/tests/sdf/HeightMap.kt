package me.anno.tests.sdf

import me.anno.ecs.Entity
import me.anno.ecs.components.light.DirectionalLight
import me.anno.ecs.components.shaders.SkyBox
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.sdf.DebugMode
import me.anno.sdf.shapes.SDFHeightMap
import me.anno.utils.OS

fun main() {
    // todo define a sample with bricks (SDFArray2)
    testSceneWithUI("SDFHeightMap", Entity().apply {
        addChild(DirectionalLight())
        addChild(SkyBox())
        addChild(SDFHeightMap().apply {
            maxSteps = 50
            localReliability = 0.5f
            source = OS.pictures.getChild("Maps/Bricks.png")
            maxHeight = 0.1f
            debugMode = DebugMode.NUM_STEPS
        })
    })
}
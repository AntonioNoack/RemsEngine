package me.anno.tests.sdf

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.material.Material
import me.anno.engine.ECSRegistry
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView
import me.anno.sdf.CombinationMode
import me.anno.sdf.SDFGroup
import me.anno.sdf.arrays.SDFArray2
import me.anno.sdf.arrays.SDFArrayMapper
import me.anno.sdf.random.SDFRandomRotation
import me.anno.sdf.random.SDFRandomUV
import me.anno.sdf.shapes.SDFBox
import me.anno.sdf.shapes.SDFCylinder
import me.anno.sdf.shapes.SDFHeart
import me.anno.sdf.shapes.SDFHeightMap
import me.anno.sdf.shapes.SDFTorus
import me.anno.sdf.uv.LinearUVMapper
import me.anno.utils.OS
import me.anno.utils.types.Floats.toRadians

/**
 * brick wall with randomly crooked bricks:
 * of that, a large donut with a heart-shaped cutout
 * */
fun main() {
    // OfficialExtensions.initForTests()
    ECSRegistry.init()
    SceneView.testSceneWithUI("SDFArray2", Entity().apply {
        add(SDFArray2().apply {
            maxSteps = 500
            useModulatorMaterials = true
            addChild(SDFGroup().apply {
                // todo why are they not showing up?
                if (false) addChild(LinearUVMapper())
                addChild(SDFTorus().apply {
                    scale = 500f
                    materialId = 0
                })
                addChild(SDFHeart().apply {
                    bound1(-0.1f, 10f, 2)
                    rotation = rotation.rotateX((-90f).toRadians())
                    scale = 600f
                    materialId = 1
                    position.set(0f, 0f, 50f)
                })
                combinationMode = CombinationMode.DIFFERENCE1
            })
            modulatorIndex = 1
            sdfMaterials = listOf(
                Material().apply {
                    diffuseMap = OS.pictures
                        // .getChild("Textures/Rainbow/SpectrumH.png")
                        .getChild("speckle.jpg")
                }.ref,
                Material().apply {
                    diffuseBase.set(1f, 0.3f, 0.2f, 1f)
                }.ref,
            )
            cellSize.set(10f, 2f, 5f)
            count.set(1000)
            relativeOverlap.set(2.5f / 10f, 2.5f / 2f, 2.5f / 5f)
            // todo file input for sdfMaterials: add/create new materials (even if just temporary)
            //  todo add option to then save them after creation
            // todo try material for sphere
            addChild(SDFGroup().apply {
                smoothness = 0.1f
                if (false) addChild(SDFRandomRotation().apply {
                    minAngleDegrees.set(-5f, 0f, -5f)
                    maxAngleDegrees.set(+5f, 0f, +5f)
                    appliedPortion = 0.2f
                    seedXOR = 1234
                })
                addChild(SDFRandomUV())
                addChild(SDFBox().apply {
                    smoothness = 0.2f
                    halfExtends.set(5f, 1f, 2.5f)
                })
                addChild(SDFGroup().apply {
                    position.set(0f, 1f, 0f)
                    addChild(SDFCylinder().apply {
                        smoothness = 0.1f
                        radius = 0.75f
                        halfHeight = 0.5f
                    })
                    // nice looking logo on top of studs
                    // very performance hungry, because I don't know how far an object is
                    // it might be enough to use normal maps
                    //  todo calculate tangents, and add more options for UV mappings
                    addChild(SDFHeightMap().apply {
                        position.set(0f, 0.48f, 0f)
                        scale = 0.5f
                        source = OS.pictures.getChild("Maps/Bricks2.png")
                        maxHeight = 0.1f
                    })
                    if (false) addChild(LinearUVMapper())
                    addChild(SDFArrayMapper().apply {
                        count.set(4, 1, 2)
                        cellSize.set(2.5f)
                    })
                })
            })
        })
    })
}
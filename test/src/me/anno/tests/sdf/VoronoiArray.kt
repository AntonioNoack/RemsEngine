package me.anno.tests.sdf

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Material
import me.anno.engine.ui.render.SceneView
import me.anno.sdf.arrays.SDFVoronoiArray
import me.anno.sdf.random.SDFRandomUV
import me.anno.sdf.shapes.SDFCone
import me.anno.sdf.shapes.SDFPlane
import me.anno.utils.OS

fun main() {
    // build nice, small forest :)
    SceneView.testSceneWithUI("SDFVoronoiArray", Entity().apply {
        // ground
        addChild(SDFPlane().apply {
            sdfMaterials = listOf(Material().apply {
                diffuseBase.set(0.3f, 0.5f, 0.3f, 1f)
            }.ref)
        })
        // trees
        addChild(SDFCone().apply {
            addChild(SDFVoronoiArray().apply {
                min.set(-1e3f)
                max.set(+1e3f)
            })
            addChild(SDFRandomUV())
            radius = 0.4f
            sdfMaterials = listOf(Material().apply {
                // add texture with random shades of green :)
                // https://www.wallpaperup.com/259563/green_landscapes_nature_trees_grass_parks.html
                diffuseMap = OS.pictures.getChild("RemsStudio/8c841f59b8dedb0b63abcac91cb82392-1000.jpg")
            }.ref)
        })
    })
}
package me.anno.tests.sdf

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Material
import me.anno.engine.ui.render.SceneView
import me.anno.image.raw.IntImage
import me.anno.sdf.arrays.SDFVoronoiArray
import me.anno.sdf.random.SDFRandomScale
import me.anno.sdf.random.SDFRandomUV
import me.anno.sdf.shapes.SDFCone
import me.anno.sdf.shapes.SDFPlane
import me.anno.sdf.uv.LinearUVMapper

fun main() {
    // build nice, small forest :)
    // add texture with random shades of green :)
    // https://www.wallpaperup.com/259563/green_landscapes_nature_trees_grass_parks.html
    // "compressed" using SortColors.kt
    val colorPalette = IntImage(
        6, 6, intArrayOf(
            0x000000, 0x000300, 0x030100, 0x060105, 0x080300, 0x120404,
            0x062400, 0x172408, 0x192406, 0x1b240f, 0x1f240d, 0x2c240d,
            0x193800, 0x263808, 0x28380b, 0x2a3815, 0x2d3816, 0x3d3824,
            0x295000, 0x39501a, 0x3c5015, 0x3e501c, 0x42502d, 0x575033,
            0x447303, 0x5b7337, 0x5f732c, 0x63732a, 0x687337, 0x85734d,
            0xbeee67, 0xd7f2ab, 0xddf899, 0xe3f8b3, 0xeaf5ca, 0xffffe5
        ), false
    ).ref
    val paletteMaterial = Material().apply {
        diffuseMap = colorPalette
    }.ref
    SceneView.testSceneWithUI("SDFVoronoiArray", Entity().apply {
        // ground
        addChild(SDFPlane().apply {
            addChild(LinearUVMapper())
            sdfMaterials = listOf(paletteMaterial)
        })
        // trees
        addChild(SDFCone().apply {
            addChild(SDFVoronoiArray().apply {
                min.set(-1e3f)
                max.set(+1e3f)
            })
            addChild(SDFRandomUV())
            addChild(SDFRandomScale())
            radius = 0.4f
            sdfMaterials = listOf(paletteMaterial)
        })
        // grass
        addChild(SDFCone().apply {
            val s = 0.02f
            scale = s
            limit = 1e5f
            addChild(SDFVoronoiArray().apply {
                min.set(-1e3f / s)
                max.set(+1e3f / s)
            })
            radius = 0.2f
            addChild(SDFRandomUV())
            sdfMaterials = listOf(paletteMaterial)
        })
    })
}
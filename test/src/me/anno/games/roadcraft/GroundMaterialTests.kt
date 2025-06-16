package me.anno.games.roadcraft

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.terrain.ColorMap
import me.anno.ecs.components.mesh.terrain.DefaultNormalMap
import me.anno.ecs.components.mesh.terrain.HeightMap
import me.anno.ecs.components.mesh.terrain.RectangleTerrainModel
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.maths.Maths.clamp
import me.anno.maths.noise.PerlinNoise
import me.anno.utils.Color.rgba
import org.joml.Vector4f
import kotlin.math.floor

fun flattenFraction(x: Float): Float {
    return clamp((x - 0.5f) * 5f + 0.5f)
}

fun flattenFraction(value: Int, groups: Int, div: Int, s: Int): Int {
    var float = value * (groups - 1f) / div
    val floored = floor(float)
    float = floored + flattenFraction(float - floored)
    return (float * s).toInt()
}

fun createSampleTerrain(w: Int, h: Int): Mesh {
    val noise = PerlinNoise(2145, 8, 0.5f, -30f, 30f, Vector4f(0.03f))
    val heightMap = HeightMap { x, y -> noise.getSmooth(x.toFloat(), y.toFloat()) }
    val normalMap = DefaultNormalMap(heightMap, 1f, false)
    val colorMap = ColorMap { x, y ->
        val numGroups = 3
        val group = flattenFraction(y, numGroups, h, 51)
        val numBlends = 3
        val blending = flattenFraction(x, numBlends, w, 51)
        rgba(blending, blending, blending, group)
    }
    val mesh = RectangleTerrainModel.generateRegularQuadHeightMesh(
        w, h, false, 1f,
        Mesh(), heightMap, normalMap, colorMap
    )
    mesh.materials = listOf(TerrainShader.material.ref)
    mesh.sandHeight = ShortArray(w * h)
    return mesh
}

val sandType = Attribute("sandHeight", AttributeType.UINT16_NORM, 1)

// todo if a bridge is built, we need to fix the height there to the bridge height, so sand behaves properly
var Mesh.sandHeight: ShortArray?
    get() = getAttr("sandHeight", ShortArray::class)
    set(value) = setAttr("sandHeight", value, sandType)

fun main() {

    OfficialExtensions.initForTests()

    val w = 256
    val h = 256
    val terrain = createSampleTerrain(w, h)

    val scene = Entity()
        .add(MeshComponent(terrain))
        .add(TerrainPainting())

    // todo debug UI to draw sand and tarmac on top, and then smooth it
    testSceneWithUI("Terrain", scene)
}
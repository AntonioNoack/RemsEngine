package me.anno.tests.utils

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.terrain.TerrainUtils
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView
import me.anno.gpu.buffer.DrawMode
import me.anno.mesh.vox.meshing.BlockSide
import me.anno.ui.editor.color.spaces.Oklab
import me.anno.utils.Color.toRGB
import org.joml.Vector3f
import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random

fun createColorsPoints(n: Int): Mesh {
    val rgb = Vector3f()
    val lab = Vector3f()
    val rnd = Random(1234L)
    val positions = FloatArray(n * 3)
    val colors = IntArray(n)
    for (i in 0 until n) {
        rgb.set(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat()).mul(2f).sub(1f)
        rgb.div(max(abs(rgb.x), max(abs(rgb.y), abs(rgb.z)))).mul(0.5f).add(0.5f)
        Oklab.fromRGB(rgb, lab)
        lab.get(positions, 3 * i)
        colors[i] = rgb.toRGB()
    }
    val mesh = Mesh()
    mesh.positions = positions
    mesh.color0 = colors
    mesh.drawMode = DrawMode.POINTS
    return mesh
}

fun createColorsMesh(s: Int): Mesh {
    val rgb = Vector3f()
    val lab = Vector3f()
    val positions = FloatArray(s * s * 6 * 3)
    val colors = IntArray(s * s * 6)
    var dstI = 0
    for (side in BlockSide.entries) {
        val center = Vector3f(side.x, side.y, side.z)
        val dx = Vector3f(side.y, side.z, side.x)
        val dy = Vector3f(side.z, side.x, side.y)
        if (dy.cross(dx, Vector3f()).dot(center) < 0f) {
            dy.negate()
        }
        for (yi in 0 until s) {
            for (xi in 0 until s) {
                rgb.set(center)
                dx.mulAdd(xi / (s - 1f) * 2f - 1f, rgb, rgb)
                dy.mulAdd(yi / (s - 1f) * 2f - 1f, rgb, rgb)
                rgb.mul(0.5f).add(0.5f)
                Oklab.fromRGB(rgb, lab)
                lab.get(positions, 3 * dstI)
                colors[dstI++] = rgb.toRGB()
            }
        }
    }
    val mesh = Mesh()
    TerrainUtils.generateQuadIndices(s, s, false, mesh)
    val srcIndices = mesh.indices!!
    val dstIndices = IntArray(srcIndices.size * 6)
    for (k in 0 until 6) {
        val dstOffset = srcIndices.size * k
        val srcOffset = s * s * k
        for (i in srcIndices.indices) {
            dstIndices[dstOffset + i] = srcIndices[i] + srcOffset
        }
    }
    mesh.indices = dstIndices
    mesh.positions = positions
    mesh.color0 = colors
    return mesh
}

fun main() {
    val mesh = createColorsMesh(120)
    SceneView.testSceneWithUI("Oklab", mesh) {
        it.renderView.renderMode = RenderMode.COLOR
    }
}
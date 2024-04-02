package me.anno.tests.utils

import me.anno.ecs.components.mesh.Mesh
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.SceneView
import me.anno.gpu.buffer.DrawMode
import me.anno.ui.editor.color.spaces.Oklab
import me.anno.utils.Color.toRGB
import org.joml.Vector3f
import kotlin.math.abs
import kotlin.math.max
import kotlin.random.Random

fun createColors(n: Int): Pair<FloatArray, IntArray> {
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
    return positions to colors
}

fun main() {
    val mesh = Mesh()
    val (pos, col) = createColors(100000)
    mesh.positions = pos
    mesh.color0 = col
    mesh.drawMode = DrawMode.POINTS
    SceneView.testSceneWithUI("Oklab", mesh) {
        it.renderer.renderMode = RenderMode.COLOR
    }
}
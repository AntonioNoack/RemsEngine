package me.anno.bench

import me.anno.Engine
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshCache
import me.anno.ecs.components.mesh.utils.NormalCalculator.calculateSmoothNormals
import me.anno.engine.OfficialExtensions
import me.anno.utils.Clock
import me.anno.utils.OS.desktop
import me.anno.utils.types.Floats.toRadians
import kotlin.math.cos

/**
 * smooth normals with angle limit was extremely slow ~3500ms/run,
 * now it's much better with 120ms/run for a mesh with 110k triangles.
 * */
fun main() {
    OfficialExtensions.initForTests()
    val source = desktop.getChild("Plate48x48.json")
    val mesh = MeshCache.getEntry(source).waitFor() as Mesh
    val clock = Clock("SmoothNormalsBench")
    clock.benchmark(1, 20, "Smooth Normals") {
        calculateSmoothNormals1(mesh, 35f.toRadians())
    }
    Engine.requestShutdown()
}

fun calculateSmoothNormals1(mesh: Mesh, maxAngleRadians: Float) {
    calculateSmoothNormals(mesh, cos(maxAngleRadians), 0f)
}

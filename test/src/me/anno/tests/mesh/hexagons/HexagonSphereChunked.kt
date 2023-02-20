package me.anno.tests.mesh.hexagons

import me.anno.ecs.Entity
import me.anno.ecs.components.chunks.spherical.HexagonSphere
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.maths.Maths

fun main() {
    val n = 100
    val hexagons = createHexSphere(n)
    var i0 = 0
    val entity = Entity()
    for (i in 0 until chunkCount) {
        val mesh = Mesh()
        val i1 = calculateChunkEnd(i, n)
        createFaceMesh(mesh, hexagons, i0, i1, if (i == 0) 12 else 0)
        i0 = i1
        val subEntity = Entity()
        val meshComp = MeshComponent(mesh)
        subEntity.add(meshComp)
        /*val bounds = mesh.ensureBounds()
        // todo this is too simple, use something working...
        // enable/disable chunks based on direction
        subEntity.add(object : Component() {
            override fun clone() = throw NotImplementedError()
            override fun onVisibleUpdate(): Boolean {
                // sample just the main direction
                val cam = RenderState.cameraDirection
                meshComp.isEnabled = bounds.testPoint(-cam.x.toFloat(), -cam.y.toFloat(), -cam.z.toFloat())
                return true
            }
        })*/
        entity.add(subEntity)
    }
    testSceneWithUI(entity)
}

val chunkCount = 12

/**
 * if you want to split your world into chunks for culling, you can use this function
 * it will return the ranges of indices for 21 chunks;
 * the first chunk will be visible most times, because it contains the borders and edges
 * */
fun calculateChunkEnd(i: Int, n: Int): Int {
    val special = HexagonSphere.lineCount * (n + 1) + HexagonSphere.pentagonCount
    val perSide = (n * (n + 1)) shr 1
    return special + Maths.min(i, n) * perSide
}
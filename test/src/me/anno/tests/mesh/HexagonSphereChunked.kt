package me.anno.tests.mesh

import me.anno.ecs.Entity
import me.anno.ecs.components.chunks.spherical.HexagonSphere
import me.anno.ecs.components.chunks.spherical.HexagonSphere.calculateChunkEnd
import me.anno.ecs.components.chunks.spherical.HexagonSphere.chunkCount
import me.anno.ecs.components.chunks.spherical.HexagonSphere.createFaceMesh
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI

fun main() {
    val n = 100
    val hexagons = HexagonSphere.createHexSphere(n)
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
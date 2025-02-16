package me.anno.tests.mesh.hexagons.physics

import me.anno.Time
import me.anno.bullet.HexagonSpherePhysics
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
import me.anno.engine.raycast.RayQuery
import me.anno.engine.raycast.Raycast
import me.anno.engine.ui.render.SceneView
import me.anno.input.Input
import me.anno.input.Key
import me.anno.io.files.FileReference
import me.anno.maths.Maths
import me.anno.maths.chunks.spherical.HexagonSphere
import me.anno.tests.mesh.hexagons.ControllerOnSphere
import me.anno.tests.mesh.hexagons.HexMCWorldSave
import me.anno.tests.mesh.hexagons.HexagonSphereMCWorld
import me.anno.tests.mesh.hexagons.air
import me.anno.tests.mesh.hexagons.createMesh
import me.anno.tests.mesh.hexagons.grass
import me.anno.utils.files.Files.formatFileSize
import org.joml.Vector3d
import org.joml.Vector3f

class HSPhysicsControls(
    val scene: Entity,
    val sphere: HexagonSphere,
    val chunks: Map<HexagonSphere.Chunk, Mesh>,
    val it: SceneView,
    val triQ: MCTriangleQuery,
    val physics: HexagonSpherePhysics,
    val file: FileReference,
    val save: HexMCWorldSave,
    val world: HexagonSphereMCWorld,
    val len: Float,
) : ControllerOnSphere(it.renderView, null) {

    override fun onUpdate() {
        // super.onUpdate()
        // jumping
        if (triQ.touchesFloor && Input.wasKeyPressed(' ')) {
            physics.addForce(up.x, up.y, up.z, 3f * len)
        }
        // friction
        val dtx = Time.deltaTime.toFloat() * (if (triQ.touchesFloor) 5f else 1f)
        physics.velocity.mul(1f - Maths.dtTo01(dtx))
        // reset floor touching state
        triQ.touchesFloor = false
        // execute physics
        physics.update(Time.deltaTime.toFloat())
        // update visuals & control transform
        position.set(physics.currPosition)//.mul(1.0 + len * shape.halfHeight) // eye height
        onChangePosition()
        updateViewRotation(false)

        if (Input.wasKeyPressed('s') && Input.isControlDown) {
            println("Saving")
            file.getParent().mkdirs()
            save.write(world, file)
            println("Saved, ${file.length().formatFileSize()}")
        }
    }

    // todo show inventory
    // todo serialization
    var inventory = grass

    override fun onMouseClicked(x: Float, y: Float, button: Key, long: Boolean) {
        // resolve click
        val start = it.renderView.cameraPosition
        val dir = Vector3d(it.renderView.mouseDirection)
        val query = RayQuery(start, dir, 10.0)
        val hit = Raycast.raycast(scene, query)
        if (hit) {
            val result = query.result
            val setBlock = button == Key.BUTTON_RIGHT
            val testBlock = button == Key.BUTTON_MIDDLE
            if (setBlock) {
                // move hit back slightly
                dir.mulAdd(-sphere.len * 0.05, result.positionWS, result.positionWS)
            } else {
                // move hit more into the block
                val d = result.geometryNormalWS
                val l = -sphere.len * 0.25
                result.positionWS.add(d.x * l, d.y * l, d.z * l)
            }
            val hexagon = sphere.findClosestHexagon(Vector3f(result.positionWS))
            val h = result.positionWS.length().toFloat()
            val yj = world.yi(h).toInt()
            if (yj !in 0 until world.sy) return
            if (testBlock) {
                inventory = triQ.getWorld(hexagon)[yj]
            } else {
                // set block
                world.setBlock(hexagon, yj, if (setBlock) inventory else air)
                // physics need to be updated as well
                triQ.worldCache.remove(hexagon)
                // invalidate chunk
                val invalidChunks = HashSet<HexagonSphere.Chunk>()
                invalidChunks.add(sphere.findChunk(hexagon))
                // invalidate neighbor chunks
                sphere.ensureNeighbors(hexagon)
                for (neighbor in hexagon.neighbors) {
                    invalidChunks.add(sphere.findChunk(neighbor!!))
                }
                for (key in invalidChunks) {
                    val mesh = chunks[key]!!
                    val (_, tri, si, sj) = key
                    createMesh(sphere.queryChunk(tri, si, sj), world, null, mesh)
                }
            }
        }
    }

    override fun moveCamera(dx: Float, dy: Float, dz: Float) {
        val dy2 = dy * 5f
        physics.addForce(
            (dx * right.x + dy2 * up.x - dz * forward.x),
            (dx * right.y + dy2 * up.y - dz * forward.y),
            (dx * right.z + dy2 * up.z - dz * forward.z),
            2f * Time.deltaTime.toFloat()
        )
    }
}

package me.anno.tests.terrain.v1

import me.anno.ecs.Entity
import me.anno.engine.raycast.RayHit
import me.anno.engine.raycast.RayQuery
import me.anno.engine.raycast.Raycast
import me.anno.engine.ui.control.ControlScheme
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.input.Input
import me.anno.maths.Maths.length
import me.anno.maths.Maths.sq
import me.anno.tests.terrain.v1.TerrainChunkSystem.Companion.sx
import me.anno.tests.terrain.v1.TerrainChunkSystem.Companion.sz
import me.anno.utils.Color.toRGB
import org.joml.Vector3d
import kotlin.math.ceil
import kotlin.math.floor

// add terrain drawing systems
//  - using our Sims sample?
//  - using our chunk system?
//  - like the tsunami module?

// todo lod system
// todo gpu accelerated?

fun main() {
    val scene = Entity()
    val settings = EditSettings()
    scene.add(settings)
    val chunkSystem = TerrainChunkSystem(Entity(scene))
    scene.add(chunkSystem)
    testSceneWithUI("Terrain", scene) {
        it.editControls = object : ControlScheme(it.renderView) {
            fun edit(xi: Int, zi: Int, effect0: Float, hit: RayHit) {

                val comp = chunkSystem.getChunk(xi, 0, zi, true)!!
                val mesh = comp.getMesh()
                if (mesh.positions == null || mesh.color0 == null) {
                    me.anno.tests.LOGGER.warn("Missing positions/color for $xi/$zi :(")
                    return
                }

                val falloffFactor = -100f / sq(hit.distance).toFloat()

                val offsetX = (comp.dx - hit.positionWS.x).toFloat()
                val offsetZ = (comp.dz - hit.positionWS.z).toFloat()

                val editor = TerrainEditor(offsetX, offsetZ, falloffFactor, effect0, mesh)

                when (settings.editMode) {
                    TerrainEditMode.PAINTING -> {
                        val color = settings.color.toRGB()
                        editor.paint(color)
                    }
                    TerrainEditMode.SMOOTHING -> {
                        editor.smooth()
                    }
                    TerrainEditMode.FLATTENING -> {
                        editor.flatten()
                    }
                    TerrainEditMode.ADDING -> {
                        val strength = 0.03f *
                                hit.distance.toFloat() * effect0 *
                                (if (Input.isShiftDown) -1 else +1)
                        editor.adding(strength)
                    }
                }

                mesh.calculateNormals(true)
                mesh.invalidateGeometry()
            }

            override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
                if (Input.isLeftDown && (dx != 0f || dy != 0f)) {
                    // draw height / paint
                    val query = renderView.rayQuery()
                    val hit = Raycast.raycast(scene, query)
                    val comp = query.result.component as? TerrainChunk
                    val mesh = comp?.getMeshOrNull()
                    if (hit && mesh != null) {
                        val result = query.result
                        val effect0 = length(dx, dy) / width
                        val radius = result.distance * 0.3f

                        val x0 = (result.positionWS.x - radius) / sz
                        val x1 = (result.positionWS.x + radius) / sz

                        val z0 = (result.positionWS.z - radius) / sx
                        val z1 = (result.positionWS.z + radius) / sx

                        val x0i = floor(x0).toInt()
                        val z0i = floor(z0).toInt()
                        val x1i = ceil(x1).toInt()
                        val z1i = ceil(z1).toInt()

                        for (zi in z0i..z1i) {
                            for (xi in x0i..x1i) {
                                edit(xi, zi, effect0, result)
                            }
                        }
                    }
                } else super.onMouseMoved(x, y, dx, dy)
            }
        }
    }
}

package me.anno.tests.terrain

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.terrain.TerrainUtils
import me.anno.engine.raycast.Raycast
import me.anno.engine.ui.control.ControlScheme
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.input.Input
import me.anno.maths.Maths.length
import me.anno.maths.Maths.min
import me.anno.maths.Maths.mix
import me.anno.maths.Maths.mixARGB
import me.anno.maths.Maths.sq
import me.anno.utils.Color.black
import me.anno.utils.Color.white
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

// add terrain drawing systems
//  - using our Sims sample?
//  - using our chunk system?
//  - like the tsunami module?

// todo chunks -> scalable to any size
// todo lod system
// todo gpu accelerated?

fun main() {
    val w = 64
    val h = 64
    val scene = Entity()
    val mesh = Mesh()
    TerrainUtils.generateRegularQuadHeightMesh(w, h, 0, w, false, 1f,
        mesh, {
            val x = it % w - w * 0.5f
            val y = it / w - h * 0.5f
            val s = 6f / (w + h)
            sin(x) * sin(y) + 10f * cos(x * s) * cos(y * s)
        }, { _, _, _, dst -> dst.set(0f, 1f, 0f) }, { -1 })
    mesh.calculateNormals(true)
    scene.add(MeshComponent(mesh))
    testSceneWithUI(scene) {
        it.editControls = object : ControlScheme(it.renderer) {
            override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
                if (Input.isLeftDown && (dx != 0f || dy != 0f)) {
                    // draw height / paint
                    val hit = Raycast.raycast(
                        scene, it.renderer.cameraPosition,
                        it.renderer.mouseDirection, 0.0, 0.0,
                        1e9, -1,
                    )
                    if (hit != null) {

                        val falloffFactor = -90f / sq(hit.distance).toFloat()
                        val pos = mesh.positions!!
                        val col = mesh.color0!!
                        fun falloff(x: Float, z: Float): Float {
                            return exp(falloffFactor * sq(x - hit.positionWS.x, z - hit.positionWS.z)).toFloat()
                        }

                        val effect0 = length(dx, dy) / w
                        if (Input.isAltDown) {
                            // painting
                            val color = if (Input.isShiftDown) white else black
                            for (i in col.indices) {
                                val j = i * 3
                                val wi = min(effect0 * falloff(pos[j], pos[j + 2]), 1f)
                                col[i] = mixARGB(col[i], color, wi)
                            }
                        } else if (Input.isControlDown && Input.isShiftDown) {
                            // smoothing
                            val effect = 5f * effect0
                            val dxi = 3
                            val dyi = w * 3
                            for (yi in 0 until h) {
                                for (xi in 0 until w) {
                                    val i = (xi + yi * w) * 3 + 1
                                    var sum = 4f * pos[i]
                                    var weight = 4f
                                    if (xi > 0) {
                                        sum += pos[i - dxi]
                                        weight++
                                    }
                                    if (xi < w - 1) {
                                        sum += pos[i + dxi]
                                        weight++
                                    }
                                    if (yi > 0) {
                                        sum += pos[i - dyi]
                                        weight++
                                    }
                                    if (yi < h - 1) {
                                        sum += pos[i + dyi]
                                        weight++
                                    }
                                    val wi = min(effect * falloff(pos[i - 1], pos[i + 1]), 1f)
                                    pos[i] = mix(pos[i], sum / weight, wi)
                                }
                            }
                        } else if (Input.isControlDown) {
                            // flattening
                            var sum = 0f
                            var weight = 1e-9f
                            for (i in pos.indices step 3) {
                                val wi = effect0 * falloff(pos[i], pos[i + 2])
                                sum += pos[i + 1] * wi
                                weight += wi
                            }
                            val avg = sum / weight
                            for (i in pos.indices step 3) {
                                val wi = min(effect0 * falloff(pos[i], pos[i + 2]), 1f)
                                pos[i + 1] = mix(pos[i + 1], avg, wi)
                            }
                        } else {
                            // adding / subtracting
                            val effect = 0.03f *
                                    hit.distance.toFloat() * effect0 *
                                    (if (Input.isShiftDown) -1 else +1)
                            for (i in pos.indices step 3) {
                                pos[i + 1] += effect * falloff(pos[i], pos[i + 2])
                            }
                        }
                        mesh.calculateNormals(true)
                        mesh.invalidateGeometry()
                    }
                } else super.onMouseMoved(x, y, dx, dy)
            }
        }
    }
}
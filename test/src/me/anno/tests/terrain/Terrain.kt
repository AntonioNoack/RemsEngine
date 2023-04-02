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
import me.anno.maths.Maths.mix
import me.anno.maths.Maths.sq
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

// add terrain drawing systems
//  - using our Sims sample?
//  - using our chunk system?
//  - like the tsunami module?

// todo chunks -> scalable to any size

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
                    // draw height / todo paint
                    val hit = Raycast.raycast(
                        scene, it.renderer.cameraPosition,
                        it.renderer.mouseDirection, 0.0, 0.0,
                        1e9, -1,
                    )
                    if (hit != null) {

                        val falloffFactor = -90f / sq(hit.distance).toFloat()
                        val pos = mesh.positions!!
                        fun falloff(x: Float, z: Float): Float {
                            return exp(falloffFactor * sq(x - hit.positionWS.x, z - hit.positionWS.z)).toFloat()
                        }

                        // todo smoothing
                        if (Input.isControlDown) {
                            // flattening
                            val effect = length(dx, dy) / w
                            var sum = 0f
                            var weight = 1e-9f
                            for (i in pos.indices step 3) {
                                val wi = effect * falloff(pos[i], pos[i + 2])
                                sum += pos[i + 1] * wi
                                weight += wi
                            }
                            val avg = sum / weight
                            for (i in pos.indices step 3) {
                                val wi = effect * falloff(pos[i], pos[i + 2])
                                pos[i + 1] = mix(pos[i + 1], avg, wi)
                            }
                        } else {
                            // adding / subtracting
                            val effect = 0.03f *
                                    hit.distance.toFloat() * length(dx, dy) / w *
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
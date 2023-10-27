package me.anno.tests.terrain

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.annotations.Type
import me.anno.ecs.components.chunks.cartesian.ChunkSystem
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.ProceduralMesh
import me.anno.ecs.components.mesh.terrain.TerrainUtils
import me.anno.engine.raycast.RayHit
import me.anno.engine.raycast.RayQuery
import me.anno.engine.raycast.Raycast
import me.anno.engine.ui.control.ControlScheme
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.input.Input
import me.anno.maths.Maths.length
import me.anno.maths.Maths.max
import me.anno.maths.Maths.min
import me.anno.maths.Maths.mix
import me.anno.maths.Maths.mixARGB
import me.anno.maths.Maths.sq
import me.anno.utils.Color.toRGB
import org.joml.Vector3f
import kotlin.math.*

// add terrain drawing systems
//  - using our Sims sample?
//  - using our chunk system?
//  - like the tsunami module?

// todo lod system
// todo gpu accelerated?

val wBits = 5
val hBits = 5
val w = 1 shl wBits
val h = 1 shl hBits

class TerrainChunk(
    val xi: Int, val zi: Int
) : ProceduralMesh() {

    val dx = (xi + 0.5f) * w
    val dz = (zi + 0.5f) * h

    override fun generateMesh(mesh: Mesh) {
        val s = 0.1f
        TerrainUtils.generateRegularQuadHeightMesh(
            w + 1, h + 1, false, 1f,
            mesh, { xi, zi ->
                val x = xi + dx
                val y = zi + dz
                sin(x) * sin(y) + 10f * cos(x * s) * cos(y * s)
            })
        mesh.calculateNormals(true)
    }
}

class TerrainEditor(
    val offsetX: Float,
    val offsetZ: Float,
    val falloffFactor: Float,
    val effect0: Float,
    val mesh: Mesh
) {

    val pos = mesh.positions!!
    val col = mesh.color0!!

    val falloff0 = exp(-9f)

    fun falloff(x: Float, z: Float): Float {
        return max(exp(falloffFactor * sq(x + offsetX, z + offsetZ)) - falloff0, 0f)
    }

    fun paint(color: Int) {
        val strength = 20f * effect0
        for (i in col.indices) {
            val j = i * 3
            val wi = min(strength * falloff(pos[j], pos[j + 2]), 1f)
            col[i] = mixARGB(col[i], color, wi)
        }
    }

    fun smooth() {
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
    }

    fun flatten() {
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
    }

    fun adding(strength: Float) {
        for (i in pos.indices step 3) {
            pos[i + 1] += strength * falloff(pos[i], pos[i + 2])
        }
    }
}

enum class TerrainEditMode {
    ADDING,
    PAINTING,
    SMOOTHING,
    FLATTENING,
}

class EditSettings : Component() {
    var editMode: TerrainEditMode = TerrainEditMode.PAINTING

    @Type("Color3")
    var color = Vector3f(1f, 0f, 0f)
}

data class TerrainElement(val height: Float, val color: Int)

class TerrainChunkSystem(val childrenContainer: Entity) : ChunkSystem<TerrainChunk, TerrainElement>(wBits, 0, hBits) {

    override fun createChunk(chunkX: Int, chunkY: Int, chunkZ: Int, size: Int): TerrainChunk {
        return TerrainChunk(chunkX, chunkZ)
    }

    override fun getIndex(localX: Int, localY: Int, localZ: Int): Int {
        return localX + localZ * (w + 1)
    }

    private fun setElement(container: TerrainChunk, index: Int, value: TerrainElement) {
        val mesh = container.getMeshOrNull()
        mesh.positions!![index * 3 + 1] = value.height
        mesh.color0!![index] = value.color
    }

    override fun setElement(
        container: TerrainChunk, localX: Int, localY: Int, localZ: Int,
        index: Int, element: TerrainElement
    ): Boolean {
        setElement(container, index, element)
        // update neighbor chunks, too
        if (localX == 0) {
            setElement(
                getChunk(container.xi - 1, 0, container.zi, true)!!,
                getIndex(w, 0, localZ), element
            )
        }
        if (localZ == 0) {
            setElement(
                getChunk(container.xi, 0, container.zi - 1, true)!!,
                getIndex(localX, 0, h), element
            )
            if (localX == 0) {
                setElement(
                    getChunk(container.xi - 1, 0, container.zi - 1, true)!!,
                    getIndex(w, 0, h), element
                )
            }
        }
        return true
    }

    override fun getElement(
        container: TerrainChunk,
        localX: Int, localY: Int, localZ: Int, index: Int
    ): TerrainElement {
        val mesh = container.getMeshOrNull()
        return TerrainElement(mesh.positions!![index * 3 + 1], mesh.color0!![index])
    }

    val visibleChunks = HashSet<TerrainChunk>()

    fun manageChunkLoading() {
        val rv = RenderView.currentInstance ?: return
        val x0 = (rv.position.x / w).toInt()
        val z0 = (rv.position.z / h).toInt()
        for (zi in -10..10) {
            for (xi in -10..10) {
                val visible = xi * xi + zi * zi < 81
                val chunk = getChunk(x0 + xi, 0, z0 + zi, visible)
                if (chunk != null && (chunk in visibleChunks) != visible) {
                    if (visible) {
                        val child = Entity("${chunk.xi}/${chunk.zi}", chunk)
                            .setPosition(chunk.dx.toDouble(), 0.0, chunk.dz.toDouble())
                        childrenContainer.add(child)
                        visibleChunks.add(chunk)
                    } else {
                        childrenContainer.remove(chunk.entity!!)
                        visibleChunks.remove(chunk)
                    }
                }
            }
        }
    }

    override fun onUpdate(): Int {
        manageChunkLoading()
        return 1
    }
}

fun main() {
    val scene = Entity()
    val settings = EditSettings()
    scene.add(settings)
    val chunkSystem = TerrainChunkSystem(Entity(scene))
    scene.add(chunkSystem)
    testSceneWithUI("Terrain", scene) {
        it.editControls = object : ControlScheme(it.renderer) {
            fun edit(xi: Int, zi: Int, effect0: Float, hit: RayHit) {

                val comp = chunkSystem.getChunk(xi, 0, zi, true)!!
                val mesh = comp.getMesh()

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
                    val query = RayQuery(it.renderer.cameraPosition, it.renderer.mouseDirection, 1e9)
                    val hit = Raycast.raycastClosestHit(scene, query)
                    val comp = query.result.component as? TerrainChunk
                    val mesh = comp?.getMeshOrNull()
                    if (hit && mesh != null) {
                        val result = query.result
                        val effect0 = length(dx, dy) / width
                        val radius = result.distance * 0.3f

                        val x0 = (result.positionWS.x - radius) / w
                        val x1 = (result.positionWS.x + radius) / w

                        val z0 = (result.positionWS.z - radius) / h
                        val z1 = (result.positionWS.z + radius) / h

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
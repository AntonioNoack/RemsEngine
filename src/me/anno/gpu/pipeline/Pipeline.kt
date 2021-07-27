package me.anno.gpu.pipeline

import me.anno.ecs.Entity
import me.anno.ecs.components.light.LightComponent
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.MeshRenderer
import me.anno.ecs.components.mesh.RendererComponent
import me.anno.gpu.DepthMode
import me.anno.gpu.ShaderLib.pbrModelShader
import me.anno.gpu.blending.BlendMode
import me.anno.io.ISaveable
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.utils.types.AABBs.isEmpty
import org.joml.*
import org.lwjgl.opengl.GL11.GL_FRONT
import kotlin.math.cos
import kotlin.math.sin

// todo idea: the scene rarely changes -> we can reuse it, and just update the uniforms
// and the graph may be deep, however meshes may be only in parts of the tree
class Pipeline : Saveable() {

    // todo pipelines, that we need:
    //  - 3d world,
    //  - 2d ui,
    //  - ...
    // todo every local player needs its own pipeline to avoid too much sorting

    // todo we can sort by material and shaders...
    // todo or by distance...

    val stages = ArrayList<PipelineStage>()

    val lightPseudoStage = PipelineStage(
        "lights", PipelineStage.Sorting.NO_SORTING, BlendMode.PURE_ADD,
        DepthMode.LESS, false, GL_FRONT, pbrModelShader
    )
    private val lightPseudoRenderer = MeshRenderer()

    lateinit var defaultStage: PipelineStage

    fun addMesh(mesh: Mesh?, renderer: RendererComponent, entity: Entity) {
        mesh ?: return
        val materials = mesh.materials
        if (materials.isEmpty()) {
            val stage = defaultStage
            stage.add(renderer, mesh, entity, 0)
        } else {
            for (index in materials.indices) {
                val material = materials[index]
                val stage = material.pipelineStage ?: defaultStage
                stage.add(renderer, mesh, entity, index)
            }
        }
    }

    fun addLight(mesh: Mesh?, entity: Entity) {
        mesh ?: return
        val stage = lightPseudoStage
        stage.add(lightPseudoRenderer, mesh, entity, 0)
    }

    // todo collect all buffers + materials, which need to be drawn at a certain stage, and then draw them together
    fun draw(cameraMatrix: Matrix4f, cameraPosition: Vector3d) {
        for (stage in stages) {
            stage.bindDraw(cameraMatrix, cameraPosition)
        }

        // todo test the culling by drawing 1000 spheres, and using a non-moving frustum

    }

    fun reset() {
        lightPseudoStage.reset()
        for (stage in stages) {
            stage.reset()
        }
    }

    var contained = 0
    var nonContained = 0

    fun fill(rootElement: Entity) {
        contained = 0
        nonContained = 0
        // todo more complex traversal:
        // todo exclude static entities by their AABB
        // todo exclude entities, if they contain no meshes
        // todo exclude entities, if they are off-screen
        // todo reuse the pipeline state for multiple frames
        //  - add a margin, so entities at the screen border can stay visible
        //  - partially populate the pipeline?
        rootElement.validateTransforms()
        rootElement.validateAABBs()
        subFill(rootElement)
        // println("$contained/$nonContained")
    }

    private fun subFill(entity: Entity) {
        val renderer = entity.getComponent(false, RendererComponent::class)
        val components = entity.components
        for (i in components.indices) {
            val c = components[i]
            if (c.isEnabled) {
                if (renderer != null && c is MeshComponent) {
                    addMesh(c.mesh, renderer, entity)
                }
                if (c is LightComponent) {
                    addLight(c.getLightPrimitive(), entity)
                }
            }
        }
        val children = entity.children
        for (i in children.indices) {
            val child = children[i]
            if (child.isEnabled) {
                val needsDrawing = contains(child.aabb)
                if (needsDrawing) subFill(child)
                if (needsDrawing) contained++
                else nonContained += child.sizeOfHierarchy
            }
        }
    }

    val planes = Array(6) { Vector4d() }
    val normals = Array(6) { Vector3d() }
    val positions = Array(6) { Vector3d() }

    fun calculatePlanes(
        near: Double,
        far: Double,
        fovYRadians: Double,
        aspectRatio: Double, // w/h
        rotation: Quaterniond,
        cameraPosition: Vector3d
    ) {

        // todo not completely correct yet, fix that!
        // todo debug the view cone by drawing lines...

        // calculate all planes
        // all positions and normals of the planes

        // near
        positions[0].set(0.0, 0.0, -near)
        normals[0].set(0.0, 0.0, +1.0)

        // far
        positions[1].set(0.0, 0.0, -far)
        normals[1].set(0.0, 0.0, -1.0)

        // the other positions need no rotation
        val pos0 = positions[0]
        val pos1 = positions[1]
        rotation.transform(pos0)
        rotation.transform(pos1)
        pos0.add(cameraPosition)
        pos1.add(cameraPosition)

        // calculate the position of the sideways planes: 0, because they go trough the center
        // then comes the rotation: rotate 0 = 0
        // then add the camera position ->
        // in summary just use the camera position
        for (i in 2 until 6) {
            // assignment is faster than copying :D
            // just the camera position must not change (largely)
            positions[i] = cameraPosition
        }

        // more complicated: calculate the normals of the sideways planes
        val angleY = fovYRadians * 0.5// * borderFixFactor
        val cosY = cos(angleY)
        val sinY = sin(angleY)
        normals[2].set(0.0, +cosY, +sinY)
        normals[3].set(0.0, -cosY, +sinY)

        val angleX = angleY * aspectRatio
        val cosX = cos(angleX)
        val sinX = sin(angleX)
        normals[4].set(+cosX, 0.0, +sinX)
        normals[5].set(-cosX, 0.0, +sinX)

        for (i in 0 until 6) {
            rotation.transform(normals[i])
            val position = positions[i]
            val normal = normals[i]
            val distance = position.dot(normal)
            planes[i].set(normal, -distance)
        }

    }

    // not perfect: the border seems to have issues...
    // -> we just add a small border of 10% and hope it works in all cases
    fun contains(aabb: AABBd): Boolean {
        if (aabb.isEmpty()) return false
        // https://www.gamedev.net/forums/topic/512123-fast--and-correct-frustum---aabb-intersection/
        for (i in 0 until 6) {
            val plane = planes[i]
            val minX = if (plane.x > 0) aabb.minX else aabb.maxX
            val minY = if (plane.y > 0) aabb.minY else aabb.maxY
            val minZ = if (plane.z > 0) aabb.minZ else aabb.maxZ
            // outside
            val dot0 = plane.dot(minX, minY, minZ, 1.0)
            if (dot0 >= 0.0) return false
        }
        return true
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        writer.writeObjectList(this, "stages", stages)
    }

    override fun readObject(name: String, value: ISaveable?) {
        when (name) {
            "stages" -> stages.add(value as? PipelineStage ?: return)
            else -> super.readObject(name, value)
        }
    }

    override fun readObjectArray(name: String, values: Array<ISaveable?>) {
        when (name) {
            "stages" -> {
                stages.clear()
                stages.addAll(values.filterIsInstance<PipelineStage>())
            }
            else -> super.readObjectArray(name, values)
        }
    }

    override val className: String = "Pipeline"
    override val approxSize: Int = 10
    override fun isDefaultValue(): Boolean = false

}
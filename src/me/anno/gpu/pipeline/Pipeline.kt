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
import org.joml.Matrix4f
import org.joml.Vector3d
import org.lwjgl.opengl.GL11.GL_FRONT

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
            for ((index, material) in mesh.materials.withIndex()) {
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
    }

    fun reset() {
        lightPseudoStage.reset()
        for (stage in stages) {
            stage.reset()
        }
    }

    fun fill(rootElement: Entity) {
        rootElement.simpleTraversal(false) { entity ->
            val renderer = entity.getComponent<RendererComponent>(false)
            if (renderer != null) {
                val meshComponents = entity.getComponents<MeshComponent>(false)
                for (meshComponent in meshComponents) {
                    addMesh(meshComponent.mesh, renderer, entity)
                }
            }
            val lights = entity.getComponents<LightComponent>(false)
            for (light in lights) {
                addLight(light.getLightPrimitive(), entity)
            }
            false
        }
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
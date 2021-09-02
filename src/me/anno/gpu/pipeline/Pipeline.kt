package me.anno.gpu.pipeline

import me.anno.ecs.Entity
import me.anno.ecs.components.cache.MaterialCache
import me.anno.ecs.components.cache.MeshCache
import me.anno.ecs.components.light.AmbientLight
import me.anno.ecs.components.light.LightComponent
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.Mesh.Companion.defaultMaterial
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.RendererComponent
import me.anno.engine.ui.render.ECSShaderLib.pbrModelShader
import me.anno.engine.ui.render.Frustum
import me.anno.engine.ui.render.RenderView
import me.anno.gpu.DepthMode
import me.anno.gpu.GFX
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.pipeline.M4x3Delta.set4x3delta
import me.anno.io.ISaveable
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.utils.sorting.MergeSort.mergeSort
import me.anno.utils.structures.Compare.ifDifferent
import org.joml.AABBd
import org.joml.Matrix4f
import org.joml.Vector3d
import org.joml.Vector3f
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
        "lights", Sorting.NO_SORTING, 0, BlendMode.PURE_ADD,
        DepthMode.GREATER, false, GL_FRONT, pbrModelShader
    )
    // private val lightPseudoRenderer = MeshRenderer()

    lateinit var defaultStage: PipelineStage

    var lastClickId = 0

    val frustum = Frustum()

    val ambient = Vector3f()

    private fun addMesh(mesh: Mesh, renderer: RendererComponent, entity: Entity, clickId: Int) {
        val materials = mesh.materials
        if (materials.isEmpty()) {
            val stage = defaultStage
            stage.add(renderer, mesh, entity, 0, clickId)
        } else {
            for (index in materials.indices) {
                val material = MaterialCache[materials[index], defaultMaterial]
                val stage = material.pipelineStage ?: defaultStage
                stage.add(renderer, mesh, entity, index, clickId)
            }
        }
    }

    private fun addMeshDepth(mesh: Mesh, renderer: RendererComponent, entity: Entity) {
        defaultStage.add(renderer, mesh, entity, 0, 0)
    }

    private fun addMeshInstanced(mesh: Mesh, entity: Entity, clickId: Int) {
        val materials = mesh.materials
        if (materials.isEmpty()) {
            val stage = defaultStage
            stage.addInstanced(mesh, entity, 0, clickId)
        } else {
            for (index in materials.indices) {
                val material = MaterialCache[materials[index], defaultMaterial]
                val stage = material.pipelineStage ?: defaultStage
                stage.addInstanced(mesh, entity, index, clickId)
            }
        }
    }

    private fun addMeshInstancedDepth(mesh: Mesh, entity: Entity) {
        defaultStage.addInstanced(mesh, entity, 0, 0)
    }

    private fun addLight(light: LightComponent, entity: Entity, cameraPosition: Vector3d, worldScale: Double) {
        val mesh = light.getLightPrimitive()
        val stage = lightPseudoStage
        // update light transform
        // its drawn position probably should be smoothed -> we probably should use the drawnMatrix instead of the global one
        // we may want to set a timestamp, so we don't update it twice? no, we fill the pipeline only once
        val invWorldTransform = light.invWorldMatrix
        val drawTransform = stage.getDrawMatrix(entity.transform, GFX.gameTime)
        invWorldTransform.identity()
            .set4x3delta(drawTransform, cameraPosition, worldScale)
            .invert()
        if (light.isInstanced) {
            stage.addInstanced(mesh, entity, 0, 0)
        } else {
            stage.add(light, mesh, entity, 0, 0)
        }
    }

    fun draw(cameraMatrix: Matrix4f, cameraPosition: Vector3d, worldScale: Double) {
        for (stage in stages) {
            stage.bindDraw(this, cameraMatrix, cameraPosition, worldScale)
        }
    }

    fun drawDepth(cameraMatrix: Matrix4f, cameraPosition: Vector3d, worldScale: Double) {
        GFX.check()
        defaultStage.drawDepth(this, cameraMatrix, cameraPosition, worldScale)
        GFX.check()
    }

    fun reset() {
        ambient.set(0f)
        lightPseudoStage.reset()
        defaultStage.reset()
        for (stage in stages) {
            stage.reset()
        }
    }

    fun fill(rootElement: Entity, cameraPosition: Vector3d, worldScale: Double) {
        // todo more complex traversal:
        // done exclude static entities by their AABB
        // done exclude entities, if they contain no meshes
        // done exclude entities, if they are off-screen
        // todo reuse the pipeline state for multiple frames
        //  - add a margin, so entities at the screen border can stay visible
        //  - partially populate the pipeline?
        rootElement.validateTransforms()
        rootElement.validateAABBs()
        lastClickId = subFill(rootElement, 1, cameraPosition, worldScale)
        // LOGGER.debug("$contained/$nonContained")
    }

    fun fillDepth(rootElement: Entity, cameraPosition: Vector3d, worldScale: Double){
        rootElement.validateTransforms()
        rootElement.validateAABBs()
        subFillDepth(rootElement, cameraPosition, worldScale)
    }

    // 256 = absolute max number of lights
    // we could make this higher for testing...
    val lights = arrayOfNulls<DrawRequest>(RenderView.MAX_LIGHTS)

    // todo don't always create a list, just fill the data...
    /**
     * creates a list of relevant lights for a forward-rendering draw call of a mesh or region
     * */
    fun getClosestRelevantNLights(region: AABBd, numberOfLights: Int, lights: Array<DrawRequest?>): Int {
        val lightStage = lightPseudoStage
        if (numberOfLights <= 0) return 0
        // todo if there are more than N lights, create a 3D or 4D lookup array: (x/size,y/size,z/size,log2(size)|0)
        if (lightStage.size < numberOfLights) {
            // todo always clear the lights array
            // check if already filled:
            val size = lightStage.size
            if (lights[0] == null) {
                for (i in 0 until size) {
                    lights[i] = lightStage.drawRequests[i]
                }
                // todo lights with shadow could get their own category in the shader
                // todo probably would make more sense, because they can get complicated
                // sort by type, and whether they have a shadow
                mergeSort(lights, 0, size) { a, b ->
                    val va = a!!.component as LightComponent
                    val vb = b!!.component as LightComponent
                    va.hasShadow.compareTo(vb.hasShadow).ifDifferent {
                        va.lightType.shadowMapType.compareTo(vb.lightType.shadowMapType)
                    }
                }
            }// else done
            return size
        } else {
            // todo find the closest / most relevant lights (large ones)

        }
        return 0
    }

    private fun subFill(entity: Entity, clickId0: Int, cameraPosition: Vector3d, worldScale: Double): Int {
        entity.hasBeenVisible = true
        var clickId = clickId0
        val renderer = entity.getComponent(RendererComponent::class, false)
        val components = entity.components
        for (i in components.indices) {
            val component = components[i]
            if (component.isEnabled) {
                if (renderer != null && component is MeshComponent) {
                    val mesh = MeshCache[component.mesh]
                    if (mesh != null) {
                        component.clickId = clickId
                        if (component.isInstanced) {
                            addMeshInstanced(mesh, entity, clickId)
                        } else {
                            addMesh(mesh, renderer, entity, clickId)
                        }
                        clickId++
                    }
                }
                if (component is LightComponent) {
                    addLight(component, entity, cameraPosition, worldScale)
                }
                if (component is AmbientLight) {
                    ambient.add(component.color)
                }
            }
        }
        val children = entity.children
        for (i in children.indices) {
            val child = children[i]
            if (child.isEnabled && frustum.isVisible(child.aabb)) {
                clickId = subFill(child, clickId, cameraPosition, worldScale)
            }
        }
        return clickId
    }

    private fun subFillDepth(entity: Entity, cameraPosition: Vector3d, worldScale: Double) {
        entity.hasBeenVisible = true
        val renderer = entity.getComponent(RendererComponent::class, false)
        val components = entity.components
        for (i in components.indices) {
            val component = components[i]
            if (component.isEnabled) {
                if (renderer != null && component is MeshComponent) {
                    val mesh = MeshCache[component.mesh]
                    if (mesh != null) {
                        if (component.isInstanced) {
                            addMeshInstancedDepth(mesh, entity)
                        } else {
                            addMeshDepth(mesh, renderer, entity)
                        }
                    }
                }
            }
        }
        val children = entity.children
        for (i in children.indices) {
            val child = children[i]
            if (child.isEnabled && frustum.isVisible(child.aabb)) {
                subFillDepth(child, cameraPosition, worldScale)
            }
        }
    }

    fun findDrawnSubject(searchedId: Int, entity: Entity): Any? {
        if (entity.clickId == searchedId) return entity
        val renderer = entity.getComponent(RendererComponent::class, false)
        if (renderer != null) {
            val components = entity.components
            for (i in components.indices) {
                val c = components[i]
                if (c.isEnabled && c is MeshComponent) {
                    if (c.clickId == searchedId) return c
                }
            }
        }
        val children = entity.children
        for (i in children.indices) {
            val child = children[i]
            if (child.isEnabled) {
                val aabb = child.aabb
                val needsDrawing = frustum.isVisible(aabb)
                if (needsDrawing) {
                    val found = findDrawnSubject(searchedId, child)
                    if (found != null) return found
                }
            }
        }
        return null
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
package me.anno.gpu.pipeline

import me.anno.ecs.Entity
import me.anno.ecs.components.cache.MaterialCache
import me.anno.ecs.components.cache.MeshCache
import me.anno.ecs.components.light.AmbientLight
import me.anno.ecs.components.light.LightComponent
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.Mesh.Companion.defaultMaterial
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ui.render.Frustum
import me.anno.engine.ui.render.RenderView
import me.anno.gpu.DepthMode
import me.anno.gpu.GFX
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.pipeline.M4x3Delta.set4x3delta
import me.anno.gpu.pipeline.PipelineStage.Companion.getDrawMatrix
import me.anno.io.ISaveable
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.utils.sorting.MergeSort.mergeSort
import me.anno.utils.structures.Compare.ifDifferent
import org.joml.AABBd
import org.joml.Matrix4f
import org.joml.Vector3d
import org.joml.Vector3f

// todo idea: the scene rarely changes -> we can reuse it, and just update the uniforms
// and the graph may be deep, however meshes may be only in parts of the tree
class Pipeline(val deferred: DeferredSettingsV2) : Saveable() {

    // todo pipelines, that we need:
    //  - 3d world,
    //  - 2d ui,
    //  - ...
    // todo every local player needs its own pipeline to avoid too much sorting

    // todo we can sort by material and shaders...
    // todo or by distance...

    val stages = ArrayList<PipelineStage>()

    // depth doesn't matter for lights
    val lightPseudoStage = PipelineLightStage(DepthMode.ALWAYS, deferred)

    lateinit var defaultStage: PipelineStage

    var lastClickId = 0

    val frustum = Frustum()

    val ambient = Vector3f()

    fun hasTooManyLights(): Boolean {
        return lightPseudoStage.size > RenderView.MAX_FORWARD_LIGHTS
    }

    private fun getDefaultStage(mesh: Mesh, material: Material?): PipelineStage {
        // todo analyse, whether the material has transparency, and if so,
        // todo add it to the transparent pass
        return defaultStage
    }

    private fun addMesh(mesh: Mesh, renderer: MeshComponent, entity: Entity, clickId: Int) {
        val materials = mesh.materials
        if (materials.isEmpty()) {
            val stage = defaultStage
            stage.add(renderer, mesh, entity, 0, clickId)
        } else {
            for (index in materials.indices) {
                val material = MaterialCache[materials[index], defaultMaterial]
                val stage = material.pipelineStage ?: getDefaultStage(mesh, material)
                stage.add(renderer, mesh, entity, index, clickId)
            }
        }
    }

    private fun addMeshDepth(mesh: Mesh, renderer: MeshComponent, entity: Entity) {
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
        // for debugging of the light shapes
        // addMesh(light.getLightPrimitive(), MeshRenderer(), entity, 0)
        val stage = lightPseudoStage
        // update light transform
        // its drawn position probably should be smoothed -> we probably should use the drawnMatrix instead of the global one
        // we may want to set a timestamp, so we don't update it twice? no, we fill the pipeline only once
        val invWorldTransform = light.invWorldMatrix
        val drawTransform = getDrawMatrix(entity.transform, GFX.gameTime)
        invWorldTransform.identity()
            .set4x3delta(drawTransform, cameraPosition, worldScale)
            .invert()
        stage.add(light, entity)
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
        lights.fill(null)
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

    fun fillDepth(rootElement: Entity, cameraPosition: Vector3d, worldScale: Double) {
        rootElement.validateTransforms()
        rootElement.validateAABBs()
        subFillDepth(rootElement, cameraPosition, worldScale)
    }

    // todo fix deferred rendering for scenes with many lights

    val lights = arrayOfNulls<LightRequest<*>>(RenderView.MAX_FORWARD_LIGHTS)

    /**
     * creates a list of relevant lights for a forward-rendering draw call of a mesh or region
     * */
    fun getClosestRelevantNLights(region: AABBd, numberOfLights: Int, lights: Array<LightRequest<*>?>): Int {
        val lightStage = lightPseudoStage
        if (numberOfLights <= 0) return 0
        val size = lightStage.size
        if (size < numberOfLights) {
            // check if already filled:
            if (lights[0] == null) {
                for (i in 0 until size) {
                    lights[i] = lightStage[i]
                }
                // sort by type, and whether they have a shadow
                mergeSort(lights, 0, size) { a, b ->
                    val va = a!!.light
                    val vb = b!!.light
                    va.hasShadow.compareTo(vb.hasShadow).ifDifferent {
                        va.lightType.shadowMapType.compareTo(vb.lightType.shadowMapType)
                    }
                }
            }// else done
            return size
        } else {
            // todo if there are more than N lights, create a 3D or 4D lookup array: (x/size,y/size,z/size,log2(size)|0)
            // todo find the closest / most relevant lights (large ones)

        }
        return 0
    }

    private fun subFill(entity: Entity, clickId0: Int, cameraPosition: Vector3d, worldScale: Double): Int {
        entity.hasBeenVisible = true
        var clickId = clickId0
        val components = entity.components
        for (i in components.indices) {
            val component = components[i]
            if (component.isEnabled) {
                if (component is MeshComponent) {
                    val mesh = MeshCache[component.mesh]
                    if (mesh != null) {
                        component.clickId = clickId
                        if (component.isInstanced) {
                            addMeshInstanced(mesh, entity, clickId)
                        } else {
                            addMesh(mesh, component, entity, clickId)
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
        val components = entity.components
        for (i in components.indices) {
            val component = components[i]
            if (component.isEnabled) {
                if (component is MeshComponent) {
                    val mesh = MeshCache[component.mesh]
                    if (mesh != null) {
                        if (component.isInstanced) {
                            addMeshInstancedDepth(mesh, entity)
                        } else {
                            addMeshDepth(mesh, component, entity)
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
        val components = entity.components
        for (i in components.indices) {
            val c = components[i]
            if (c.isEnabled && c is MeshComponent) {
                if (c.clickId == searchedId) return c
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
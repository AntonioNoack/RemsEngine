package me.anno.ecs.components.light.sky

import me.anno.cache.AsyncCacheData
import me.anno.cache.FileCacheList
import me.anno.ecs.Transform
import me.anno.ecs.annotations.Type
import me.anno.ecs.components.collider.CollidingComponent
import me.anno.ecs.components.light.sky.shaders.SkyShaderBase
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.material.utils.TypeValue
import me.anno.ecs.interfaces.Renderable
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.raycast.RayQuery
import me.anno.engine.raycast.Raycast
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.engine.serialization.SerializedProperty
import me.anno.engine.ui.render.RenderState
import me.anno.gpu.GFXState
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.shader.GLSLType
import me.anno.mesh.Shapes
import me.anno.utils.types.Booleans.hasFlag
import org.joml.AABBd
import org.joml.Matrix4x3
import org.joml.Quaternionf
import org.joml.Vector3f

open class SkyboxBase : CollidingComponent(), Renderable {

    @NotSerializedProperty
    var shader: SkyShaderBase?
        get() = material.shader as? SkyShaderBase
        set(value) {
            material.shader = value
        }

    @NotSerializedProperty
    val material: Material = Material()

    @Type("Color3HDR")
    @SerializedProperty
    var skyColor: Vector3f = Vector3f(0.48f, 0.81f, 1.68f)
        set(value) {
            field.set(value)
        }

    @SerializedProperty
    var worldRotation = Quaternionf()
        set(value) {
            field.set(value)
        }

    val materials: FileCacheList<Material>

    init {
        // rendering properties
        material.shader = defaultShaderBase
        material.shaderOverrides["skyColor"] = TypeValue(GLSLType.V3F, skyColor)
        material.shaderOverrides["worldRot"] = TypeValue(GLSLType.V4F, worldRotation)
        material.shaderOverrides["reversedDepth"] =
            TypeValue(GLSLType.V1B, { GFXState.depthMode.currentValue.reversedDepth })
        material.shaderOverrides["isPerspective"] = TypeValue(GLSLType.V1B, { RenderState.isPerspective })
        materials = FileCacheList(listOf(material.ref)) { AsyncCacheData(material) }
    }

    override fun hasRaycastType(typeMask: Int) = typeMask.hasFlag(Raycast.SKY)
    override fun raycast(query: RayQuery): Boolean {
        return if (query.result.distance >= 1e308) {
            query.result.distance = 1e308
            query.result.positionWS.set(query.direction)
                .normalize()
                .mul(1e308) // cannot be multiplied in one step, because it could overflow to Infinity
            true
        } else false
    }

    override fun fill(pipeline: Pipeline, transform: Transform) {
        pipeline.skybox = this
        clickId = pipeline.getClickId(this)
    }

    open fun getMesh() = mesh

    override fun fillSpace(globalTransform: Matrix4x3, dstUnion: AABBd) {
        dstUnion.all() // skybox is visible everywhere
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is SkyboxBase) return
        dst.shader = shader
        dst.skyColor.set(skyColor)
        dst.worldRotation.set(worldRotation)
    }

    companion object {
        val mesh = Shapes.smoothCube.back
        val defaultShaderBase = SkyShaderBase("skyBase")
    }
}
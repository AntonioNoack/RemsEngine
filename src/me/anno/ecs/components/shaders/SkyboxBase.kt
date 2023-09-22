package me.anno.ecs.components.shaders

import me.anno.Time
import me.anno.ecs.Entity
import me.anno.ecs.annotations.Type
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.MeshComponentBase
import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.raycast.RayHit
import me.anno.engine.ui.render.RenderState
import me.anno.gpu.GFXState
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.shader.GLSLType
import me.anno.io.serialization.NotSerializedProperty
import me.anno.io.serialization.SerializedProperty
import me.anno.mesh.Shapes
import org.joml.*

open class SkyboxBase : MeshComponentBase() {

    // todo override raytracing for clicking: if ray goes far enough, let it click us

    init {
        castShadows = false
        receiveShadows = false
    }

    @SerializedProperty
    var shader: SkyShaderBase?
        get() = material.shader as? SkyShaderBase
        set(value) {
            material.shader = value
        }

    @NotSerializedProperty
    val material: Material = Material()

    @Type("Color3HDR")
    @SerializedProperty
    var skyColor: Vector3f = Vector3f(0.2f, 0.4f, 0.6f)
        set(value) {
            field.set(value)
        }

    @SerializedProperty
    var worldRotation = Quaternionf()
        set(value) {
            field.set(value)
        }

    init {
        material.shader = defaultShaderBase
        material.shaderOverrides["skyColor"] = TypeValue(GLSLType.V3F, skyColor)
        material.shaderOverrides["worldRot"] = TypeValue(GLSLType.V4F, worldRotation)
        material.shaderOverrides["reversedDepth"] =
            TypeValue(GLSLType.V1B, { GFXState.depthMode.currentValue.reversedDepth })
        material.shaderOverrides["isPerspective"] = TypeValue(GLSLType.V1B, { RenderState.isPerspective })
        materials = listOf(material.ref)
    }

    override fun hasRaycastType(typeMask: Int) = false
    override fun raycast(
        entity: Entity,
        start: Vector3d,
        direction: Vector3d,
        end: Vector3d,
        radiusAtOrigin: Double,
        radiusPerUnit: Double,
        typeMask: Int,
        includeDisabled: Boolean,
        result: RayHit
    ) = false

    override fun fill(
        pipeline: Pipeline,
        entity: Entity,
        clickId: Int
    ): Int {
        lastDrawn = Time.gameTimeN
        pipeline.skybox = this
        this.clickId = clickId
        return clickId + 1
    }

    override fun getMesh() = mesh

    init {
        globalAABB.all()
        localAABB.all()
    }

    override fun fillSpace(globalTransform: Matrix4x3d, aabb: AABBd): Boolean {
        aabb.all() // skybox is visible everywhere
        return true
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as SkyboxBase
        dst.shader = shader
        dst.skyColor.set(skyColor)
        dst.worldRotation.set(worldRotation)
    }

    override val className: String get() = "SkyboxBase"

    companion object {
        val mesh = Shapes.smoothCube.back
        val defaultShaderBase = SkyShaderBase("skyBase")
            .apply {
                ignoreNameWarnings(
                    "diffuseBase", "normalStrength", "emissiveBase",
                    "roughnessMinMax", "metallicMinMax", "occlusionStrength", "finalTranslucency", "finalClearCoat",
                    "tint", "hasAnimation", "localTransform", "invLocalTransform", "worldScale", "tiling",
                    "forceFieldColorCount", "forceFieldUVCount",
                )
            }
    }
}
package me.anno.ecs.components.light

import me.anno.ecs.annotations.HideInInspector
import me.anno.ecs.annotations.Range
import me.anno.ecs.components.light.PointLight.Companion.cubeMesh
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.io.serialization.NotSerializedProperty
import kotlin.math.tan

// a cone light
class SpotLight() : LightComponent(LightType.SPOT) {

    constructor(src: SpotLight) : this() {
        src.copy(this)
    }

    // for a large angle, it just becomes a point light
    @Range(0.0, 100.0)
    var coneAngle = 1.0

    @HideInInspector
    @NotSerializedProperty
    val fovRadians
        get() = 2.0 * tan(coneAngle)

    // for deferred rendering, this could be optimized
    override fun getLightPrimitive(): Mesh = cubeMesh

    override fun clone(): SpotLight {
        return SpotLight(this)
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SpotLight
        clone.coneAngle = coneAngle
    }

    override val className: String = "SpotLight"

    companion object {
        val coneFunction = "smoothstep(0.0, 1.0, (-localNormal.z-(1.0-coneAngle))/coneAngle)"
    }

}
package me.anno.ecs.components.light.sky

import me.anno.Time
import me.anno.ecs.Entity
import me.anno.ecs.annotations.Docs
import me.anno.ecs.annotations.Group
import me.anno.ecs.annotations.Range
import me.anno.ecs.annotations.Type
import me.anno.ecs.components.light.DirectionalLight
import me.anno.ecs.components.light.sky.shaders.SkyShader
import me.anno.ecs.components.mesh.material.utils.TypeValue
import me.anno.ecs.components.mesh.material.utils.TypeValueV3
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.systems.OnUpdate
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.engine.serialization.SerializedProperty
import me.anno.engine.ui.render.DefaultSun
import me.anno.gpu.shader.GLSLType
import me.anno.utils.pooling.JomlPools
import org.joml.Quaternionf
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.max

open class Skybox : SkyboxBase(), OnUpdate {

    // todo the sky controls ambient and primary directional light...
    //  -> make the sky actually control the primary directional light

    @SerializedProperty
    var sunRotation: Quaternionf = Quaternionf(DefaultSun.defaultSunEntity.rotation)
        set(value) {
            field.set(value)
                .normalize()
        }

    @Docs("Property for automatic daylight cycle; set the z-euler property, when sunRotation has an x-euler value and vice-versa")
    @SerializedProperty
    var sunSpeed: Quaternionf = Quaternionf()

    @Range(0.0, 1.0)
    @Group("Cirrus")
    @SerializedProperty
    var cirrus: Float = 0.4f

    @Group("Cirrus")
    @SerializedProperty
    var cirrusOffset: Vector3f = Vector3f()
        set(value) {
            field.set(value)
        }

    @Group("Cirrus")
    @SerializedProperty
    var cirrusSpeed: Vector3f = Vector3f(0.005f, 0f, 0f)
        set(value) {
            field.set(value)
        }

    @Range(0.0, 1.0)
    @Group("Cumulus")
    @SerializedProperty
    var cumulus: Float = 0.8f

    @Group("Cumulus")
    @SerializedProperty
    var cumulusOffset: Vector3f = Vector3f()
        set(value) {
            field.set(value)
        }

    @Group("Cumulus")
    @SerializedProperty
    var cumulusSpeed = Vector3f(0.03f, 0f, 0f)
        set(value) {
            field.set(value)
        }

    @Type("Color3HDR")
    @SerializedProperty
    var nadirColor = Vector3f()
        set(value) {
            field.set(value)
            nadir.set(value.x, value.y, value.z, nadirSharpness)
        }

    @Range(0.0, 1e9)
    @SerializedProperty
    var nadirSharpness
        get() = nadir.w
        set(value) {
            nadir.w = value
        }

    @NotSerializedProperty
    private var nadir = Vector4f(0f, 0f, 0f, 1f)
        set(value) {
            field.set(value)
            nadirColor.set(value.x, value.y, value.z)
        }

    @SerializedProperty
    var sunBaseDir = Vector3f(0f, 0f, 1f) // like directional lights
        set(value) {
            field.set(value).safeNormalize()
        }

    @SerializedProperty
    var spherical = false

    init {
        material.shader = defaultShader
        material.shaderOverrides["cirrus"] = TypeValue(GLSLType.V1F) { cirrus }
        material.shaderOverrides["cumulus"] = TypeValue(GLSLType.V1F) { cumulus }
        material.shaderOverrides["nadir"] = TypeValue(GLSLType.V4F, nadir)
        material.shaderOverrides["cirrusOffset"] = TypeValue(GLSLType.V3F, cirrusOffset)
        material.shaderOverrides["cumulusOffset"] = TypeValue(GLSLType.V3F, cumulusOffset)
        material.shaderOverrides["sphericalSky"] = TypeValue(GLSLType.V1B) { spherical }
        material.shaderOverrides["sunDir"] = TypeValueV3(GLSLType.V3F, Vector3f()) {
            it.set(sunBaseDir).rotate(sunRotation)
        }
    }

    override fun onUpdate() {
        val dt = Time.deltaTime.toFloat()
        cirrusSpeed.mulAdd(dt, cirrusOffset, cirrusOffset)
        cumulusSpeed.mulAdd(dt, cumulusOffset, cumulusOffset)
        sunRotation.mul(JomlPools.quat4f.borrow().identity().slerp(sunSpeed, dt))
    }

    fun applyOntoSun(sun: Entity, sun1: DirectionalLight, brightness: Float) {
        // only works if sunBaseDir is 1.0
        val sunDir = Vector3f(sunBaseDir).rotate(sunRotation)
        val sr = sunRotation
        val wr = worldRotation
        sun.rotation = sun.rotation
            .set(sr.x.toDouble(), sr.y.toDouble(), sr.z.toDouble(), sr.w.toDouble())
            .mul(
                wr.x.toDouble(),
                wr.y.toDouble(),
                wr.z.toDouble(),
                wr.w.toDouble()
            ) // todo correct order... not working
        // todo set color based on angle, including red in the twilight
        val light = brightness * max(sunDir.y, 1e-9f)
        val old = max(sun1.color.x, max(sun1.color.y, sun1.color.z))
        sun1.color.mul(light / old)
        sun.transform.teleportUpdate()
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is Skybox) return
        dst.sunRotation.set(sunRotation)
        dst.sunBaseDir.set(sunBaseDir)
        dst.cirrus = cirrus
        dst.cumulus = cumulus
        dst.cumulusSpeed.set(cumulusSpeed)
        dst.cumulusOffset.set(cumulusOffset)
        dst.cirrusSpeed.set(cirrusSpeed)
        dst.cirrusOffset.set(cirrusOffset)
        dst.nadir.set(nadir)
        dst.sunSpeed.set(sunSpeed)
    }

    companion object {
        val defaultShader = SkyShader("skybox")
        val defaultSky = Skybox()
    }
}
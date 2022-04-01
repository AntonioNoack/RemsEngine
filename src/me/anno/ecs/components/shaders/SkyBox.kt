package me.anno.ecs.components.shaders

import me.anno.ecs.Component
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.MeshBaseComponent
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.shader.BaseShader
import me.anno.io.serialization.NotSerializedProperty
import me.anno.io.serialization.SerializedProperty
import me.anno.mesh.Shapes
import me.anno.utils.types.AABBs.all
import org.joml.AABBd
import org.joml.Matrix4x3d

class SkyBox : MeshBaseComponent() {

    @SerializedProperty
    var shader
        get() = material.shader
        set(value) {
            material.shader = value
        }

    @NotSerializedProperty
    val material = Material.create()

    init {
        material.shader
        materials = listOf(material.ref!!)
    }

    override fun getMesh() = Shapes.cube11Smooth

    override fun fillSpace(globalTransform: Matrix4x3d, aabb: AABBd): Boolean {
        aabb.all() // skybox is visible everywhere
        return true
    }

    override fun clone(): Component {
        val clone = SkyBox()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SkyBox
        clone.shader = shader
    }

    override val className = "SkyBox"

    companion object {



        /**
         * using the Nishita Sky Model from Blender
         * (An Analytic Model for Full Spectral Sky-Dome Radiance,
         * Adding a Solar Radiance Function to the Hosek Skylight Model)
         * */
        val defaultShader = BaseShader()
    }

}
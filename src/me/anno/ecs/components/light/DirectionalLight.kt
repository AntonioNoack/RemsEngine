package me.anno.ecs.components.light

import me.anno.ecs.components.light.PointLight.Companion.cubeMesh
import me.anno.ecs.components.mesh.Mesh

class DirectionalLight : LightComponent(LightType.DIRECTIONAL) {

    override fun getLightPrimitive(): Mesh = cubeMesh

    override fun clone(): DirectionalLight {
        val clone = DirectionalLight()
        copy(clone)
        return clone
    }

    override val className: String = "DirectionalLight"

}
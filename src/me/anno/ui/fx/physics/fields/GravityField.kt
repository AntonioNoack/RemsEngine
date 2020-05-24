package me.anno.ui.fx.physics.fields

import org.joml.Vector3f
import me.anno.ui.fx.physics.ForceField

class GravityField: ForceField() {

    var gravity = -9.81f

    override fun getForce(position: Vector3f, speed: Vector3f, particleId: Int, time: Float): Vector3f = Vector3f(0f, gravity, 0f)
    override fun getShaderUniforms(): String = "uniform float gravity;\n"
    override fun getShaderForce(): String = "vec3 force = vec3(gravity);\n"

}
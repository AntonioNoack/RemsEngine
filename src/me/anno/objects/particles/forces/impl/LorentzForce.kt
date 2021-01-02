package me.anno.objects.particles.forces.impl

import me.anno.language.translation.Dict
import me.anno.objects.particles.Particle
import me.anno.objects.particles.ParticleState
import me.anno.ui.editor.color.spaces.HSLuv
import me.anno.ui.editor.sceneView.Grid
import me.anno.utils.Vectors.times
import org.joml.Matrix4fArrayList
import org.joml.Vector3f
import org.joml.Vector4f

class LorentzForce : PerParticleForce(
    Dict["Lorentz Force", "obj.force.lorentz"],
    Dict["Circular motion by velocity and rotation axis", "obj.force.lorentz.desc"]
) {

    override fun getForce(state: ParticleState, time: Double, particles: List<Particle>): Vector3f {
        val v = state.dPosition
        return v.cross(getDirection(time) * strength[time], Vector3f())
    }

    override fun onDraw(stack: Matrix4fArrayList, time: Double, color: Vector4f) {
        super.onDraw(stack, time, color)
        // todo lorentz fields, which are locally limited
        // draw the rod
        // Grid.drawLine(stack, color * red, Vector3f(0f, +1f, 0f), Vector3f(0f, +0f, 0f))
        // Grid.drawLine(stack, color * blue, Vector3f(0f, 0f, 0f), Vector3f(0f, -1f, 0f))
    }

    override fun getClassName() = "LorentzForce"

    companion object {
        // must be lazy, because it calls shader.compile()
        private val blue by lazy { HSLuv.toRGB(0.719, 0.965, 0.601, 1.000) }
        private val red by lazy { HSLuv.toRGB(0.042, 0.965, 0.601, 1.000) }
    }

}
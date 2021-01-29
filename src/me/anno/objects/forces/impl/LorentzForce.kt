package me.anno.objects.forces.impl

import me.anno.objects.particles.Particle
import me.anno.objects.particles.ParticleState
import me.anno.ui.editor.color.spaces.HSLuv
import me.anno.utils.types.Vectors.times
import org.joml.Vector3f

// todo lorentz fields, which are locally limited (magnets)
class LorentzForce : PerParticleForce(
    "Lorentz Force",
    "Circular motion by velocity and rotation axis", "lorentz"
) {

    override fun getForce(state: ParticleState, time: Double, particles: List<Particle>): Vector3f {
        val v = state.dPosition
        return v.cross(getDirection(time) * strength[time], Vector3f())
    }

    //override fun onDraw(stack: Matrix4fArrayList, time: Double, color: Vector4f) {
        //super.onDraw(stack, time, color)
        // draw the rod
        // Grid.drawLine(stack, color * red, Vector3f(0f, +1f, 0f), Vector3f(0f, +0f, 0f))
        // Grid.drawLine(stack, color * blue, Vector3f(0f, 0f, 0f), Vector3f(0f, -1f, 0f))
    //}

    override fun getClassName() = "LorentzForce"

    companion object {
        // must be lazy, because it calls shader.compile()
        private val blue by lazy { HSLuv.toRGB(0.719, 0.965, 0.601, 1.000) }
        private val red by lazy { HSLuv.toRGB(0.042, 0.965, 0.601, 1.000) }
    }

}
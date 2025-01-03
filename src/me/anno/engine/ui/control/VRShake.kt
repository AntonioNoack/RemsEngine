package me.anno.engine.ui.control

import me.anno.Time
import me.anno.Time.uiDeltaTime
import me.anno.engine.Events.addEvent
import me.anno.input.VROffset
import me.anno.maths.Maths.dtTo10
import me.anno.maths.noise.PerlinNoise
import me.anno.utils.pooling.JomlPools

class VRShake(val falloff: Float, val strength: Float) {

    companion object {
        private const val noiseSpeed = 30f
        fun startShaking(falloff: Float = 5f, strength: Float = 0.05f) {
            VRShake(falloff, strength).updateShake()
        }
    }

    val noise = PerlinNoise(Time.nanoTime, 5, 0.5f, -1f, 1f)
    var time = 0f

    fun updateShake() {
        // todo we refactored this, so check that it still works
        val dt = uiDeltaTime.toFloat()
        val amplitude = dtTo10(time * falloff)
        val additional = VROffset.additionalRotation
        val ry = additional.getEulerAnglesYXZ(JomlPools.vec3f.borrow()).y
        val x = time * noiseSpeed
        additional.rotationYXZ(
            ry + noise[x, 2f] * amplitude,
            noise[x, 0f] * amplitude,
            noise[x, 1f] * amplitude
        )
        if (amplitude > 1e-9f) {
            time += dt
            addEvent(1, ::updateShake)
        }
    }
}
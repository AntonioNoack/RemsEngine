package me.anno.audio.remfx.modulator

import me.anno.audio.remfx.RemFXContext
import me.anno.audio.remfx.effect.Sound
import me.anno.engine.ui.render.RenderState
import me.anno.maths.Maths.PIf
import me.anno.maths.Maths.sq
import me.anno.utils.pooling.JomlPools
import org.joml.Vector2f
import org.joml.Vector3f
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class SpacialModulator(sound: Sound) : Modulator(sound) {

    // todo filter high sounds in the distance? (should be easy to compute, just use a low-pass)
    // todo allow frequency distortion by moving relatively? we could just adjust the time...

    val position = Vector3f()
    var volume = 32e3f
    var radius = 100f

    override fun update(context: RemFXContext) {
        val falloff = 5f / sq(radius)
        val (vl, vr) = calculateStereoAttenuation(position.x, position.y, position.z, volume, falloff)
        setVolume(vl, vr)
    }

    companion object {
        fun calculateStereoAttenuation(
            px: Float, py: Float, pz: Float,
            volume: Float, falloff: Float,
            dst: Vector2f = JomlPools.vec2f.create(),
        ): Vector2f {
            val diff = JomlPools.vec3f.create()
            val pos = RenderState.cameraPosition
            diff.set(
                px - pos.x,
                py - pos.y,
                pz - pos.z,
            ).rotateInv(RenderState.cameraRotation)

            val diffSq = diff.lengthSquared()
            val invDist = 1f / sqrt(diffSq + 0.1f)
            val forward = 0.8f - diff.z * invDist * 0.2f // penalty for being behind the sound
            val amplitude = volume / (1f + falloff * diffSq) * forward

            // equal-power panning
            val pan = diff.x * invDist
            val angle = (pan + 1f) * PIf * 0.25f

            return dst.set(
                cos(angle) * amplitude,
                sin(angle) * amplitude
            )
        }
    }
}
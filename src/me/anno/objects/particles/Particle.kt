package me.anno.objects.particles

import me.anno.gpu.GFX
import me.anno.objects.Transform
import me.anno.objects.forces.ForceField
import me.anno.utils.Floats.toRadians
import me.anno.utils.Maths
import me.anno.utils.Vectors.plus
import me.anno.utils.Vectors.times
import me.anno.utils.structures.UnsafeArrayList
import me.anno.video.MissingFrameException
import org.joml.Matrix4fArrayList
import org.joml.Vector3f
import org.joml.Vector4f

class Particle(
    var type: Transform,
    val birthTime: Double,
    val lifeTime: Double,
    val mass: Float
) {

    val states = UnsafeArrayList<ParticleState>()

    val position = Vector3f()
    val rotation = Vector3f()
    val color = Vector3f(1f)

    var opacity = 1f
    val scale = Vector3f(1f)

    fun lastTime(simulationStep: Double) = birthTime + (states.size - 2) * simulationStep

    fun getValue(index0: Int, indexF: Float, dst: Vector3f, getValue: (ParticleState) -> Vector3f): Vector3f {
        val state0 = states.getOrElse(index0) { states.last() }
        val state1 = states.getOrElse(index0 + 1) { states.last() }
        return getValue(state0).lerp(getValue(state1), indexF, dst)
    }

    fun getPosition(index0: Int, indexF: Float): Vector3f {
        val state0 = states.getOrElse(index0) { states.last() }
        val state1 = states.getOrElse(index0 + 1) { states.last() }
        return state0.position.lerp(state1.position, indexF, position)
    }

    fun getRotation(index0: Int, indexF: Float): Vector3f {
        val state0 = states.getOrElse(index0) { states.last() }
        val state1 = states.getOrElse(index0 + 1) { states.last() }
        return state0.rotation.lerp(state1.rotation, indexF, rotation)
    }

    fun getColor(index0: Int, indexF: Float): Vector3f {
        val state0 = states.getOrElse(index0) { states.last() }
        val state1 = states.getOrElse(index0 + 1) { states.last() }
        return state0.color.lerp(state1.color, indexF, color)
    }

    fun isAlive(time: Double) = (time - birthTime) in 0.0..lifeTime

    fun getLifeOpacity(time: Double, timeStep: Double, fadingIn: Double, fadingOut: Double): Double {
        if (lifeTime < timeStep) return 0.0
        val particleTime = time - birthTime
        if (particleTime <= 0.0 || particleTime >= lifeTime) return 0.0
        val fading = fadingIn + fadingOut
        if (fading > lifeTime) {
            return getLifeOpacity(time, timeStep, lifeTime * fadingIn / fading, lifeTime * fadingOut / fading)
        }
        if (particleTime < fadingIn) return particleTime / fadingIn
        if (particleTime > lifeTime - fadingOut) return (lifeTime - particleTime) / fadingOut
        return 1.0
    }

    fun draw(
        stack: Matrix4fArrayList,
        time: Double, color: Vector4f,
        simulationStep: Double,
        fadeIn: Double, fadeOut: Double
    ) {
        val lifeOpacity = getLifeOpacity(time, simulationStep, fadeIn, fadeOut).toFloat()
        val opacity = Maths.clamp(lifeOpacity * opacity, 0f, 1f)
        if (opacity > 1e-3f) {// else not visible
            stack.pushMatrix()

            try {

                val particleTime = time - birthTime
                val index = particleTime / simulationStep
                val index0 = index.toInt()
                val indexF = Maths.fract(index).toFloat()

                val state0 = states.getOrElse(index0) { states.last() }
                val state1 = states.getOrElse(index0 + 1) { states.last() }

                val position = state0.position.lerp(state1.position, indexF, position)
                val rotation = state0.rotation.lerp(state1.rotation, indexF, rotation)

                if(position.lengthSquared() > 1e-26f) stack.translate(position)
                if (rotation.y != 0f) stack.rotateY(rotation.y.toRadians())
                if (rotation.x != 0f) stack.rotateX(rotation.x.toRadians())
                if (rotation.z != 0f) stack.rotateZ(rotation.z.toRadians())
                if(scale.x != 1f || scale.y != 1f || scale.z != 1f) stack.scale(scale)

                val color0 = state0.color.lerp(state1.color, indexF, this.color)

                // normalize time for calculated functions?
                // node editor? like in Blender or Unreal Engine
                val particleColor = Vector4f(color0, opacity).mul(color)
                type.draw(stack, time - birthTime, particleColor)

            } catch (e: IndexOutOfBoundsException) {
                if (GFX.isFinalRendering) throw MissingFrameException("$this")
            }

            stack.popMatrix()
        }
    }

    fun step(simulationStep: Double, forces: List<ForceField>, aliveParticles: List<Particle>) {

        val oldState = states.last()
        val force = Vector3f()
        val time = states.size * simulationStep + birthTime
        forces.forEach { field ->
            val subForce = field.getForce(oldState, time, aliveParticles)
            val forceLength = subForce.length()
            if (forceLength.isFinite()) {
                force.add(
                    if (forceLength < 1000f) {
                        subForce
                    } else {
                        subForce * (1000f / forceLength)
                    }
                )
            }
        }
        val ddPosition = force / mass
        val dt = simulationStep.toFloat()
        val dPosition = oldState.dPosition + ddPosition * dt
        val position = oldState.position + dPosition * dt
        val newState = ParticleState()
        newState.position = position
        newState.dPosition = dPosition
        newState.rotation = oldState.rotation + oldState.dRotation * dt
        newState.dRotation = oldState.dRotation // todo rotational friction or acceleration???...
        newState.color = oldState.color
        states.add(newState)

    }

}
package me.anno.objects.particles

import me.anno.objects.Transform
import org.joml.Quaternionf
import org.joml.Vector3f

class ParticleInfo(
    var type: Transform,
    var birthIndex: Int,
    var lifeIndices: Int,
    val mass: Float){

    val states = ArrayList<ParticleState>()

    fun getPosition(index0: Int, indexF: Float): Vector3f {
        val state0 = states[index0]
        val state1 = states[index0+1]
        return state0.position.lerp(state1.position, indexF)
    }

    fun getRotation(index0: Int, indexF: Float): Quaternionf {
        val state0 = states[index0]
        val state1 = states[index0+1]
        return state0.rotation.slerp(state1.rotation, indexF)
    }

    fun getScale(index0: Int, indexF: Float): Vector3f {
        val state0 = states[index0]
        val state1 = states[index0+1]
        return state0.scale.lerp(state1.scale, indexF)
    }

    fun getLifeOpacity(time: Float, timeStep: Float, fadingIn: Float, fadingOut: Float): Float {
        if(lifeIndices < 1) return 0f
        val lifeTime = lifeIndices * timeStep
        val localTime = time - birthIndex * timeStep
        if(localTime < 0f || localTime > lifeTime) return 0f
        val fading = fadingIn + fadingOut
        if(fading > lifeTime){
            return getLifeOpacity(time, timeStep, lifeTime * fadingIn/fading, lifeTime * fadingOut/fading)
        }
        if(localTime < fadingIn) return localTime/fadingIn
        if(localTime > lifeTime - fadingOut) return (lifeTime-localTime)/fadingOut
        return 1f
    }

}
package me.anno.ui.fx.physics

import me.anno.gpu.framebuffer.Framebuffer
import org.joml.Vector3f

// todo do the physics on the gpu, or the cpu???
// todo until 10k particles, cpu should be fine,
// todo but the more the betta :D

// todo bake physics?
// todo easy particles on the me.anno.gpu,
// todo more complex ones on the cpu???

// todo no, just calculate the effects and physics on the me.anno.gpu, using float textures :D

class ParticleGroup(val size: Int, val frameCount: Int){

    // todo particle by time?
    // todo full life on texture?
    // todo too heavy texture??

    val forceFields = ArrayList<ForceField>()
    val postProcessingFields = ArrayList<ForceField>() // don't effect movement

    val data0 = Framebuffer(size, frameCount/2, 2, true, false)
    val data1 = Framebuffer(size, (frameCount+1)/2, 2, true, false)

    fun destroy(){
        data0.destroy()
        data1.destroy()
    }

    fun getSrc(frame: Int) = if(frame % 2 == 0) data0 else data1
    fun getDst(frame: Int) = if(frame % 2 == 0) data1 else data0

    fun getSrcRow(frame: Int) = frame/2
    fun getDstRow(frame: Int) = (frame+1)/2

    fun initLocations(locations: Array<Vector3f>){

    }

    fun initVelocities(velocities: Array<Vector3f>){

    }

    fun initRotations(rotations: Array<Vector3f>){

    }

}
package me.anno.objects.particles

import org.joml.Quaternionf
import org.joml.Vector3f

class ParticleState(){

    val position = Vector3f()
    val dPosition = Vector3f()

    val rotation = Quaternionf()
    val dRotation = Vector3f()

    val scale = Vector3f()

}
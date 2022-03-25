package me.anno.ecs.components.mesh.sdf.modifiers

import org.joml.Vector3f

/** sample on how you can create bends, e.g. for bridges; is equal to Inigo Quilez' "cheap bend op" */
fun sdfBend(source: Vector3f, destination: Vector3f, origin: Vector3f, strength: Float): SDFTwist {
    val twist = SDFTwist()
    twist.source = source
    twist.destination = destination
    twist.center = origin
    twist.strength = strength
    return twist
}
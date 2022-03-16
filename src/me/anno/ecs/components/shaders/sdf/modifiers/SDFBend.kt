package me.anno.ecs.components.shaders.sdf.modifiers

import org.joml.Vector3f

// todo 2d shapes, somehow combine them into 3d...
// https://www.iquilezles.org/www/articles/distfunctions2d/distfunctions2d.htm
// https://www.shadertoy.com/playlist/MXdSRf&from=36&num=12

// todo more 3d shapes
// https://www.iquilezles.org/www/articles/distfunctions/distfunctions.htm


/** sample on how you can create bends, e.g. for bridges; is equal to Inigo Quilez' "cheap bend op" */
fun sdfBend(source: Vector3f, destination: Vector3f, origin: Vector3f, strength: Float): SDFTwist {
    val twist = SDFTwist()
    twist.source = source
    twist.destination = destination
    twist.center = origin
    twist.strength = strength
    return twist
}
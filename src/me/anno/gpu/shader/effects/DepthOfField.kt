package me.anno.gpu.shader.effects

import kotlin.math.abs

// todo near blurred with coc is single image at largest coc:
// todo blurring there is wanted

// todo in focus has no blurring

// todo far buffers, maybe multiples, rendered without the front objects to avoid bleeding

object DepthOfField {

    fun circleOfConfusion(aperture: Float, focalLength: Float, objectDistance: Float,
        planeInFocus: Float) =
        abs(aperture * (focalLength * (objectDistance - planeInFocus)) / (objectDistance * (planeInFocus - focalLength)))

    fun objectDistance(z: Float, zNear: Float, zFar: Float) = -zFar * zNear / (z * (zFar - zNear) - zFar)

    fun circleOfConfusion2(z: Float, cocScale: Float, cocBias: Float) = abs(z * cocScale + cocBias)

    fun cocScale(aperture: Float, focalLength: Float, planeInFocus: Float, zNear: Float, zFar: Float) =
        (aperture * focalLength * planeInFocus * (zFar - zNear)) / ((planeInFocus - focalLength) * zNear * zFar)

    fun cocBias(aperture: Float, focalLength: Float, planeInFocus: Float, zNear: Float, zFar: Float) =
        (aperture * focalLength * (zNear - planeInFocus)) / ((planeInFocus * focalLength) * zNear)

    // todo create a dof effect:
    // todo create some, 3? textures:
    // todo pure, 1/4, 1/16

    // todo we additionally need access to the depth texture


}
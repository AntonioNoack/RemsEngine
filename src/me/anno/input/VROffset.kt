package me.anno.input

import org.joml.Quaternionf
import org.joml.Vector3d

/**
 * values for changing the VR view:
 *  - an offset in world space
 *  - a rotation for making looking certain ways easier (e.g. while sitting or in bed)
 *  - interface for potential shake-effects, nausea-effects and similar
 * */
object VROffset {
    val additionalRotation = Quaternionf()
    val additionalOffset = Vector3d()
}
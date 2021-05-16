package me.anno.mesh.fbx.model

import me.anno.mesh.fbx.structure.FBXNode
import me.anno.utils.Tabs

class FBXAnimationCurve(node: FBXNode) : FBXObject(node) {

    val defaultValue = node.getProperty("Default") as? Double ?: 0.0
    val times = node.getLongArray("KeyTime")!!.map { it.toDouble() / legacyTimeFactor }
    val values = node.getFloatArray("KeyValueFloat")!!

    // val attrFlags = node.getIntArray("KeyAttrFlags") // interpolation mode and such
    // val attrDataFloat = node.getIntArray("KeyAttrDataFloat") // ??, floats as ints in my example; vermutlich sowas wie tangent weights
    // val attrRefCount = node.getIntArray("KeyAttrRefCount") // how often which keyframe is used -> idc

    override fun toString(depth0: Int, depth: Int, filter: (FBXObject, FBXObject) -> Boolean): String {
        return Tabs.spaces(depth0 * 2) + "AnimationCurve(${values.joinToString()} @ ${times.joinToString()})\n"
    }

    companion object {
        // https://forums.autodesk.com/t5/fbx-forum/finding-animcurve-time/m-p/9695113/highlight/true#M10011
        val legacyTimeFactor = 46186158000L
        // val newTimeFactor = 141120000L
    }

}
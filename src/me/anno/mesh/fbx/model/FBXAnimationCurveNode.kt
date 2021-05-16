package me.anno.mesh.fbx.model

import me.anno.mesh.fbx.structure.FBXNode
import me.anno.utils.Tabs

class FBXAnimationCurveNode(node: FBXNode) : FBXObject(node) {

    var x = 0f
    var y = 0f
    var z = 0f
    var lockInfluenceWeights = false

    override fun onReadProperty70(name: String, value: Any) {
        when (name) {
            "d|X" -> x = value as Float
            "d|Y" -> y = value as Float
            "d|Z" -> z = value as Float
            "d|lockInfluenceWeights" -> lockInfluenceWeights = value as Boolean
            else -> super.onReadProperty70(name, value)
        }
    }

    override fun toString(depth0: Int, depth: Int, filter: (parent: FBXObject, child: FBXObject) -> Boolean): String {
        val tabs = Tabs.spaces(depth * 2 + 1)
        return Tabs.spaces(depth0 * 2) +
                "$depth ${javaClass.simpleName.substring(3)}:$name:$subType ($x $y $z)\n" +
                overrides.filter { filter(this, it.second) }
                    .joinToString("") { (key, value) -> "$tabs $key: ${value.toString(0, depth + 1, filter)}" } +
                children.filter { filter(this, it) }.joinToString("") { it.toString(depth + 1, depth + 1, filter) }
    }

}
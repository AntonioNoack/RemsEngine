package me.anno.mesh.fbx.model

import me.anno.mesh.fbx.structure.FBXNode

class FBXAnimationCurveNode(node: FBXNode): FBXObject(node) {
    var x = 0f
    var y = 0f
    var z = 0f
    override fun onReadProperty70(name: String, value: Any) {
        when(name){
            "d|X" -> x = value as Float
            "d|Y" -> y = value as Float
            "d|Z" -> z = value as Float
            else -> super.onReadProperty70(name, value)
        }
    }
}
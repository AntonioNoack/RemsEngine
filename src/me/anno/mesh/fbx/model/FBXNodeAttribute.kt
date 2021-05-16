package me.anno.mesh.fbx.model

import me.anno.mesh.fbx.structure.FBXNode

class FBXNodeAttribute(node: FBXNode): FBXObject(node){
    // what is this???
    // looks like this class adds out-of-spec attributes...
    // like camera switching
    val typeFlags = node.getFirst("TypeFlags")?.properties ?: emptyArray()
    override fun onReadProperty70(name: String, value: Any) {
        // ...
    }
}
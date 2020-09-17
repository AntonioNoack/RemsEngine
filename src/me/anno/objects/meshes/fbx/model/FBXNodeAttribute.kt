package me.anno.objects.meshes.fbx.model

import me.anno.objects.meshes.fbx.structure.FBXNode

class FBXNodeAttribute(node: FBXNode): FBXObject(node){
    // what is this???
    // looks like this class adds out-of-spec attributes...
    // like camera switching
    val typeFlags = node["TypeFlags"].firstOrNull()?.properties ?: emptyArray()
    override fun onReadProperty70(name: String, value: Any) {
        // ...
    }
}
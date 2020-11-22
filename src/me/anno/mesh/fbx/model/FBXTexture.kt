package me.anno.mesh.fbx.model

import me.anno.mesh.fbx.structure.FBXNode

class FBXTexture(node: FBXNode): FBXObject(node){
    override fun onReadProperty70(name: String, value: Any) {
        // ignore all, we don't need it
    }
}
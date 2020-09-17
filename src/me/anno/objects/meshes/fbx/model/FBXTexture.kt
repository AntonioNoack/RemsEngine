package me.anno.objects.meshes.fbx.model

import me.anno.objects.meshes.fbx.structure.FBXNode

class FBXTexture(node: FBXNode): FBXObject(node){
    override fun onReadProperty70(name: String, value: Any) {
        // ignore all, we don't need it
    }
}
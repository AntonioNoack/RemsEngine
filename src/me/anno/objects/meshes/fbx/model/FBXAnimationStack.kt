package me.anno.objects.meshes.fbx.model

import me.anno.objects.meshes.fbx.structure.FBXNode

class FBXAnimationStack(node: FBXNode): FBXObject(node) {
    var description = ""
    var localStartNanos = 0L
    var localStopNanos = 0L
    override fun onReadProperty70(name: String, value: Any) {
        when(name){
            "Description" -> description = value as String
            "LocalStart" -> localStartNanos = value as Long
            "LocalStop" -> localStopNanos = value as Long
            "ReferenceStart", "ReferenceStop" -> Unit // in my examples equal to local start / stop
            else -> super.onReadProperty70(name, value)
        }
    }
}
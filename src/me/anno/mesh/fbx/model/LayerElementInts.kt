package me.anno.mesh.fbx.model

import me.anno.gpu.buffer.StaticBuffer
import me.anno.mesh.fbx.structure.FBXNode

class LayerElementInts(n: FBXNode, val components: Int) : LayerElement(n) {

    val data = n.getIntArray("Materials")!!
    fun put(vertIndex: Int, totalVertIndex: Int, faceIndex: Int, buffer: StaticBuffer, ints: Boolean) {
        var index = (when (accessType) {
            0 -> vertIndex
            1 -> faceIndex
            2 -> totalVertIndex
            else -> 0
        }) * components
        if(ints){
            for (i in 0 until components) {
                buffer.putInt(data.getOrElse(index++){ 0 })
            }
        } else {
            for (i in 0 until components) {
                buffer.put(data[index++].toFloat())
            }
        }
    }

}
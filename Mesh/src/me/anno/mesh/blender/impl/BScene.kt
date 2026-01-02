package me.anno.mesh.blender.impl

import me.anno.mesh.blender.ConstructorData

@Suppress("unused")
class BScene(ptr: ConstructorData) : BlendData(ptr) {

    val id = getPartStruct("id") as BID
    // val nodeTree = getStructArray("*nodetree")
    val renderData = getPartStruct("r") as BRenderData

    override fun toString(): String {
        return "Scene { $id, $renderData }"
    }

}
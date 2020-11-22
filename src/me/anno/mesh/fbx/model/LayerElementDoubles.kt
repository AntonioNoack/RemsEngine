package me.anno.mesh.fbx.model

import me.anno.gpu.buffer.StaticBuffer
import me.anno.mesh.fbx.structure.FBXNode

class LayerElementDoubles(n: FBXNode, val components: Int) : LayerElement(n) {

    val data = n.getDoubleArray("Normals") ?: n.getDoubleArray("Colors") ?: n.getDoubleArray("UV")!!
    val dataIndex = if (when (n.getProperty("ReferenceInformationType")) {
            "Direct" -> false
            "IndexToDirect", "Index" -> true
            else -> true // idk -> use if available
        }
    ) (n.getIntArray("NormalsIndex") ?: n.getIntArray("UVIndex")) else null // a second remapping xD ; could exist for colors, materials (useless) and colors as well

    fun put(vertIndex: Int, totalVertIndex: Int, faceIndex: Int, buffer: StaticBuffer) {
        var index = (when (accessType) {
            0 -> vertIndex
            1 -> faceIndex
            2 -> totalVertIndex
            else -> 0
        })
        if (dataIndex != null) index = dataIndex[index]
        index *= components
        for (i in 0 until components) {
            buffer.put(data[index++].toFloat())
        }
    }

}
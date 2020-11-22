package me.anno.mesh.fbx.model

import me.anno.mesh.fbx.structure.FBXNode

open class LayerElement(n: FBXNode) {
    val accessType = when (val mapping = n.getProperty("MappingInformationType") as String) {
        "ByVertex", "ByVertice" -> 0 // smooth
        "ByPolygon" -> 1 // hard
        "ByPolygonVertex" -> 2 // for every vert from every poly separate; smooth + hard mixed
        "AllSame" -> 3
        else -> throw RuntimeException("Unknown mapping type $mapping")
    }
}
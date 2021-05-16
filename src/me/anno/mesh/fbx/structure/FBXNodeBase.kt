package me.anno.mesh.fbx.structure

interface FBXNodeBase {

    val children: ArrayList<FBXNode>

    fun getAll(name: String) = children.filter { it.nameOrType == name }
    fun getFirst(name: String) = children.firstOrNull { it.nameOrType == name }
    fun <V> mapAll(name: String, mapping: (FBXNode) -> V) =
        children.filter { it.nameOrType == name }.map(mapping)

}
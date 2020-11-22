package me.anno.mesh.fbx.structure

interface FBXNodeBase {

    val children: ArrayList<FBXNode>

    operator fun get(name: String) = children.filter { it.nameOrType == name }

}
package me.anno.objects.meshes.fbx.structure

interface FBXNodeBase {

    val children: ArrayList<FBXNode>

    operator fun get(name: String) = children.filter { it.nameOrType == name }

}
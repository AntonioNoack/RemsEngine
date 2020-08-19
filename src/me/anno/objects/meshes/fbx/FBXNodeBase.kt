package me.anno.objects.meshes.fbx

open class FBXNodeBase {

    val children = ArrayList<FBXNode>()

    operator fun get(name: String) = children.filter { it.name == name }

}
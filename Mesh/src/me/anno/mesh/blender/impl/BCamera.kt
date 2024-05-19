package me.anno.mesh.blender.impl

import me.anno.mesh.blender.ConstructorData

@Suppress("SpellCheckingInspection", "unused")
class BCamera(ptr: ConstructorData) : BlendData(ptr) {

    val id = inside("id") as? BID

    val near = float("clipsta")
    val far = float("clipend")
    val orthoScale = float("ortho_scale")

}
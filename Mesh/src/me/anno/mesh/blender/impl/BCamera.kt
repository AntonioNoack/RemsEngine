package me.anno.mesh.blender.impl

import me.anno.mesh.blender.ConstructorData

@Suppress("SpellCheckingInspection", "unused")
class BCamera(ptr: ConstructorData) : BlendData(ptr) {

    val id = inside("id") as? BID

    val near = f32("clipsta")
    val far = f32("clipend")
    val orthoScale = f32("ortho_scale")

}
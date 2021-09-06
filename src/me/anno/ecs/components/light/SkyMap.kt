package me.anno.ecs.components.light

import me.anno.ecs.Component
import me.anno.gpu.shader.BaseShader
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef

// todo or render from shader
// todo always find closest to object
// todo bake surrounding lighting for reflections
// todo blur
// todo hdr
class SkyMap : Component() {

    var shader: BaseShader? = null
    var imageSource: FileReference = InvalidRef

    override fun clone(): SkyMap {
        val clone = SkyMap()
        copy(clone)
        return SkyMap()
    }

}
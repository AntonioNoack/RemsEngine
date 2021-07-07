package me.anno.mesh.assimp

import me.anno.io.files.FileReference
import org.joml.Vector4f

class Material(val ambient: Vector4f, val diffuse: Vector4f, val specular: Vector4f, var alpha: Float) {

    constructor() : this(Vector4f(1f), Vector4f(1f), Vector4f(1f), 1f)

    var diffuseMap: FileReference? = null
    var normalMap: FileReference? = null

}

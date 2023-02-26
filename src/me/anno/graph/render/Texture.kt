package me.anno.graph.render

import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.utils.Color.white4
import org.joml.Vector4f

class Texture private constructor(val tex: ITexture2D, val mapping: String, val color: Vector4f) {
    constructor(v: ITexture2D) : this(v, "", white4)
    constructor(v: Vector4f) : this(whiteTexture, "", v)
    constructor(tex: ITexture2D, mapping: String) : this(tex, mapping, white4)
}
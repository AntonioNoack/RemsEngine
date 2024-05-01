package me.anno.image.thumbs

import me.anno.gpu.texture.ITexture2D
import me.anno.graph.hdb.HDBKey
import me.anno.io.files.FileReference
import me.anno.utils.structures.Callback

fun interface ThumbGenerator {
    fun generate(srcFile: FileReference, dstFile: HDBKey, size: Int, callback: Callback<ITexture2D>)
}
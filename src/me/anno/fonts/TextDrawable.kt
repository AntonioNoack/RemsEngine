package me.anno.fonts

import me.anno.cache.ICacheData
import me.anno.fonts.mesh.DrawMeshCallback
import org.joml.AABBf

interface TextDrawable : ICacheData {

    val bounds: AABBf

    fun draw(startIndex: Int, endIndex: Int, drawBuffer: DrawMeshCallback)
}
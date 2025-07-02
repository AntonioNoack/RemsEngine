package me.anno.fonts

import me.anno.cache.ICacheData
import me.anno.ecs.components.mesh.Mesh
import me.anno.fonts.signeddistfields.TextSDF
import org.joml.AABBf

abstract class TextDrawable : ICacheData {
    val bounds = AABBf()

    fun interface DrawBufferCallback {
        fun draw(mesh: Mesh?, textSDF: TextSDF?, xOffset: Float): Boolean
    }

    abstract fun draw(startIndex: Int, endIndex: Int, drawBuffer: DrawBufferCallback)
}
package me.anno.fonts.mesh

import me.anno.cache.ICacheData
import me.anno.ecs.components.mesh.Mesh
import me.anno.fonts.signeddistfields.TextSDF
import org.joml.AABBf

abstract class TextRepBase : ICacheData {
    val bounds = AABBf()

    fun interface DrawBufferCallback {
        fun draw(mesh: Mesh?, textSDF: TextSDF?, offset: Float)
    }

    abstract fun draw(startIndex: Int, endIndex: Int, drawBuffer: DrawBufferCallback)
}
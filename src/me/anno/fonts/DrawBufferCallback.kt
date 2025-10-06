package me.anno.fonts

import me.anno.ecs.components.mesh.Mesh
import me.anno.fonts.signeddistfields.TextSDF

fun interface DrawBufferCallback {
    /**
     * return true when done
     * */
    fun draw(mesh: Mesh?, textSDF: TextSDF?, x0: Float, x1: Float, y: Float, lineWidth: Float): Boolean
}
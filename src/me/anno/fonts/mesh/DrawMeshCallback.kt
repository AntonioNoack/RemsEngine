package me.anno.fonts.mesh

import me.anno.ecs.components.mesh.Mesh

fun interface DrawMeshCallback {
    /**
     * return true when done
     * */
    fun draw(mesh: Mesh, x0: Float, x1: Float, y: Float, lineWidth: Float, glyphIndex: Int): Boolean
}
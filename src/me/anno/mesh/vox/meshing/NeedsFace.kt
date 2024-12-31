package me.anno.mesh.vox.meshing

fun interface NeedsFace {
    fun needsFace(insideBlockId: Int, outsideBlockId: Int): Boolean
}
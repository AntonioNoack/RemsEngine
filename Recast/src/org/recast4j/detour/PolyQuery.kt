package org.recast4j.detour

interface PolyQuery {
    fun process(tile: MeshTile, poly: Poly, ref: Long)
}
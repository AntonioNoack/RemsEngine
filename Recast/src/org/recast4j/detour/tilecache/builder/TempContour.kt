package org.recast4j.detour.tilecache.builder

import org.recast4j.IntArrayList

internal class TempContour {

    var vertices = IntArrayList()
    var numVertices = 0
    var poly = IntArrayList()

    fun npoly(): Int {
        return poly.size
    }

    fun clear() {
        numVertices = 0
        vertices.clear()
    }
}
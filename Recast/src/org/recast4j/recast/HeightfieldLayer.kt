package org.recast4j.recast

import org.joml.Vector3f

class HeightfieldLayer {

    val bmin = Vector3f()
    val bmax = Vector3f()
    var cellSize = 0f
    var cellHeight = 0f

    /**
     * The size of the heightfield. (Along the x/z-axis in cell units.)
     */
    var width = 0
    var height = 0

    /**
     * The bounds of usable data.
     */
    var minX = 0
    var maxX = 0
    var minH = 0
    var maxH = 0
    var minZ = 0
    var maxZ = 0

    /**
     * The heightfield, size: w*h
     */
    lateinit var heights: IntArray

    /**
     * Area ids. Size same as heights
     */
    lateinit var areas: IntArray

    /**
     * Packed neighbor connection information. Size same as heights
     */
    lateinit var cons: IntArray
}
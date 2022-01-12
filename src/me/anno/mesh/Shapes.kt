package me.anno.mesh

import me.anno.ecs.components.mesh.Mesh


object Shapes {

    // a cube (12t, 8p) has a volume of 8.0m² to cover a sphere,
    // while a sphere itself only has 4.2m³,
    // and a tetrahedron (8t, 6p) has 6.9m³ -> use the tetrahedron

    val cube = Mesh()
        .apply {
            positions = floatArrayOf(
                -1f, -1f, -1f,
                -1f, -1f, +1f,
                -1f, +1f, -1f,
                -1f, +1f, +1f,
                +1f, -1f, -1f,
                +1f, -1f, +1f,
                +1f, +1f, -1f,
                +1f, +1f, +1f,
            )
            indices = intArrayOf(
                0, 1, 3, 3, 2, 0,
                1, 5, 7, 7, 3, 1,
                2, 3, 7, 7, 6, 2,
                4, 0, 2, 2, 6, 4,
                4, 6, 7, 7, 5, 4,
                1, 0, 4, 4, 5, 1
            )
        }

    val sphereCoveringTetrahedron = Mesh()
        .apply {
            val s = 1.226f * 1.414f // scale to cover a sphere
            positions = floatArrayOf(
                0f, +s, 0f,
                +s, 0f, 0f,
                0f, 0f, +s,
                -s, 0f, 0f,
                0f, 0f, -s,
                0f, -s, 0f
            )
            indices = intArrayOf(
                0, 1, 2, 0, 2, 3, 0, 3, 4, 0, 4, 1,
                1, 2, 5, 2, 3, 5, 3, 4, 5, 4, 1, 5
            )
        }

    val tetrahedron = Mesh()
        .apply {
            val s = 1f
            positions = floatArrayOf(
                0f, +s, 0f,
                +s, 0f, 0f,
                0f, 0f, +s,
                -s, 0f, 0f,
                0f, 0f, -s,
                0f, -s, 0f
            )
            indices = intArrayOf(
                0, 1, 2, 0, 2, 3, 0, 3, 4, 0, 4, 1,
                1, 2, 5, 2, 3, 5, 3, 4, 5, 4, 1, 5
            )
        }

}
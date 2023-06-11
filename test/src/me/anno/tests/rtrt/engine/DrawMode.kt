package me.anno.tests.rtrt.engine

enum class DrawMode(val id: Int) {
    NORMAL(0),
    TLAS_DEPTH(1),
    BLAS_DEPTH(2),
    TRIS_DEPTH(3),
    GLOBAL_ILLUMINATION(5),
    SIMPLE_SHADOW(6),
}

package me.anno.maths.geometry

interface SplittablePoint<Impl> {
    fun split(b: Impl, f: Float): Impl
}
package me.anno.utils

object Done : Throwable() {
    private fun readResolve(): Any = Done
}
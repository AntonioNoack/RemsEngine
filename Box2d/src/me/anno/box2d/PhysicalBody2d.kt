package me.anno.box2d

abstract class PhysicalBody2d : PhysicsBody2d() {

    @Suppress("unused")
    val inertia get(): Float = nativeInstance?.inertia ?: 0f

}
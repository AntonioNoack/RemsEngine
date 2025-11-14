package me.anno.bullet.constraints

import me.anno.io.saveable.Saveable

class ConstraintMotor(var targetVelocity: Float, var maxForce: Float) : Saveable() {
    fun set(src: ConstraintMotor) {
        targetVelocity = src.targetVelocity
        maxForce = src.maxForce
    }
}
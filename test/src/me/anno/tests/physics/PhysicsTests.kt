package me.anno.tests.physics

import me.anno.ecs.components.physics.Physics

fun Physics<*, *>.testStep() {
    step(false)
    updateDynamicEntities(timeNanos)
    validateEntityTransforms()
}
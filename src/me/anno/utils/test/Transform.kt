package me.anno.utils.test

import me.anno.ecs.Entity
import org.joml.Quaterniond
import org.joml.Vector3d

fun main() {

    val e = Entity()

    val t = e.transform

    e.validateTransforms()

    t.localPosition = Vector3d(1.0, 2.0, 3.0)

    e.validateTransforms()

    t.localRotation = Quaterniond().rotateX(10.0)

    e.validateTransforms()

    println(t.localPosition)


}
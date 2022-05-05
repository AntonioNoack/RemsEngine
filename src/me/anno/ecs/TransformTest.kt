package me.anno.ecs

import org.joml.Quaterniond
import org.joml.Vector3d

fun main() {

    val e = Entity()
    val t = e.transform

    t.localPosition = Vector3d(1.0, 2.0, 3.0)
    t.localRotation = Quaterniond().rotateX(10.0)

    println(t.localPosition)
    println(t.globalPosition)

}
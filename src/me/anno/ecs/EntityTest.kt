package me.anno.ecs

import me.anno.ecs.components.camera.CameraComponent
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.MeshRenderer
import me.anno.ecs.prefab.PrefabComponent1
import me.anno.ecs.prefab.PrefabEntity1

fun main() {

    val base = PrefabEntity1()
    base.add(MeshComponent())
    base.add(CameraComponent())

    val main = PrefabEntity1()
    main.prefab = base
    main.add(PrefabComponent1(MeshRenderer()))

}
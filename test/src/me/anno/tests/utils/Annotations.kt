package me.anno.tests.utils

import me.anno.ecs.Entity
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.io.serialization.CachedReflections
import kotlin.reflect.full.declaredMembers

fun main() {

    val clazz = PrefabSaveable::class

    println(clazz.declaredMembers.filter { it.name == "prefab" })

    val java = clazz.java

    val field = java.declaredFields.first { it.name == "prefab" }
    println(field)
    println("[${field.annotations.joinToString()}]")
    val setter = java.getMethod("setPrefab", Prefab::class.java)
    val getter = java.getMethod("getPrefab")
    println(setter)
    println(getter)

    println("[${setter.annotations.joinToString()}]")
    println("[${getter.annotations.joinToString()}]")

    run {
        // val setter = java.getMethod("setPrefab\$annotations")
        val getter = java.getMethod("getPrefab\$annotations")
        println(getter)
        println("[${getter.annotations.joinToString()}]")
    }

    val test = CachedReflections(Entity::class)
    println(test.allProperties["prefab"])
    println(test.allProperties["isEnabled"])

}
package me.anno.tests.ecs

import me.anno.Engine
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.ECSRegistry
import me.anno.io.zip.InnerTmpFile
import me.anno.mesh.Shapes.flatCube
import kotlin.test.assertEquals

/**
 * Tests whether .ref creates a correct prefab:
 *  - first a scene is created using direct instances
 *  - then a Prefab is created using .ref, and all properties are registered in the prefab automatically
 *  - then the Prefab is saved to a String, and that is wrapped in a FileReference
 *  - this then is read using the PrefabCache,
 *  - and a new instance of that deserialized prefab gets instantiated.
 *  Finally, they are compared for equality.
 * */
fun main() {

    ECSRegistry.initMeshes()
    ECSRegistry.initPrefabs()

    val scene = Entity()
    for (i in 0 until 3) {
        val group = Entity("Group $i", scene)
        for (j in 0 until 3) {
            val mesh = Entity("Mesh $i.$j", group)
            mesh.add(MeshComponent(flatCube.front))
            mesh.position = mesh.position.set(Math.random(), Math.random(), Math.random())
            mesh.rotation = mesh.rotation
                .rotateX(Math.random())
                .rotateY(Math.random())
                .rotateZ(Math.random())
            mesh.scale = mesh.scale.set(Math.random(), Math.random(), Math.random())
        }
    }

    val asText = scene.ref.readTextSync()
    val asFile = InnerTmpFile.InnerTmpTextFile(asText, "json")

    val prefab = PrefabCache[asFile]!!
    prefab.invalidateInstance()
    compare(scene, prefab.createInstance() as Entity)

    Engine.requestShutdown()
}

fun compare(a: Entity, b: Entity) {
    assertEquals(a.name, b.name)
    assertEquals(a.position, b.position)
    assertEquals(a.rotation, b.rotation)
    assertEquals(a.scale, b.scale)
    assertEquals(a.children.size, b.children.size)
    for (i in 0 until a.children.size) {
        compare(a.children[i], b.children[i])
    }
    assertEquals(a.components.size, b.components.size)
    for (i in 0 until a.components.size) {
        compare(a.components[i], b.components[i])
    }
}

fun compare(a: Component, b: Component) {
    assertEquals(a::class, b::class)
    assertEquals(a.name, b.name)
    if (a is MeshComponent) {
        assertEquals(a.meshFile, (b as MeshComponent).meshFile)
    }
}
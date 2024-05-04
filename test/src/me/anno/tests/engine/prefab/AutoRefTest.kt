package me.anno.tests.engine.prefab

import me.anno.Engine
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.ECSRegistry
import me.anno.io.files.inner.temporary.InnerTmpTextFile
import me.anno.maths.Maths
import me.anno.mesh.Shapes
import org.junit.jupiter.api.Test
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
class AutoRefTest {

    @Test
    fun test() {

        ECSRegistry.init()

        val scene = createScene()

        val asText = scene.ref.readTextSync()
        val asFile = InnerTmpTextFile(asText, "json")

        val prefab = PrefabCache[asFile]!!

        if (false) {
            println(asText)

            println(scene.prefab!!.adds)
            println(prefab.adds)

            println(scene.prefab!!.sets)
            println(prefab.sets)
        }

        prefab.invalidateInstance()
        compare(scene, prefab.createInstance() as Entity)

        Engine.requestShutdown()
    }

    fun createScene(): Entity {
        val scene = Entity()
        for (i in 0 until 3) {
            val group = Entity("Group $i", scene)
            for (j in 0 until 3) {
                val mesh = Entity("Mesh $i.$j", group)
                mesh.add(MeshComponent(Shapes.flatCube.front))
                mesh.setPosition(Maths.random(), Maths.random(), Maths.random())
                mesh.setRotation(Maths.random(), Maths.random(), Maths.random())
                mesh.setScale(Maths.random(), Maths.random(), Maths.random())
            }
        }
        return scene
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
}
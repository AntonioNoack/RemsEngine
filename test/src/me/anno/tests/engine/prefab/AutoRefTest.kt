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
import me.anno.tests.FlakyTest
import me.anno.utils.assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

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
    @FlakyTest
    @Execution(ExecutionMode.SAME_THREAD)
    fun test() {

        Engine.cancelShutdown()
        ECSRegistry.init()

        val scene = createScene()

        val asText = scene.ref.readTextSync()
        val asFile = InnerTmpTextFile(asText, "json")

        val prefab = PrefabCache[asFile].waitFor()!!.prefab!!

        if (true) {
            println(asText)

            println(scene.prefab!!.adds)
            println(prefab.adds)

            println(scene.prefab!!.sets)
            println(prefab.sets)
        }

        prefab.invalidateInstance()
        compare(scene, prefab.newInstance() as Entity)
    }

    fun createScene(): Entity {
        val scene = Entity()
        for (i in 0 until 3) {
            val group = Entity("Group $i", scene)
            for (j in 0 until 3) {
                val mesh = Entity("Mesh $i.$j", group)
                mesh.add(MeshComponent(Shapes.flatCube.front))
                mesh.setPosition(Maths.random(), Maths.random(), Maths.random())
                mesh.setRotation(Maths.random().toFloat(), Maths.random().toFloat(), Maths.random().toFloat())
                mesh.setScale(Maths.random().toFloat(), Maths.random().toFloat(), Maths.random().toFloat())
            }
        }
        return scene
    }

    fun compare(a: Entity, b: Entity) {
        assertEquals(a.name, b.name)
        assertEquals(a.position, b.position, 1e-7)
        assertEquals(a.rotation, b.rotation, 1e-7)
        assertEquals(a.scale, b.scale, 1e-7)
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
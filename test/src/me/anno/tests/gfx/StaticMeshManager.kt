package me.anno.tests.gfx

import me.anno.Build
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.unique.StaticMeshManager
import me.anno.engine.ui.render.SceneView
import me.anno.mesh.Shapes
import me.anno.studio.StudioBase
import me.anno.utils.Color
import java.util.*

fun main() {

    // todo we need to be able to render a 1M scene at 60 fps (current state: 100k at 15 fps)
    //  - hasRenderable needs to be re-evaluated when we change something, so we don't iterate over it in subFill
    //  - size check on subFill is broken, renders even when viewing from 500Mm distance

    // todo when we use this class, clicking in Editor is broken, because clickId isn't persisted / done on GPU

    // disable glGetError()
    Build.isDebug = false

    val mesh = Shapes.flatCube.front
    val scene = Entity()
    val random = Random(1234L)
    val materials = listOf(
        Material.diffuse(0xff0000 or Color.black),
        Material.diffuse(0x00ff00 or Color.black),
        Material.diffuse(0x0000ff or Color.black),
    ).map { listOf(it.ref) }

    // todo
    //  - and a world with implicit motion
    // done
    //  - test a world with multiple materials
    //  - test a world with tons of meshes / draw calls: they shall be reduced to 1
    //  - create random hierarchy for more realistic testing
    fun create(entity: Entity, mi: Int) {
        for (i in 0 until mi) {
            val r = 300.0
            val x = (random.nextFloat() - 0.5f) * r
            val y = (random.nextFloat() - 0.5f) * r
            val z = (random.nextFloat() - 0.5f) * r
            val child = Entity(entity)
            child.setPosition(x, y, z)
            val comp = MeshComponent(mesh)
            comp.materials = materials.random()
            comp.isInstanced = false // for testing
            child.add(comp)
            create(child, mi - 1)
        }
    }
    create(scene, 8)

    scene.add(StaticMeshManager())
    SceneView.testSceneWithUI("StaticMeshManager", scene) {
        StudioBase.instance?.enableVSync = false
    }
}
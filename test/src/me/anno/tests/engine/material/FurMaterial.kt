package me.anno.tests.engine.material

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshCache
import me.anno.ecs.components.mesh.material.FurMeshComponent
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.utils.OS.downloads

/**
 * shell texturing like in https://www.youtube.com/watch?v=9dr-tRQzij4 by Acerola
 * */
fun main() {
    OfficialExtensions.initForTests()
    val mesh = (MeshCache.getEntry(downloads.getChild("3d/bunny.obj")).waitFor() as Mesh).shallowClone()
    mesh.calculateNormals(true) // clone and recalculating normals, because the bunny file, I have, has flat normals
    val scene = Entity()
    scene.add(FurMeshComponent(mesh))
    testSceneWithUI("Shell Textures", scene)
}

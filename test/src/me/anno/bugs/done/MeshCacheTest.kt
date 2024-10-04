package me.anno.bugs.done

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshCache
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.DefaultAssets
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.ui.UIColors

/**
 * normals look broken, maybe scaled incorrectly???
 * transforms were broken -> is now fixed :)
 * */
fun main() {
    val meshFile = DefaultAssets.flatCube.ref
    val redMaterial = Material.diffuse(UIColors.axisXColor)
    val blueMaterial = Material.diffuse(UIColors.axisZColor)
    val meshEntity = Entity()
        .add(MeshComponent(meshFile, redMaterial))
        .setScale(2.0)
    val scaledFile = meshEntity.ref
    val scaledPrefab = PrefabCache[scaledFile]!!
    val scaledEntity = scaledPrefab.createInstance() as Entity
    scaledEntity.name = "Scaled"
    val scaledMesh = MeshCache[scaledFile]!!
    // fixed scene is incorrect: both shown meshes should be the same size
    //  one is 2x bigger, the other is 2²x bigger
    val scene = Entity("Scene")
        .add(scaledEntity)
        .add(
            Entity("MeshCache-Scaled")
                .setPosition(-12.0, 0.0, 0.0)
                .add(MeshComponent(scaledMesh, blueMaterial))
        )
    // fixed AABB of scene is incorrect
    testSceneWithUI("MeshCache broken", scene)
}
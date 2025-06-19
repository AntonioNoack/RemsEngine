package me.anno.tests.mesh.fbx

import me.anno.Engine
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.forAllComponentsInChildren
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.MaterialCache
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.OfficialExtensions
import me.anno.io.files.FileReference
import me.anno.io.files.Reference.getReference

fun main() {
    OfficialExtensions.initForTests()
    // load fbx with incorrect path -> how do we display it?
    // we showed it like we loaded it,
    // now we try to correct it by indexing all files from the same zip :)
    val path = getReference("E:/Assets/Sources/POLYGON_War_Pack_Source_Files.zip/POLYGON_War_Demo_Scene.fbx/Scene.json")
    PrefabCache.getPrefabSampleInstance(path).waitFor { scene, err ->
        err?.printStackTrace()
        if (scene is Entity) {
            val matCache = HashSet<FileReference>()
            scene.forAllComponentsInChildren(MeshComponent::class) {
                val matRef = it.getMeshOrNull()!!.materials.firstOrNull()
                if (matRef != null && matCache.add(matRef)) {
                    val mat = MaterialCache[matRef]
                    val diffuse = mat?.diffuseMap
                    println("${it.name} -> $matRef -> $diffuse")
                }
            }
        }
        Engine.requestShutdown()
    }
}
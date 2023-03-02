package me.anno.tests.mesh.fbx

import me.anno.Engine
import me.anno.ecs.Entity
import me.anno.ecs.components.cache.MaterialCache
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.prefab.PrefabCache.getPrefabInstance
import me.anno.io.files.FileReference
import me.anno.io.files.FileReference.Companion.getReference

fun main() {
    // load fbx with incorrect path -> how do we display it?
    // we showed it like we loaded it,
    // now we try to correct it by indexing all files from the same zip :)
    val path = getReference("E:/Assets/Sources/POLYGON_War_Pack_Source_Files.zip/POLYGON_War_Demo_Scene.fbx/Scene.json")
    val scene = getPrefabInstance(path) as Entity
    val matCache = HashSet<FileReference>()
    scene.firstComponentInChildren(MeshComponent::class) {
        val matRef = it.getMesh()!!.materials.firstOrNull()
        if (matRef != null && matCache.add(matRef)) {
            val mat = MaterialCache[matRef]
            val diffuse = mat?.diffuseMap
            println("${it.name} -> $matRef -> $diffuse")
        }
        false
    }
    Engine.requestShutdown()
}
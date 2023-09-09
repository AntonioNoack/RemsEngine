package me.anno.tests.ecs

import me.anno.Engine
import me.anno.ecs.components.mesh.MeshCache
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.ECSRegistry
import me.anno.io.zip.InnerFolderCache
import me.anno.utils.Clock
import me.anno.utils.OS.documents

fun main() {
    ECSRegistry.initMeshes()
    Clock().benchmark(3, 10, "Loading Mesh") {
        MeshCache[documents.getChild("Blender/SK_Mannequin.FBX")]
        MeshCache.clear()
        PrefabCache.clear()
        InnerFolderCache.clear()
    }
    Engine.requestShutdown()
}
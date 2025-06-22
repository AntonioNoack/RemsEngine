package me.anno.bench

import me.anno.Engine
import me.anno.ecs.components.mesh.MeshCache
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.OfficialExtensions
import me.anno.io.files.inner.InnerFolderCache
import me.anno.utils.Clock
import me.anno.utils.OS.documents

fun main() {
    OfficialExtensions.initForTests()
    Clock("MeshLoader").benchmark(3, 10, "Loading Mesh") {
        MeshCache.getEntry(documents.getChild("Blender/SK_Mannequin.FBX")).waitFor()!!
        MeshCache.clear()
        PrefabCache.clear()
        InnerFolderCache.clear()
    }
    Engine.requestShutdown()
}
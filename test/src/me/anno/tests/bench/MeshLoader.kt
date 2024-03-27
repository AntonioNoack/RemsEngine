package me.anno.tests.bench

import me.anno.Engine
import me.anno.ecs.components.mesh.MeshCache
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.ECSRegistry
import me.anno.engine.OfficialExtensions
import me.anno.io.files.inner.InnerFolderCache
import me.anno.utils.Clock
import me.anno.utils.OS.documents

fun main() {
    OfficialExtensions.initForTests()
    ECSRegistry.initMeshes()
    Clock().benchmark(3, 10, "Loading Mesh") {
        MeshCache[documents.getChild("Blender/SK_Mannequin.FBX")]!!
        MeshCache.clear()
        PrefabCache.clear()
        InnerFolderCache.clear()
    }
    Engine.requestShutdown()
}
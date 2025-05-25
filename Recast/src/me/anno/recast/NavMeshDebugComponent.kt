package me.anno.recast

import me.anno.ecs.Component
import me.anno.ecs.systems.OnDrawGUI
import me.anno.engine.serialization.NotSerializedProperty
import me.anno.gpu.pipeline.Pipeline
import org.recast4j.detour.MeshData

class NavMeshDebugComponent() : Component(), OnDrawGUI {

    constructor(navMeshData: NavMeshData) : this() {
        data = navMeshData.meshData
    }

    @NotSerializedProperty
    var data: MeshData? = null

    override fun onDrawGUI(pipeline: Pipeline, all: Boolean) {
        NavMeshDebug.drawNavMesh(entity, data)
    }
}
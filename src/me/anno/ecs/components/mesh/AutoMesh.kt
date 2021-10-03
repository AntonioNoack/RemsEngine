package me.anno.ecs.components.mesh

import me.anno.ecs.prefab.Prefab
import me.anno.io.serialization.NotSerializedProperty
import me.anno.io.zip.InnerTmpFile

abstract class AutoMesh : MeshComponent() {

    @NotSerializedProperty
    val meshPrefab = Prefab("Mesh")

    @NotSerializedProperty
    val file = InnerTmpFile.InnerTmpPrefabFile(meshPrefab)

    var needsUpdate = false

    fun invalidate() {
        needsUpdate = true
        // todo register for rare update? instead of onUpdate()
    }

    abstract fun generateMesh()

    abstract override fun clone(): AutoMesh

    override fun onUpdate(): Int {
        if (needsUpdate) {
            needsUpdate = false
            generateMesh()
        }
        return 1
    }

}
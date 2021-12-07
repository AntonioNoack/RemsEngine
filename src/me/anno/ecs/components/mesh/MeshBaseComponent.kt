package me.anno.ecs.components.mesh

import me.anno.ecs.Entity
import me.anno.ecs.components.CollidingComponent
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.shader.Shader
import me.anno.io.serialization.SerializedProperty
import me.anno.utils.types.AABBs.transformUnion
import org.joml.AABBd
import org.joml.Matrix4x3d

abstract class MeshBaseComponent : CollidingComponent() {

    // todo respect these properties
    @SerializedProperty
    var receiveShadows = true

    @SerializedProperty
    var castShadows = true

    var isInstanced = false

    open fun ensureBuffer() {}

    abstract fun getMesh(): Mesh?

    override fun fillSpace(globalTransform: Matrix4x3d, aabb: AABBd): Boolean {
        ensureBuffer()
        val mesh = getMesh()
        if (mesh != null) {
            // add aabb of that mesh with the transform
            mesh.ensureBuffer()
            mesh.aabb.transformUnion(globalTransform, aabb)
        }
        return true
    }

    open fun defineVertexTransform(shader: Shader, entity: Entity, mesh: Mesh) {
        shader.v1("hasAnimation", false)
    }

    fun draw(shader: Shader, materialIndex: Int) {
        getMesh()?.draw(shader, materialIndex)
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as MeshBaseComponent
        clone.castShadows = castShadows
        clone.receiveShadows = receiveShadows
        clone.isInstanced = isInstanced
    }

}
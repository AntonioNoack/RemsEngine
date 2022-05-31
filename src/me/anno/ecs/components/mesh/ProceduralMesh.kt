package me.anno.ecs.components.mesh

import me.anno.config.DefaultConfig
import me.anno.ecs.annotations.DebugAction
import me.anno.ecs.annotations.DebugProperty
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.EditorState
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.SceneView
import me.anno.io.serialization.NotSerializedProperty
import me.anno.ui.debug.TestStudio
import org.joml.AABBd
import org.joml.Matrix4x3d

/**
 * class for generating procedural meshes
 * */
abstract class ProceduralMesh : MeshComponentBase() {

    val data = Mesh()

    override fun getMesh() = data

    @NotSerializedProperty
    var needsUpdate = true

    @DebugProperty
    val numberOfPoints
        get() = (data.positions?.size ?: -3) / 3

    @DebugProperty
    val numberOfTriangles: Int
        get() {
            val indices = data.indices
            if (indices != null) return indices.size / 3
            val positions = data.positions
            return if (positions != null) positions.size / 9 else -1
        }

    @DebugAction
    fun invalidateMesh() {
        needsUpdate = true
        // todo register for rare update? instead of onUpdate()
    }

    override fun ensureBuffer() {
        if (needsUpdate) {
            needsUpdate = false
            generateMesh(data)
            data.invalidateGeometry()
            invalidateAABB()
        }
    }

    override fun fillSpace(globalTransform: Matrix4x3d, aabb: AABBd): Boolean {
        ensureBuffer()
        return super.fillSpace(globalTransform, aabb)
    }

    abstract fun generateMesh(mesh: Mesh)

    abstract override fun clone(): ProceduralMesh

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as ProceduralMesh
        clone.needsUpdate = clone.needsUpdate || needsUpdate
        clone.materials = materials
    }

    override fun onUpdate(): Int {
        ensureBuffer()
        return 32
    }

    companion object {

        /**
         * creates an instance of ProceduralMesh, that uses generate() to generate its mesh;
         * this is meant for testing only
         * */
        fun createProceduralMesh(generate: (mesh: Mesh) -> Unit): ProceduralMesh {
            return object : ProceduralMesh() {
                override fun clone() = throw NotImplementedError()
                override fun generateMesh(mesh: Mesh) {
                    generate(mesh)
                }
            }
        }

        /**
         * opens a new window, in which the mesh, which is being generated once, will be shown
         * this is meant for testing only
         * */
        fun showProceduralMeshInScene(generate: (mesh: Mesh) -> Unit) {
            TestStudio.testUI {
                EditorState.prefabSource = createProceduralMesh(generate).ref
                SceneView(EditorState, PlayMode.EDITING, DefaultConfig.style)
                    .setWeight(1f)
            }
        }

    }

}
package me.anno.ecs.components.anim

import me.anno.ecs.Entity
import me.anno.ecs.annotations.Type
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.Mesh.Companion.defaultMaterial
import me.anno.ecs.interfaces.Renderable
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ui.render.RenderState.worldScale
import me.anno.gpu.GFX
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.shader.Shader
import me.anno.gpu.texture.Texture2D
import me.anno.io.files.FileReference
import me.anno.io.serialization.NotSerializedProperty
import me.anno.io.serialization.SerializedProperty
import me.anno.maths.Maths.length
import me.anno.maths.Maths.min
import me.anno.mesh.assimp.Bone
import me.anno.utils.types.Vectors
import org.joml.*

class Skeleton : PrefabSaveable(), Renderable {

    @SerializedProperty
    var bones: List<Bone> = emptyList()

    @Type("Map<String, Animation/Reference>")
    @SerializedProperty
    var animations = HashMap<String, FileReference>()

    @NotSerializedProperty
    private var previewMesh: Mesh? = null

    @NotSerializedProperty
    private var bonePositions: Array<Vector3f>? = null

    fun draw(shader: Shader, stack: Matrix4x3f, skinningMatrices: Array<Matrix4x3f>?) {

        if (previewMesh == null) {
            val mesh = Mesh()
            this.previewMesh = mesh
            val size = bones.size * boneMeshVertices.size
            mesh.positions = FloatArray(size)
            mesh.boneWeights = FloatArray(size / 3 * 4) { if (it and 3 == 0) 1f else 0f }
            mesh.boneIndices = ByteArray(size / 3 * 4)
            bonePositions = Array(bones.size) { Vector3f() }
        }

        val mesh = previewMesh!!
        val bonePositions = bonePositions!!
        for (i in bones.indices) bonePositions[i].set(bones[i].bindPosition)
        /*if (skinningMatrices != null) {
            for (i in bones.indices) {
                skinningMatrices[i].transformPosition(bonePositions[i])
            }
        }*/
        generateSkeleton(bones, bonePositions, mesh.positions!!, mesh.boneIndices)
        mesh.invalidateGeometry()
        shader.m4x3("localTransform", stack)
        shader.v1f("worldScale", worldScale.toFloat())

        if (skinningMatrices != null) {
            val location = shader["jointTransforms"]
            val shaderSupportsSkinning = location >= 0
            shader.v1b("hasAnimation", shaderSupportsSkinning)
            if (shaderSupportsSkinning) {
                AnimRenderer.upload(location, skinningMatrices)
            }
        } else {
            shader.v1b("hasAnimation", false)
        }

        defaultMaterial.bind(shader)
        mesh.draw(shader, 0)
    }

    override fun fill(
        pipeline: Pipeline,
        entity: Entity,
        clickId: Int,
        cameraPosition: Vector3d,
        worldScale: Double
    ): Int {
        // todo optimize this (avoid allocations)
        val bones = bones
        if (bones.isEmpty()) return clickId
        val mesh = Mesh()
        // in a tree with N nodes, there is N-1 lines
        val size = (bones.size - 1) * boneMeshVertices.size
        mesh.positions = Texture2D.floatArrayPool[size, false, true]
        mesh.normals = Texture2D.floatArrayPool[size, true, true]
        val bonePositions = Array(bones.size) { bones[it].bindPosition }
        generateSkeleton(bones, bonePositions, mesh.positions!!, null)
        pipeline.fill(mesh, cameraPosition, worldScale)
        GFX.addGPUTask("free", 1) {
            Texture2D.floatArrayPool.returnBuffer(mesh.positions)
            Texture2D.floatArrayPool.returnBuffer(mesh.normals)
            mesh.destroy()
        }
        return clickId
    }

    override fun clone(): PrefabSaveable {
        val clone = Skeleton()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        clone as Skeleton
        clone.animations.clear()
        clone.animations.putAll(animations)
        clone.bones = ArrayList(bones)
    }

    override val className = "Skeleton"

    companion object {

        private const val s = 0.14f
        private const val t = 0.81f

        /**
         * a bone mesh like in Blender;
         * each line represents a face
         * */
        val boneMeshVertices = floatArrayOf(
            +s, t, +s, +s, t, -s, 0f, 1f, 0f,
            +s, t, -s, -s, t, -s, 0f, 1f, 0f,
            -s, t, +s, +s, t, +s, 0f, 1f, 0f,
            -s, t, -s, -s, t, +s, 0f, 1f, 0f,
            +s, t, -s, +s, t, +s, 0f, 0f, 0f,
            +s, t, +s, -s, t, +s, 0f, 0f, 0f,
            -s, t, +s, -s, t, -s, 0f, 0f, 0f,
            -s, t, -s, +s, t, -s, 0f, 0f, 0f,
        )

        fun generateSkeleton(
            bones: List<Bone>,
            bonePositions: Array<Vector3f>,
            positions: FloatArray,
            boneIndices: ByteArray?
        ) {
            // todo when we have the data, use the rotation data to rotate the bone... somehow...
            var j = 0
            var k = 0
            boneIndices?.fill(0)
            val mat = Matrix3f()
            val tmp = Vector3f()
            val dirX = Vector3f()
            val dirY = Vector3f()
            val dirZ = Vector3f()
            // estimate the size
            val bounds = AABBf()
            for (boneId in bones.indices) {
                bounds.union(bonePositions[boneId])
            }
            val sizeEstimate = length(bounds.deltaX(), bounds.deltaY(), bounds.deltaZ())
            val maxBoneThickness = 0.2f * sizeEstimate
            var firstBone = true
            for (boneId in bones.indices) {
                val dstBone = bones[boneId]
                if (dstBone.parentId < 0) continue
                val srcBone = bones[dstBone.parentId]
                val srcPos = bonePositions[dstBone.parentId]
                val dstPos = bonePositions[boneId]
                dirY.set(dstPos).sub(srcPos)
                val length = dirY.length()
                if (length > sizeEstimate * 1e-4f) {
                    if (firstBone && bones.size > 10) {
                        firstBone = false
                        continue
                    } // skip the root bone, because it's awkward
                    val thickness = min(length, maxBoneThickness)
                    // find orthogonal directions
                    Vectors.findTangent(dirY, dirX).normalize(thickness)
                    dirZ.set(dirX).cross(dirY).normalize(thickness)
                    mat.set(dirX, dirY, dirZ)
                    // add a bone from src to dst
                    for (i in 0 until boneMeshVertices.size - 2 step 3) {
                        tmp.set(boneMeshVertices[i], boneMeshVertices[i + 1], boneMeshVertices[i + 2])
                        mat.transform(tmp)
                        tmp.add(srcPos)
                        positions[j++] = tmp.x
                        positions[j++] = tmp.y
                        positions[j++] = tmp.z
                        if (boneIndices != null) {
                            boneIndices[k] = srcBone.id.toByte()
                            k += 4
                        }
                    }
                }
            }
        }

    }

}
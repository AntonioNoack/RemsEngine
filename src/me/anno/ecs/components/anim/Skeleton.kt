package me.anno.ecs.components.anim

import me.anno.ecs.annotations.Type
import me.anno.ecs.components.mesh.AnimRenderer
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.Mesh.Companion.defaultMaterial
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.shader.Shader
import me.anno.io.ISaveable
import me.anno.io.base.BaseWriter
import me.anno.io.files.FileReference
import me.anno.io.serialization.NotSerializedProperty
import me.anno.io.serialization.SerializedProperty
import me.anno.mesh.assimp.Bone
import me.anno.utils.maths.Maths.length
import me.anno.utils.maths.Maths.min
import me.anno.utils.types.AABBs.deltaX
import me.anno.utils.types.AABBs.deltaY
import me.anno.utils.types.AABBs.deltaZ
import me.anno.utils.types.Vectors
import org.joml.AABBf
import org.joml.Matrix3f
import org.joml.Matrix4x3f
import org.joml.Vector3f

class Skeleton : PrefabSaveable() {

    @SerializedProperty
    var bones: List<Bone> = emptyList()

    @Type("Map<String, Animation/Reference>")
    @SerializedProperty
    var animations: Map<String, FileReference> = emptyMap()


    @NotSerializedProperty
    private var mesh: Mesh? = null

    @NotSerializedProperty
    private var bonePositions: Array<Vector3f>? = null

    fun draw(shader: Shader, stack: Matrix4x3f, skinningMatrices: Array<Matrix4x3f>?) {

        if (mesh == null) {
            val mesh = Mesh()
            this.mesh = mesh
            val size = bones.size * boneMeshVertices.size
            mesh.positions = FloatArray(size)
            mesh.boneWeights = FloatArray(size / 3 * 4) { if (it and 3 == 0) 1f else 0f }
            mesh.boneIndices = ByteArray(size / 3 * 4)
            bonePositions = Array(bones.size) { Vector3f() }
        }


        val mesh = mesh!!
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

        if (skinningMatrices != null) {
            val location = shader["jointTransforms"]
            val shaderSupportsSkinning = location >= 0
            shader.v1("hasAnimation", shaderSupportsSkinning)
            if (shaderSupportsSkinning) {
                AnimRenderer.upload(location, skinningMatrices)
            }
        } else {
            shader.v1("hasAnimation", false)
        }

        defaultMaterial.defineShader(shader)
        mesh.draw(shader, 0)
    }

    override fun readObjectArray(name: String, values: Array<ISaveable?>) {
        when (name) {
            "bones" -> bones = values.filterIsInstance<Bone>()
            else -> super.readObjectArray(name, values)
        }
    }

    override fun save(writer: BaseWriter) {
        super.save(writer)
        val bones = bones
        if (bones.isNotEmpty()) {
            writer.writeObjectList(this, "bones", bones)
        }
    }

    override fun clone(): PrefabSaveable {
        val clone = Skeleton()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        clone as Skeleton
        clone.animations = animations
        clone.bones = bones
    }

    override val className: String = "Skeleton"

    companion object {

        val boneMeshVertices = listOf(// a bone mesh like in Blender
            -0.14, 0.81, -0.14, 0.14, 0.81, -0.14, 0.0, 1.0, 0.0, 0.14, 0.81, 0.14, -0.14, 0.81, 0.14,
            0.0, 1.0, 0.0, 0.14, 0.81, -0.14, -0.14, 0.81, -0.14, 0.0, 0.0, 0.0, -0.14, 0.81, 0.14, 0.14, 0.81,
            0.14, 0.0, 0.0, 0.0, 0.14, 0.81, 0.14, 0.14, 0.81, -0.14, 0.0, 0.0, 0.0, -0.14, 0.81, -0.14, -0.14,
            0.81, 0.14, 0.0, 0.0, 0.0, 0.14, 0.81, -0.14, 0.14, 0.81, 0.14, 0.0, 1.0, 0.0, -0.14, 0.81, 0.14,
            -0.14, 0.81, -0.14, 0.0, 1.0, 0.0
        ).map { it.toFloat() }.toFloatArray()

        fun generateSkeleton(
            bones: List<Bone>,
            bonePositions: Array<Vector3f>,
            positions: FloatArray,
            boneIndices: ByteArray?
        ) {
            // todo when we have the data, use the rotation data to rotate the bone... somehow...
            // todo sometimes the bones seem to be turned inwards... why? (ExitingCar.fbx)
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
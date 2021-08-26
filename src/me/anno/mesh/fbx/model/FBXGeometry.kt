package me.anno.mesh.fbx.model

import me.anno.animation.skeletal.FBXSkeletalHierarchy
import me.anno.animation.skeletal.SkeletalWeights
import me.anno.animation.skeletal.Skeleton
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.StaticBuffer
import me.anno.mesh.fbx.model.FBXShader.maxWeightsDefault
import me.anno.mesh.fbx.structure.FBXNode
import me.anno.mesh.fbx.structure.FBXReader
import org.apache.logging.log4j.LogManager
import kotlin.math.max
import kotlin.math.min

class FBXGeometry(node: FBXNode) : FBXObject(node) {

    val xyz = node.getDoubleArray("Vertices")!!
    val vertexCount = xyz.size / 3
    val faces = node.getIntArray("PolygonVertexIndex")!! // polygons, each: 0, 1, 2, 3 ... , ~8

    val weightValues = FloatArray(vertexCount * maxWeightsDefault)
    val weightIndices = IntArray(vertexCount * maxWeightsDefault)

    fun addWeight(vertexIndex: Int, boneIndex: Int, weight: Float) {
        val i0 = vertexIndex * maxWeightsDefault
        for (i in i0 until i0 + maxWeightsDefault) {
            val oldWeight = weightValues[i]
            if (weight > oldWeight) {
                weightValues[i] = weight
                weightIndices[i] = boneIndex
                break
            }
        }
    }

    fun generateSkeleton(): Skeleton {
        val weights = SkeletalWeights(maxWeightsDefault, weightIndices, weightValues) // so simple <3
        val boneMap = bones.withIndex().associate { (index, bone) -> bone to index }
        val names = bones.map { it.name }.toTypedArray()
        val parentIndices = bones.map { boneMap[it.parent] ?: -1 }.toIntArray()
        val hierarchy = FBXSkeletalHierarchy(names, parentIndices, bones)
        return Skeleton(hierarchy, weights)
    }

    fun generateMesh(
        xyzName: String,
        normalName: String?,
        materialIndexName: String?,
        materialInts: Boolean,
        maxUVs: Int,
        maxWeights: Int
    ): StaticBuffer {

        val uvMapCount = min(maxUVs, uvs.size) // could be changed
        val weightCount = min(maxWeightsDefault, maxWeights)
        val attributes = arrayListOf(Attribute(xyzName, 3))

        if (normalName != null) {
            attributes += Attribute("normals", 3)
        }

        if (materialIndexName != null) {
            attributes += Attribute(
                "materialIndex",
                if (materialInts) AttributeType.UINT32 else AttributeType.FLOAT, 1
            )
        }

        if (weightCount > 0) {
            attributes += Attribute("weightValues", weightCount)
            attributes += Attribute("weightIndices", AttributeType.SINT8, weightCount)
        }

        val relevantUVMaps = Array(uvMapCount) { uvs[it] }
        for (i in 0 until uvMapCount) {
            attributes += Attribute(
                when (i) {
                    0 -> "uvs"
                    else -> "uvs$i"
                }, 2
            )
        }

        val buffer = StaticBuffer(attributes, faceCount * 3)
        // ("faces: $faceCount, x3 = ${3*faceCount}, face-indices: ${faces.size}")

        var a = 0
        var b = 0
        var j = -1
        var faceIndex = 0

        val vertices = xyz
        val normals = normals

        val weightIndexLimit = max(0, bones.lastIndex)

        fun put(vertIndex: Int, totalVertIndex: Int) {
            val vo = vertIndex * 3
            // xyz
            buffer.put(vertices[vo].toFloat(), vertices[vo + 1].toFloat(), vertices[vo + 2].toFloat())
            if (normalName != null) {
                // normals
                normals.put(vertIndex, totalVertIndex, faceIndex, buffer)
            }
            if (materialIndexName != null) {
                // material index
                materialIDs?.put(vertIndex, totalVertIndex, faceIndex, buffer, materialInts) ?: run {
                    if (materialInts) {
                        buffer.putInt(0)
                    } else {
                        buffer.put(0f)
                    }
                }
            }
            if (weightCount > 0) {
                // weights (yes, they are a bit more complicated, as they are not given directly)
                val wIndex0 = vertIndex * maxWeightsDefault
                val wIndex1 = wIndex0 + weightCount
                var weightSum = 0f
                for (i in wIndex0 until wIndex1) {// count weights for normalization
                    weightSum += weightValues[i]
                }
                val minWeight = 0.01f
                val weightNormFactor = 1f / max(minWeight, weightSum)
                weightSum = 0f
                for (i in wIndex0 until wIndex1) {// weight values
                    val value = weightValues[i] * weightNormFactor
                    buffer.put(if (i == wIndex1 - 1) 1f - weightSum else value)
                    weightSum += value
                }
                for (i in wIndex0 until wIndex1) {// weight indices
                    val index = weightIndices[i]
                    buffer.putUByte(index)
                    if(index < 0 || index > weightIndexLimit) throw IllegalStateException("$index >= ${bones.size}")
                }
            }
            if (maxUVs > 0) {
                // uvs
                relevantUVMaps.forEach { map ->
                    map.put(vertIndex, totalVertIndex, faceIndex, buffer)
                }
            }
        }

        var ai = 0
        var bi = 0
        for ((ci, rc) in faces.withIndex()) {
            val c = if (rc < 0) -1 - rc else rc
            when (j) {
                -1 -> {
                    a = c
                    ai = ci
                }
                +0 -> {
                    b = c
                    bi = ci
                }
                else -> {
                    // add triangle abc
                    put(a, ai)
                    put(b, bi)
                    put(c, ci)
                    faceIndex++
                    b = c
                    bi = ci
                }
            }
            if (rc < 0) {
                // end vertex
                j = -1
            } else {
                j++
            }
        }

        // LOGGER.info("${buffer.nioBuffer!!.position()} vs ${buffer.nioBuffer!!.capacity()}")

        return buffer

    }

    private fun initFaceCount(): Int {
        var j = -1
        var faceCount = 0
        for (i in faces) {
            if (i < 0) {
                // end vertex
                if (j > 0) faceCount += j
                j = -1
            } else {
                j++
            }
        }
        return faceCount
    }

    var faceCount = initFaceCount()

    val normals = LayerElementDoubles(node.getFirst("LayerElementNormal")!!, 3)

    // val vertexColors = node["LayerElementColor"].map { LayerElementDA(it, 3) }

    // are we interested in vertex colors?
    // are we interested in UVs? yes

    val uvs = node.mapAll("LayerElementUV") { LayerElementDoubles(it, 2) }

    val materialIDs = node.getFirst("LayerElementMaterial")?.run {
        LayerElementInts(this, 1)
    }

    // there is information, which could swizzle uv and color values...

    val bones = ArrayList<FBXDeformer>()
    var nextBoneIndex = 0
    fun findBoneWeights() {

        val deformers = this.children.filterIsInstance<FBXDeformer>()
        if (FBXReader.printDebugMessages) LOGGER.info("Deformers: ${deformers.size}")
        if (deformers.size != 1) {
            if (deformers.size > 1) LOGGER.warn("Unexpected multiple deformers for root under geometry")
            return
        }

        val rootDeformer = deformers[0]
        val bones: List<FBXDeformer> = rootDeformer.children.filterIsInstance<FBXDeformer>()
        if (FBXReader.printDebugMessages) LOGGER.info("Bones: ${bones.size}")
        if (bones.isEmpty()) return

        val bonesByName = bones.associateBy { it.name.split(' ').last() }

        fun getModel(deformer: FBXDeformer) = deformer.children.first { it is FBXModel } as FBXModel

        val todo = ArrayList<Pair<FBXModel, FBXDeformer>>(100)

        val rootBone = bones[0]
        todo += getModel(rootBone) to rootBone

        while (todo.isNotEmpty()) {
            val (model, bone) = todo.removeAt(todo.lastIndex)
            findBoneWeights(bone)
            for (childModel in model.children) {
                if (childModel is FBXModel) {
                    val childBone = bonesByName[childModel.name]
                    if (childBone != null) {
                        childBone.parent = bone
                        todo += childModel to childBone
                    } // else end bone without transform
                }
            }
        }

    }

    private fun findBoneWeights(bone: FBXDeformer) {
        val weights = bone.weights ?: return
        val indices = bone.indices ?: return
        bones += bone
        val boneIndex = nextBoneIndex++
        if (FBXReader.printDebugMessages) LOGGER.info("Bone $boneIndex: ${bone.name}, ${bone.depth}")
        bone.index = boneIndex
        indices.forEachIndexed { index, vertIndex ->
            addWeight(vertIndex, boneIndex, weights[index].toFloat())
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(FBXGeometry::class)
    }

}

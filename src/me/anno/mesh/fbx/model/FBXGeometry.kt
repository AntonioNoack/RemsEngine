package me.anno.mesh.fbx.model

import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.StaticBuffer
import me.anno.mesh.fbx.model.FBXShader.maxWeightsDefault
import me.anno.mesh.fbx.structure.FBXNode
import org.apache.logging.log4j.LogManager
import kotlin.math.max
import kotlin.math.min

class FBXGeometry(node: FBXNode) : FBXObject(node) {

    val xyz = node.getDoubleArray("Vertices")!!
    val vertexCount = xyz.size / 3
    val faces = node.getIntArray("PolygonVertexIndex")!! // polygons, each: 0, 1, 2, 3 ... , ~8
    val weights = FloatArray(vertexCount * maxWeightsDefault * 2)

    fun addWeight(vertexIndex: Int, boneIndex: Int, weight: Float) {
        var baseIndex = vertexIndex * maxWeightsDefault * 2
        for (i in 0 until maxWeightsDefault) {
            val oldWeight = weights[baseIndex]
            if (weight > oldWeight) {
                weights[baseIndex++] = weight
                weights[baseIndex] = boneIndex.toFloat()
                break
            }
            baseIndex += 2
        }
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
            attributes += Attribute("weightIndices", weightCount)
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
                materialIDs?.put(vertIndex, totalVertIndex, faceIndex, buffer, materialInts) ?: {
                    if (materialInts) {
                        buffer.putInt(0)
                    } else {
                        buffer.put(0f)
                    }
                }()
            }
            if (weightCount > 0) {
                // weights (yes, they are a bit more complicated, as they are not given directly)
                var weightBaseIndex = vertIndex * maxWeightsDefault * 2
                var weightSum = 0f
                for (i in 0 until weightCount) {// count weights for normalization
                    weightSum += weights[weightBaseIndex]
                    weightBaseIndex += 2
                }
                val minWeight = 0.01f
                val weightNormFactor = 1f / max(minWeight, weightSum)
                weightBaseIndex -= weightCount * 2
                buffer.put(max(minWeight, weights[weightBaseIndex]) * weightNormFactor)
                weightBaseIndex += 2
                for (i in 1 until weightCount) {// weight values
                    buffer.put(weights[weightBaseIndex] * weightNormFactor)
                    weightBaseIndex += 2
                }
                weightBaseIndex -= weightCount * 2 - 1
                for (i in 0 until weightCount) {// weight indices
                    buffer.put(weights[weightBaseIndex])
                    weightBaseIndex += 2
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

        // println("${buffer.nioBuffer!!.position()} vs ${buffer.nioBuffer!!.capacity()}")

        return buffer

    }

    val faceCount = {// OMG lol
        var j = -1
        var sum = 0
        for (i in faces) {
            if (i < 0) {
                // end vertex
                if (j > 0) sum += j
                j = -1
            } else {
                j++
            }
        }
        sum
    }()

    val normals = LayerElementDoubles(node["LayerElementNormal"].first(), 3)

    // val vertexColors = node["LayerElementColor"].map { LayerElementDA(it, 3) }

    // are we interested in vertex colors?
    // are we interested in UVs? yes

    val uvs = node["LayerElementUV"].map { LayerElementDoubles(it, 2) }

    val materialIDs = node["LayerElementMaterial"].firstOrNull()?.run {
        LayerElementInts(this, 1)
    }

    // there is information, which could swizzle uv and color values...

    val bones = ArrayList<FBXDeformer>()
    var nextBoneIndex = 0
    fun findBoneWeights(model: FBXModel) {
        val todo = ArrayList<Pair<FBXModel, FBXDeformer>>(100)
        val deformer = children
            .filterIsInstance<FBXDeformer>()
            .firstOrNull() ?: return
        val boneMap = deformer.children
            .filterIsInstance<FBXDeformer>()
            .associateBy { it.name.split(' ').last() }
        LOGGER.info(boneMap.keys.toString())
        todo += model to (boneMap[model.name] ?: throw RuntimeException("Bone ${model.name} wasn't found"))
        while (true) {
            val (lastModel, lastBone) = todo.lastOrNull() ?: break
            todo.removeAt(todo.lastIndex)
            findBoneWeights(lastBone)
            lastModel.children.filterIsInstance<FBXModel>()
                .forEach { model2 ->
                    val bone = boneMap[model2.name]
                    if (bone != null) {
                        bone.parent = lastBone
                        todo += model2 to bone
                    } // else end bone without transform
                }
        }
    }

    fun findBoneWeights(bone: FBXDeformer) {
        val weights = bone.weights ?: return
        val indices = bone.indices ?: return
        bones += bone
        val boneIndex = nextBoneIndex++
        println("$boneIndex: ${bone.name}, ${bone.depth}")
        bone.index = boneIndex
        indices.forEachIndexed { index, vertIndex ->
            addWeight(vertIndex, boneIndex, weights[index].toFloat())
        }
    }

    companion object {
        private val LOGGER = LogManager.getLogger(FBXGeometry::class)
    }

}

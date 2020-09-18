package me.anno.objects.meshes.fbx.model

import me.anno.gpu.ShaderLib
import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.StaticFloatBuffer
import me.anno.gpu.shader.ShaderPlus
import me.anno.objects.meshes.fbx.structure.FBXNode
import kotlin.math.max
import kotlin.math.min

class FBXGeometry(node: FBXNode) : FBXObject(node) {

    companion object {
        const val maxWeights = 4 // 1 .. 4
        const val maxBones = 256
        fun getShader(
            v3DBase: String, positionPostProcessing: String,
            y3D: String, getTextureLib: String
        ): ShaderPlus {
            return ShaderLib.createShaderPlus(
                "fbx", v3DBase +
                        "a3 xyz;\n" +
                        "a2 uvs;\n" +
                        "a3 normals;\n" +
                        // "a1 materialIndex;\n" +
                        "in ivec3 weightIndices;\n" +
                        "a3 weightValues;\n" +
                        "uniform mat4x4 transforms[$maxBones];\n" +
                        "void main(){\n" +
                        "   vec3 localPosition = (transforms[weightIndices.x] * vec4(xyz, 1.0)).xyz * weightValues.x;\n" + //  * weightValues.x
                        "   if(weightValues.y > 0.01) localPosition += (transforms[weightIndices.y] * vec4(xyz, 1.0)).xyz * weightValues.y;\n" +
                        "   if(weightValues.z > 0.01) localPosition += (transforms[weightIndices.z] * vec4(xyz, 1.0)).xyz * weightValues.z;\n" +
                        // "   if(weightValues.w > 0.01) localPosition += (transforms[int(weightIndices.w)] * vec4(xyz, 1.0)).xyz * weightValues.w;\n" +
                        "   gl_Position = transform * vec4(localPosition, 1.0);\n" + // already include second transform? yes, we should probably do that
                        "   uv = uvs;\n" +
                        "   normal = normals;\n" +
                        positionPostProcessing +
                        "}", y3D + "" +
                        "varying vec3 normal;\n", "" +
                        "uniform vec4 tint;" +
                        "uniform sampler2D tex;\n" +
                        getTextureLib +
                        "void main(){\n" +
                        "   vec4 color = getTexture(tex, uv);\n" +
                        "   color.rgb *= 0.5 + 0.5 * dot(vec3(1.0, 0.0, 0.0), normal);\n" +
                        "   gl_FragColor = tint * color;\n" +
                        "}", listOf()
            )
        }
    }

    val xyz = node.getDoubleArray("Vertices")!!
    val vertexCount = xyz.size / 3
    val faces = node.getIntArray("PolygonVertexIndex")!! // polygons, each: 0, 1, 2, 3 ... , ~8
    val weights = FloatArray(vertexCount * maxWeights * 2)

    fun addWeight(vertexIndex: Int, boneIndex: Int, weight: Float) {
        var baseIndex = vertexIndex * maxWeights * 2
        for (i in 0 until maxWeights) {
            val oldWeight = weights[baseIndex]
            if (weight > oldWeight) {
                weights[baseIndex++] = weight
                weights[baseIndex] = boneIndex.toFloat()
                break
            }
            baseIndex += 2
        }
    }

    fun generateMesh(): StaticFloatBuffer {

        val uvMapCount = min(1, uvs.size) // could be changed
        val weightCount = maxWeights
        val attributes = arrayListOf(
            Attribute("xyz", 3),
            Attribute("normals", 3),
            Attribute("materialIndex", 1)
        )

        attributes += Attribute("weightValues", weightCount)
        attributes += Attribute("weightIndices", weightCount)

        val relevantUVMaps = Array(uvMapCount) { uvs[it] }
        for (i in 0 until uvMapCount) {
            attributes += Attribute(
                when (i) {
                    0 -> "uvs"
                    else -> "uvs$i"
                }, 2
            )
        }

        val buffer = StaticFloatBuffer(attributes, faceCount * 3)
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
            // normals
            normals.put(vertIndex, totalVertIndex, faceIndex, buffer)
            // material index
            materialIDs.put(vertIndex, totalVertIndex, faceIndex, buffer)
            // weights (yes, they are a bit more complicated, as they are not given directly)
            var weightBaseIndex = vertIndex * maxWeights * 2
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
            // uvs
            relevantUVMaps.forEach { map ->
                map.put(vertIndex, totalVertIndex, faceIndex, buffer)
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

        println("${buffer.floatBuffer.position()} vs ${buffer.floatBuffer.capacity()}")

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

    open class LayerElement(n: FBXNode) {
        val accessType = when (val mapping = n.getProperty("MappingInformationType") as String) {
            "ByVertex", "ByVertice" -> 0 // smooth
            "ByPolygon" -> 1 // hard
            "ByPolygonVertex" -> 2 // for every vert from every poly separate; smooth + hard mixed
            "AllSame" -> 3
            else -> throw RuntimeException("Unknown mapping type $mapping")
        }
    }

    class LayerElementDA(n: FBXNode, val components: Int) : LayerElement(n) {
        val data = n.getDoubleArray("Normals") ?: n.getDoubleArray("Colors") ?: n.getDoubleArray("UV")!!
        val dataIndex = if (when (n.getProperty("ReferenceInformationType")) {
                "Direct" -> false
                "IndexToDirect", "Index" -> true
                else -> true // idk -> use if available
            }
        ) n.getIntArray("NormalsIndex")
            ?: n.getIntArray("UVIndex") else null // a second remapping xD ; could exist for colors, materials (useless) and colors as well

        fun put(vertIndex: Int, totalVertIndex: Int, faceIndex: Int, buffer: StaticFloatBuffer) {
            var index = (when (accessType) {
                0 -> vertIndex
                1 -> faceIndex
                2 -> totalVertIndex
                else -> 0
            })
            if (dataIndex != null) index = dataIndex[index]
            index *= components
            for (i in 0 until components) {
                buffer.put(data[index++].toFloat())
            }
        }
    }

    class LayerElementIA(n: FBXNode, val components: Int) : LayerElement(n) {
        val data = n.getIntArray("Materials")!!
        fun put(vertIndex: Int, totalVertIndex: Int, faceIndex: Int, buffer: StaticFloatBuffer) {
            var index = (when (accessType) {
                0 -> vertIndex
                1 -> faceIndex
                2 -> totalVertIndex
                else -> 0
            }) * components
            for (i in 0 until components) {
                buffer.put(data[index++].toFloat())
            }
        }
    }

    val normals = LayerElementDA(node["LayerElementNormal"].first(), 3)

    // val vertexColors = node["LayerElementColor"].map { LayerElementDA(it, 3) }

    // are we interested in vertex colors?
    // are we interested in UVs? yes

    val uvs = node["LayerElementUV"].map { LayerElementDA(it, 2) }

    val materialIDs = LayerElementIA(node["LayerElementMaterial"].first(), 1)

    // there is information, which could swizzle uv and color values...

    val bones = ArrayList<FBXDeformer>()
    var nextBoneIndex = 0
    fun findBoneWeights(model: FBXModel) {
        val todo = ArrayList<Pair<FBXModel, FBXDeformer>>(100)
        val boneMap = children
            .filterIsInstance<FBXDeformer>().first().children
            .filterIsInstance<FBXDeformer>()
            .associateBy { it.name.split(' ').last() }
        println(boneMap.keys)
        todo += model to (boneMap[model.name] ?: throw RuntimeException("Bone ${model.name} wasn't found"))
        while (true) {
            val (lastModel, lastBone) = todo.lastOrNull() ?: break
            todo.removeAt(todo.lastIndex)
            findBoneWeights(lastBone)
            lastModel.children.filterIsInstance<FBXModel>()
                .forEach { model ->
                    val bone = boneMap[model.name]
                    if(bone != null){
                        bone.parent = lastBone
                        todo += model to bone
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

}

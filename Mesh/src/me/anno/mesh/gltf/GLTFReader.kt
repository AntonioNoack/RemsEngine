package me.anno.mesh.gltf

import me.anno.ecs.components.anim.AnimationState
import me.anno.ecs.components.anim.Bone
import me.anno.ecs.components.anim.ImportedAnimation
import me.anno.ecs.components.anim.Skeleton
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabReadable
import me.anno.ecs.prefab.change.Path
import me.anno.gpu.CullMode
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.pipeline.PipelineStage
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.graph.hdb.ByteSlice
import me.anno.io.Streams.readLE16
import me.anno.io.Streams.readLE32
import me.anno.io.Streams.readLE32F
import me.anno.io.base64.Base64
import me.anno.io.files.FileReference
import me.anno.io.files.inner.InnerFolder
import me.anno.io.files.inner.temporary.InnerTmpByteFile
import me.anno.io.json.generic.JsonReader
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.max
import me.anno.maths.Maths.min
import me.anno.mesh.assimp.AnimatedMeshesLoader.createBoneByBone
import me.anno.mesh.gltf.GLTFConstants.BINARY_CHUNK_MAGIC
import me.anno.mesh.gltf.GLTFConstants.FILE_MAGIC
import me.anno.mesh.gltf.GLTFConstants.GL_BYTE
import me.anno.mesh.gltf.GLTFConstants.GL_FLOAT
import me.anno.mesh.gltf.GLTFConstants.GL_SHORT
import me.anno.mesh.gltf.GLTFConstants.GL_UNSIGNED_BYTE
import me.anno.mesh.gltf.GLTFConstants.GL_UNSIGNED_INT
import me.anno.mesh.gltf.GLTFConstants.GL_UNSIGNED_SHORT
import me.anno.mesh.gltf.GLTFConstants.JSON_CHUNK_MAGIC
import me.anno.mesh.gltf.reader.Accessor
import me.anno.mesh.gltf.reader.AnimSampler
import me.anno.mesh.gltf.reader.BufferView
import me.anno.mesh.gltf.reader.Node
import me.anno.mesh.gltf.reader.Texture
import me.anno.mesh.gltf.writer.Sampler
import me.anno.utils.Color.rgba
import me.anno.utils.assertions.assertTrue
import me.anno.utils.async.Callback
import me.anno.utils.async.Callback.Companion.map
import me.anno.utils.async.Callback.Companion.mapCallback
import me.anno.utils.files.Files.nextName
import me.anno.utils.structures.lists.Lists.createArrayList
import me.anno.utils.structures.lists.Lists.createList
import me.anno.utils.structures.lists.Lists.sortedByTopology
import me.anno.utils.structures.lists.Lists.wrap
import me.anno.utils.types.AnyToDouble.getDouble
import me.anno.utils.types.AnyToFloat.getFloat
import me.anno.utils.types.AnyToInt.getInt
import me.anno.utils.types.Arrays.readLE32
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4d
import org.joml.Matrix4f
import org.joml.Matrix4x3f
import org.joml.Quaterniond
import org.joml.Quaternionf
import org.joml.Vector2f
import org.joml.Vector2i
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector4f
import java.io.IOException

/**
 * todo implement a GLTF-reader,
 *  so we can get rid of that aspect of Assimp, and have the proper materials;
 *  we also could support some of their extensions in the future
 * */
class GLTFReader(val src: FileReference) {

    // todo read double-sided

    companion object {
        private val LOGGER = LogManager.getLogger(GLTFWriter::class)

        const val BRIGHTNESS_FACTOR = 5f // looks good

        fun readAsFolder(src: FileReference, callback: Callback<InnerFolder>) {
            src.readBytes { bytes, err ->
                if (bytes != null) {
                    GLTFReader(src).readAnyGLTF(bytes, callback)
                } else callback.err(err)
            }
        }
    }

    lateinit var json: Map<String, Any?>

    private val innerFolder = InnerFolder(src)
    private val materialFolder = InnerFolder(innerFolder, "materials")
    private val meshFolder = InnerFolder(innerFolder, "meshes")

    private val buffers = ArrayList<ByteSlice>()
    private val embeddedFiles = ArrayList<Pair<String, ByteArray>>()

    private val bufferViews = ArrayList<BufferView>()
    private val accessors = ArrayList<Accessor>()
    private val materials = ArrayList<FileReference>()
    private val meshes = ArrayList<List<FileReference>>()
    private val images = ArrayList<FileReference>()
    private val samplers = ArrayList<Sampler>()
    private val textures = ArrayList<Texture>()
    private val animations = ArrayList<FileReference>()
    private val skins = ArrayList<FileReference>()
    private val nodes = ArrayList<Node>()
    private val scenes = ArrayList<Prefab>()

    private fun decodeDataURI(uri: String): ByteArray {
        // data:application/gltf-buffer;base64,
        val zeroPrefix = "data:"
        val prefix = ";base64,"
        val i = uri.indexOf(prefix)
        if (i < 0) throw IllegalStateException("Expected $prefix in uri")
        val mimeType = uri.substring(zeroPrefix.length, i)
        val base64 = uri.substring(i + prefix.length)
        val data = Base64.decodeBase64(base64)
        embeddedFiles.add(mimeType to data)
        return data
    }

    private fun resolveUri(uri: String): FileReference {
        return if (uri.startsWith("data:")) {
            InnerTmpByteFile(decodeDataURI(uri))
        } else {
            src.getSibling(uri)
        }
    }

    fun readTextGLTF(bytes: ByteArray, callback: Callback<InnerFolder>) {
        val reader = JsonReader(bytes)
        json = reader.readObject()

        val bufferFiles =
            getList(json["buffers"]).map { buffer0 ->
                val buffer = buffer0 as? Map<*, *> ?: emptyMap<Any?, Any?>()
                resolveUri(buffer["uri"].toString())
            }

        bufferFiles.mapCallback({ _, file, cb ->
            file.readBytes(cb)
        }, callback.map { buffers1 ->
            buffers.addAll(buffers1.map { ByteSlice(it) })
            readCommon()
            innerFolder
        })
    }

    fun readBinaryGLTF(input: ByteArray, callback: Callback<InnerFolder>) {
        // confirm magic
        if (input.readLE32(0) != FILE_MAGIC) {
            callback.err(IOException("Invalid Magic"))
            return
        }

        val version = input.readLE32(4)
        val fileSize = input.readLE32(8)
        if (version != 2) {
            LOGGER.warn("Unknown glTF version $version, trying to read it like version 2")
        }

        readChunks(input, fileSize)
        readCommon()
        callback.ok(innerFolder)
    }

    private fun readChunks(input: ByteArray, fileSize: Int) {
        var position = 12
        var remaining = min(fileSize - position, input.size)
        while (remaining >= 8) {
            val chunkSize = input.readLE32(position)
            val chunkType = input.readLE32(position + 4)
            position += 8
            remaining -= chunkSize + 4
            if (remaining >= 0) {
                if (chunkType == JSON_CHUNK_MAGIC || chunkType == BINARY_CHUNK_MAGIC) {
                    val slice = ByteSlice(input, position until position + chunkSize)
                    if (chunkType == JSON_CHUNK_MAGIC) json = JsonReader(slice.stream()).readObject()
                    else buffers.add(slice)
                }
                position += chunkSize
            } else break
        }
    }

    fun readAnyGLTF(input: ByteArray, callback: Callback<InnerFolder>) {
        val firstChar = input.getOrNull(0) ?: 0
        if (firstChar == 'g'.code.toByte()) readBinaryGLTF(input, callback)
        else readTextGLTF(input, callback)
    }

    private fun readCommon() {
        readBufferViews()
        readAccessors()
        readSamplers()
        readImages()
        readTextures()
        readMaterials()
        readNodes()
        readSkins()
        readMeshes()
        readAnimations()
        readScenes()
        writeEmbeddedFiles()

        // prevent further modifications
        innerFolder.sealPrefabs()
    }

    private fun getExtensionFromMimeType(mimeType: String?): String {
        return when (mimeType) {
            "image/png" -> "png"
            "image/jpeg" -> "jpeg"
            "application/json" -> "json"
            else -> "bin"
        }
    }

    private fun writeEmbeddedFiles() {
        var i = 0
        for ((mimeType, data) in embeddedFiles) {
            val extension = getExtensionFromMimeType(mimeType)
            innerFolder.createByteChild("${i++}.$extension", data)
        }
    }

    private fun getMap(instance: Any?): Map<*, *> {
        return instance as? Map<*, *> ?: emptyMap<Any?, Any?>()
    }

    private fun getList(instance: Any?): List<*> {
        return instance as? List<*> ?: emptyList<Any?>()
    }

    private inline fun <V> forEachMap(attrName: String, dst: ArrayList<V>, map: (Map<*, *>) -> V): List<V> {
        return forEachMap(json[attrName], dst, map)
    }

    private inline fun <V> forEachMap(instance: Any?, dst: ArrayList<V>, map: (Map<*, *>) -> V): List<V> {
        val list = getList(instance)
        dst.ensureCapacity(list.size)
        for (i in list.indices) {
            dst.add(map(getMap(list[i])))
        }
        return dst
    }

    private inline fun forEachMap(instance: Any?, map: (Map<*, *>) -> Unit) {
        val list = getList(instance)
        for (i in list.indices) {
            map(getMap(list[i]))
        }
    }

    private fun readNodes() {
        val tmp4x4 = Matrix4d()
        val tmp16 = DoubleArray(16)
        forEachMap("nodes", nodes) { node ->
            val node1 = Node(nodes.size)
            node1.name = node["name"] as? String
            node1.children = getList(node["children"]).map { getInt(it) }
            val matrix = getList(node["matrix"])
            val pos = getList(node["translation"])
            val rot = getList(node["rotation"])
            val sca = getList(node["scale"])
            if (pos.size == 3) node1.translation = Vector3d(
                getDouble(pos[0]), getDouble(pos[1]), getDouble(pos[2])
            )
            if (rot.size == 4) node1.rotation = Quaterniond(
                getDouble(rot[0]), getDouble(rot[1]), getDouble(rot[2]), getDouble(rot[3])
            )
            if (sca.size == 3) node1.scale = Vector3d(
                getDouble(sca[0]), getDouble(sca[1]), getDouble(sca[2])
            )
            if (matrix.size == 16) {
                for (i in tmp16.indices) tmp16[i] = getDouble(matrix[i])
                tmp4x4.set(tmp16)
                node1.translation = tmp4x4.getTranslation(Vector3d())
                node1.rotation = tmp4x4.getUnnormalizedRotation(Quaterniond())
                node1.scale = tmp4x4.getScale(Vector3d())
            }
            node1.mesh = getInt(node["mesh"], -1)
            node1.skin = getInt(node["skin"], -1)
            node1
        }
        for (node in nodes) {
            for (child in node.children) {
                nodes[child].parent = node
            }
        }
    }

    private fun readAnimations() {
        val usedNames = HashSet<String>()
        val animationFolder = InnerFolder(innerFolder, "animations")

        val sortedNodes = nodes.sortedByTopology { it.parent.wrap() }!!

        val numNodes = nodes.size
        val rotateByNode = createArrayList<AnimSampler?>(numNodes, null)
        val translateByNode = createArrayList<AnimSampler?>(numNodes, null)
        val scaleByNode = createArrayList<AnimSampler?>(numNodes, null)
        val samplers = ArrayList<AnimSampler>()

        val pos = Vector3f()
        val rot = Quaternionf()
        val sca = Vector3f()

        val tmp3 = Vector3f()
        val tmp4 = Quaternionf()

        forEachMap("animations", animations) { anim ->

            samplers.clear()
            forEachMap(anim["samplers"], samplers) { node ->
                val sampler = AnimSampler()
                sampler.times = loadFloatArray(getInt(node["input"], -1), 1)
                sampler.values = loadFloatArray(getInt(node["output"], -1), -1)
                sampler.interpolation = node["interpolation"].toString()
                sampler
            }

            translateByNode.fill(null)
            rotateByNode.fill(null)
            scaleByNode.fill(null)

            forEachMap(anim["channels"]) { node ->
                val sampler = samplers[getInt(node["sampler"])]
                val target = getMap(node["target"])
                val targetNode = getInt(target["node"])
                val targetPath = target["path"]
                when (targetPath as? String) {
                    "rotation" -> rotateByNode[targetNode] = sampler
                    "translation" -> translateByNode[targetNode] = sampler
                    "scale" -> scaleByNode[targetNode] = sampler
                }
            }

            // create a list of all bone nodes...
            // todo how do we find out, which skeleton is the correct one???
            val skeletonId = 0
            val skeletonRef = skins[skeletonId]
            val skeleton = (skeletonRef as PrefabReadable)
                .readPrefab().getSampleInstance() as Skeleton
            val bones = skeleton.bones

            var time = 0f
            var idx0 = 0
            var idx1 = 0
            var idxF = 0f

            fun getTime(times: FloatArray) {
                idx0 = times.binarySearch(time)
                if (idx0 < 0) idx0 = -1 - idx0
                idx0 = min(max(idx0 - 1, 0), times.size - 2)
                idx1 = idx0 + 1
                idxF = (time - times[idx0]) / (times[idx1] - times[idx0])
            }

            fun getVector3f(sampler: AnimSampler?, byNode: Vector3d?, default: Float, dst: Vector3f) {
                if (sampler != null) {
                    getTime(sampler.times!!)
                    val values = sampler.values!!
                    dst.set(values, idx0 * 3)
                        .lerp(tmp3.set(values, idx1 * 3), idxF)
                } else if (byNode != null) {
                    dst.set(byNode)
                } else dst.set(default)
            }

            fun getQuaternionf(sampler: AnimSampler?, byNode: Quaterniond?, dst: Quaternionf) {
                if (sampler != null) {
                    getTime(sampler.times!!)
                    val values = sampler.values!!
                    dst.set(values, idx0 * 4)
                        .slerp(tmp4.set(values, idx1 * 4), idxF)
                } else if (byNode != null) {
                    dst.set(byNode)
                } else dst.identity()
            }

            // calculate how many frames we need
            val duration = samplers.maxOfOrNull { it.times?.last() ?: 0f } ?: 0f
            val numFrames = max(samplers.maxOfOrNull { it.times?.size ?: 0 } ?: 0, 1)
            val dt = if (duration > 0f) duration / numFrames else 0f

            val jointNodes = jointNodes[skeletonId]
            val skinningMatrices = (0 until numFrames).map { frameIndex ->
                time = frameIndex * dt
                for (i in sortedNodes.indices) {
                    val node = sortedNodes[i]
                    if (i == 0) assertTrue(node.parent == null)

                    getVector3f(translateByNode[node.id], node.translation, 0f, pos)
                    getVector3f(scaleByNode[node.id], node.scale, 1f, sca)
                    getQuaternionf(rotateByNode[node.id], node.rotation, rot)

                    val parent = node.parent
                    if (parent != null) {
                        node.globalJointTransform.set(parent.globalJointTransform)
                    } else {
                        node.globalJointTransform.identity()
                    }

                    node.globalJointTransform.translate(pos).rotate(rot).scale(sca)
                }

                jointNodes.map { node ->
                    node.globalJointTransform
                        .mul(bones[node.boneId].inverseBindPose, Matrix4x3f())
                }
            }

            val prefab = Prefab("ImportedAnimation")
            prefab["skeleton"] = skeletonRef
            prefab["duration"] = duration
            prefab["frames"] = skinningMatrices

            val imported = ImportedAnimation()
            imported.skeleton = skeletonRef
            imported.duration = duration
            imported.frames = skinningMatrices
            val bbbPrefab = createBoneByBone(imported, jointNodes.size, null, null)

            val name = anim["name"] as? String ?: "Animation"
            val animFolder = InnerFolder(animationFolder, nextName(name, usedNames))
            animFolder.createPrefabChild("BoneByBone.json", bbbPrefab)
            animFolder.createPrefabChild("Imported.json", prefab)
        }

        for (skin in skins) {
            val prefab = (skin as PrefabReadable).readPrefab()
            prefab["animations"] = animations.associateBy { it.getParent().name }
        }
    }

    private val jointNodes = ArrayList<List<Node>>()

    private fun readSkins() {
        val tmp = Matrix4f()
        val skeletons1 = InnerFolder(innerFolder, "skeletons")
        forEachMap("skins", skins) { src ->

            val prefab = Prefab("Skeleton")

            val joints = getList(src["joints"]).map { nodes[getInt(it)] }
            jointNodes.add(joints)
            for (j in joints.indices) {
                joints[j].boneId = j
            }

            val inverseBindMatrixId = getInt(src["inverseBindMatrices"], -1)
            val inverseBindMatrixData = loadFloatArray(inverseBindMatrixId, 16)!!
            val inverseBindMatrices = createList(joints.size) { boneId ->
                tmp.set(inverseBindMatrixData, boneId * 16)
                Matrix4x3f().set(tmp)
            }
            val bones = joints.mapIndexed { boneId, node ->
                val bone = Bone(boneId, node.parent?.boneId ?: -1, node.name ?: "Bone$boneId")
                bone.setInverseBindPose(inverseBindMatrices[boneId])
                bone
            }
            prefab["bones"] = bones
            val name = if (skins.isEmpty()) "Skeleton.json" else "Skeleton${skins.size}.json"
            skeletons1.createPrefabChild(name, prefab)
        }
    }

    private fun readScenes() {
        val mainScene = getInt(json["scene"])
        val scenesFolder = InnerFolder(innerFolder, "scenes")
        val usedNames = HashSet<String>()
        var nextSceneId = 0
        forEachMap("scenes", scenes) { scene ->
            val prefab = Prefab("Entity")
            if (scene["name"] is String) prefab["name"] = scene["name"]
            val nodes = getList(scene["nodes"]).map { nodes[getInt(it)] }
            if (nodes.size != 1) {
                // multiple roots -> append to single root
                val usedNames1 = HashSet<String>()
                for (node in nodes) {
                    addNode(prefab, Path.ROOT_PATH, node, usedNames1)
                }
            } else {
                // single root
                writeNode(prefab, Path.ROOT_PATH, nodes.first())
            }
            val sceneId = nextSceneId++
            val fileName = nextName(prefab.instanceName ?: "Scene", usedNames)
            if (sceneId == mainScene) {
                innerFolder.createPrefabChild("Scene.json", prefab)
            } else {
                scenesFolder.createPrefabChild("$fileName.json", prefab)
            }
            prefab
        }
    }

    private fun addNode(prefab: Prefab, parentPath: Path, node: Node, usedNames: HashSet<String>) {
        val name = nextName(node.name ?: "Node", usedNames)
        val path = prefab.add(parentPath, 'e', "Entity", name)
        writeNode(prefab, path, node)
    }

    private fun writeNode(prefab: Prefab, path: Path, node: Node) {
        if (node.translation != null) prefab[path, "position"] = node.translation
        if (node.rotation != null) prefab[path, "rotation"] = node.rotation
        if (node.scale != null) prefab[path, "scale"] = node.scale
        if (node.children.isNotEmpty()) {
            val usedNames = HashSet<String>()
            for (child in node.children) {
                addNode(prefab, path, nodes[child], usedNames)
            }
        }
        if (node.mesh in meshes.indices) {
            val hasSkin = node.skin in skins.indices
            val clazz = if (hasSkin) "AnimMeshComponent" else "MeshComponent"
            val meshes = meshes[node.mesh]
            for (i in meshes.indices) {
                val meshPath = prefab.add(path, 'c', clazz, "Mesh$i")
                prefab[meshPath, "meshFile"] = meshes[i]
                if (hasSkin && animations.isNotEmpty()) {
                    prefab[meshPath, "animations"] = listOf(AnimationState(animations.first(), 1f))
                }
            }
        }
    }

    private fun readImages() {
        forEachMap("images", images) { image ->
            if ("uri" in image) resolveUri(image["uri"].toString())
            else {
                // instead of URI, there could also be bufferView: 8, mimeType: image/png
                // extension can be deduced from mimeType-property
                val extension = getExtensionFromMimeType(image["mimeType"] as? String)
                val buffer = bufferViews[getInt(image["bufferView"])]
                InnerTmpByteFile(buffer.bytes(), extension)
            }
        }
    }

    private fun readBufferViews() {
        forEachMap("bufferViews", bufferViews) { view ->
            val idx = getInt(view["buffer"])
            assertTrue(idx in buffers.indices)
            val buffer = buffers[idx]
            val length = getInt(view["byteLength"])
            val offset = getInt(view["byteOffset"])
            assertTrue(offset >= 0 && length >= 0 && offset + length <= buffer.size)
            BufferView(buffer.bytes, offset + buffer.range.first, length)
        }
    }

    private fun readAccessors() {
        forEachMap("accessors", accessors) { accessor ->
            val view0 = bufferViews[getInt(accessor["bufferView"])]
            val offset = getInt(accessor["byteOffset"])
            val view = if (offset == 0) view0
            else BufferView(view0.buffer, view0.offset + offset, view0.length - offset)
            val compType = getInt(accessor["componentType"])
            val count = getInt(accessor["count"])
            val type = accessor["type"].toString()
            val numComp = when (type) {
                "SCALAR" -> 1
                "VEC2" -> 2
                "VEC3" -> 3
                "VEC4" -> 4
                "MAT4" -> 16
                else -> -1
            }
            Accessor(view, compType, count, numComp)
            // todo implement sparse accessors, which are apparently used for morph targets
        }
    }

    private fun readMaterials() {
        forEachMap("materials", materials) { material ->
            val prefab = Prefab("Material")
            val name = (material["name"] as? String ?: "").ifBlank { "${materials.size}" }
            prefab["name"] = name
            val pbr = getMap(material["pbrMetallicRoughness"])
            val baseColor = getList(pbr["baseColorFactor"])
            if (baseColor.size == 4) {
                prefab["diffuseBase"] = Vector4f(
                    getFloat(baseColor[0]),
                    getFloat(baseColor[1]),
                    getFloat(baseColor[2]),
                    getFloat(baseColor[3])
                )
            }
            val color = getMap(pbr["baseColorTexture"])
            val colorTex = textures.getOrNull(getInt(color["index"], -1))
            if (colorTex != null) {
                prefab["diffuseMap"] = colorTex.source
                prefab["clamping"] = colorTex.sampler.clamping
                prefab["linearFiltering"] = colorTex.sampler.filtering == Filtering.LINEAR
            }
            prefab["metallicMinMax"] = Vector2f(0f, getFloat(pbr["metallicFactor"], 1f))
            prefab["roughnessMinMax"] = Vector2f(0f, getFloat(pbr["roughnessFactor"], 1f))
            val metallicRoughnessMap = getInt(getMap(pbr["metallicRoughnessTexture"])["index"], -1)
            if (metallicRoughnessMap in textures.indices) {
                val tex = textures[metallicRoughnessMap]
                prefab["metallicMap"] = tex.source.getChild("b.png")
                prefab["roughnessMap"] = tex.source.getChild("r.png")
            }
            val occlusion = getMap(material["occlusionTexture"])
            val occlusionTex = textures.getOrNull(getInt(occlusion["index"], -1))
            if (occlusionTex != null) {
                prefab["occlusionMap"] = occlusionTex.source
                prefab["occlusionStrength"] = getFloat(occlusion["strength"], 1f)
            }
            val emissive = getMap(material["emissiveTexture"])
            val emissiveTex = textures.getOrNull(getInt(emissive["index"], -1))
            if (emissiveTex != null) {
                prefab["emissiveMap"] = emissiveTex.source
                val emissiveFactor = getList(material["emissiveFactor"])
                if (emissiveFactor.size == 3) {
                    prefab["emissiveBase"] = Vector3f(
                        getFloat(emissiveFactor[0]),
                        getFloat(emissiveFactor[1]),
                        getFloat(emissiveFactor[2]),
                    ).mul(BRIGHTNESS_FACTOR)
                }
            }
            val normal = getMap(material["normalTexture"])
            val normalTex = textures.getOrNull(getInt(normal["index"], -1))
            if (normalTex != null) {
                prefab["normalMap"] = normalTex.source
                prefab["normalStrength"] = getFloat(normal["scale"], 1f)
            }
            if (material["alphaMode"] == "BLEND") {
                prefab["pipelineStage"] = PipelineStage.TRANSPARENT
            }
            if (material["doubleSided"] == true) {
                prefab["cullMode"] = CullMode.BOTH
            }
            materialFolder.createPrefabChild("$name.json", prefab)
        }
    }

    // todo cameras
    // todo lights

    private fun readSamplers() {
        forEachMap("samplers", samplers) { sampler ->
            val mag = getInt(sampler["magFilter"])
            val wrap = getInt(sampler["wrapS"])
            val filter = if (mag == Filtering.LINEAR.mag) Filtering.LINEAR else Filtering.NEAREST
            val clamp = Clamping.entries.firstOrNull { it.mode == wrap } ?: Clamping.REPEAT
            Sampler(filter, clamp)
        }
    }

    private fun readTextures() {
        forEachMap("textures", textures) { texture ->
            val source = images[getInt(texture["source"])]
            val sampler = samplers[getInt(texture["sampler"])]
            Texture(source, sampler)
        }
    }

    private val floatArrayCache = HashMap<Vector2i, FloatArray?>()

    private fun loadFloatArray(accessorId: Int, numComponents: Int): FloatArray? {
        val accessor = accessors.getOrNull(accessorId) ?: return null
        val stream = accessor.view.stream()
        val numComponents1 = if (numComponents < 0) accessor.numComponents else numComponents
        return floatArrayCache.getOrPut(Vector2i(accessorId, numComponents1)) {
            val size = accessor.count * numComponents1
            when (accessor.componentType) {
                GL_FLOAT -> FloatArray(size) { stream.readLE32F() }
                else -> {
                    LOGGER.warn("Unknown type for float array: ${accessor.componentType}")
                    null
                }
            }
        }
    }

    private val intArrayCache = HashMap<Vector2i, IntArray?>()

    @Suppress("SameParameterValue")
    private fun loadIntArray(accessorId: Int, numComponents: Int): IntArray? {
        val accessor = accessors.getOrNull(accessorId) ?: return null
        val stream = accessor.view.stream()
        return intArrayCache.getOrPut(Vector2i(accessorId, numComponents)) {
            val size = accessor.count * numComponents
            when (accessor.componentType) {
                GL_UNSIGNED_INT -> IntArray(size) { stream.readLE32() }
                GL_SHORT -> IntArray(size) { stream.readLE16().shl(16).shr(16) }
                GL_UNSIGNED_SHORT -> IntArray(size) { stream.readLE16() }
                GL_BYTE -> IntArray(size) { stream.read().shl(24).shr(24) }
                GL_UNSIGNED_BYTE -> IntArray(size) { stream.read() }
                else -> {
                    LOGGER.warn("Unknown type for int array: ${accessor.componentType}")
                    null
                }
            }
        }
    }

    @Suppress("SameParameterValue")
    private fun loadColorArray(accessorId: Int): IntArray? {
        val accessor = accessors.getOrNull(accessorId) ?: return null
        val stream = accessor.view.stream()
        return when (accessor.componentType) {
            GL_UNSIGNED_BYTE -> IntArray(accessor.count) {
                rgba(stream.read(), stream.read(), stream.read(), stream.read())
            }
            else -> {
                LOGGER.warn("Unknown type for color array: ${accessor.componentType}")
                null
            }
        }
    }

    private fun readMeshes() {
        val usedNames = HashSet<String>()
        forEachMap("meshes", meshes) { mesh ->
            forEachMap(mesh["primitives"], ArrayList()) { prim ->
                val prefab = Prefab("Mesh")
                for ((name, value) in getMap(prim["attributes"])) {
                    val id = getInt(value)
                    when (name) {
                        "POSITION" -> prefab["positions"] = loadFloatArray(id, 3)
                        "NORMAL" -> prefab["normals"] = loadFloatArray(id, 3)
                        "TANGENT" -> prefab["tangents"] = loadFloatArray(id, 4)
                        "TEXCOORD_0" -> {
                            val uvs = loadFloatArray(id, 2)
                            if (uvs != null) {
                                // flip v/y-axis
                                for (i in 1 until uvs.size step 2) {
                                    uvs[i] = 1f - uvs[i]
                                }
                                prefab["uvs"] = uvs
                            }
                        }
                        "COLOR_0" -> prefab["color0"] = loadColorArray(id)
                        "COLOR_1" -> prefab["color1"] = loadColorArray(id)
                        "COLOR_2" -> prefab["color2"] = loadColorArray(id)
                        "COLOR_3" -> prefab["color3"] = loadColorArray(id)
                        "JOINTS_0" -> {
                            val indices = loadIntArray(id, 4)
                            if (indices != null) {
                                prefab["boneIndices"] = ByteArray(indices.size) { boneIdX4 ->
                                    clamp(indices[boneIdX4], 0, 255).toByte() // already is mapped properly
                                }
                            }
                        }
                        "WEIGHTS_0" -> prefab["boneWeights"] = loadFloatArray(id, 4)
                    }
                }
                prefab["indices"] = loadIntArray(getInt(prim["indices"], -1), 1)
                prefab["drawMode"] = getInt(prim["mode"], DrawMode.TRIANGLES.id)
                prefab["materials"] = materials.getOrNull(getInt(prim["material"], -1)).wrap()
                // todo how do we decide the skeleton??
                if (skins.isNotEmpty()) {
                    prefab["skeleton"] = skins.first()
                }
                // todo read targets for morph targets
                val name = nextName(prim["name"] as? String ?: "Node", usedNames)
                meshFolder.createPrefabChild("$name.json", prefab)
            }
        }
    }
}
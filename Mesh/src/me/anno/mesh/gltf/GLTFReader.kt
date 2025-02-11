package me.anno.mesh.gltf

import me.anno.ecs.components.anim.Bone
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.change.Path
import me.anno.engine.debug.DebugPoint
import me.anno.engine.debug.DebugShapes
import me.anno.gpu.buffer.DrawMode
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.io.Streams.readLE16
import me.anno.io.Streams.readLE32
import me.anno.io.Streams.readLE32F
import me.anno.io.Streams.readNBytes2
import me.anno.io.Streams.skipN
import me.anno.io.base64.Base64
import me.anno.io.files.FileReference
import me.anno.io.files.inner.InnerFolder
import me.anno.io.files.inner.temporary.InnerTmpByteFile
import me.anno.io.json.generic.JsonReader
import me.anno.maths.Maths.clamp
import me.anno.mesh.assimp.CreateSceneNode.Companion.nextName
import me.anno.mesh.gltf.GLTFConstants.BINARY_CHUNK_MAGIC
import me.anno.mesh.gltf.GLTFConstants.GL_FLOAT
import me.anno.mesh.gltf.GLTFConstants.GL_UNSIGNED_INT
import me.anno.mesh.gltf.GLTFConstants.GL_UNSIGNED_SHORT
import me.anno.mesh.gltf.GLTFConstants.JSON_CHUNK_MAGIC
import me.anno.mesh.gltf.reader.Accessor
import me.anno.mesh.gltf.reader.AnimSampler
import me.anno.mesh.gltf.reader.Animation
import me.anno.mesh.gltf.reader.BufferView
import me.anno.mesh.gltf.reader.Channel
import me.anno.mesh.gltf.reader.Node
import me.anno.mesh.gltf.reader.Texture
import me.anno.mesh.gltf.writer.Sampler
import me.anno.utils.assertions.assertTrue
import me.anno.utils.async.Callback
import me.anno.utils.async.Callback.Companion.map
import me.anno.utils.async.Callback.Companion.mapCallback
import me.anno.utils.structures.lists.Lists.createList
import me.anno.utils.types.AnyToDouble.getDouble
import me.anno.utils.types.AnyToFloat.getFloat
import me.anno.utils.types.AnyToInt.getInt
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4d
import org.joml.Matrix4f
import org.joml.Matrix4x3f
import org.joml.Quaterniond
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f
import java.io.IOException
import java.io.InputStream

/**
 * todo implement a GLTF-reader,
 *  so we can get rid of that aspect of Assimp, and have the proper materials;
 *  we also could support some of their extensions in the future
 * */
class GLTFReader(val src: FileReference) {

    companion object {
        private val LOGGER = LogManager.getLogger(GLTFWriter::class)
    }

    lateinit var json: Map<String, Any?>

    private val innerFolder = InnerFolder(src)
    private val materialFolder = InnerFolder(innerFolder, "materials")
    private val meshFolder = InnerFolder(innerFolder, "meshes")

    private val buffers = ArrayList<ByteArray>()
    private val embeddedFiles = ArrayList<Pair<String, ByteArray>>()

    private val bufferViews = ArrayList<BufferView>()
    private val accessors = ArrayList<Accessor>()
    private val materials = ArrayList<FileReference>()
    private val meshes = ArrayList<List<FileReference>>()
    private val images = ArrayList<FileReference>()
    private val samplers = ArrayList<Sampler>()
    private val textures = ArrayList<Texture>()
    private val animations = ArrayList<Animation>()
    private val skins = ArrayList<Prefab>()
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

    fun readTextGLTF(
        input: InputStream, first: Char,
        callback: Callback<InnerFolder>
    ) {
        val reader = JsonReader(input)
        reader.putBack(first)
        json = reader.readObject()

        val bufferFiles =
            getList(json["buffers"]).map { buffer0 ->
                val buffer = buffer0 as? Map<*, *> ?: emptyMap<Any?, Any?>()
                resolveUri(buffer["uri"].toString())
            }

        bufferFiles.mapCallback({ _, file, cb ->
            file.readBytes(cb)
        }, callback.map { buffers1 ->
            buffers.addAll(buffers1)
            readCommon()
            innerFolder
        })
    }

    fun readBinaryGLTF(input: InputStream, callback: Callback<InnerFolder>) {
        // confirm magic
        val charL = input.read().toChar()
        val charT = input.read().toChar()
        val charF = input.read().toChar()
        if (charL != 'l' || charT != 'T' || charF != 'F') {
            callback.err(IOException("Invalid Magic"))
            return
        }

        val version = input.readLE32()
        val fileSize = input.readLE32()
        if (version != 2) {
            LOGGER.warn("Unknown glTF version $version, trying to read it like version 2")
        }

        var remaining = fileSize - 12
        while (remaining >= 8) {
            val chunkSize = input.readLE32()
            val chunkType = input.readLE32()
            remaining -= chunkSize + 4
            if (remaining >= 0) {
                if (chunkType == JSON_CHUNK_MAGIC || chunkType == BINARY_CHUNK_MAGIC) {
                    val bytes = input.readNBytes2(chunkSize, false)
                    if (bytes.size < chunkSize) break // error
                    if (chunkType == JSON_CHUNK_MAGIC) json = JsonReader(bytes).readObject()
                    else buffers.add(bytes)
                } else input.skipN(chunkSize.toLong())
            } else break
        }

        readCommon()
        callback.ok(innerFolder)
    }

    fun readAnyGLTF(input: InputStream, callback: Callback<InnerFolder>) {
        val firstChar = input.read()
        if (firstChar == 'g'.code) readBinaryGLTF(input, callback)
        else readTextGLTF(input, firstChar.toChar(), callback)
    }

    private fun readCommon() {
        readBufferViews()
        readAccessors()
        readSamplers()
        readImages()
        readTextures()
        readMaterials()
        readMeshes()
        readNodes()
        readSkins()
        readAnimations()
        readScenes()
        writeEmbeddedFiles()
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

    private fun readNodes() {
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
                val m = Matrix4d()
                m.set(DoubleArray(16) { getDouble(matrix[it]) })
                m.transpose() // is transposed compared to JOML
                node1.translation = m.getTranslation(Vector3d())
                node1.rotation = m.getUnnormalizedRotation(Quaterniond())
                node1.scale = m.getScale(Vector3d())
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
        forEachMap("animations", animations) { anim ->
            val animation = Animation()
            forEachMap(anim["channels"], animation.channels) { chan ->
                val channel = Channel()
                channel.sampler = getInt(chan["sampler"])
                val target = getMap(chan["target"])
                channel.targetNode = getInt(target["node"])
                channel.targetPath = target["path"].toString()
                channel
            }
            forEachMap(anim["samplers"], animation.samplers) { samp ->
                val sampler = AnimSampler()
                sampler.input = getInt(samp["input"])
                sampler.output = getInt(samp["output"])
                sampler.interpolation = samp["interpolation"].toString()
                sampler
            }
            animation.name = anim["name"].toString()
            animation
        }
    }

    private fun readSkins() {
        val tmp = Matrix4f()
        val skeletons1 = InnerFolder(innerFolder, "skeletons")
        forEachMap("skins", skins) { src ->
            val prefab = Prefab("Skeleton")
            val joints = getList(src["joints"]).map { nodes[getInt(it)] }
            val inverseBindMatrixId = getInt(src["inverseBindMatrices"], -1)
            val inverseBindMatrixData = loadFloatArray(inverseBindMatrixId, 16)!!
            val inverseBindMatrices = createList(joints.size) { boneId ->
                tmp.set(inverseBindMatrixData, boneId * 16)
                Matrix4x3f().set(tmp)
            }
            val bones = joints.mapIndexed { boneId, node ->
                val bone = Bone(boneId, node.parent?.id ?: -1, node.name ?: "Bone$boneId")
                bone.setInverseBindPose(inverseBindMatrices[boneId])
                bone
            }
            // show bind-matrices for debugging
            inverseBindMatrices.forEach { m ->
                DebugShapes.debugPoints.add(DebugPoint(Vector3d(m.getTranslation(Vector3f())), -1, 1e3f))
            }
            prefab["bones"] = bones
            // todo link all animations
            if (skins.isEmpty()) {
                // todo put all of them in a folder, link to the first one
                innerFolder.createPrefabChild("Skeleton.json", prefab)
            } else {
                skeletons1.createPrefabChild("Skeleton${skins.size}.json", prefab)
            }
            prefab
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
            println("nodes for scene: $nodes")
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
            val clazz = if (node.skin in skins.indices) "AnimMeshComponent" else "MeshComponent"
            val meshes = meshes[node.mesh]
            for (i in meshes.indices) {
                val meshPath = prefab.add(path, 'c', clazz, "Mesh$i")
                prefab[meshPath, "meshFile"] = meshes[i]
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
            BufferView(buffer, offset, length)
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
            Accessor(view, compType, count, type)
            // todo implement sparse accessors, which are apparently used for morph targets
        }
    }

    private fun readMaterials() {
        forEachMap("materials", materials) { material ->
            val prefab = Prefab("Material")
            val name = (material["name"] as? String ?: "").ifBlank { "${materials.size}" }
            prefab["name"] = name
            val pbr = getMap(material["pbrMetallicRoughness"])
            val color = getMap(pbr["baseColorTexture"])
            val colorTex = textures[getInt(color["index"], -1)]
            prefab["diffuseMap"] = colorTex.source
            prefab["clamping"] = colorTex.sampler.clamping
            prefab["linearFiltering"] = colorTex.sampler.filtering == Filtering.LINEAR
            prefab["metallicMinMax"] = Vector2f(0f, getFloat(pbr["metallicFactor"], 1f))
            prefab["roughnessMinMax"] = Vector2f(0f, getFloat(pbr["roughnessFactor"], 1f))
            // todo baseColorFactor, normalTexture, occlusionTexture, emissiveTexture, emissiveFactor
            // todo metallicRoughnessTexture
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

    private fun loadFloatArray(accessorId: Int, numComponents: Int): FloatArray? {
        val accessor = accessors.getOrNull(accessorId) ?: return null
        val stream = accessor.view.stream()
        return when (accessor.componentType) {
            GL_FLOAT -> FloatArray(accessor.count * numComponents) { stream.readLE32F() }
            else -> {
                LOGGER.warn("Unknown type for float array: ${accessor.componentType}")
                null
            }
        }
    }

    @Suppress("SameParameterValue")
    private fun loadIntArray(accessorId: Int, numComponents: Int): IntArray? {
        val accessor = accessors.getOrNull(accessorId) ?: return null
        val stream = accessor.view.stream()
        return when (accessor.componentType) {
            GL_UNSIGNED_INT -> IntArray(accessor.count * numComponents) { stream.readLE32() }
            GL_UNSIGNED_SHORT -> IntArray(accessor.count * numComponents) { stream.readLE16() }
            else -> {
                LOGGER.warn("Unknown type for int array: ${accessor.componentType}")
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
                        // todo these colors are probably the incorrect type...
                        "COLOR_0" -> prefab["color0"] = loadIntArray(id, 1)
                        "COLOR_1" -> prefab["color1"] = loadIntArray(id, 1)
                        "COLOR_2" -> prefab["color2"] = loadIntArray(id, 1)
                        "COLOR_3" -> prefab["color3"] = loadIntArray(id, 1)
                        "JOINTS_0" -> {
                            val indices = loadIntArray(id, 4)
                            if (indices != null) {
                                prefab["boneIndices"] = ByteArray(indices.size) { boneId ->
                                    clamp(boneId, 0, 255).toByte()
                                }
                            }
                        }
                        "WEIGHTS_0" -> prefab["boneWeights"] = loadFloatArray(id, 4)
                    }
                }
                prefab["indices"] = loadIntArray(getInt(prim["indices"], -1), 1)
                prefab["drawMode"] = getInt(prim["mode"], DrawMode.TRIANGLES.id)
                prefab["material"] = materials[getInt(prim["material"])]
                // todo read targets for morph targets
                // todo read skins for skeletal animations
                val name = nextName(prim["name"] as? String ?: "Node", usedNames)
                meshFolder.createPrefabChild("$name.json", prefab)
            }
        }
    }
}
package me.anno.mesh.gltf

import me.anno.ecs.prefab.Prefab
import me.anno.gpu.buffer.DrawMode
import me.anno.io.Streams.readLE32
import me.anno.io.Streams.readLE32F
import me.anno.io.Streams.readNBytes2
import me.anno.io.Streams.skipN
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.inner.InnerFolder
import me.anno.io.json.generic.JsonReader
import me.anno.mesh.gltf.GLTFConstants.BINARY_CHUNK_MAGIC
import me.anno.mesh.gltf.GLTFConstants.GL_FLOAT
import me.anno.mesh.gltf.GLTFConstants.GL_UNSIGNED_INT
import me.anno.mesh.gltf.GLTFConstants.JSON_CHUNK_MAGIC
import me.anno.mesh.gltf.reader.Accessor
import me.anno.mesh.gltf.reader.BufferView
import me.anno.utils.assertions.assertTrue
import me.anno.utils.async.Callback
import me.anno.utils.async.Callback.Companion.map
import me.anno.utils.async.Callback.Companion.mapCallback
import me.anno.utils.types.AnyToFloat.getFloat
import me.anno.utils.types.AnyToInt.getInt
import org.apache.logging.log4j.LogManager
import org.joml.Vector2f
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

    val buffers = ArrayList<ByteArray>()

    private fun resolveUri(uri: String): FileReference {
        return src.getSibling(uri)
    }

    fun readTextGLTF(
        input: InputStream, first: Char,
        callback: Callback<InnerFolder>
    ) {
        val reader = JsonReader(input)
        reader.putBack(first)
        json = reader.readObject()

        val bufferFiles =
            (json["buffers"] as? List<*> ?: emptyList<Any?>()).map { buffer0 ->
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
        readImages()
        readMaterials()
        readMeshes()
        // todo create scene from meshes
    }

    private fun getMap(instance: Any?): Map<*, *> {
        return instance as? Map<*, *> ?: emptyMap<Any?, Any?>()
    }

    private inline fun <V> forEachMap(attrName: String, dst: ArrayList<V>, map: (Map<*, *>) -> V): List<V> {
        return forEachMap(json[attrName], dst, map)
    }

    private inline fun <V> forEachMap(instance: Any?, dst: ArrayList<V>, map: (Map<*, *>) -> V): List<V> {
        for (view in instance as? List<*> ?: emptyList<Any?>()) {
            dst.add(map(getMap(view)))
        }
        return dst
    }

    private val bufferViews = ArrayList<BufferView>()
    private val accessors = ArrayList<Accessor>()
    private val materials = ArrayList<FileReference>()
    private val meshes = ArrayList<List<FileReference>>()
    private val images = ArrayList<FileReference>()

    private fun readImages() {
        forEachMap("images", images) { image ->
            resolveUri(image["uri"].toString())
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
            prefab["diffuseMap"] = images.getOrNull(getInt(color["index"], -1)) ?: InvalidRef
            prefab["metallicMinMax"] = Vector2f(0f, getFloat(pbr["metallicFactor"], 1f))
            prefab["roughnessMinMax"] = Vector2f(0f, getFloat(pbr["roughnessFactor"], 1f))
            materialFolder.createPrefabChild("$name.json", prefab)
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
            else -> {
                LOGGER.warn("Unknown type for int array: ${accessor.componentType}")
                null
            }
        }
    }

    private fun readMeshes() {
        var k = 0
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
                        "COLOR_0" -> prefab["color0"] = loadIntArray(id, 1)
                        "COLOR_1" -> prefab["color1"] = loadIntArray(id, 1)
                        "COLOR_2" -> prefab["color2"] = loadIntArray(id, 1)
                        "COLOR_3" -> prefab["color3"] = loadIntArray(id, 1)
                    }
                }
                prefab["indices"] = loadIntArray(getInt(prim["indices"], -1), 1)
                prefab["drawMode"] = getInt(prim["mode"], DrawMode.TRIANGLES.id)
                prefab["material"] = materials[getInt(prim["material"])]
                // todo read targets for morph targets
                // todo read skins for skeletal animations
                meshFolder.createPrefabChild("${k++}.json", prefab)
            }
        }
    }
}
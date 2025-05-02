package me.anno.mesh.fbx

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.change.Path
import me.anno.io.Streams.consumeMagic
import me.anno.io.Streams.readLE16
import me.anno.io.Streams.readLE32
import me.anno.io.Streams.readLE64
import me.anno.io.Streams.readLE64F
import me.anno.io.Streams.readNBytes2
import me.anno.io.files.FileReference
import me.anno.io.files.inner.InnerFolder
import me.anno.utils.async.Callback
import me.anno.utils.async.Callback.Companion.map
import me.anno.utils.structures.CountingInputStream
import me.anno.utils.structures.arrays.FloatArrayList
import me.anno.utils.structures.arrays.IntArrayList
import me.anno.utils.structures.lists.Lists.castToList
import me.anno.utils.structures.lists.Lists.pop
import org.apache.logging.log4j.LogManager
import java.io.EOFException
import java.io.IOException
import java.io.InputStream

/**
 * basic reader for FBX 6000, which is unsupported by my version of Assimp.
 * */
object FBX6000 {

    private val LOGGER = LogManager.getLogger(FBX6000::class)
    fun parseBinaryFBX6000(source: InputStream): Map<String, List<Any>> {

        val stream = CountingInputStream(source)
        stream.consumeMagic("Kaydara FBX Binary")

        fun InputStream.skipN(n: Int) {
            print("// [FBX6000] ? [${stream.position}]")
            for (i in 0 until n) {
                print(" ")
                print(read())
            }
            println()
        }

        fun str(len: Int): String {
            val bytes = ByteArray(len)
            stream.readNBytes2(len, bytes, true)
            return bytes.decodeToString()
        }

        fun str() = str(stream.read())

        val stack = ArrayList<HashMap<String, ArrayList<Any>>>()
        stack.add(HashMap())

        stream.skipN(5) // unknown
        val version = stream.readLE32()
        LOGGER.debug("Version: $version")

        fun readValue(): Any {
            return when (val code = stream.read()) {
                'I'.code -> stream.readLE32()
                'D'.code -> stream.readLE64F()
                'S'.code -> str(stream.readLE32())
                'R'.code -> stream.readNBytes2(stream.readLE32(), true)
                'C'.code -> stream.read().toChar()
                'L'.code -> stream.readLE64()
                'Y'.code -> stream.readLE16() // ??
                else -> throw IOException("Unknown code ${code.toChar()}")
            }
        }

        fun read(): Boolean {
            val endOfBlock = stream.readLE32()
            val type = stream.readLE32()
            val dataLength = stream.readLE32()
            val key = str()
            if (dataLength < 0) throw EOFException()
            when (type) {
                0 -> {
                    if (key.isEmpty()) {
                        if (endOfBlock != 0) throw IllegalArgumentException()
                        return true
                    } else {
                        if (dataLength != 0) throw IllegalArgumentException()
                        val index = stack.size
                        val map = HashMap<String, ArrayList<Any>>()
                        val list = stack.last().getOrPut(key, ::ArrayList)
                        list.add(map)
                        stack.add(map)
                        var needsNewObject = false
                        while (stream.position < endOfBlock) {
                            if (needsNewObject) {
                                val map2 = HashMap<String, ArrayList<Any>>()
                                list.add(map2)
                                stack[index] = map2
                            }
                            needsNewObject = read()
                        }
                        stack.pop()
                    }
                }
                1 -> {
                    if (dataLength < 1) throw IllegalStateException()
                    val value = readValue()
                    stack.last().getOrPut(key, ::ArrayList).add(value)
                }
                else -> {
                    if (key == "Property") {
                        val value = ArrayList<Any>(type)
                        for (i in 0 until type) value.add(readValue())
                        stack.last().getOrPut(key, ::ArrayList).add(value)
                    } else {
                        val value = stack.last().getOrPut(key, ::ArrayList)
                        value.ensureCapacity(value.size + type)
                        for (i in 0 until type) value.add(readValue())
                    }
                }
            }
            return false
        }

        try {
            while (true) {
                read()
            }
        } catch (_: EOFException) {
        }

        return stack.first()
    }

    @Suppress("unchecked_cast")
    fun readBinaryFBX6000AsMeshes(source: InputStream): List<Mesh> {
        val meshes = ArrayList<Mesh>()
        val data = parseBinaryFBX6000(source)
        val objects = data["Objects"]
        if (objects != null) {
            for (oi in objects.indices) {
                val obj = objects[oi]
                obj as? Map<*, *> ?: continue
                val positions0 = obj["Vertices"] as? List<Double> ?: continue
                val normals0 = obj["Normals"] as? List<Double> ?: continue
                val indices = obj["PolygonVertexIndex"] as? List<Int> ?: continue
                val uvData = objects.subList(oi, objects.size)
                    .firstOrNull { it is Map<*, *> && it["UV"] is List<*> } as? Map<*, *>
                val uvs1 = uvData?.run { (this["UV"].castToList(Double::class)).map { it.toFloat() }.toFloatArray() }
                val uvIndices = uvData?.run { this["UVIndex"].castToList(Int::class) }
                val mesh = Mesh()
                when (val type = (obj["MappingInformationType"] as? List<*>)?.first()) {
                    "ByPolygonVertex" -> {
                        val size = indices.size * 2
                        val positions2 = FloatArrayList(size * 3)
                        val normals2 = FloatArrayList(size * 3)
                        val uvs2 = if (uvs1 != null) FloatArrayList(size * 2) else null
                        val positions1 = positions0.map { it.toFloat() }.toFloatArray()
                        val normals1 = normals0.map { it.toFloat() }.toFloatArray()
                        var i0 = 0
                        for (i in 2 until indices.size) {
                            val idx = indices[i]
                            if (idx < 0) {
                                fun pt(i: Int) {
                                    var i3 = indices[i]
                                    if (i3 < 0) i3 = -1 - i3
                                    val j3 = i * 3
                                    positions2.addAll(positions1, i3 * 3, 3)
                                    normals2.addAll(normals1, j3, 3)
                                    if (uvs1 != null) {
                                        uvs2!!
                                        val k2 = uvIndices!![i] * 2
                                        uvs2.addAll(uvs1, k2, 2)
                                    }
                                }
                                for (j in i0 + 1 until i) {
                                    pt(i0)
                                    pt(j - 1)
                                    pt(j)
                                }
                                pt(i0)
                                pt(i - 1)
                                pt(i)
                                i0 = i + 1
                            }
                        }
                        mesh.positions = positions2.toFloatArray()
                        mesh.normals = normals2.toFloatArray()
                        mesh.uvs = uvs2?.toFloatArray()
                    }
                    "ByVertex", "ByVertice" -> {
                        val size = indices.size * 2
                        val positions1 = positions0.map { it.toFloat() }.toFloatArray()
                        val normals1 = normals0.map { it.toFloat() }.toFloatArray()
                        val indices2 = IntArrayList(size)
                        var i0 = 0
                        for (i in 2 until indices.size) {
                            val idx = indices[i]
                            if (idx < 0) {
                                fun pt(i: Int) {
                                    var i3 = indices[i]
                                    if (i3 < 0) i3 = -1 - i3
                                    indices2.add(i3)
                                }
                                for (j in i0 + 1 until i) {
                                    pt(i0)
                                    pt(j - 1)
                                    pt(j)
                                }
                                pt(i0)
                                pt(i - 1)
                                pt(i)
                                i0 = i + 1
                            }
                        }
                        mesh.positions = positions1
                        mesh.normals = normals1
                        mesh.uvs = uvs1 // correct??? uv indices neglected
                        mesh.indices = indices2.toIntArray()
                    }
                    // https://banexdevblog.wordpress.com/2014/06/23/a-quick-tutorial-about-the-fbx-ascii-format/
                    else -> throw IOException("$type not yet implemented")
                }
                meshes.add(mesh)
            }
        }
        return meshes
    }

    fun readBinaryFBX6000AsFolder(file: FileReference, callback: Callback<InnerFolder>) {
        file.inputStream(callback.map { stream ->
            val meshes = readBinaryFBX6000AsMeshes(stream)
            val root = InnerFolder(file)
            val all = Prefab("Entity")
            for (i in meshes.indices) {
                val mesh = meshes[i]
                val meshPrefab = Prefab("Mesh")
                meshPrefab["positions"] = mesh.positions
                meshPrefab["indices"] = mesh.indices
                meshPrefab["normals"] = mesh.normals
                meshPrefab["uvs"] = mesh.uvs
                meshPrefab._sampleInstance = mesh
                val meshFileName = "$i.json"
                val meshFile = root.createPrefabChild(meshFileName, meshPrefab)
                val meshComp = all.add(Path.ROOT_PATH, 'c', "MeshComponent", meshFileName)
                all[meshComp, "isInstanced"] = true
                all[meshComp, "meshFile"] = meshFile
            }
            root.createPrefabChild("Scene.json", all)
            root
        })
    }
}
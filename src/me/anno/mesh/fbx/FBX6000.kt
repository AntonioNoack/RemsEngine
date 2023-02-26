package me.anno.mesh.fbx

import me.anno.ecs.components.mesh.Mesh
import me.anno.io.Streams.consumeMagic
import me.anno.io.Streams.readDoubleLE
import me.anno.io.Streams.readLE32
import me.anno.io.Streams.readLE64
import me.anno.utils.LOGGER
import me.anno.utils.structures.arrays.ExpandingFloatArray
import me.anno.utils.structures.lists.Lists.pop
import me.anno.utils.types.InputStreams.readNBytes2
import org.apache.commons.compress.utils.CountingInputStream
import java.io.EOFException
import java.io.IOException
import java.io.InputStream

object FBX6000 {

    fun parseBinaryFBX6000(source: InputStream): Map<String, List<Any>> {

        val stream = CountingInputStream(source)
        stream.consumeMagic("Kaydara FBX Binary")

        fun InputStream.skipN(n: Int) {
            print("// [FBX6000] ? [${stream.bytesRead}]")
            for (i in 0 until n) {
                print(" ")
                print(read())
            }
            println()
        }

        fun str(len: Int): String {
            val bytes = ByteArray(len)
            stream.readNBytes2(len, bytes, true)
            return String(bytes)
        }

        fun str() = str(stream.read())

        val stack = ArrayList<HashMap<String, ArrayList<Any>>>()
        stack.add(HashMap())

        stream.skipN(5) // unknown
        val version = stream.readLE32()
        LOGGER.debug("Version: $version")

        fun v(): Any {
            return when (val code = stream.read()) {
                'I'.code -> stream.readLE32()
                'D'.code -> stream.readDoubleLE()
                'S'.code -> str(stream.readLE32())
                'R'.code -> stream.readNBytes2(stream.readLE32(), true)
                'C'.code -> stream.read().toChar()
                'L'.code -> stream.readLE64()
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
                        val list = stack.last().getOrPut(key) { ArrayList() }
                        list.add(map)
                        stack.add(map)
                        var needsNewObject = false
                        while (stream.bytesRead < endOfBlock) {
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
                    val value = v()
                    stack.last().getOrPut(key) { ArrayList() }.add(value)
                }
                else -> {
                    if (key == "Property") {
                        val value = ArrayList<Any>(type)
                        for (i in 0 until type) value.add(v())
                        stack.last().getOrPut(key) { ArrayList() }.add(value)
                    } else {
                        val value = stack.last().getOrPut(key) { ArrayList() }
                        value.ensureCapacity(value.size + type)
                        for (i in 0 until type) value.add(v())
                    }
                }
            }
            return false
        }

        try {
            while (true) {
                read()
            }
        } catch (ignored: EOFException) {

        }

        return stack.first()

    }

    @Suppress("unchecked_cast")
    fun readBinaryFBX6000AsMeshes(source: InputStream): List<Mesh> {
        val meshes = ArrayList<Mesh>()
        val data = parseBinaryFBX6000(source)
        val objects = data["Objects"]
        // todo load UVs...
        if (objects != null) {
            for (obj in objects) {
                obj as? Map<*, *> ?: continue
                val positions0 = obj["Vertices"] as? List<Double> ?: continue
                val normals0 = obj["Normals"] as? List<Double> ?: continue
                val indices = obj["PolygonVertexIndex"] as? List<Int> ?: continue
                val mesh = Mesh()
                when (val type = (obj["MappingInformationType"] as? List<*>)?.first()) {
                    "ByPolygonVertex" -> {
                        val positions2 = ExpandingFloatArray(1024)
                        val normals2 = ExpandingFloatArray(1024)
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
                                    positions2.add(positions1, i3 * 3, 3)
                                    normals2.add(normals1, j3, 3)
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
                    }
                    else -> throw IOException("$type not yet implemented")
                }
                meshes.add(mesh)
            }
        }
        return meshes
    }

}
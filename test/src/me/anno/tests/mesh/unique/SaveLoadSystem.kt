package me.anno.tests.mesh.unique

import me.anno.graph.hdb.ByteSlice
import me.anno.graph.hdb.HierarchicalDatabase
import me.anno.utils.OS.documents
import org.joml.Vector3i
import java.io.ByteArrayOutputStream

class SaveLoadSystem(name: String) {

    val db = HierarchicalDatabase(
        "blocks",
        documents.getChild("RemsEngine/Tests/$name"),
        10_000_000,
        30_000L, 0L
    )

    val hash = 0L

    fun getPath(chunkId: Vector3i): List<String> {
        return listOf("${chunkId.x},${chunkId.y},${chunkId.z}")
    }

    fun get(chunkId: Vector3i, async: Boolean, callback: (HashMap<Vector3i, Byte>) -> Unit) {
        db.get(getPath(chunkId), hash, async) { slice ->
            if (slice != null) {
                slice.stream().use { stream ->
                    val answer = HashMap<Vector3i, Byte>()
                    while (true) {
                        val x = stream.read()
                        val y = stream.read()
                        val z = stream.read()
                        val b = stream.read()
                        if (b < 0) break
                        answer[Vector3i(x, y, z)] = b.toByte()
                    }
                    callback(answer)
                }
            } else callback(HashMap())
        }
    }

    fun put(chunkId: Vector3i, blocks: Map<Vector3i, Byte>) {
        val stream = ByteArrayOutputStream(blocks.size * 4)
        for ((k, v) in blocks) {
            stream.write(k.x)
            stream.write(k.y)
            stream.write(k.z)
            stream.write(v.toInt())
        }
        val bytes = stream.toByteArray()
        db.put(getPath(chunkId), hash, ByteSlice(bytes))
    }
}
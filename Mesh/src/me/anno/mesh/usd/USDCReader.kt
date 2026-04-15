package me.anno.mesh.usd

import me.anno.io.binary.ByteArrayIO.readLE32
import me.anno.io.binary.ByteArrayIO.readLE32F
import me.anno.io.binary.ByteArrayIO.readLE64
import me.anno.utils.files.Files.formatFileSize

class USDCReader(val data: ByteArray, val path: String) {

    init {
        println("Reading ${data.size.formatFileSize()} @$path")
    }

    private var pos = 0

    private lateinit var toc: Map<String, Section>
    private lateinit var tokens: Array<String>
    private lateinit var strings: Array<String>
    private lateinit var paths: Array<String>

    private val specs = ArrayList<Spec>()

    data class Section(val offset: Int, val size: Int)

    fun read(): USDPrim {
        readHeader()
        toc = readTOC()

        tokens = readStringTable("TOKENS")
        strings = readStringTable("STRINGS")
        paths = readStringTable("PATHS")

        specs.addAll(readSpecs())

        return buildStage()
    }

    private fun readHeader() {
        val magic = readString(8)
        if (magic != "PXR-USDC") error("Not USDC")

        val version = readInt()
        val flags = readInt()
        val tocOffset = readLong()
        println("version $version, toc@$tocOffset")

        pos = tocOffset.toInt()
    }

    private fun readTOC(): Map<String, Section> {

        val count = readInt()
        val map = HashMap<String, Section>()

        println("reading $count toc entries")
        repeat(count) {
            val name = readToken()
            val offset = readLong().toInt()
            val size = readInt()
            map[name] = Section(offset, size)
        }

        return map
    }

    private fun readStringTable(name: String): Array<String> {

        val sec = toc[name] ?: return emptyArray()
        pos = sec.offset

        val count = readInt()
        return Array(count) {
            val len = readInt()
            val s = String(data, pos, len)
            pos += len
            s
        }
    }

    data class Spec(
        val path: String,
        val type: String,
        val properties: MutableMap<String, Any?> = HashMap(),
        val children: MutableList<Int> = ArrayList(),
        var isInstance: Boolean = false,
        var prototype: String? = null
    )

    private fun readSpecs(): List<Spec> {

        val sec = toc["SPECS"] ?: return emptyList()
        pos = sec.offset

        val count = readInt()
        val list = ArrayList<Spec>(count)

        repeat(count) {

            val pathIndex = readInt()
            val typeIndex = readInt()

            val spec = Spec(
                path = paths[pathIndex],
                type = tokens[typeIndex]
            )

            val flags = readInt()
            val isInstance = (flags and 1) != 0
            val hasPrototype = (flags and 2) != 0

            if (isInstance) spec.isInstance = true

            if (hasPrototype) {
                val protoIndex = readInt()
                spec.prototype = paths[protoIndex]
            }

            val fieldCount = readInt()

            repeat(fieldCount) {
                val key = tokens[readInt()]
                val value = readValue()
                spec.properties[key] = value
            }

            list.add(spec)
        }

        return list
    }

    private fun readValue(): Any? {

        return when (readByte().toInt()) {

            0 -> null
            1 -> readInt()
            2 -> readFloat()
            3 -> tokens[readInt()]
            4 -> strings[readInt()]
            5 -> paths[readInt()]

            10 -> { // float array
                val n = readInt()
                FloatArray(n) { readFloat() }
            }

            11 -> { // int array
                val n = readInt()
                IntArray(n) { readInt() }
            }

            else -> error("Unknown USDC value type")
        }
    }

    private fun buildStage(): USDPrim {

        val root = USDPrim("Root", "Root")

        val byPath = HashMap<String, USDPrim>()

        // create prims first
        for (spec in specs) {
            val prim = USDPrim(spec.type, spec.path.substringAfterLast('/'))
            prim.properties.putAll(spec.properties)
            byPath[spec.path] = prim
        }

        // attach hierarchy
        for (spec in specs) {
            val parentPath = spec.path.substringBeforeLast('/', "")

            val parent = byPath[parentPath]
            val child = byPath[spec.path]

            if (parent != null && child != null) {
                parent.children.add(child)
            } else {
                root.children.add(child!!)
            }
        }

        // resolve instancing
        for (spec in specs) {
            if (spec.isInstance && spec.prototype != null) {

                val instance = byPath[spec.path]!!
                val proto = byPath[spec.prototype]

                if (proto != null) {
                    instance.children.clear()
                    instance.children.addAll(proto.children)
                }
            }
        }

        return root
    }

    private fun decompressIfNeeded(input: ByteArray): ByteArray {
        // stub
        return input
    }

    private fun readInt(): Int {
        val v = data.readLE32(pos)
        pos += 4
        return v
    }

    private fun readLong(): Long {
        val v = data.readLE64(pos)
        pos += 8
        return v
    }

    private fun readFloat(): Float {
        val v = data.readLE32F(pos)
        pos += 4
        return v
    }

    private fun readByte(): Byte = data[pos++]

    private fun readString(n: Int): String {
        if (n == 0) return ""
        val str = String(data, pos, n)
        println("str '$str'")
        pos += n
        return str
    }

    private fun readToken(): String {
        val len = readInt()
        return readString(len)
    }

}
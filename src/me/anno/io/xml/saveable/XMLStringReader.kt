package me.anno.io.xml.saveable

import me.anno.io.ISaveable
import me.anno.io.xml.generic.XMLNode
import me.anno.io.xml.generic.XMLReader
import org.apache.logging.log4j.LogManager
import java.io.ByteArrayInputStream

// todo read our XML format...
class XMLStringReader(val data: CharSequence) {

    companion object {
        private val LOGGER = LogManager.getLogger(XMLStringReader::class)
    }

    val children = ArrayList<ISaveable>()

    init {
        val data1 = XMLReader().read(ByteArrayInputStream(data.toString().encodeToByteArray())) as XMLNode
        for (child in data1.children) {
            child as? XMLNode ?: continue
            val instance = readInstance(child) ?: continue
            children.add(instance)
        }
    }

    fun readInstance(node: XMLNode): ISaveable? {
        val instance = ISaveable.createOrNull(node.type)
        if (instance != null) {
            for ((name, typeValue) in node.attributes) {
                val i0 = typeValue.indexOf(':')
                if (i0 > 0) {
                    val type = typeValue.substring(0, i0)
                    val value = typeValue.substring(i0 + 1)
                    readValue(instance, name, type, value)
                }
            }
            // todo read all children?...
        } else LOGGER.warn("Unknown type ${node.type}")
        return instance
    }


    private fun <ArrayType> a1(
        value: String,
        separatorSymbol: Char,
        createArray: (Int) -> ArrayType,
        readValue: (vs: ArrayType, i: Int, v: String) -> Unit
    ): ArrayType {
        val comma = value.indexOf(separatorSymbol)
        val size = value.substring(0, comma).toInt()
        val array = createArray(size)
        var pi = comma + 1
        for (i in 0 until size) {
            val pj = value.indexOf(separatorSymbol, pi)
            if (pi >= pj) break
            readValue(array, i, value.substring(pi, pj))
            pi = pj + 1
        }
        return array
    }

    private fun <ArrayType> a1(
        value: String,
        createArray: (Int) -> ArrayType,
        readValue: (vs: ArrayType, i: Int, v: String) -> Unit
    ): ArrayType {
        return a1(value, ',', createArray, readValue)
    }

    private inline fun <reified ArrayType> a2(
        value: String,
        noinline createArray: (Int) -> ArrayType,
        noinline readValue: (vs: ArrayType, i: Int, v: String) -> Unit
    ): Array<ArrayType> {
        val empty = createArray(0)
        return a1(value, ';', { Array(it) { empty } }, { vs, i, v ->
            vs[i] = a1(v, ',', createArray, readValue)
        })
    }

    private fun readValue(instance: ISaveable, name: String, type: String, value: String) {
        when (type) {
            // todo implement all types
            "b" -> instance.readBoolean(name, value.equals("true", true) || value.equals("1", true))
            "i" -> instance.readInt(name, value.toInt())
            "i[]" -> instance.readIntArray(
                name, a1(value, { IntArray(it) }, { vs, i, v -> vs[i] = v.toInt() })
            )
            "l" -> instance.readLong(name, value.toLong())
            "l[]" -> instance.readLongArray(
                name, a1(value, { LongArray(it) }, { vs, i, v -> vs[i] = v.toLong() })
            )
            "f" -> instance.readFloat(name, value.toFloat())
            "f[]" -> instance.readFloatArray(
                name, a1(value, { FloatArray(it) }, { vs, i, v -> vs[i] = v.toFloat() })
            )
            "d" -> instance.readDouble(name, value.toDouble())
            "d[]" -> instance.readDoubleArray(
                name, a1(value, { DoubleArray(it) }, { vs, i, v -> vs[i] = v.toDouble() })
            )
            "S" -> instance.readString(name, value)
            "S[]" -> instance.readStringArray(
                name, a1(value, { Array(it) { "" } }, { vs, i, v -> vs[i] = v })
            ) // todo unescape commas
            else -> LOGGER.warn("Unknown type {}", type)
        }
    }
}
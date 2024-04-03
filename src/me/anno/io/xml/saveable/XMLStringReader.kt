package me.anno.io.xml.saveable

import me.anno.io.Saveable
import me.anno.io.json.saveable.SimpleType
import me.anno.io.xml.generic.XMLNode
import me.anno.io.xml.generic.XMLReader
import me.anno.utils.structures.lists.Lists.createArrayList
import me.anno.utils.types.Strings.indexOf2
import org.apache.logging.log4j.LogManager
import java.io.ByteArrayInputStream

// todo read our XML format...
class XMLStringReader(val data: CharSequence) {

    companion object {

        private val LOGGER = LogManager.getLogger(XMLStringReader::class)
        private val registry = HashMap<String, (String) -> Any?>()

        fun register(type: String, reader: (String) -> Any?) {
            registry[type] = reader
        }

        private fun <Type, ArrayType> register1(
            type: SimpleType,
            reader: (String) -> Type,
            createArray: (size: Int) -> ArrayType,
            insert: (array: ArrayType, index: Int, value: Type) -> Unit
        ) {
            register(type.scalar, reader)
            register(type.array) {
                a1(it, createArray) { vs, i, v ->
                    insert(vs, i, reader(v))
                }
            }
            register(type.array2d) {
                a2(it, createArray) { vs, i, v ->
                    insert(vs, i, reader(v))
                }
            }
        }

        init {
            register1(SimpleType.BOOLEAN, {
                it.equals("true", true) || it == "1"
            }, { BooleanArray(it) }, { vs, i, v -> vs[i] = v })
            register1(SimpleType.BYTE, String::toByte, { ByteArray(it) }, { vs, i, v -> vs[i] = v })
            register1(SimpleType.SHORT, String::toShort, { ShortArray(it) }, { vs, i, v -> vs[i] = v })
            register1(SimpleType.INT, String::toInt, { IntArray(it) }, { vs, i, v -> vs[i] = v })
            register1(SimpleType.LONG, String::toLong, { LongArray(it) }, { vs, i, v -> vs[i] = v })
            register1(SimpleType.FLOAT, String::toFloat, { FloatArray(it) }, { vs, i, v -> vs[i] = v })
            register1(SimpleType.DOUBLE, String::toDouble, { DoubleArray(it) }, { vs, i, v -> vs[i] = v })
        }

        private fun <ArrayType> a1(
            value: String,
            separatorSymbol: Char,
            createArray: (Int) -> ArrayType,
            readValue: (vs: ArrayType, i: Int, v: String) -> Unit
        ): ArrayType {
            val sepIndex = value.indexOf(separatorSymbol)
            val size = value.substring(0, sepIndex).toInt()
            val array = createArray(size)
            var pi = sepIndex + 1
            for (i in 0 until size) {
                if (pi >= value.length) {
                    break
                }
                val pj = value.indexOf2(separatorSymbol, pi)
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

        private fun <ArrayType> a2(
            value: String,
            createArray: (Int) -> ArrayType,
            readValue: (vs: ArrayType, i: Int, v: String) -> Unit
        ): ArrayList<ArrayType> {
            val empty = createArray(0)
            return a1(value, ';', { createArrayList(it, empty) }, { vs, i, v ->
                vs[i] = a1(v, ',', createArray, readValue)
            })
        }
    }

    val children = ArrayList<Saveable>()

    init {
        val data1 = XMLReader().read(ByteArrayInputStream(data.toString().encodeToByteArray())) as XMLNode
        for (child in data1.children) {
            child as? XMLNode ?: continue
            val instance = readInstance(child) ?: continue
            children.add(instance)
        }
    }

    fun readInstance(node: XMLNode): Saveable? {
        val instance = Saveable.createOrNull(node.type)
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

    private fun readValue(instance: Saveable, name: String, type: String, value: String) {
        val reader = registry[type]
        if (reader != null) {
            instance.setProperty(name, reader(value))
        } else when (type) {
            // todo implement all types
            SimpleType.STRING.scalar -> instance.setProperty(name, value)
            SimpleType.STRING.array -> instance.setProperty(
                name, a1(value, { Array(it) { "" } }, { vs, i, v -> vs[i] = v })
            ) // todo unescape commas
            else -> LOGGER.warn("Unknown type {}", type)
        }
    }
}
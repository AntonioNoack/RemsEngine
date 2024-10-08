package me.anno.engine.projects

import me.anno.engine.EngineBase
import me.anno.engine.projects.GameEngineProject.Companion.encoding
import me.anno.io.Streams.consumeMagic
import me.anno.io.Streams.writeString
import me.anno.io.binary.BinaryReader
import me.anno.io.binary.BinaryWriter
import me.anno.io.files.FileReference
import me.anno.io.json.generic.JsonFormatter
import me.anno.io.json.generic.JsonReader
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.io.saveable.Saveable
import me.anno.io.xml.generic.XMLNode
import me.anno.io.xml.generic.XMLReader
import me.anno.io.xml.generic.XMLWriter
import me.anno.io.xml.saveable.XML2JSON
import me.anno.io.yaml.generic.YAMLReader
import me.anno.io.yaml.saveable.YAML2JSON
import me.anno.utils.assertions.assertEquals
import java.io.ByteArrayOutputStream
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterInputStream

enum class FileEncoding(val id: Int) {
    PRETTY_JSON(0),
    COMPACT_JSON(1),
    YAML(2),
    PRETTY_XML(3),
    COMPACT_XML(4),
    BINARY(5);

    companion object {
        const val BINARY_MAGIC = "RemsEngineZZ"
        const val MAIN_NODE_NAME = "RemsEngine"
    }

    val extension: String
        get() = when (encoding) {
            PRETTY_JSON, COMPACT_JSON -> "json"
            PRETTY_XML, COMPACT_XML -> "xml"
            YAML -> "yaml"
            BINARY -> "rem"
        }

    val isPretty: Boolean
        get() = when (this) {
            PRETTY_JSON, PRETTY_XML, YAML -> true
            else -> false
        }

    fun getForExtension(file: FileReference): FileEncoding {
        return getForExtension(file.lcExtension)
    }

    fun getForExtension(lcExtension: String): FileEncoding {
        return when (lcExtension) {
            "json" -> if (isPretty) PRETTY_JSON else COMPACT_JSON
            "xml" -> if (isPretty) PRETTY_XML else COMPACT_XML
            "yaml" -> YAML
            "bin" -> BINARY
            else -> this
        }
    }

    fun encode(value: Saveable, workspace: FileReference): ByteArray {
        return encode(listOf(value), workspace)
    }

    private fun decodeJsonLike(jsonLike: Any?, safely: Boolean): List<Saveable> {
        val root = ((jsonLike as? Map<*, *>)?.get(MAIN_NODE_NAME) ?: jsonLike) as List<*>
        val json = JsonFormatter.format(root, "", Int.MAX_VALUE)
        return JsonStringReader.read(json, EngineBase.workspace, safely)
    }

    fun decode(bytes: ByteArray, workspace: FileReference, safely: Boolean): List<Saveable> {
        when (this) {
            PRETTY_JSON, COMPACT_JSON -> {
                return JsonStringReader.read(bytes.inputStream(), workspace, safely)
            }
            YAML -> {
                val reader = bytes.inputStream().bufferedReader()
                val node = YAMLReader.parseYAML(reader, beautify = false)
                val jsonLike = YAML2JSON.fromYAML(node)
                return decodeJsonLike(jsonLike, safely)
            }
            COMPACT_XML, PRETTY_XML -> {
                val str = bytes.inputStream()
                val node = XMLReader().read(str) as XMLNode
                assertEquals(MAIN_NODE_NAME, node.type)
                val jsonLike = XML2JSON.fromXML(node)
                return decodeJsonLike(jsonLike, safely)
            }
            else -> {
                val str = bytes.inputStream()
                str.consumeMagic(FileEncoding.BINARY_MAGIC)
                val zis = InflaterInputStream(str)
                val reader = BinaryReader(zis)
                reader.readAllInList()
                return reader.allInstances
            }
        }
    }

    fun encode(
        values: List<Saveable>, workspace: FileReference,
        resourceMap: Map<FileReference, FileReference> = emptyMap()
    ): ByteArray {
        val encoding = this
        return if (encoding != BINARY) {
            val json = JsonStringWriter.toText(values, workspace, resourceMap)
            when (encoding) {
                PRETTY_JSON -> JsonFormatter.format(json)
                COMPACT_JSON -> json
                YAML -> {
                    val jsonNode = JsonReader(json).readArray()
                    val yamlNode = YAML2JSON.toYAML(MAIN_NODE_NAME, jsonNode, 0)
                    yamlNode.toString()
                }
                COMPACT_XML, PRETTY_XML -> {
                    val jsonNode = JsonReader(json).readArray()
                    val xmlNode = XML2JSON.toXML(MAIN_NODE_NAME, jsonNode)
                    val indentation = if (this == PRETTY_XML) "  " else null
                    XMLWriter.write(xmlNode, indentation, true)
                }
                else -> ""
            }.encodeToByteArray()
        } else {
            ByteArrayOutputStream().use { bos ->
                bos.writeString(BINARY_MAGIC) // magic
                DeflaterOutputStream(bos).use { zos ->
                    val writer = BinaryWriter(zos, workspace)
                    writer.resourceMap = resourceMap
                    for (value in values) {
                        writer.add(value)
                    }
                    writer.writeAllInList()
                    writer.close()
                }
                bos.close()
                bos.toByteArray()
            }
        }
    }
}
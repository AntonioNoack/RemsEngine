package me.anno.engine.projects

import me.anno.engine.projects.GameEngineProject.Companion.encoding
import me.anno.io.Streams.consumeMagic
import me.anno.io.Streams.writeString
import me.anno.io.binary.BinaryReader
import me.anno.io.binary.BinaryWriter
import me.anno.io.files.FileReference
import me.anno.io.json.generic.JsonFormatter
import me.anno.io.json.generic.JsonLike.decodeJsonLike
import me.anno.io.json.generic.JsonLike.jsonToXML
import me.anno.io.json.generic.JsonLike.jsonToYAML
import me.anno.io.json.generic.JsonLike.xmlBytesToJsonLike
import me.anno.io.json.generic.JsonLike.yamlBytesToJsonLike
import me.anno.io.json.saveable.JsonStringReader
import me.anno.io.json.saveable.JsonStringWriter
import me.anno.io.saveable.Saveable
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

    fun decode(bytes: ByteArray, workspace: FileReference, safely: Boolean): List<Saveable> {
        when (this) {
            COMPACT_JSON, PRETTY_JSON -> {
                return JsonStringReader.read(bytes.inputStream(), workspace, safely)
            }
            COMPACT_XML, PRETTY_XML -> {
                val jsonLike = xmlBytesToJsonLike(bytes)
                return decodeJsonLike(jsonLike, safely)
            }
            YAML -> {
                val jsonLike = yamlBytesToJsonLike(bytes)
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

    fun encode(value: Saveable, workspace: FileReference): ByteArray {
        return encode(listOf(value), workspace)
    }

    fun encode(
        values: List<Saveable>, workspace: FileReference,
        resourceMap: Map<FileReference, FileReference> = emptyMap()
    ): ByteArray {
        val encoding = this
        return if (encoding != BINARY) {
            val json = JsonStringWriter.toText(values, workspace, resourceMap)
            println("json: $json")
            when (encoding) {
                COMPACT_JSON -> json
                PRETTY_JSON -> JsonFormatter.format(json)
                COMPACT_XML -> jsonToXML(json, false)
                PRETTY_XML -> jsonToXML(json, true)
                YAML -> jsonToYAML(json)
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
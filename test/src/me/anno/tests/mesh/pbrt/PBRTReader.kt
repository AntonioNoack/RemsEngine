package me.anno.tests.mesh.pbrt

import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.change.Path
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.files.ReadLineIterator
import me.anno.io.files.inner.InnerFolder
import me.anno.utils.async.Callback
import me.anno.utils.types.Strings.indexOf2
import me.anno.utils.types.Strings.isBlank
import me.anno.utils.types.Strings.isNotBlank2
import org.apache.logging.log4j.LogManager
import org.joml.Matrix4d
import org.joml.Quaterniond
import org.joml.Vector3d

/**
 * PBRT is a text-based scene-file-format that uses relative paths to neighboring files
 * */
object PBRTReader {

    private val LOGGER = LogManager.getLogger(PBRTReader::class)

    fun readAsFolder(file: FileReference, callback: Callback<InnerFolder>) {
        file.readLines(256) { lines, err ->
            if (lines != null) {
                callback.ok(readAsFolder(file, file.getParent(), lines))
            } else callback.err(err)
        }
    }

    private fun readAsFolder(file: FileReference, resources: FileReference, lines: ReadLineIterator): InnerFolder {
        val root = InnerFolder(file)
        val scene = Prefab("Entity")

        val stack = ArrayList<Path>()
        stack.add(Path.ROOT_PATH)

        var nextName = ""
        var ctr = 0

        fun genName(): String {
            return "_${ctr++}"
        }

        while (true) {
            val line = lines.readLineRaw() ?: break

            var i = 0
            fun readString(): String {
                i = line.skipSpaces(i)
                return if (i >= line.length) ""
                else if (line[i] == '"') {
                    val ei = line.indexOf2('"', i + 1)
                    val str = line.substring(i + 1, ei)
                    i = ei + 1
                    str
                } else if (i + 1 < line.length && line[i] == '#' && line[i + 1] == ' ') {
                    val hi = line.skipSpaces(i + 1)
                    val ei = line.indexOf2(' ', hi)
                    val str = "#" + line.substring(hi, ei)
                    i = ei
                    str
                } else {
                    val ei = line.indexOf2(' ', i + 1)
                    val str = line.substring(i, ei)
                    i = ei
                    str
                }
            }

            when (val type = readString()) {
                "#Name" -> {
                    nextName = readString()
                }
                "AttributeBegin" -> {
                    val curr = stack.last()
                    val next = scene.add(
                        curr, 'e', "Entity",
                        nextName.ifBlank(::genName)
                    )
                    stack.add(next)
                    nextName = ""
                }
                "AttributeEnd" -> {
                    stack.removeLast()
                }
                "Transform" -> {
                    // Transform [ 1.260539 0 -0.764475 0 0 1.474239 0 0 0.764475 0 1.260539 0 1234.6528 0.323862 435.2381 1  ]
                    val i2 = line.indexOf2('[', i + 1)
                    val i3 = line.indexOf2(']', i2 + 1)
                    if (i3 in i2 + 16 until line.length) {
                        val values = line.substring(i2 + 1, i3)
                            .split(' ').mapNotNull { it.toDoubleOrNull() }
                        if (values.size == 16) {
                            val m4x4 = Matrix4d()
                                .set(values.toDoubleArray())
                                .transpose()
                            val curr = stack.last()
                            scene[curr, "position"] = m4x4.getTranslation(Vector3d())
                            scene[curr, "rotation"] = m4x4.getUnnormalizedRotation(Quaterniond())
                            scene[curr, "scale"] = m4x4.getScale(Vector3d())
                        } else LOGGER.warn("Expected exactly 16 values in Transform")
                    } else LOGGER.warn("Expected float-list in Transform")
                }
                "Shape" -> {
                    // the first string seems to be the file type:
                    // "trianglemesh" has more data on the following lines
                    // "plymesh" is a .ply mesh file
                    var source = ""
                    while (true) {
                        val str = readString()
                        if (str.isEmpty()) break
                        if (str == "string filename") {
                            source = readString()
                            break
                        }
                    }
                    if (source.isNotBlank2()) {
                        // find referenced file
                        val sourceI = findFile(source, resources)
                        if (sourceI != InvalidRef) {
                            val path = scene.add(stack.last(), 'c', "MeshComponent", genName())
                            scene[path, "meshFile"] = sourceI
                        } else LOGGER.warn("Missing $source in $resources")
                    }
                }
                else -> LOGGER.warn("Unknown Statement: $type")
            }
        }

        root.createPrefabChild("Scene.json", scene)
        return root
    }

    /**
     * file paths are absolute to the project root, so we have to either find that, or go back up until we find it
     * */
    fun findFile(source0: String, resources: FileReference): FileReference {
        var source = source0
        while (true) {
            val sourceI = resources.getChildUnsafe(source, false)
            if (sourceI.exists) return sourceI
            val idx = source.indexOf('/')
            if (idx >= 0) {
                source = source.substring(idx + 1)
            } else return InvalidRef
        }
    }

    fun CharSequence.skipSpaces(i0: Int = 0): Int {
        var i = i0
        while (i < length && this[i].isBlank()) {
            i++
        }
        return i
    }
}
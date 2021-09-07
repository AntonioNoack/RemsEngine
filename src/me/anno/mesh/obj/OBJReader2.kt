package me.anno.mesh.obj

import me.anno.ecs.prefab.CAdd
import me.anno.ecs.prefab.CSet
import me.anno.ecs.prefab.Path
import me.anno.ecs.prefab.Prefab
import me.anno.fonts.mesh.Triangulation
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.zip.InnerFolder
import me.anno.mesh.Point
import me.anno.utils.files.Files.findNextFileName
import me.anno.utils.structures.arrays.ExpandingFloatArray
import me.anno.utils.structures.arrays.ExpandingIntArray
import org.apache.logging.log4j.LogManager
import org.joml.Vector2f
import org.joml.Vector3f
import java.io.EOFException
import java.io.IOException
import java.io.InputStream

// in a really heavy scene (San Miguel with 10M vertices),
// with unpacking it from a zip file,
// this was 3x faster than assimp: 13s instead of 40s
// without unzipping, it still was 10s for this, and 34s for assimp
// (yes, when using assimp, I am copying everything, which is non optimal)
class OBJReader2(input: InputStream, val file: FileReference) : OBJMTLReader(input) {

    constructor(file: FileReference) : this(file.inputStream(), file)

    companion object {
        private val LOGGER = LogManager.getLogger(OBJReader2::class)
    }

    val folder = InnerFolder(file)
    val materialsFolder = InnerFolder(folder, "materials")
    val meshesFolder = InnerFolder(folder, "meshes")

    val prefab = Prefab("Entity")

    // inside the function, because not used materials aren't relevant to us
    val materials = HashMap<String, FileReference>()
    val defaultSize = 256

    private var lastMaterial: FileReference = InvalidRef
    private val positions = ExpandingFloatArray(defaultSize)
    private val normals = ExpandingFloatArray(defaultSize)
    private val uvs = ExpandingFloatArray(defaultSize)

    private val facePositions = ExpandingFloatArray(defaultSize)
    private val faceNormals = ExpandingFloatArray(defaultSize)
    private val faceUVs = ExpandingFloatArray(defaultSize)

    private val points = ExpandingIntArray(256)

    private fun putPoint(p: Point) {
        facePositions += p.position
        faceNormals += p.normal
        faceUVs += p.uv!!
    }

    private fun putPoint(index: Int) {
        val vertex = points[index]
        val normal = points[index + 1]
        val uv = points[index + 2]
        facePositions += positions[vertex]
        facePositions += positions[vertex + 1]
        facePositions += positions[vertex + 2]
        if (normal >= 0) {
            faceNormals += normals[normal]
            faceNormals += normals[normal + 1]
            faceNormals += normals[normal + 2]
        } else {
            faceNormals += 0f
            faceNormals += 0f
            faceNormals += 0f
        }
        if (uv >= 0) {
            faceUVs += uvs[uv]
            faceUVs += uvs[uv + 1]
        } else {
            faceUVs += 0f
            faceUVs += 0f
        }
    }

    private fun putLinePoint(index: Int) {
        val vertex = points[index]
        facePositions += positions[vertex]
        facePositions += positions[vertex + 1]
        facePositions += positions[vertex + 2]
        faceNormals += 0f
        faceNormals += 0f
        faceNormals += 0f
        faceUVs += 0f
        faceUVs += 0f
    }

    private var lastObjectName = ""
    private var lastGroupName = ""

    private var lastGroupPath: Path = Path.ROOT_PATH
    private var lastObjectPath: Path = Path.ROOT_PATH
    private var groupCountInScene = 0
    private var objectCountInGroup = 0
    private var meshCountInObject = 0

    fun newGroup() {
        lastGroupPath = prefab.add(
            CAdd(Path.ROOT_PATH, 'e', "Entity", lastGroupName),
            groupCountInScene++
        )
        objectCountInGroup = 0
        newObject()
    }

    fun newObject() {
        if (lastGroupName.isNotEmpty() && lastGroupPath.isEmpty()) {
            newGroup()
        }
        lastObjectPath = prefab.add(
            CAdd(lastGroupPath, 'e', "Entity", lastObjectName),
            objectCountInGroup++
        )
        meshCountInObject = 0
    }

    fun finishMesh() {
        if (facePositions.size > 0) {
            if (lastObjectPath.isEmpty()) newObject()
            val mesh = Prefab("Mesh")
            var name = "$lastObjectName.json"
            mesh.setProperty("material", lastMaterial)
            mesh.setProperty("positions", facePositions.toFloatArray())
            mesh.setProperty("normals", faceNormals.toFloatArray())
            mesh.setProperty("uvs", faceUVs.toFloatArray())
            // find good new name for mesh
            if (meshesFolder.getChild(name) != InvalidRef) {
                name = "$lastObjectName-${lastMaterial.name}"
                val fileI = meshesFolder.getChild(name)
                if (fileI != InvalidRef) {
                    name = findNextFileName(fileI, 1, '-', 1)
                }
            }
            // add mesh component to last object
            val meshRef = meshesFolder.createPrefabChild(name, mesh)
            val add = CAdd(lastObjectPath, 'c', "MeshComponent", meshRef.nameWithoutExtension)
            prefab.add(add)
            prefab.add(CSet(add.getChildPath(meshCountInObject++), "mesh", meshRef))
            facePositions.clear()
            faceNormals.clear()
            faceUVs.clear()
        }
    }

    fun finishGroup() {
        finishMesh()
        lastObjectPath = Path.ROOT_PATH
        lastGroupPath = Path.ROOT_PATH
    }

    fun finishObject() {
        finishMesh()
        lastObjectPath = Path.ROOT_PATH
    }

    init {
        try {

            while (true) {
                // read the line
                skipSpaces()
                when (val char0 = nextChar()) {
                    '#' -> skipLine()
                    'o' -> {
                        // new object begins
                        skipSpaces()
                        finishObject()
                        lastObjectName = readUntilSpace()
                        skipLine()
                    }
                    'g' -> {
                        // new group begins
                        finishGroup()
                        skipSpaces()
                        lastGroupName = readUntilSpace()
                    }
                    'u' -> {
                        if (nextChar() == 's' && nextChar() == 'e' && nextChar() == 'm' && nextChar() == 't' && nextChar() == 'l') {
                            skipSpaces()
                            finishMesh()
                            lastMaterial = materials[readUntilSpace()] ?: InvalidRef
                        } else skipLine()
                    }
                    'v' -> {
                        when (nextChar()) {
                            ' ', '\t' -> {
                                skipSpaces()
                                positions += readFloat()
                                skipSpaces()
                                positions += readFloat()
                                skipSpaces()
                                positions += readFloat()
                                skipLine()
                            }
                            't' -> {
                                skipSpaces()
                                uvs += readFloat()
                                skipSpaces()
                                uvs += readFloat()
                                skipLine()
                            }
                            'n' -> {
                                skipSpaces()
                                normals += readFloat()
                                skipSpaces()
                                normals += readFloat()
                                skipSpaces()
                                normals += readFloat()
                                skipLine()
                            }
                            else -> skipLine()
                        }
                    }
                    's' -> {
                        // smoothness -> ignore?
                        // at least Blender sets smoothness by normals
                        // what would it be otherwise???...
                        skipLine()
                    }
                    'm' -> {
                        // mtllib
                        if (nextChar() == 't' && nextChar() == 'l' && nextChar() == 'l' && nextChar() == 'i' && nextChar() == 'b') {
                            val file2 = readFile(file)
                            if (file2.exists && !file2.isDirectory) {
                                try {
                                    val folder = MTLReader2.readAsFolder(file2, materialsFolder)
                                    val subMaterials = folder.listChildren()!!
                                    materials.putAll(subMaterials.map {
                                        it.nameWithoutExtension to it
                                    })
                                } catch (e: IOException) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
                    'l' -> {

                        points.clear()

                        pts@ while (true) {
                            when (val next = next()) {
                                ' '.code, '\t'.code -> {
                                }
                                '\n'.code -> break@pts
                                else -> {
                                    putBack(next)
                                    points += readIndex() * 3
                                }
                            }
                        }

                        for (i in 1 until points.size) {
                            putLinePoint(i - 1)
                            putLinePoint(i)
                            putLinePoint(i) // degenerate triangle
                        }

                    }
                    'f' -> {

                        points.clear()

                        pts@ while (true) {
                            when (val next = nextChar()) {
                                ' ', '\t' -> {
                                }
                                '\n' -> break@pts
                                else -> {
                                    putBack(next)
                                    val vertexIndex0 = readInt()
                                    val vertexIndex = (vertexIndex0 - 1)
                                    var uvIndex = -1
                                    var normalIndex = -1
                                    if (putBack == '/'.code) {
                                        nextChar()
                                        uvIndex = (readInt() - 1)
                                        if (putBack == '/'.code) {
                                            nextChar()
                                            normalIndex = (readInt() - 1)
                                        }
                                    }
                                    points += vertexIndex * 3
                                    points += normalIndex * 3
                                    points += uvIndex * 2
                                }
                            }
                        }

                        when (points.size / 3) {
                            in 0..1 -> {
                            }
                            2 -> {
                                // a line...
                                putPoint(0)
                                putPoint(3)
                                putPoint(3)
                            }
                            3 -> {
                                putPoint(0)
                                putPoint(3)
                                putPoint(6)
                            }
                            4 -> {
                                putPoint(0)
                                putPoint(3)
                                putPoint(6)
                                putPoint(6)
                                putPoint(9)
                                putPoint(0)
                            }
                            else -> {
                                // triangulate the points correctly
                                // currently is the most expensive step, because of so much allocations:
                                // points, the array, the return list, ...
                                val points2 = Array(points.size / 3) {
                                    val vi = points[it * 3]
                                    val ni = points[it * 3 + 1]
                                    val ui = points[it * 3 + 2]
                                    Point(
                                        Vector3f(
                                            positions[vi],
                                            positions[vi + 1],
                                            positions[vi + 2]
                                        ), Vector3f(
                                            normals[ni],
                                            normals[ni + 1],
                                            normals[ni + 2]
                                        ), Vector2f(
                                            uvs[ui],
                                            uvs[ui + 1]
                                        )
                                    )
                                }
                                val triangles = Triangulation.ringToTrianglesPoint(points2)
                                for (i in triangles.indices step 3) {
                                    putPoint(triangles[i])
                                    putPoint(triangles[i + 1])
                                    putPoint(triangles[i + 2])
                                }
                            }
                        }
                    }
                    else -> {
                        putBack(char0)
                        LOGGER.warn("Unknown obj tag ${readUntilSpace()}")
                        skipLine()
                    }
                }
            }
        } catch (e: EOFException) {
        }
        finishObject()
        reader.close()
    }

}
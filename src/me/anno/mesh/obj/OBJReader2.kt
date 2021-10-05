package me.anno.mesh.obj

import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.change.CAdd
import me.anno.ecs.prefab.change.Path
import me.anno.fonts.mesh.Triangulation
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.zip.InnerFolder
import me.anno.mesh.Point
import me.anno.utils.files.Files.findNextFileName
import me.anno.utils.maths.Maths.clamp
import me.anno.utils.maths.Maths.pow
import me.anno.utils.structures.arrays.ExpandingFloatArray
import me.anno.utils.structures.arrays.ExpandingIntArray
import org.apache.logging.log4j.LogManager
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

    val folder = InnerFolder(file)
    val materialsFolder = lazy { InnerFolder(folder, "materials") }
    val meshesFolder = lazy { InnerFolder(folder, "meshes") }

    val scenePrefab = Prefab("Entity")

    init {
        folder.createPrefabChild("Scene.json", scenePrefab)
    }

    // inside the function, because not used materials aren't relevant to us
    private val materials = HashMap<String, FileReference>()

    // guess to the sizes to allocate, so in the best case, we allocate a little more,
    // but don't allocate twice for no reason
    private val defaultSize1 = clamp(file.length() / 64, 64, 1 shl 20).toInt()
    private val defaultSize2 = clamp(pow(file.length().toDouble(), 0.66).toInt(), 64, 1 shl 20)

    private var lastMaterial: FileReference = InvalidRef
    private val positions = ExpandingFloatArray(3 * defaultSize1)
    private val normals = ExpandingFloatArray(3 * defaultSize1)
    private val uvs = ExpandingFloatArray(2 * defaultSize1)

    private val facePositions = ExpandingFloatArray(3 * defaultSize2)
    private val faceNormals = ExpandingFloatArray(3 * defaultSize2)
    private val faceUVs = ExpandingFloatArray(2 * defaultSize2)

    private val points = ExpandingIntArray(256)

    fun printSizes() {
        LOGGER.info(
            "positions: ${positions.size}, normals: ${normals.size}, uvs: ${uvs.size}, " +
                    "facePositions: ${facePositions.capacity}, faceNormals: ${faceNormals.capacity}, faceUVs: ${faceUVs.capacity}, " +
                    "points: ${points.size}, defaultSize: $defaultSize1/$defaultSize2"
        )
    }

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

    private fun putLinePoint(vertex: Int) {
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

    private fun newGroup() {
        lastGroupPath = scenePrefab.add(
            CAdd(Path.ROOT_PATH, 'e', "Entity", lastGroupName),
            groupCountInScene++
        )
        objectCountInGroup = 0
        newObject()
    }

    private fun newObject() {
        if (lastGroupName.isNotEmpty() && lastGroupPath.isEmpty()) {
            newGroup()
        }
        lastObjectPath = scenePrefab.add(
            CAdd(lastGroupPath, 'e', "Entity", lastObjectName),
            objectCountInGroup++
        )
        meshCountInObject = 0
    }

    private fun finishMesh() {
        if (facePositions.size > 0) {
            if (lastObjectPath.isEmpty()) newObject()
            val mesh = Prefab("Mesh")
            var name = "$lastObjectName.json"
            mesh.setProperty("material", lastMaterial)
            mesh.setProperty("positions", facePositions.toFloatArray())
            mesh.setProperty("normals", faceNormals.toFloatArray())
            mesh.setProperty("uvs", faceUVs.toFloatArray())
            val meshesFolder = meshesFolder.value
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
            mesh.source = meshRef
            val add =
                scenePrefab.add(lastObjectPath, 'c', "MeshComponent", meshRef.nameWithoutExtension, meshCountInObject++)
            scenePrefab.set(add, "mesh", meshRef)
            // LOGGER.debug("Clearing at ${facePositions.size / 3}")
            facePositions.clear()
            faceNormals.clear()
            faceUVs.clear()
        }
    }

    private fun finishGroup() {
        finishMesh()
        lastObjectPath = Path.ROOT_PATH
        lastGroupPath = Path.ROOT_PATH
    }

    private fun finishObject() {
        finishMesh()
        lastObjectPath = Path.ROOT_PATH
    }

    private fun readNewObject() {
        // new object begins
        skipSpaces()
        finishObject()
        lastObjectName = readUntilSpace()
        skipLine()
    }

    private fun readNewGroup() {
        // new group begins
        finishGroup()
        skipSpaces()
        lastGroupName = readUntilSpace()
    }

    private fun readUseMaterial() {
        if (nextChar() == 's' && nextChar() == 'e' && nextChar() == 'm' && nextChar() == 't' && nextChar() == 'l') {
            skipSpaces()
            finishMesh()
            lastMaterial = materials[readUntilSpace()] ?: InvalidRef
        } else skipLine()
    }

    private fun readPosition() {
        skipSpaces()
        positions += readFloat()
        skipSpaces()
        positions += readFloat()
        skipSpaces()
        positions += readFloat()
        skipLine()
    }

    private fun readUVs() {
        skipSpaces()
        uvs += readFloat()
        skipSpaces()
        uvs += readFloat()
        skipLine()
    }

    private fun readNormals() {
        skipSpaces()
        normals += readFloat()
        skipSpaces()
        normals += readFloat()
        skipSpaces()
        normals += readFloat()
        skipLine()
    }

    private fun readMaterialLib() {
        // mtllib
        if (nextChar() == 't' && nextChar() == 'l' && nextChar() == 'l' && nextChar() == 'i' && nextChar() == 'b') {
            val file2 = readFile(file)
            if (file2.exists && !file2.isDirectory) {
                try {
                    val folder = MTLReader2.readAsFolder(file2, materialsFolder.value)
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

    private fun readLine() {
        points.clear()
        skipSpaces()
        val idx0 = readIndex()
        skipSpaces()
        val idx1 = readIndex()
        val next0 = nextChar()
        if (next0 == '\n') {
            putLinePoint(idx0)
            putLinePoint(idx1)
            putLinePoint(idx1) // degenerate triangle
        } else {
            putBack(next0)
            points.add(idx0)
            points.add(idx1)
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
            var previous = points[0]
            for (i in 1 until points.size) {
                putLinePoint(previous)
                val next = points[i]
                putLinePoint(next)
                putLinePoint(next) // degenerate triangle
                previous = next
            }
        }
    }

    private fun readFacePoints() {
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
    }

    private fun triangulateFace() {

        // triangulate the points correctly
        // currently is the most expensive step, because of so much allocations:
        // points, the array, the return list, ...

        val points2 = Array(points.size / 3) {
            val point = Point.stack.create()
            val vi = points[it * 3]
            val ni = points[it * 3 + 1]
            val ui = points[it * 3 + 2]
            point.position.set(
                positions[vi],
                positions[vi + 1],
                positions[vi + 2]
            )
            if (ni >= 0) {
                point.normal.set(
                    normals[ni],
                    normals[ni + 1],
                    normals[ni + 2]
                )
            } else point.normal.set(0f)
            if (ui >= 0) {
                point.uv!!.set(
                    uvs[ui],
                    uvs[ui + 1]
                )
            } else point.uv!!.set(0f)
            point
        }
        val triangles = Triangulation.ringToTrianglesPoint(points2)
        for (i in triangles.indices step 3) {
            putPoint(triangles[i])
            putPoint(triangles[i + 1])
            putPoint(triangles[i + 2])
        }

        Point.stack.sub(points2.size)

    }

    private fun readFace() {

        readFacePoints()

        when (points.size / 3) {
            0 -> {
            } // nothing...
            1 -> {
                // a single, floating point
                putPoint(0)
                putPoint(0)
                putPoint(0)
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
            else -> triangulateFace()
        }
    }

    init {

        try {

            while (true) {
                // read the line
                skipSpaces()
                when (val char0 = nextChar()) {
                    '#' -> skipLine()
                    'o' -> readNewObject()
                    'g' -> readNewGroup()
                    'u' -> readUseMaterial()
                    'v' -> {
                        when (nextChar()) {
                            ' ', '\t' -> readPosition()
                            't' -> readUVs()
                            'n' -> readNormals()
                            else -> skipLine()
                        }
                    }
                    // smoothness -> ignore?
                    // at least Blender sets smoothness by normals
                    // what would it be otherwise???...
                    's' -> skipLine()
                    'm' -> readMaterialLib()
                    'l' -> readLine()
                    'f' -> readFace()
                    else -> {
                        putBack(char0)
                        val tagName = readUntilSpace()
                        if (tagName.isNotEmpty()) LOGGER.warn("Unknown obj tag $tagName")
                        skipLine()
                    }
                }
            }
        } catch (e: EOFException) {
        }

        finishGroup()
        reader.close()

        // UVCorrection.correct(folder)
        folder.sealPrefabs()

    }

    companion object {

        private val LOGGER = LogManager.getLogger(OBJReader2::class)

        fun readAsFolder(file: FileReference): InnerFolder {
            return file.inputStream().use { OBJReader2(it, file) }.folder
        }

    }

}
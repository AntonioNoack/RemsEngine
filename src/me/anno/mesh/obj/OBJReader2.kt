package me.anno.mesh.obj

import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.change.CAdd
import me.anno.ecs.prefab.change.Path
import me.anno.fonts.mesh.Triangulation
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.zip.InnerFolder
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.pow
import me.anno.mesh.Point
import me.anno.utils.Clock
import me.anno.utils.OS.downloads
import me.anno.utils.files.Files.findNextFileName
import me.anno.utils.files.Files.findNextName
import me.anno.utils.structures.arrays.ExpandingFloatArray
import me.anno.utils.structures.arrays.ExpandingIntArray
import me.anno.utils.structures.lists.Lists.any2
import org.apache.logging.log4j.LogManager
import java.io.EOFException
import java.io.IOException
import java.io.InputStream

// in a really heavy scene (San Miguel with 10M vertices),
// with unpacking it from a zip file,
// this was 3x faster than assimp: 13s instead of 40s
// without unzipping, it still was 10s for this, and 34s for assimp
// (yes, when using assimp, I am copying everything, which is non-optimal)
class OBJReader2(input: InputStream, val file: FileReference) : OBJMTLReader(input) {

    constructor(file: FileReference) : this(file.inputStream(), file)

    val folder = InnerFolder(file)
    val materialsFolder by lazy { InnerFolder(folder, "materials") }
    val meshesFolder by lazy { InnerFolder(folder, "meshes") }

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

    private var numPositions = 0
    private var numUVs = 0
    private var numNormals = 0

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
        val positions = positions
        val facePositions = facePositions
        facePositions.ensureExtra(3)
        facePositions.addUnsafe(positions[vertex])
        facePositions.addUnsafe(positions[vertex + 1])
        facePositions.addUnsafe(positions[vertex + 2])
        val faceNormals = faceNormals
        faceNormals.ensureExtra(3)
        if (normal in 0 until normals.size - 2) {
            val normals = normals
            faceNormals.addUnsafe(normals[normal])
            faceNormals.addUnsafe(normals[normal + 1])
            faceNormals.addUnsafe(normals[normal + 2])
        } else {
            faceNormals.addUnsafe(0f, 0f, 0f)
        }
        val faceUVs = faceUVs
        faceUVs.ensureExtra(2)
        if (uv in 0 until uvs.size - 1) {
            val uvs = uvs
            faceUVs.addUnsafe(uvs[uv], uvs[uv + 1])
        } else {
            faceUVs.addUnsafe(0f, 0f)
        }
    }

    private fun putLinePoint(index: Int) {
        val vertex = index * 3
        try {
            facePositions += positions[vertex]
            facePositions += positions[vertex + 1]
            facePositions += positions[vertex + 2]
            faceNormals += 0f
            faceNormals += 0f
            faceNormals += 0f
            faceUVs += 0f
            faceUVs += 0f
        } catch (e: ArrayIndexOutOfBoundsException) {
            e.printStackTrace()
        }
    }

    private var lastObjectName = "Mesh0"
    private var lastGroupName = ""

    private var lastGroupPath: Path = Path.ROOT_PATH
    private var lastObjectPath: Path = Path.ROOT_PATH
    private var groupCountInScene = 0
    private var objectCountInGroup = 0
    private var meshCountInObject = 0

    private fun newGroup() {
        // this check could be accelerated for huge obj files (currently O(n²))
        if (scenePrefab.adds.any2 { it.path == Path.ROOT_PATH && it.nameId == lastGroupName }) {
            // group was already used
            lastGroupPath = Path.ROOT_PATH.added(lastGroupName, 0, 'e')
            objectCountInGroup = 1 // we don't really know it
        } else {
            lastGroupPath = scenePrefab.add(
                CAdd(Path.ROOT_PATH, 'e', "Entity", lastGroupName),
                groupCountInScene++, -1
            )
            objectCountInGroup = 0
        }
        newObject()
    }

    private fun newObject() {
        if (lastGroupName.isNotEmpty() && lastGroupPath.isEmpty()) {
            newGroup()
        }
        // if entity already exists, find new name
        // this check could be accelerated for huge obj files (currently O(n²))
        if (scenePrefab.adds.any2 { it.path == lastGroupPath && it.nameId == lastObjectName }) {
            lastObjectPath = lastGroupPath.added(lastObjectName, 0, 'e')
            meshCountInObject = 1 // we don't really know it
        } else {
            lastObjectPath = scenePrefab.add(
                CAdd(lastGroupPath, 'e', "Entity", lastObjectName),
                objectCountInGroup++, -1
            )
            // in case there is no new name, create one ourselves
            lastObjectName = findNextName(lastObjectName, '.')
            meshCountInObject = 0
        }
    }

    private fun finishMesh() {
        if (facePositions.size > 0) {
            if (lastObjectPath.isEmpty()) newObject()
            val mesh = Prefab("Mesh")
            val name = lastObjectPath.nameId
            var fileName = "$name.json"
            mesh.setProperty("material", lastMaterial)
            mesh.setProperty("positions", facePositions.toFloatArray())
            mesh.setProperty("normals", faceNormals.toFloatArray())
            mesh.setProperty("uvs", faceUVs.toFloatArray())
            val meshesFolder = meshesFolder
            // find good new name for mesh
            if (meshesFolder.getChild(fileName) != InvalidRef) {
                fileName = "$name-${lastMaterial.name}"
                val fileI = meshesFolder.getChild(fileName)
                if (fileI != InvalidRef) {
                    fileName = findNextFileName(fileI, 1, '-', 1)
                }
            }
            // add mesh component to last object
            val meshRef = meshesFolder.createPrefabChild(fileName, mesh)
            mesh.source = meshRef
            var prefabName = name
            var add: Path
            nameSearch@ while (true) {
                try {
                    add = scenePrefab.add(lastObjectPath, 'c', "MeshComponent", prefabName, meshCountInObject)
                    break@nameSearch
                } catch (e: IllegalArgumentException) {
                    // continue searching a better name...
                    prefabName = findNextName(prefabName, '.')
                }
            }
            meshCountInObject++
            scenePrefab[add, "mesh"] = meshRef
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
        numPositions++
    }

    private fun readUVs() {
        skipSpaces()
        uvs += readFloat()
        skipSpaces()
        uvs += readFloat()
        skipLine()
        numUVs++
    }

    private fun readNormals() {
        skipSpaces()
        normals += readFloat()
        skipSpaces()
        normals += readFloat()
        skipSpaces()
        normals += readFloat()
        skipLine()
        numNormals++
    }

    private fun readMaterialLib() {
        // mtllib
        if (nextChar() == 't' && nextChar() == 'l' && nextChar() == 'l' && nextChar() == 'i' && nextChar() == 'b') {
            val file2 = readFile(file)
            if (file2.exists && !file2.isDirectory) {
                try {
                    val folder = MTLReader2.readAsFolder(file2, materialsFolder)
                    val subMaterials = folder.listChildren()
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
        val numVertices = positions.size / 3
        val idx0 = readIndex(numVertices)
        skipSpaces()
        val idx1 = readIndex(numVertices)
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
                        points += readIndex(numVertices) * 3
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
        val points = points
        points.clear()
        val numPositions = numPositions
        val numNormals = numNormals
        val numUVs = numUVs
        pts@ while (true) {
            when (val next = nextChar()) {
                ' ', '\t', '\r' -> {
                }
                '\n' -> break@pts
                '+', '-', in '0'..'9' -> {
                    putBack(next)
                    val vertexIndex = readIndex(numPositions)
                    var uvIndex = -1
                    var normalIndex = -1
                    if (putBack == '/'.code) {
                        nextChar()
                        uvIndex = readIndex(numUVs)
                        if (putBack == '/'.code) {
                            nextChar()
                            normalIndex = readIndex(numNormals)
                        }
                    }
                    points.ensureCapacity(points.size + 3)
                    points.addUnsafe(vertexIndex * 3, normalIndex * 3, uvIndex * 2)
                    if (points.size % 250 == 0) LOGGER.warn("Large polygon in $file, ${points.size / 3} points, '$next'")
                }
                else -> {
                    LOGGER.warn("Unexpected character $next in face line")
                    skipLine()
                    return
                }
            }
        }
    }

    private fun triangulateFace() {

        // triangulate the points correctly
        // currently is the most expensive step, because of so many allocations:
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
        } catch (_: EOFException) {
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

        @JvmStatic
        fun main(args: Array<String>) {
            val source = downloads.getChild("San_Miguel/san-miguel.obj")
            // 20MB, so larger than the L3 cache of my CPU
            // so the theoretical speed limit is my memory bandwidth
            // 3.2Gb/s -> 400MB/s -> 20MB file should be readable within 0.05s
            if (source.length() < 100e6) {
                val data = source.readText() // remove material references for clearer reading performance
                    .replace("mtllib", "#mtllib")
                    .toByteArray()
                val clock = Clock()
                for (i in 0 until 1000) {
                    clock.start()
                    OBJReader2(data.inputStream(), source)
                    clock.stop("Reading OBJ with 20MB", data.size)
                }
            } else {
                val clock = Clock()
                for (i in 0 until 1000) {
                    clock.start()
                    OBJReader2(source.inputStream(), source)
                    clock.stop("Reading OBJ with 20MB", source.length().toInt())
                }
            }
            // 0.5s, so 10x slower than possible... ok, but slow...
            // goes down to 0.13s after the first 10 runs
            // and with "optimizations" (trial and error what is faster),
            // we now get down to 0.105s
            // 2x slower than theoretically possible -> ok, I think
        }

    }

}
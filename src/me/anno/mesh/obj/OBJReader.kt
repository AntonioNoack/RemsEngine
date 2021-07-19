package me.anno.mesh.obj

import me.anno.fonts.mesh.Triangulation
import me.anno.io.files.FileReference
import me.anno.mesh.Line
import me.anno.mesh.Mesh
import me.anno.mesh.Model
import me.anno.mesh.Point
import org.apache.logging.log4j.LogManager
import org.joml.Vector2f
import org.joml.Vector3f
import java.io.EOFException
import java.io.File
import java.io.IOException
import java.io.InputStream

class OBJReader(input: InputStream, val file: File?) : OBJMTLReader(input) {

    constructor(file: FileReference) : this(file.inputStream(), null)

    companion object {
        private val LOGGER = LogManager.getLogger(OBJReader::class)
    }

    val objects = ArrayList<Model>()

    /**
     * x,y,z,u,v,nx,ny,nz
     * */
    val pointsByMaterial = HashMap<Material, ArrayList<Float>>()

    init {
        // inside the function, because not used materials aren't relevant to us
        val materials = HashMap<String, Material>()
        val defaultSize = 256
        try {
            lateinit var byMaterialPoints: ArrayList<Float>
            var mesh: Mesh? = null
            var model: Model? = null
            var lastMaterial = ""
            val vertices = ArrayList<Vector3f>(defaultSize)
            val normals = ArrayList<Vector3f>(defaultSize)
            val uvs = ArrayList<Vector2f>(defaultSize)
            // indices start at 1, so we can use the 0th element for default values
            vertices += Vector3f()
            uvs += Vector2f()
            normals += Vector3f(0f, 0f, 0f)
            while (true) {
                // read the line
                skipSpaces()
                val char0 = next()
                if (char0 == '#'.code) {
                    // just a comment
                    skipLine()
                } else {
                    putBack(char0)
                    when (val cmd = readUntilSpace()) {
                        "o" -> {// a new object begins...
                            // do I care? not really...
                            skipSpaces()
                            val name = readUntilSpace()
                            model = Model(
                                name, arrayListOf(
                                    Mesh(
                                        lastMaterial,
                                        ArrayList(defaultSize),
                                        ArrayList(16)
                                    )
                                )
                            )
                            mesh = model.meshes[0]
                            skipLine()
                        }
                        "usemtl" -> {
                            skipSpaces()
                            val materialName = readUntilSpace()
                            val material = materials.getOrPut(materialName) { Material() }
                            var points = pointsByMaterial[material]
                            if (points == null) {
                                points = ArrayList(defaultSize * 8)
                                pointsByMaterial[material] = points
                            }
                            if (model != null) {
                                // clear the model, if it was unknown...
                                if (model.meshes.last().points!!.isEmpty()) {
                                    (model.meshes as MutableList).apply {
                                        removeAt(lastIndex)
                                    }
                                }
                                mesh = Mesh(materialName, ArrayList(defaultSize), ArrayList(16))
                                (model.meshes as MutableList).add(mesh)
                            }
                            lastMaterial = materialName
                            byMaterialPoints = points
                        }
                        "v" -> vertices.add(readVector3f())
                        "vt" -> uvs.add(readVector2f())
                        "vn" -> normals.add(readVector3f())
                        "s" -> {
                            // smoothness -> ignore?
                            // at least Blender sets smoothness by normals
                            // what would it be otherwise???...
                            skipLine()
                        }
                        "l" -> {

                            val points = ArrayList<Vector3f>()
                            pts@ while (true) {
                                when (val next = next()) {
                                    ' '.code, '\t'.code -> {
                                    }
                                    '\n'.code -> break@pts
                                    else -> {
                                        // support negative indices? haven't seen them in the wild yet...
                                        putBack(next)
                                        val vertexIndex = readUntilSpace().toInt()
                                        points += vertices[vertexIndex]
                                    }
                                }
                            }

                            if (mesh != null) {
                                val lines = mesh.lines as ArrayList<Line>
                                for (i in 1 until points.size) {
                                    lines.add(Line(points[i - 1], points[i]))
                                }
                            }

                        }
                        "f" -> {

                            val points = ArrayList<Point>()
                            pts@ while (true) {
                                when (val next = next()) {
                                    ' '.code, '\t'.code -> {
                                    }
                                    '\n'.code -> break@pts
                                    else -> {
                                        // support negative indices? haven't seen them in the wild yet...
                                        putBack(next)
                                        val indices = readUntilSpace().split('/')
                                        val vertexIndex = indices[0].toInt()
                                        val uvIndex = indices.getOrNull(1)?.toIntOrNull()
                                        val normalIndex = indices.getOrNull(2)?.toIntOrNull()
                                        points += Point(
                                            vertices[vertexIndex],
                                            normals[normalIndex ?: 0],
                                            uvs[uvIndex ?: 0]
                                        )
                                    }
                                }
                            }

                            fun putPoint(point: Point) {
                                if (mesh != null) {
                                    val p = mesh.points as ArrayList<Point>
                                    p.add(point)
                                }
                                val (coordinates, normal, uv) = point
                                byMaterialPoints.add(coordinates.x)
                                byMaterialPoints.add(-coordinates.y) // y is flipped
                                byMaterialPoints.add(coordinates.z)
                                byMaterialPoints.add(uv?.x ?: 0f)
                                byMaterialPoints.add(uv?.y ?: 0f)
                                byMaterialPoints.add(normal.x)
                                byMaterialPoints.add(normal.y)
                                byMaterialPoints.add(normal.z)
                            }

                            fun putPoint(index: Int) {
                                putPoint(points[index])
                            }

                            when (points.size) {
                                in 0..2 -> {
                                }
                                3 -> {
                                    putPoint(0)
                                    putPoint(1)
                                    putPoint(2)
                                }
                                else -> {
                                    // triangulate the points correctly
                                    val triangles = Triangulation.ringToTrianglesPoint(points)
                                    for (i in triangles.indices step 3) {
                                        putPoint(triangles[i])
                                        putPoint(triangles[i + 1])
                                        putPoint(triangles[i + 2])
                                    }
                                }
                            }
                        }
                        "mtllib" -> {
                            if (file != null) {
                                val file2 = readFile(file)
                                if (file2.exists() && !file2.isDirectory) {
                                    try {
                                        materials.putAll(MTLReader(file2).materials)
                                    } catch (e: IOException) {
                                        e.printStackTrace()
                                    }
                                }
                            }
                        }
                        else -> {
                            LOGGER.info("Unknown tag in obj: $cmd")
                            skipLine()
                        }
                    }
                }
            }
        } catch (e: EOFException) {
        }
        reader.close()
    }

}
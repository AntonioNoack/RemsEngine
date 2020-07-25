package me.anno.objects.meshes.obj

import org.apache.logging.log4j.LogManager
import org.joml.Vector2f
import org.joml.Vector3f
import java.io.EOFException
import java.io.File

class OBJReader(val file: File): OBJMTLReader(file.inputStream().buffered()){

    companion object {
        private val LOGGER = LogManager.getLogger(OBJReader::class)
        private val defaultSize = 256
    }

    val materials = HashMap<String, Material>()

    /**
     * x,y,z,u,v,nx,ny,nz
     * */
    val pointsByMaterial = HashMap<Material?, ArrayList<Float>>()

    init {
        try {
            lateinit var trianglePoints: ArrayList<Float>
            val vertices = ArrayList<Vector3f>(defaultSize)
            val normals = ArrayList<Vector3f>(defaultSize)
            val uvs = ArrayList<Vector2f>(defaultSize)
            // indices start at 1, so we can use the 0th element for default values
            vertices += Vector3f()
            uvs += Vector2f()
            normals += Vector3f(0f, 0f, 0f)
            while(true){
                // read the line
                skipSpaces()
                val char0 = next()
                if(char0 == '#'.toInt()){
                    // just a comment
                    skipLine()
                } else {
                    putBack(char0)
                    when(val name = readUntilSpace()){
                        "o" -> {// a new object begins...
                            // do I care? not really...
                            skipLine()
                        }
                        "usemtl" -> {
                            skipSpaces()
                            val materialName = readUntilSpace()
                            val material = materials[materialName]
                            if(material == null){
                                LOGGER.warn("Material $materialName was not found!")
                            }
                            var points = pointsByMaterial[material]
                            if(points == null){
                                points = ArrayList(defaultSize * 8)
                                pointsByMaterial[material] = points
                            }
                            trianglePoints = points
                        }
                        "v" -> vertices.add(readVector3f())
                        "vt" -> uvs.add(readVector2f())
                        "vn" -> normals.add(readVector3f())
                        "f" -> {
                            val points = ArrayList<Point>()
                            pts@while(true){
                                when(val next = next()){
                                    ' '.toInt(), '\t'.toInt() -> {}
                                    '\n'.toInt() -> break@pts
                                    else -> {
                                        // support negative indices? haven't seen them in the wild yet...
                                        putBack(next)
                                        val indices = readUntilSpace().split('/')
                                        val vertexIndex = indices[0].toInt()
                                        val uvIndex = indices.getOrNull(1)?.toIntOrNull()
                                        val normalIndex = indices.getOrNull(2)?.toIntOrNull()
                                        points += Triple(vertices[vertexIndex], uvs[uvIndex ?: 0], normals[normalIndex ?: 0])
                                    }
                                }
                            }

                            fun putPoint(point: Point){
                                val (coordinates, uv, normal) = point
                                trianglePoints.add(coordinates.x)
                                trianglePoints.add(coordinates.y)
                                trianglePoints.add(coordinates.z)
                                trianglePoints.add(uv.x)
                                trianglePoints.add(uv.y)
                                trianglePoints.add(normal.x)
                                trianglePoints.add(normal.y)
                                trianglePoints.add(normal.z)
                            }

                            fun putPoint(index: Int){
                                putPoint(points[index])
                            }

                            when(points.size){
                                in 0 .. 2 -> {}
                                3 -> {
                                    putPoint(0)
                                    putPoint(1)
                                    putPoint(2)
                                }
                                else -> {
                                    // todo triangulate the points correctly
                                    for(i in 2 until points.size){
                                        putPoint(0)
                                        putPoint(1)
                                        putPoint(i)
                                    }
                                }
                            }
                        }
                        "mtllib" -> {
                            val file2 = readFile(file)
                            if(file2.exists() && !file2.isDirectory){
                                materials.putAll(MTLReader(file2).materials)
                            }
                        }
                        else -> {
                            LOGGER.info("Unknown tag in obj: $name")
                            skipLine()
                        }
                    }
                }
            }
        } catch (e: EOFException){}
        reader.close()
    }

}
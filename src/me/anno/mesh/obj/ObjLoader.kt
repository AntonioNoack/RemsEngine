package me.anno.mesh.obj

import me.anno.mesh.Mesh
import me.anno.mesh.Model
import me.anno.mesh.Point
import org.joml.Vector2f
import org.joml.Vector3f
import java.io.BufferedReader
import java.io.InputStream

object ObjLoader {

    fun loadObj(input: InputStream): List<Model> {

        val lineReader = BufferedReader(input.reader())

        val size = 1024

        // read obj
        val objects = ArrayList<Model>()
        val meshes = ArrayList<Mesh>()
        val positions = ArrayList<Vector3f>(size)
        val normals = ArrayList<Vector3f>(size)
        val uvs = ArrayList<Vector2f>(size)
        val faces = ArrayList<Point>(size)

        var objName = ""
        var material = ""

        val null3 = Vector3f()
        positions += null3
        normals += null3
        val null2 = Vector2f()
        uvs += null2

        fun closeMaterial() {
            if (faces.isNotEmpty()) {
                meshes += Mesh(material, ArrayList(faces))
                faces.clear()
            }
        }

        fun closeObject() {
            closeMaterial()
            objects += Model(objName, meshes)
        }

        while (true) {
            val line = lineReader.readLine() ?: break
            if (!line.startsWith("#")) {
                var firstSpaceIndex = line.indexOf(' ')
                if (firstSpaceIndex < 0) firstSpaceIndex = line.length
                val type = line.substring(0, firstSpaceIndex)
                val args = line
                    .substring(firstSpaceIndex + 1)
                    .split(' ')
                    .filter { it.isNotEmpty() }
                when (type) {
                    "o" -> {
                        closeObject()
                        objName = args[0]
                    }
                    "usemtl" -> {
                        closeMaterial()
                        material = args[0]
                    }
                    "v" -> positions.add(Vector3f(args[0].toFloat(), args[1].toFloat(), args[2].toFloat()))
                    "vn" -> normals.add(Vector3f(args[0].toFloat(), args[1].toFloat(), args[2].toFloat()))
                    "vt" -> uvs.add(Vector2f(args[0].toFloat(), args[1].toFloat()))
                    "s" -> {
                    } // smooth I think...
                    "f" -> {

                        fun String.parsePoint(): Point {
                            val indices = split('/').map { it.toIntOrNull() }
                            val position = positions[indices[0] ?: 0]
                            val uv = if(indices.size > 1) uvs[indices[1] ?: 0] else null2
                            val normal = if(indices.size> 2) normals[indices[2] ?: 0] else null3
                            return Point(position, normal, uv)
                        }

                        val indexGroups = args.map { group -> group.parsePoint() }
                        for (i in 1 until indexGroups.size) {
                            val zero = indexGroups[0]
                            val last = indexGroups[i]
                            val that = indexGroups[i - 1]
                            faces += zero
                            faces += last
                            faces += that
                        }
                    }
                }
            }// else comment
        }
        closeObject()

        return objects

    }

}
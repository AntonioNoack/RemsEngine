package me.anno.tests.files

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.material.Material
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.io.json.generic.JsonFormatter
import me.anno.io.numpy.NumPyReader
import me.anno.utils.OS.documents
import org.joml.Vector3f

/**
 * MakeHuman has a set of meshes, e.g., for clothing, which are saved as Python binary objects.
 * This sample demonstrates a reader for them using the NumPyReader utility.
 * */
fun main() {

    OfficialExtensions.initForTests()
    val name = "female_casualsuit01"

    val src = documents.getChild("MakeHuman/data/clothes/$name/$name.npz")
    NumPyReader.readNPZ(src) { data, err ->
        err?.printStackTrace()

        data!!

        // numbers for fedora
        val coord = data["coord"] // positions, 634x vec3
        val texco = data["texco"] // uvs, 702x vec2
        val fvert = data["fvert"] // indices, 622x ivec4
        // val vface = data["vface"] // indices, 634x ivec8
        // val nfaces = data["nfaces"] // nearly all 4s, a few 2s at the end, 634x byte
        // group, 622 zeros
        val fuvs = data["fuvs"] // 622x ivec4, mostly in order, from 0 to 701
        // print data for debugging
        println(
            JsonFormatter.format(data.filter { it.value != null }.mapValues { (_, v) ->
                v!!
                listOf(v.descriptor, v.shape, v.columnMajor, v.data)
            }, "\t", 500)
        )

        // format defined by https://github.com/makehumancommunity/makehuman/blob/master/makehuman/shared/wavefront.py
        // like OBJ file format, indices are not really supported

        val positions = coord!!.data as FloatArray
        val uvs = texco!!.data as FloatArray
        val indices = fvert!!.data as IntArray
        val uvIndices = fuvs!!.data as IntArray

        data class Vertex(val x: Float, val y: Float, val z: Float, val u: Float, val v: Float)

        val vertexList = ArrayList<Vertex>(indices.size)
        val vertices = HashMap<Vertex, Int>(indices.size)
        val normals = HashMap<Vector3f, Vector3f>()
        val tmpIndices = IntArray(indices.size)

        for (i in indices.indices) {
            val x = indices[i] * 3
            val u = uvIndices[i] * 2
            val vertex = Vertex(
                positions[x], positions[x + 1], positions[x + 2],
                uvs[u], uvs[u + 1]
            )
            tmpIndices[i] = vertices.getOrPut(vertex) {
                vertexList.add(vertex)
                vertices.size
            }
        }

        val a = Vector3f()
        val b = Vector3f()
        val c = Vector3f()
        for (i in indices.indices step 4) {
            // calculate normal
            a.set(positions, indices[i] * 3)
            b.set(positions, indices[i + 1] * 3).sub(a)
            c.set(positions, indices[i + 2] * 3).sub(a)
            // don't need to normalize, because larger triangles can be more important xD
            val n = b.cross(c)
            // add normal to all sub indices
            for (di in 0 until 4) {
                val key = Vector3f()
                key.set(positions, indices[i + di] * 3)
                normals.getOrPut(key, ::Vector3f).add(n)
            }
        }

        val newPositions = FloatArray(vertices.size * 3)
        val newNormals = FloatArray(vertices.size * 3)
        val newUVs = FloatArray(vertices.size * 2)

        for (normal in normals.values) {
            normal.normalize()
        }

        for (i in vertexList.indices) {
            val vertex = vertexList[i]
            val i2 = i + i
            val i3 = i2 + i
            newPositions[i3] = vertex.x
            newPositions[i3 + 1] = vertex.y
            newPositions[i3 + 2] = vertex.z
            val normal = normals[Vector3f(vertex.x, vertex.y, vertex.z)]!!
            newNormals[i3] = normal.x
            newNormals[i3 + 1] = normal.y
            newNormals[i3 + 2] = normal.z
            newUVs[i2] = vertex.u
            newUVs[i2 + 1] = vertex.v
        }

        // MakeHuman uses a format where each index describes a quad
        // Rem's Engine tries to do everything in triangles, so convert them
        val triIndices = IntArray(uvIndices.size / 4 * 6)
        for (i in tmpIndices.indices step 4) {
            val j = i / 4 * 6
            triIndices[j] = tmpIndices[i]
            triIndices[j + 1] = tmpIndices[i + 1]
            triIndices[j + 2] = tmpIndices[i + 2]
            triIndices[j + 3] = tmpIndices[i]
            triIndices[j + 4] = tmpIndices[i + 2]
            triIndices[j + 5] = tmpIndices[i + 3]
        }

        val mesh = Mesh()
        mesh.positions = newPositions
        mesh.indices = triIndices
        mesh.uvs = newUVs

        val material = Material()
        material.diffuseMap = src.getSibling("${name}_diffuse.png")
        material.normalMap = src.getSibling("${name}_normal.png")
        material.occlusionMap = src.getSibling("${name}_ao.png")
        mesh.material = material.ref

        testSceneWithUI("NumPy Fedora", mesh)
    }
}

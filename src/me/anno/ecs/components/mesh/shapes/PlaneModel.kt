package me.anno.ecs.components.mesh.shapes

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.terrain.TerrainUtils
import me.anno.utils.types.Triangles.subCross
import org.joml.Vector2f
import org.joml.Vector3f

/**
 * Creates a planar model with optional subdivisions.
 * */
object PlaneModel {

    fun createPlaneXZ(
        tilesU: Int, tilesV: Int,
        halfExtends: Vector2f,
    ): Mesh = createPlane(
        tilesU, tilesV,
        Vector3f(),
        Vector3f(halfExtends.x, 0f, 0f),
        Vector3f(0f, 0f, halfExtends.y)
    )

    fun createPlane(
        tilesU: Int, tilesV: Int,
        center: Vector3f = Vector3f(),
        du: Vector3f = Vector3f(1f, 0f, 0f),
        dv: Vector3f = Vector3f(0f, 0f, 1f),
    ): Mesh = createPlane(
        tilesU, tilesV,
        Vector3f(center).sub(du).sub(dv), Vector3f(center).sub(du).add(dv),
        Vector3f(center).add(du).sub(dv), Vector3f(center).add(du).add(dv)
    )

    fun createPlane(
        tilesU: Int, tilesV: Int,
        p00: Vector3f, p01: Vector3f,
        p10: Vector3f, p11: Vector3f,
    ): Mesh {
        val su = tilesU + 1
        val sv = tilesV + 1
        val vertexCount = su * sv
        val positions = FloatArray(vertexCount * 3)
        val uvs = FloatArray(vertexCount * 2)
        val mesh = Mesh()
        // positions
        val fu = 1f / (su - 1f)
        val fv = 1f / (sv - 1f)
        val tmpV0 = Vector3f()
        val tmpV1 = Vector3f()
        var i = 0
        var j = 0
        for (iv in 0 until sv) {
            val v = iv * fv
            p00.mix(p01, v, tmpV0)
            p10.mix(p11, v, tmpV1)
            tmpV1.sub(tmpV0).mul(fu)
            for (iu in 0 until su) {
                positions[i++] = tmpV0.x
                positions[i++] = tmpV0.y
                positions[i++] = tmpV0.z
                uvs[j++] = iu * fu
                uvs[j++] = 1f - v
                tmpV0.add(tmpV1)
            }
        }
        // normals
        val normals = FloatArray(positions.size)
        val normal = subCross(p00, p01, p10, Vector3f())
        val nx = normal.x
        val ny = normal.y
        val nz = normal.z
        for (k in positions.indices step 3) {
            normals[k] = nx
            normals[k + 1] = ny
            normals[k + 2] = nz
        }
        // indices
        mesh.positions = positions
        mesh.normals = normals
        mesh.uvs = uvs
        TerrainUtils.generateQuadIndices(su, sv, false, mesh)
        return mesh
    }
}
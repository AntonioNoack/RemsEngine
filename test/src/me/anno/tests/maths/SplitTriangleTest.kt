package me.anno.tests.maths

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.MeshIterators.forEachTriangleIndex
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.shapes.UVSphereModel
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.maths.Maths.mix
import me.anno.maths.Maths.posMod
import me.anno.maths.geometry.SplitTriangle.splitTriangle
import me.anno.maths.geometry.SplittablePoint
import me.anno.mesh.Triangulation
import me.anno.sdf.shapes.SDFPlane
import me.anno.sdf.shapes.SDFShape
import me.anno.ui.UIColors
import me.anno.utils.Color.mixARGB2
import me.anno.utils.Color.white
import me.anno.utils.structures.arrays.FloatArrayList
import me.anno.utils.structures.arrays.IntArrayList
import me.anno.utils.structures.lists.Lists.createList
import me.anno.utils.types.Booleans.toInt
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f

/**
 * split a sphere mesh by an SDF shape;
 *
 * e.g., could be used for cutting vegetables
 * */
fun main() {

    val sdfShape = SDFPlane().apply {
        // todo bug: middle-mesh generation doesn't work, if the cutting line is exactly on vertices
        //  (remove the following line)
        position.y += 0.1f
    }

    val mesh = UVSphereModel.createUVSphere(20, 10)
    val split = mesh.split(sdfShape)

    val scene = Entity("Scene")
        .add(
            Entity("Top")
                .add(MeshComponent(split[0], Material.diffuse(UIColors.axisXColor)))
                .add(MeshComponent(split[1], Material.diffuse(mixARGB2(UIColors.axisXColor, white, 0.2f))))
                .setPosition(0.0, 0.5, 0.0)
        )
        .add(
            Entity("Bottom")
                .add(MeshComponent(split[2], Material.diffuse(UIColors.axisZColor)))
                .add(MeshComponent(split[3], Material.diffuse(mixARGB2(UIColors.axisZColor, white, 0.2f))))
                .setPosition(0.0, 0.0, 0.0)
        )
    testSceneWithUI("SplitTriangle", scene)
}

class MeshBuilder {
    val positions = FloatArrayList(64)
    val normals = FloatArrayList(64)
    val uvs = FloatArrayList(64)

    fun build(): Mesh {
        val mesh = Mesh()
        mesh.positions = positions.toFloatArray()
        mesh.normals = normals.toFloatArray()
        mesh.uvs = uvs.toFloatArray()
        return mesh
    }
}

class Vertex(val pos: Vector3f, val nor: Vector3f, val uv: Vector2f, val dist: Float) : SplittablePoint<Vertex> {
    override fun split(b: Vertex, f: Float): Vertex {
        return Vertex(
            pos.mix(b.pos, f, Vector3f()),
            nor.mix(b.nor, f, Vector3f()),
            uv.mix(b.uv, f, Vector2f()),
            mix(dist, b.dist, f)
        )
    }
}

fun Mesh.split(shape: SDFShape): List<Mesh> {

    val pos = positions!!
    val nor = normals!!
    val uvs = uvs!!
    val result = createList(4) { MeshBuilder() }

    val tmp = Vector4f()
    val seeds = IntArrayList(0)
    fun getDist(a: Vector3f): Float {
        tmp.set(a, 0f)
        return shape.computeSDF(tmp, seeds)
    }

    fun getVertex(ai: Int): Vertex {
        val ai3 = ai * 3
        val posI = Vector3f(pos, ai3)
        return Vertex(posI, Vector3f(nor, ai3), Vector2f(uvs, ai * 2), getDist(posI))
    }

    fun addVertex(builder: MeshBuilder, a: Vertex) {
        builder.positions.add(a.pos)
        builder.normals.add(a.nor)
        builder.uvs.add(a.uv)
    }

    fun addVertex(builder: MeshBuilder, a: Vertex, normal: Vector3f) {
        builder.positions.add(a.pos)
        builder.normals.add(normal)
        builder.uvs.add(a.uv)
    }

    val rings = HashMap<Vector3f, Vertex>()
    fun addLine(a: Vertex, b: Vertex) {
        rings[a.pos] = b
    }

    fun isLine(a: Vertex): Boolean {
        return a.dist > -1e-3f
    }

    fun addTriangle(a: Vertex, b: Vertex, c: Vertex, idx: Int) {
        val builder = result[idx]
        addVertex(builder, a)
        addVertex(builder, b)
        addVertex(builder, c)
    }

    fun addTriangle(a: Vertex, b: Vertex, c: Vertex, idx: Int, normal: Vector3f) {
        val builder = result[idx]
        addVertex(builder, a, normal)
        addVertex(builder, b, normal)
        addVertex(builder, c, normal)
    }

    fun addTriangle(a: Vertex, b: Vertex, c: Vertex) {
        val dist = a.dist + b.dist + c.dist
        val idx = (dist >= 0f).toInt(0, 2)
        addTriangle(a, b, c, idx)
        if (idx == 2) {
            val al = isLine(a)
            val bl = isLine(b)
            val cl = isLine(c)
            if (!al || !bl || !cl) {
                when {
                    al && bl -> addLine(a, b)
                    bl && cl -> addLine(b, c)
                    cl && al -> addLine(c, a)
                }
            }
        }
    }

    forEachTriangleIndex { ai, bi, ci ->
        val a = getVertex(ai)
        val b = getVertex(bi)
        val c = getVertex(ci)
        val tris = splitTriangle(a, b, c, a.dist, b.dist, c.dist)
        for (i in tris.indices step 3) {
            addTriangle(tris[i], tris[i + 1], tris[i + 2])
        }
    }

    // collect rings
    while (rings.isNotEmpty()) {
        val ring = ArrayList<Vertex>()
        var ri = rings.keys.first()
        while (true) {
            val vi = rings.remove(ri)
                ?: rings.remove(rings.minByOrNull { it.key.distanceSquared(ri) }?.key) // fp inaccuracy :/
                ?: break
            ring.add(vi)
            ri = vi.pos
        }
        // triangulate rings;
        //  rings don't need to be planar... generate a curved surface somehow??
        if (ring.size < 3) continue
        val normal = Vector3f()
        val tmp1 = Vector3f()
        val tmp2 = Vector3f()
        for (i in 0 until ring.size) {
            val a = ring[i].pos
            val b = ring[posMod(i + 1, ring.size)].pos
            val c = ring[posMod(i + 2, ring.size)].pos
            // normal += (b-a) x (c-a)
            normal.add(b.sub(a, tmp1).cross(c.sub(a, tmp2)))
        }
        normal.safeNormalize()
        normal.negate(tmp1)
        val ringToVertex = ring.associateBy { it.pos }
        val triangles = Triangulation.ringToTrianglesVec3f(ring.map { it.pos })
        val tris = triangles.map { ringToVertex[it]!! }
        for (i in tris.indices step 3) {
            addTriangle(tris[i], tris[i + 1], tris[i + 2], 1, normal)
            addTriangle(tris[i], tris[i + 2], tris[i + 1], 3, tmp1)
        }
    }

    return result.map { it.build() }
}
package me.anno.maths.geometry

import me.anno.ecs.components.mesh.BoneWeights
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshIterators.forEachTriangleIndex
import me.anno.ecs.components.mesh.utils.MeshBuilder
import me.anno.maths.Maths.mix
import me.anno.maths.Maths.posMod
import me.anno.maths.Maths.sq
import me.anno.maths.geometry.TriangleSplitter.splitTriangle
import me.anno.mesh.Triangulation
import me.anno.utils.Color.mixARGB2
import me.anno.utils.structures.arrays.FloatArrayListUtils.add
import me.anno.utils.structures.lists.Lists.createList
import me.anno.utils.types.Booleans.toInt
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f

/**
 * can split a Mesh on a plane; will return four meshes (maybe only one, if no cut was needed),
 * which represent the left side, right side, and the cut-surfaces.
 * */
object MeshSplitter {

    fun interface V3F {
        fun call(v: Vector3f): Float
    }

    class MeshBuilderImpl(vc: Mesh) : MeshBuilder(vc) {

        fun addVertex(a: SplittableVertex, normal: Vector3f) {
            positions.add(a.position)
            normals?.add(normal)
            uvs?.add(a.uv!!)
            colors?.add(a.color)
            if (a.boneWeights != null) {
                boneWeights?.add(a.boneWeights.weights)
                if (boneIndices != null) {
                    val boneIds = a.boneWeights.boneIds
                    boneIndices.add(BoneWeights.getBoneId(boneIds, 0))
                    boneIndices.add(BoneWeights.getBoneId(boneIds, 1))
                    boneIndices.add(BoneWeights.getBoneId(boneIds, 2))
                    boneIndices.add(BoneWeights.getBoneId(boneIds, 3))
                }
            }
        }

        fun addVertex(a: SplittableVertex) {
            addVertex(a, a.normal)
        }

        fun addTriangle(a: SplittableVertex, b: SplittableVertex, c: SplittableVertex) {
            addVertex(a)
            addVertex(b)
            addVertex(c)
        }

        fun addTriangle(
            a: SplittableVertex,
            b: SplittableVertex,
            c: SplittableVertex,
            normal: Vector3f
        ) {
            addVertex(a, normal)
            addVertex(b, normal)
            addVertex(c, normal)
        }
    }

    class SplittableVertex(
        val position: Vector3f, val normal: Vector3f,
        val tangent: Vector3f?, val uv: Vector2f?,
        val color: Int, val boneWeights: BoneWeights?,
        val dist: Float
    ) : SplittablePoint<SplittableVertex> {
        override fun split(b: SplittableVertex, f: Float): SplittableVertex {
            return SplittableVertex(
                position.mix(b.position, f, Vector3f()),
                normal.mix(b.normal, f, Vector3f()),
                tangent?.mix(b.tangent!!, f, Vector3f()),
                uv?.mix(b.uv!!, f, Vector2f()),
                mixARGB2(color, b.color, f),
                boneWeights?.mix(b.boneWeights!!, f),
                mix(dist, b.dist, f)
            )
        }
    }

    class VertexCreator(val mesh: Mesh, val distance: V3F) {

        val positions = mesh.positions!!
        val normals = mesh.normals!!

        fun getBoneWeights(i: Int): BoneWeights? {
            val boneWeights = mesh.boneWeights
            val boneIndices = mesh.boneIndices
            if (boneWeights == null || boneIndices == null) {
                return null
            }
            val weights = Vector4f(boneWeights, i)
            val boneIds = BoneWeights.getBoneIds(boneIndices, i)
            return BoneWeights(weights, boneIds)
        }

        fun getVertex(i: Int): SplittableVertex {
            val i3 = i * 3
            val posI = Vector3f(positions, i3)
            val tangents = mesh.tangents
            val uvs = mesh.uvs
            val colors = mesh.color0
            return SplittableVertex(
                posI, Vector3f(normals, i3),
                if (tangents != null) Vector3f(tangents, i3) else null,
                if (uvs != null) Vector2f(uvs, i * 2) else null,
                if (colors != null) colors[i] else -1,
                getBoneWeights(i * 4),
                distance.call(posI)
            )
        }
    }

    class MeshSplitter(vc: VertexCreator) {

        val result = createList(4) {
            MeshBuilderImpl(vc.mesh)
        }

        val rings = HashMap<Vector3f, SplittableVertex>()
        fun addLine(a: SplittableVertex, b: SplittableVertex) {
            if (a.position != b.position) {
                rings[a.position] = b
            }
        }

        fun isLine(a: SplittableVertex): Boolean {
            return a.dist > -0.001f
        }

        fun addTriangle(a: SplittableVertex, b: SplittableVertex, c: SplittableVertex) {
            val dist = a.dist + b.dist + c.dist
            val idx = (dist >= 0f).toInt(0, 2)
            result[idx].addTriangle(a, b, c)
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

        private fun nextVertex(ri: Vector3f): SplittableVertex? {
            if (rings.isEmpty()) return null
            val toleranceSq = ri.lengthSquared() * sq(1e-5f)
            val vi = rings.remove(ri)
            if (vi != null) return vi
            val wi = rings.minBy { it.key.distanceSquared(ri) }
            if (wi.key.distanceSquared(ri) > toleranceSq) return null
            return rings.remove(wi.key)
        }

        private fun collectRing(ringVertices: ArrayList<SplittableVertex>) {
            ringVertices.clear()
            var ri = rings.keys.first()
            while (true) {
                val vi = nextVertex(ri) ?: break
                ringVertices.add(vi)
                ri = vi.position
            }
        }

        private val tmp1 = Vector3f()
        private val tmp2 = Vector3f()
        private val normal = Vector3f()

        private fun findRingNormal(
            ring: List<SplittableVertex>, normal: Vector3f,
            tmp1: Vector3f, tmp2: Vector3f
        ) {
            normal.set(0f)
            for (i in ring.indices) {
                val a = ring[i].position
                val b = ring[posMod(i + 1, ring.size)].position
                val c = ring[posMod(i + 2, ring.size)].position
                // normal += (b-a) x (c-a)
                normal.add(b.sub(a, tmp1).cross(c.sub(a, tmp2)))
            }
            normal.safeNormalize()
        }

        private fun triangulateRing(ring: List<SplittableVertex>, posNormal: Vector3f, negNormal: Vector3f) {
            val ringToVertex = ring.associateBy { it.position }
            val triangles = Triangulation.ringToTrianglesVec3f(ring.map { it.position })
            val tris = triangles.map { ringToVertex[it]!! }
            val (_, r1, _, r3) = result
            for (i in tris.indices step 3) {
                r1.addTriangle(tris[i], tris[i + 1], tris[i + 2], posNormal)
                r3.addTriangle(tris[i], tris[i + 2], tris[i + 1], negNormal)
            }
        }

        fun collectRings() {
            val ring = ArrayList<SplittableVertex>()
            while (rings.isNotEmpty()) {
                collectRing(ring)
                // triangulate rings;
                //  rings don't need to be planar... generate a curved surface somehow??
                if (ring.size < 3) continue
                findRingNormal(ring, normal, tmp1, tmp2)
                val negNormal = normal.negate(tmp1)
                triangulateRing(ring, normal, negNormal)
            }
        }
    }

    /**
     * result:
     *  [dist >= 0, dist>=0-surface, dist < 0, dist<0-surface]
     * */
    fun split(mesh: Mesh, distance: V3F): List<Mesh> {
        mesh.ensureNorTanUVs()
        val vc = VertexCreator(mesh, distance)
        val ms = MeshSplitter(vc)
        mesh.forEachTriangleIndex { ai, bi, ci ->
            val a = vc.getVertex(ai)
            val b = vc.getVertex(bi)
            val c = vc.getVertex(ci)
            val tris = splitTriangle(a, b, c, a.dist, b.dist, c.dist)
            for (i in tris.indices step 3) {
                ms.addTriangle(tris[i], tris[i + 1], tris[i + 2])
            }; false
        }
        ms.collectRings()
        return ms.result.map { it.build() }
    }
}
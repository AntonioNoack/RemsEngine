package me.anno.sdf.shapes

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshCache
import me.anno.ecs.components.mesh.MeshIterators.forEachTriangle
import me.anno.ecs.components.mesh.material.utils.TypeValue
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.GFX
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.texture.Texture2D
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.maths.bvh.BLASBranch
import me.anno.maths.bvh.BLASFiller.Companion.fillBLAS
import me.anno.maths.bvh.BLASLeaf
import me.anno.maths.bvh.BLASNode
import me.anno.maths.bvh.BLASTexture.PIXELS_PER_BLAS_NODE
import me.anno.maths.bvh.BLASTexture.createBLASTexture
import me.anno.maths.bvh.BVHBuilder
import me.anno.maths.bvh.RayTracing.intersectAABB
import me.anno.maths.bvh.SplitMethod
import me.anno.maths.bvh.TriangleTexture.createTriangleTexture
import me.anno.maths.bvh.TrisFiller.Companion.fillTris
import me.anno.sdf.SDFComposer.dot2
import me.anno.sdf.VariableCounter
import me.anno.sdf.shapes.SDFTriangle.Companion.calculateDistSq
import me.anno.sdf.shapes.SDFTriangle.Companion.udTriangle
import me.anno.utils.Color.toHexString
import me.anno.utils.structures.arrays.IntArrayList
import org.joml.AABBf
import org.joml.Vector4f
import kotlin.math.abs
import kotlin.math.sign
import kotlin.math.sqrt

/**
 * This class is just an experiment, and shouldn't really be used...
 * Performance is bad, because you're applying raytracing multiple times (70 steps typically LOL + 3 for normals) per ray.
 * */
open class SDFMesh : SDFSmoothShape() {

    enum class SDFMeshTechnique(val id: Int) {
        /**
         * Uses one texture slot for the BVH hierarchy, and one for all vertex positions.
         * */
        TEXTURE(0),

        /**
         * Generates the AABBs and triangle data as hardcoded branches and instructions in code; this saves a texture slot,
         * but also needs much longer to compile, and it was 25% slower for Suzanne on my RTX 3070
         * */
        IN_CODE(1),

        /**
         * Generates the AABBs and triangle data as a const array in code; this saves a texture slot, too, and the ideas was
         * that this potentially compiles faster... it is 5x slower than the other two methods on my RTX3070;
         * */
        CONST_ARRAY(2)
    }

    var meshFile: FileReference = InvalidRef
        set(value) {
            if (field != value) {
                invalidateShader()
                field = value
            }
        }

    override val boundsInfluencedBySmoothness: Boolean get() = true

    fun loadMesh() = MeshCache.getEntry(meshFile).waitFor("SDFMesh")

    var technique = SDFMeshTechnique.TEXTURE
        set(value) {
            if (field != value) {
                invalidateShader()
                field = value
            }
        }

    override fun calculateBaseBounds(dst: AABBf) {
        val mesh = loadMesh()
        if (mesh != null) {
            dst.set(mesh.getBounds())
            dst.addMargin(smoothness)
        } else super.calculateBaseBounds(dst)
    }

    private var lastMesh: Mesh? = null
    private var lastTris: Texture2D? = null
    private var lastBlas: Texture2D? = null

    override fun buildShader(
        builder: StringBuilder,
        posIndex0: Int,
        nextVariableId: VariableCounter,
        dstIndex: Int,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>,
        seeds: ArrayList<String>
    ) {
        val trans = buildTransform(builder, posIndex0, nextVariableId, uniforms, functions, seeds)
        smartMinBegin(builder, dstIndex)
        val mesh = loadMesh() as? Mesh
        val blas = if (mesh != null) BVHBuilder.buildBLAS(mesh, SplitMethod.MEDIAN_APPROX, 16) else null
        if (blas == null) {
            builder.append("sdBox(pos")
            builder.append(trans.posIndex)
            builder.append(",vec3(1.0))")
        } else {

            val pixelsPerVertex = 1
            val pixelsPerTriangle = 3 * pixelsPerVertex

            if (mesh !== lastMesh || technique != SDFMeshTechnique.TEXTURE) {
                lastBlas?.destroy()
                lastTris?.destroy()
                lastBlas = null
                lastTris = null
                lastMesh = mesh
            }

            val meshId = when (technique) {
                SDFMeshTechnique.TEXTURE -> "T"
                SDFMeshTechnique.IN_CODE -> "C"
                SDFMeshTechnique.CONST_ARRAY -> "V"
            } + meshFile.absolutePath.hashCode().toHexString()

            functions.add(intersectAABB)
            functions.add(dot2)
            functions.add(udTriangle)

            when (technique) {
                SDFMeshTechnique.TEXTURE -> {
                    GFX.checkIsGFXThread()

                    val blasTexture = lastBlas ?: createBLASTexture(blas, pixelsPerTriangle)
                    val trisTexture = lastTris ?: createTriangleTexture(blas, pixelsPerVertex)

                    lastBlas = blasTexture
                    lastTris = trisTexture

                    val funcCode = funcCode
                        .replace("#MAX_DEPTH", blas.maxDepth().toString())
                        .replace(
                            "#PREPARE_TREE",
                            "uvec2 nodeTexSize = uvec2(textureSize(blas#MESH_ID,0));\n" +
                                    "uvec2 triTexSize  = uvec2(textureSize(tris#MESH_ID,0));\n"
                        )
                        .replace(
                            "#LOAD_NODE", "" +
                                    "uint pixelIndex = nodeIndex * ${PIXELS_PER_BLAS_NODE}u;\n" +
                                    "uint nodeX = pixelIndex % nodeTexSize.x;\n" +
                                    "uint nodeY = pixelIndex / nodeTexSize.x;\n" +
                                    "vec4 d0 = texelFetch(blas#MESH_ID, ivec2(nodeX,   nodeY), 0);\n" +
                                    "vec4 d1 = texelFetch(blas#MESH_ID, ivec2(nodeX+1u,nodeY), 0);\n"
                        )
                        .replace(
                            "#PREPARE_TRIS",
                            "uint triX = index % triTexSize.x;\n" +
                                    "uint triY = index / triTexSize.x;\n"
                        )
                        .replace(
                            "#LOAD_TRI", "" +
                                    // actual load
                                    "vec3 p0 = texelFetch(tris#MESH_ID, ivec2(triX, triY), 0).rgb;\n" +
                                    "vec3 p1 = texelFetch(tris#MESH_ID, ivec2(triX+${pixelsPerVertex}u,triY), 0).rgb;\n" +
                                    "vec3 p2 = texelFetch(tris#MESH_ID, ivec2(triX+${pixelsPerVertex * 2}u,triY), 0).rgb;\n" +
                                    // index++
                                    "triX += ${pixelsPerTriangle}u;\n" +
                                    "if(triX >= triTexSize.x){\n" + // switch to next row of data if needed
                                    "   triX=0u;triY++;\n" +
                                    "}\n" +
                                    "index += ${pixelsPerTriangle}u;"
                        )
                        .replace("#MESH_ID", meshId)

                    functions.add(funcCode)
                    uniforms["blas$meshId"] = TypeValue(GLSLType.S2D, blasTexture)
                    uniforms["tris$meshId"] = TypeValue(GLSLType.S2D, trisTexture)
                }
                SDFMeshTechnique.IN_CODE -> {

                    val builder1 = StringBuilder()
                    fun build(node: BLASNode) {
                        val bounds = node.bounds
                        builder1.append("if(intersectAABB(p,i,vec3(")
                            .append(bounds.minX).append(',')
                            .append(bounds.minY).append(',')
                            .append(bounds.minZ).append("),vec3(")
                            .append(bounds.maxX).append(',')
                            .append(bounds.maxY).append(',')
                            .append(bounds.maxZ).append("),s,r)){\n")
                        when (node) {
                            is BLASBranch -> {
                                build(node.n0)
                                build(node.n1)
                            }
                            is BLASLeaf -> {
                                val positions = node.geometry.positions
                                val indices = node.geometry.indices
                                for (j in node.start until node.start + node.length) {
                                    builder1.append("r=min(r,udTriangle(p")
                                    for (i in 0 until 3) {
                                        val i3 = indices[j * 3 + i] * 3
                                        builder1.append(",vec3(")
                                            .append(positions[i3]).append(',')
                                            .append(positions[i3 + 1]).append(',')
                                            .append(positions[i3 + 2]).append(')')
                                    }
                                    builder1.append("));\n")
                                }
                            }
                        }
                        builder1.append("}\n")
                    }
                    build(blas)

                    val funcCode = "" +
                            "float sdf$meshId(vec3 p, vec3 d, float s){\n" +
                            "   float r = 1e38;\n" +
                            "   if(dot(d,d) < 0.5) d = vec3(0.0,1.0,0.0);\n" +
                            "   vec3 i = 1.0 / d;\n" +
                            builder1.toString() +
                            "   return r - s;\n" +
                            "}\n"

                    functions.add(funcCode)
                }
                SDFMeshTechnique.CONST_ARRAY -> {

                    val dataBuilder = StringBuilder()
                    val numNodes = blas.countNodes()
                    dataBuilder.append("const vec4 Nodes$meshId[")
                        .append(numNodes * 2)
                        .append("] = vec4[")
                        .append(numNodes * 2)
                        .append("](")
                    fillBLAS(listOf(blas), 3) { v0, v1, bounds ->
                        dataBuilder.append("vec4(")
                            .append(bounds.minX).append(',')
                            .append(bounds.minY).append(',')
                            .append(bounds.minZ).append(',')
                            .append(Float.fromBits(v0)).append("),vec4(")
                            .append(bounds.maxX).append(',')
                            .append(bounds.maxY).append(',')
                            .append(bounds.maxZ).append(',')
                            .append(Float.fromBits(v1)).append("),")
                    }
                    dataBuilder.setLength(dataBuilder.length - 1)
                    dataBuilder.append(");\n")

                    val geometry = blas.findGeometryData()
                    val numVertices = geometry.indices.size
                    dataBuilder.append("const vec3 Vertices$meshId[")
                        .append(numVertices)
                        .append("] = vec3[")
                        .append(numVertices)
                        .append("](")
                    fillTris(listOf(blas), listOf(geometry)) { _, vertexIndex ->
                        val positions = geometry.positions
                        val k = vertexIndex * 3
                        dataBuilder.append("vec3(")
                            .append(positions[k]).append(',')
                            .append(positions[k + 1]).append(',')
                            .append(positions[k + 2]).append("),")
                    }
                    dataBuilder.setLength(dataBuilder.length - 1)
                    dataBuilder.append(");\n")

                    val funcCode =
                        dataBuilder.toString() +
                                funcCode
                                    .replace("#MAX_DEPTH", blas.maxDepth().toString())
                                    .replace("#PREPARE_TREE", "")
                                    .replace("#PREPARE_TRIS", "")
                                    .replace(
                                        "#LOAD_NODE", "" +
                                                "vec4 d0 = Nodes#MESH_ID[nodeIndex*2u];\n" +
                                                "vec4 d1 = Nodes#MESH_ID[nodeIndex*2u+1u];\n"
                                    )
                                    .replace(
                                        "#LOAD_TRI", "" +
                                                // actual load
                                                "vec3 p0 = Vertices#MESH_ID[index];\n" +
                                                "vec3 p1 = Vertices#MESH_ID[index+1u];\n" +
                                                "vec3 p2 = Vertices#MESH_ID[index+2u];\n" +
                                                // index++
                                                "index += 3u;" // "pixelsPerTriangle"/"multiplier"
                                    )
                                    .replace("#MESH_ID", meshId)

                    functions.add(funcCode)
                }
            }

            builder.append("sdf$meshId(pos").append(trans.posIndex).append(',')
            builder.append("dir").append(trans.posIndex).append(',')
            val dynamicSmoothness = dynamicSmoothness || globalDynamic
            if (dynamicSmoothness) builder.appendUniform(uniforms, GLSLType.V1F) { smoothness }
            else builder.append(smoothness)
            builder.append(")")
        }
        smartMinEnd(builder, dstIndex, nextVariableId, uniforms, functions, seeds, trans)
    }

    override fun computeSDFBase(pos: Vector4f, seeds: IntArrayList): Float {
        var minDistSq = Float.POSITIVE_INFINITY
        val mesh = loadMesh() as? Mesh ?: return minDistSq
        var absMinDistSq = minDistSq
        mesh.forEachTriangle { a, b, c ->
            val dist = calculateDistSq(a, b, c, pos, true)
            val absDist = abs(dist)
            if (absDist < absMinDistSq) {
                absMinDistSq = absDist
                minDistSq = dist
            }; false
        }
        return sign(minDistSq) * sqrt(abs(minDistSq)) + pos.w
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is SDFMesh) return
        dst.meshFile = meshFile
    }

    override fun destroy() {
        super.destroy()
        lastBlas?.destroy()
        lastTris?.destroy()
        lastBlas = null
        lastTris = null
    }

    companion object {

        val funcCode = "" +
                "float sdf#MESH_ID(vec3 pos, vec3 dir, float s){\n" +
                "   float dist = 1e38;\n" +
                "   if(dot(dir,dir) < 0.5) dir = vec3(0.0,1.0,0.0);\n" +
                "   vec3 invDir = 1.0 / dir;\n" +
                "#PREPARE_TREE\n" +
                "   uint nextNodeStack[#MAX_DEPTH];\n" +
                "   uint nodeIndex = 0u, stackIndex = 0u;\n" +
                "   uint k=uint(ZERO);\n" +
                "   while(k++<512u){\n" + // could be k<bvh.count() or true or 2^depth
                // fetch node data
                "#LOAD_NODE\n" +
                "       if(intersectAABB(pos,invDir,d0.xyz,d1.xyz,s,dist)){\n" + // bounds check
                "           uvec2 v01 = floatBitsToUint(vec2(d0.a,d1.a));\n" +
                "           if(v01.y < 3u) {\n" +
                // check closest one first like in https://github.com/mmp/pbrt-v3/blob/master/src/accelerators/bvh.cpp
                "               if(dir[v01.y] > 0.0){\n" + // if !dirIsNeg[axis]
                "                   nextNodeStack[stackIndex++] = v01.x + nodeIndex;\n" + // mark other child for later
                "                   nodeIndex++;\n" + // search child next
                "               } else {\n" +
                "                   nextNodeStack[stackIndex++] = nodeIndex + 1u;\n" + // mark other child for later
                "                   nodeIndex += v01.x;\n" + // search child next
                "               }\n" +
                "           } else {\n" +
                // this node is a leaf
                // check all triangles for intersections
                "               uint index = v01.x, end = index + v01.y;\n" +
                "#PREPARE_TRIS\n" +
                "               for(;index<end;){\n" + // triangle index -> load triangle data
                "#LOAD_TRI\n" +
                // todo better distance function with bulb on backside
                "                   dist = min(dist, udTriangle(pos,p0,p1,p2));\n" +
                "               }\n" + // next node
                "               if(stackIndex < 1u) break;\n" +
                "               nodeIndex = nextNodeStack[--stackIndex];\n" +
                "          }\n" +
                "       } else {\n" + // next node
                "           if(stackIndex < 1u) break;\n" +
                "           nodeIndex = nextNodeStack[--stackIndex];\n" +
                "       }\n" +
                "   }\n" +
                "   return dist - s;\n" +
                "}\n"
    }
}
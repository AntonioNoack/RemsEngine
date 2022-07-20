package me.anno.maths.bvh

import me.anno.Engine
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.sdf.SDFComponent.Companion.quatRot
import me.anno.ecs.components.mesh.sdf.shapes.SDFBoundingBox.Companion.boundingBoxSDF
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.ECSShaderLib.pbrModelShader
import me.anno.gpu.DepthMode
import me.anno.gpu.buffer.ComputeBuffer
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.pipeline.CullMode
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.pipeline.PipelineStage
import me.anno.gpu.pipeline.Sorting
import me.anno.gpu.shader.ComputeShader
import me.anno.gpu.shader.ComputeTextureMode
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib.coordsList
import me.anno.gpu.shader.ShaderLib.coordsVShader
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.texture.Texture2D
import me.anno.maths.bvh.BLASNode.Companion.createBLASBuffer
import me.anno.maths.bvh.BLASNode.Companion.createBLASTexture
import me.anno.maths.bvh.BLASNode.Companion.createTriangleBuffer
import me.anno.maths.bvh.BLASNode.Companion.createTriangleTexture
import me.anno.maths.bvh.RayTracing.coloring
import me.anno.maths.bvh.RayTracing.glslBLASIntersection
import me.anno.maths.bvh.RayTracing.glslComputeDefines
import me.anno.maths.bvh.RayTracing.glslGraphicsDefines
import me.anno.maths.bvh.RayTracing.glslIntersections
import me.anno.maths.bvh.RayTracing.glslRandomGen
import me.anno.maths.bvh.RayTracing.glslTLASIntersection
import me.anno.maths.bvh.RayTracing.loadMat4x3
import me.anno.maths.bvh.RayTracing2.glslBLASIntersection2
import me.anno.maths.bvh.RayTracing2.glslTLASIntersection2
import me.anno.utils.Clock
import me.anno.utils.LOGGER
import me.anno.utils.OS.desktop
import me.anno.utils.OS.documents
import me.anno.utils.OS.downloads
import me.anno.utils.structures.tuples.Quad
import org.joml.*
import org.lwjgl.opengl.GL11C

fun createSampleTLAS(maxNodeSize: Int): Quad<TLASNode, Vector3f, Quaternionf, Float> {

    // create a scene, so maybe load Sponza, and then execute our renderer on TLAS
    @Suppress("SpellCheckingInspection")
    val sources = listOf(
        downloads.getChild("ogldev-source/crytek_sponza/sponza.obj"),
        downloads.getChild("ogldev-source/dabrovic-sponza/sponza.obj"),
        downloads.getChild("ogldev-source/conference-room/conference.obj"),
        documents.getChild("TestScene4.fbx"),
        downloads.getChild("3d/XYZ arrows.obj")
    )

    val source = sources[3]
    val pipeline = Pipeline(DeferredSettingsV2(listOf(DeferredLayerType.COLOR), false))
    pipeline.defaultStage = PipelineStage(
        "default", Sorting.NO_SORTING, 0, null, DepthMode.ALWAYS, true,
        CullMode.BOTH, pbrModelShader
    )

    val prefab = PrefabCache[source]!!
    val scene = prefab.createInstance() as Entity

    scene.validateTransform()
    scene.validateAABBs()

    val aabb = scene.aabb

    val cameraPosition = Vector3d(aabb.avgX(), aabb.avgY(), aabb.maxZ * 1.5f)
    val cameraRotation = Quaterniond()
    val worldScale = 1.0 // used in Rem's Engine for astronomic scales

    pipeline.frustum.setToEverything(cameraPosition, cameraRotation)
    pipeline.fill(scene, cameraPosition, worldScale)

    if (true) {// duplicate object 25 times for testing
        val dx = aabb.deltaX() * 1.1
        val dy = 0.0
        val dz = aabb.deltaZ() * 1.1
        for (i in 0 until 5) {
            for (j in 0 until 5) {
                if (i + j > 0) {
                    val scene2 = prefab.createInstance() as Entity
                    // clone object to test mesh duplication
                    scene2.transform
                        .translateLocal(dx * i, dy, dz * j)
                        .rotateYLocal(0.2 * i)
                        .rotateXLocal(0.2 * j)
                    scene2.validateTransform()
                    scene2.validateAABBs()
                    pipeline.fill(scene2, cameraPosition, worldScale)
                }
            }
        }
    }

    val tlas = BVHBuilder.buildTLAS(pipeline.defaultStage, cameraPosition, worldScale, SplitMethod.MEDIAN, maxNodeSize)
    return Quad(tlas, Vector3f().set(cameraPosition), Quaternionf(cameraRotation), 0.2f)

}

const val random = "uint seed = initRand(pixelId, uint(frameIndex));\n"

const val shading = "" +
        "   float alpha = 1.0 / float(frameIndex + 1);\n" +
        "   float distance = Infinity;\n" +
        "   vec3 pos = worldPos, dir = worldDir;\n" +
        "   vec3 normal = vec3(0.0), normal0 = worldDir;\n" +
        "   vec3 color = vec3(1.0);\n" +
        //
        "if(drawMode==0){\n" +
        "   float roughness = 0.1;\n" +
        "   int limit = 8;" +
        "   for(int i=ZERO;i<limit;i++){\n" +
        "       distance = Infinity;\n" +
        "       intersectTLAS(pos,dir,distance,normal);\n" +
        "       if(distance < Infinity){\n" +
        // todo textures, different materials, ...
        // mix color with hit color (at the moment: light gray)
        "           color *= 0.95;\n" +
        // continue raytracing by readjusting pos & normal
        // find new ray direction depending on BRDF
        "           pos = pos + dir * distance;\n" +
        // diffuse:
        "           dir = normalize(normal + nextRandS3(seed));\n" +
        // metallic:
        // "           dir = normalize(reflect(dir, normalize(normal)) + roughness * nextRandS3(seed));\n" +
        // add small offset, so we don't have shadow acne
        "           pos += dir * 0.01;\n" +
        "           if(i==limit-1) color = vec3(0.0);\n" +
        "           normal0 = normal;\n" +
        "       } else {\n" +
        // we hit the sky -> query the sky color
        "           normal = normalize(normal0);\n" +
        "           color *= 20.0 * mix(sky0,sky1,normal.y*.5+.5);\n" +
        "           break;\n" +
        "       }\n" +
        "   }\n" +
        "} else {" +
        "uint a = intersectTLAS(pos,dir,distance,normal);\n" +
        "if(drawMode==1) {\n" +
        // simple coloring
        "   if(dot(normal,normal)>0.0){\n" +
        "      normal = normalize(normal);\n" +
        "      color = normal*.5+.5;\n" +
        "   } else {\n" +
        // compute sky color
        "       color = mix(sky0,sky1,dir.y*.5+.5);\n" +
        "   }\n" +
        "} else {\n" +
        "   color = coloring(float(a)*0.1);\n" +
        "}}\n"

fun createGraphicsShader(tlas: TLASNode): Quad<Shader, Texture2D, Texture2D, Texture2D> {

    val uniqueMeshes = HashSet<BLASNode>(tlas.countTLASLeaves())
    tlas.forEach {
        if (it is TLASLeaf) {
            uniqueMeshes.add(it.mesh)
        }
    }

    val meshes = uniqueMeshes
        .sortedByDescending { it.countNodes() } // complex meshes first for testing and consistency
        .toList()

    val triangles = createTriangleTexture(meshes)
    val blasNodes = createBLASTexture(meshes)
    val tlasNodes = tlas.createTLASTexture() // needs to be created after blas nodes

    triangles.write(desktop.getChild("bvh/sponza-tri.png"), false, withAlpha = false)
    blasNodes.write(desktop.getChild("bvh/sponza-blas.png"), false, withAlpha = false)
    tlasNodes.write(desktop.getChild("bvh/sponza-tlas.png"), false, withAlpha = false)

    val maxTLASDepth = tlas.maxDepth()
    val maxBLASDepth = meshes.maxOf { it.maxDepth() }

    return Quad(
        Shader(
            "bvh-traversal", coordsList, coordsVShader, uvList, listOf(), "" +
                    "out vec4 dst;\n" +
                    "uniform sampler2D triangles, blasNodes, tlasNodes;\n" +
                    "uniform ivec2 size;\n" +
                    "uniform vec3 worldPos;\n" +
                    "uniform vec4 worldRot;\n" +
                    "uniform vec3 cameraOffset;\n" +
                    "uniform vec3 sky0, sky1;\n" +
                    "uniform int drawMode;\n" +
                    "uniform int frameIndex;\n" +
                    "uniform int ZERO;\n" +
                    "#define Infinity 1e15\n" + // don't make too large, we need to be able to calculate stuff with it
                    glslIntersections +
                    quatRot +
                    boundingBoxSDF + // for debugging
                    "#define nodes blasNodes\n" +
                    "#define BLAS_DEPTH $maxBLASDepth\n" +
                    "#define TLAS_DEPTH $maxTLASDepth\n" +
                    loadMat4x3 +
                    glslGraphicsDefines +
                    glslBLASIntersection +
                    glslTLASIntersection +
                    glslRandomGen +
                    coloring +
                    "void main(){\n" +
                    "   ivec2 uv = ivec2(gl_FragCoord.xy);\n" +
                    "   uint pixelId = uint(gl_FragCoord.x + gl_FragCoord.y * size.x);\n" +
                    random +
                    "   vec3 worldDir = vec3(vec2(uv)+(nextRand2(seed)-0.5)-cameraOffset.xy, cameraOffset.z);\n" +
                    "   worldDir = normalize(quatRot(worldDir, worldRot));\n" +
                    shading +
                    "   dst = vec4(color, alpha);\n" +
                    "}\n"
        ).apply {
            glslVersion = 330 // for floatBitsToUint
            setTextureIndices("triangles", "blasNodes", "tlasNodes")
        }, triangles, blasNodes, tlasNodes
    )

}

fun createComputeShader(tlas: TLASNode): Quad<ComputeShader, Texture2D, Texture2D, Texture2D> {

    val uniqueMeshes = HashSet<BLASNode>(tlas.countTLASLeaves())
    tlas.forEach {
        if (it is TLASLeaf) {
            uniqueMeshes.add(it.mesh)
        }
    }

    val meshes = uniqueMeshes
        .sortedByDescending { it.countNodes() } // complex meshes first for testing and consistency
        .toList()

    val triangles = createTriangleTexture(meshes)
    val blasNodes = createBLASTexture(meshes)
    val tlasNodes = tlas.createTLASTexture() // needs to be created after blas nodes

    // triangles.write(desktop.getChild("bvh/sponza-tri.png"), false, withAlpha = false)
    // blasNodes.write(desktop.getChild("bvh/sponza-blas.png"), false, withAlpha = false)
    // tlasNodes.write(desktop.getChild("bvh/sponza-tlas.png"), false, withAlpha = false)

    val maxTLASDepth = tlas.maxDepth()
    val maxBLASDepth = meshes.maxOf { it.maxDepth() }

    LOGGER.debug("Max TLAS depth: $maxTLASDepth, max BLAS depth: $maxBLASDepth")

    return Quad(
        ComputeShader(
            "bvh-traversal", Vector2i(16), "" +
                    "layout(rgba32f, binding = 0) uniform image2D triangles;\n" +
                    "layout(rgba32f, binding = 1) uniform image2D blasNodes;\n" +
                    "layout(rgba32f, binding = 2) uniform image2D tlasNodes;\n" +
                    "layout(rgba32f, binding = 3) uniform image2D dst;\n" +
                    "uniform ivec2 size;\n" +
                    "uniform vec3 worldPos;\n" +
                    "uniform vec4 worldRot;\n" +
                    "uniform vec3 cameraOffset;\n" +
                    "uniform vec3 sky0, sky1;\n" +
                    "uniform int drawMode;\n" +
                    "uniform int frameIndex;\n" +
                    "uniform int ZERO;\n" +
                    "#define Infinity 1e15\n" + // don't make too large, we need to be able to calculate stuff with it
                    glslIntersections +
                    quatRot +
                    boundingBoxSDF + // for debugging
                    "#define nodes blasNodes\n" +
                    "#define BLAS_DEPTH $maxBLASDepth\n" +
                    "#define TLAS_DEPTH $maxTLASDepth\n" +
                    loadMat4x3 +
                    glslComputeDefines +
                    glslBLASIntersection +
                    glslTLASIntersection +
                    glslRandomGen +
                    coloring +
                    "void main(){\n" +
                    "   ivec2 uv = ivec2(gl_GlobalInvocationID.xy);\n" +
                    "   if(all(lessThan(uv,size))){\n" +
                    "       uint pixelId = uint(gl_GlobalInvocationID.x + gl_GlobalInvocationID.y * size.x);\n" +
                    random +
                    "       vec3 worldDir = vec3(vec2(uv)+(nextRand2(seed)-0.5)-cameraOffset.xy, cameraOffset.z);\n" +
                    "       worldDir = normalize(quatRot(worldDir, worldRot));\n" +
                    shading +
                    "       vec3 oldColor = imageLoad(dst, uv).rgb;\n" +
                    "       color = mix(oldColor, color, alpha);\n" +
                    "       imageStore(dst, uv, vec4(color, 1.0));\n" +
                    "   }\n" +
                    "}\n"
        ), triangles, blasNodes, tlasNodes
    )
}

fun createComputeShaderV2(tlas: TLASNode): Quad<ComputeShader, ComputeBuffer, ComputeBuffer, ComputeBuffer> {

    val uniqueMeshes = HashSet<BLASNode>(tlas.countTLASLeaves())
    tlas.forEach {
        if (it is TLASLeaf) {
            uniqueMeshes.add(it.mesh)
        }
    }

    val meshes = uniqueMeshes
        .sortedByDescending { it.countNodes() } // complex meshes first for testing and consistency
        .toList()

    val triangles = createTriangleBuffer(meshes)
    val blasNodes = createBLASBuffer(meshes)
    val tlasNodes = tlas.createTLASBuffer() // needs to be created after blas nodes

    val maxTLASDepth = tlas.maxDepth()
    val maxBLASDepth = meshes.maxOf { it.maxDepth() }

    LOGGER.debug("Max TLAS depth: $maxTLASDepth, max BLAS depth: $maxBLASDepth")

    val shader = ComputeShader(
        "bvh-traversal", Vector2i(16), "" +
                "struct Vertex {\n" +
                "   vec3 pos;\n" +
                "   uint _pad0;\n" +
                "   vec3 nor;\n" +
                "   uint color;\n" +
                "};\n" +
                "struct BLASNode {\n" +
                "   vec3 min;\n" +
                "   uint v0;\n" +
                "   vec3 max;\n" +
                "   uint v1;\n" +
                "};\n" +
                "struct TLASNode {\n" +
                "   vec3    min;\n" +
                "   uint    v0;\n" +
                "   vec3    max;\n" +
                "   uint    v1;\n" +
                "   mat4x3 worldToLocal;\n" +
                "   mat4x3 localToWorld;\n" +
                "};\n" +
                // std430 needed? yes, core since 4.3
                "layout(std140, shared, binding = 0) readonly buffer triangles  { Vertex vertices[]; };\n" +
                "layout(std140, shared, binding = 1) readonly buffer blasBuffer { BLASNode blasNodes[]; };\n" +
                "layout(std140, shared, binding = 2) readonly buffer tlasBuffer { TLASNode tlasNodes[]; };\n" +
                "layout(rgba32f, binding = 3) uniform image2D dst;\n" +
                "uniform ivec2 size;\n" +
                "uniform vec3 worldPos;\n" +
                "uniform vec4 worldRot;\n" +
                "uniform vec3 cameraOffset;\n" +
                "uniform vec3 sky0, sky1;\n" +
                "uniform int drawMode;\n" +
                "uniform int frameIndex;\n" +
                "uniform int ZERO;\n" +
                "#define Infinity 1e15\n" + // don't make too large, we need to be able to calculate stuff with it
                glslIntersections +
                quatRot +
                boundingBoxSDF + // for debugging
                "#define BLAS_DEPTH $maxBLASDepth\n" +
                "#define TLAS_DEPTH $maxTLASDepth\n" +
                // glslComputeDefines +
                glslBLASIntersection2 +
                glslTLASIntersection2 +
                glslRandomGen +
                coloring +
                "void main(){\n" +
                "   ivec2 uv = ivec2(gl_GlobalInvocationID.xy);\n" +
                "   if(all(lessThan(uv,size))){\n" +
                "       uint pixelId = uint(gl_GlobalInvocationID.x + gl_GlobalInvocationID.y * size.x);\n" +
                random +
                "       vec3 worldDir = vec3(vec2(uv)+(nextRand2(seed)-0.5)-cameraOffset.xy, cameraOffset.z);\n" +
                "       worldDir = normalize(quatRot(worldDir, worldRot));\n" +
                shading +
                "       vec3 oldColor = imageLoad(dst, uv).rgb;\n" +
                "       color = mix(oldColor, color, alpha);\n" +
                "       imageStore(dst, uv, vec4(color, 1.0));\n" +
                "   }\n" +
                "}\n"
    )

    return Quad(shader, triangles, blasNodes, tlasNodes)
}

fun render(
    shader: ComputeShader, w: Int, h: Int,
    cameraPosition: Vector3f, cameraRotation: Quaternionf, cx: Float, cy: Float, fovZ: Float,
    triangles: Texture2D, blasNodes: Texture2D, tlasNodes: Texture2D, result: Texture2D,
    clock: Clock?
) {

    shader.use()
    shader.v2i("size", w, h)
    shader.v3f("worldPos", cameraPosition)
    shader.v4f("worldRot", cameraRotation)
    shader.v3f("cameraOffset", cx, cy, fovZ)
    shader.v3f("sky0", sky0)
    shader.v3f("sky1", sky1)
    shader.bindTexture(0, triangles, ComputeTextureMode.READ)
    shader.bindTexture(1, blasNodes, ComputeTextureMode.READ)
    shader.bindTexture(2, tlasNodes, ComputeTextureMode.READ)
    shader.bindTexture(3, result, ComputeTextureMode.WRITE)
    shader.runBySize(w, h) // test it + warm up

    if (clock != null) {
        GL11C.glFlush()
        GL11C.glFinish()
        clock.start()
        shader.runBySize(w, h)
        GL11C.glFlush()
        GL11C.glFinish()
        clock.stop("gpu")
    }

}

fun main() {

    val w = 512
    val h = w * 3 / 4

    val maxNodeSize = 16

    // 2048x1536
    val cpuBVH = true // 2150 ns/pixel on 8 tri/node max; 1705 ns/pixel on 4 or 2 incl. png encoding
    val gpuBVH = true

    if (gpuBVH) {
        ECSRegistry.initWithGFX(w, h)
    } else {
        ECSRegistry.initNoGFX()
    }

    val (tlas, cameraPosition, cameraRotation, fovZFactor) = createSampleTLAS(maxNodeSize)

    val cx = (w - 1) * 0.5
    val cy = (h - 1) * 0.5
    val fovZ = -w.toDouble() * fovZFactor

    val clock = Clock()
    clock.minTime = 0.0

    if (cpuBVH) {
        clock.start()
        renderOnCPU(w, h, "bvh/sponza-cpu-tlas.png", cx, cy, fovZ, cameraPosition, cameraRotation, tlas)
        clock.stop("cpu-tlas", w * h)
    }

    if (gpuBVH) {

        clock.stop("gpu-tlas", w * h)

        val result = Texture2D("colors", w, h, 1)
        result.create(TargetType.FloatTarget4)

        val (shader, tri, blas, tlas2) = createComputeShader(tlas)
        render(
            shader, w, h, cameraPosition, cameraRotation,
            cx.toFloat(), cy.toFloat(), fovZ.toFloat(),
            tri, blas, tlas2, result, clock
        )

        // save result to disk
        result.write(desktop.getChild("bvh/sponza-gpu-tlas.png"), flipY = true, withAlpha = false)

    }

    Engine.requestShutdown()

}
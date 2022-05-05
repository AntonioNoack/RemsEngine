package me.anno.maths.bvh

import me.anno.Engine
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.sdf.SDFComponent.Companion.quatRot
import me.anno.ecs.components.mesh.sdf.shapes.SDFBoundingBox.Companion.boundingBoxSDF
import me.anno.ecs.prefab.PrefabCache
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.ECSShaderLib.pbrModelShader
import me.anno.gpu.DepthMode
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.deferred.DeferredSettingsV2
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.pipeline.CullMode
import me.anno.gpu.pipeline.Pipeline
import me.anno.gpu.pipeline.PipelineStage
import me.anno.gpu.pipeline.Sorting
import me.anno.gpu.shader.ComputeShader
import me.anno.gpu.shader.ComputeTextureMode
import me.anno.gpu.texture.Texture2D
import me.anno.maths.bvh.BLASNode.Companion.createBLASTexture
import me.anno.maths.bvh.BLASNode.Companion.createTriangleTexture
import me.anno.maths.bvh.RayTracing.glslBLASIntersection
import me.anno.maths.bvh.RayTracing.glslIntersections
import me.anno.maths.bvh.TLASNode.Companion.PIXELS_PER_TLAS_NODE
import me.anno.utils.Clock
import me.anno.utils.OS.desktop
import me.anno.utils.OS.documents
import me.anno.utils.OS.downloads
import me.anno.utils.structures.tuples.Quad
import me.anno.utils.types.AABBs.avgX
import me.anno.utils.types.AABBs.avgY
import org.joml.*
import org.lwjgl.opengl.GL11C

fun createSampleTLAS(maxNodeSize: Int): Quad<TLASNode, Vector3f, Quaternionf, Float> {

    // create a scene, so maybe load Sponza, and then execute our renderer on TLAS
    val sources = listOf(
        downloads.getChild("ogldev-source/crytek_sponza/sponza.obj"),
        downloads.getChild("ogldev-source/dabrovic-sponza/sponza.obj"),
        downloads.getChild("ogldev-source/conference-room/conference.obj"),
        documents.getChild("TestScene4.fbx"),
        downloads.getChild("3d/XYZ arrows.obj")
    )

    val source = sources[2]
    val pipeline = Pipeline(DeferredSettingsV2(listOf(DeferredLayerType.COLOR), false))
    pipeline.defaultStage = PipelineStage(
        "default", Sorting.NO_SORTING, 0, null, DepthMode.ALWAYS, true,
        CullMode.BOTH, pbrModelShader
    )

    val prefab = PrefabCache[source]!!
    val scene = prefab.createInstance() as Entity

    scene.validateTransform()
    scene.validateAABBs()

    /*scene.forAll {
        if(it is Entity){
            println("${it.name}, ${it.hasValidAABB}, ${it.aabb}")
        }
    }*/

    val aabb = scene.aabb

    val cameraPosition = Vector3d(aabb.avgX(), aabb.avgY(), aabb.maxZ * 1.5f)
    val cameraRotation = Quaterniond()
    val worldScale = 1.0 // used in Rem's Engine for astronomic scales

    pipeline.frustum.setToEverything(cameraPosition, cameraRotation)
    pipeline.fill(scene, cameraPosition, worldScale)

    /*for (i in 0 until 5) {
        for (j in 0 until 5) {
            if (i + j > 0) {
                val scene2 = prefab.createInstance() as Entity
                // clone object to test mesh duplication
                scene2.transform.localPosition =
                    scene2.transform.localPosition.add(aabb.deltaX() * i, 0.0, aabb.deltaZ() * j)
                scene2.transform.localRotation =
                    scene2.transform.localRotation
                        .rotateY(0.1 * i)
                        .rotateX(0.1 * j)
                scene2.transform.invalidateGlobal()
                scene2.validateTransform()
                scene2.validateAABBs()

                pipeline.fill(scene2, cameraPosition, worldScale)
            }
        }
    }*/

    val tlas = BVHBuilder.buildTLAS(pipeline.defaultStage, cameraPosition, worldScale, SplitMethod.MEDIAN, maxNodeSize)
    return Quad(tlas, Vector3f().set(cameraPosition), Quaternionf(cameraRotation), 1f)

}

fun createShader(tlas: TLASNode): Quad<ComputeShader, Texture2D, Texture2D, Texture2D> {

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
                    "#define Infinity 1e15\n" + // don't make too large, we need to be able to calculate stuff with it
                    glslIntersections +
                    quatRot +
                    boundingBoxSDF + // for debugging
                    "#define nodes blasNodes\n" +
                    "#define BLAS_DEPTH $maxBLASDepth\n" +
                    glslBLASIntersection +
                    "void main(){\n" +
                    "   ivec2 pos = ivec2(gl_GlobalInvocationID.xy);\n" +
                    "   if(all(lessThan(pos,size))){\n" +
                    "       uint nodeStack[$maxTLASDepth];\n" +
                    "       uint nodeIndex = 0;\n" +
                    "       uint stackIndex = 0;\n" +
                    "       vec3 worldNormal = vec3(0.0);\n" +
                    "       float worldDistance = Infinity;\n" +
                    "       ivec2 tlasTexSize = imageSize(tlasNodes);\n" +
                    "       vec3 worldDir = vec3(vec2(pos)-cameraOffset.xy, cameraOffset.z);\n" +
                    "       worldDir = quatRot(worldDir, worldRot);\n" +
                    "       worldDir = normalize(worldDir);\n" +
                    "       vec3 worldInvDir = 1.0 / worldDir;\n" +
                    "       uint k=0,numIntersections=0,nodeCtr=0;\n" +
                    "while(true){\n" + // start of tlas
                    // fetch tlas node data
                    "   uint pixelIndex = nodeIndex * $PIXELS_PER_TLAS_NODE;\n" +
                    "   uint nodeX = pixelIndex % tlasTexSize.x;\n" +
                    "   uint nodeY = pixelIndex / tlasTexSize.x;\n" +
                    "   vec4 d0 = imageLoad(tlasNodes, ivec2(nodeX,  nodeY));\n" +
                    "   vec4 d1 = imageLoad(tlasNodes, ivec2(nodeX+1,nodeY));\n" + // tlas bounds check
                    "   if(intersectAABB(worldPos,worldInvDir,d0.xyz,d1.xyz,worldDistance)){\n" +
                    "       uvec2 v01 = floatBitsToUint(vec2(d0.a,d1.a));\n" +
                    "       if(v01.y == 0){\n" + // tlas branch
                    // to do: check closest one first like in https://github.com/mmp/pbrt-v3/blob/master/src/accelerators/bvh.h ?
                    "           nodeStack[stackIndex++] = v01.x + nodeIndex;\n" + // mark other child for later
                    "           nodeIndex++;\n" + // search child next
                    "       } else {\n" + // tlas leaf
                    "           numIntersections++;\n" +
                    // load more data and then transform ray into local coordinates
                    "           vec4 d10 = imageLoad(tlasNodes, ivec2(nodeX+2,nodeY));\n" +
                    "           vec4 d11 = imageLoad(tlasNodes, ivec2(nodeX+3,nodeY));\n" +
                    "           vec4 d12 = imageLoad(tlasNodes, ivec2(nodeX+4,nodeY));\n" +
                    "           mat4x3 worldToLocal = mat4x3(d10,d11,d12);\n" +
                    // transform ray into local coordinates
                    "           vec3 localPos = worldToLocal * vec4(worldPos, 1.0);\n" +
                    "           vec3 localDir = normalize(worldToLocal * vec4(worldDir, 0.0));\n" +
                    "           vec3 localInvDir = 1.0 / localDir;\n" +
                    // transform world distance into local coordinates
                    "           float localDistance = worldDistance * length(worldToLocal * vec4(worldDir, 0.0));\n" +
                    "           float localDistanceOld = localDistance;\n" +
                    "           vec3 localNormal;\n" +
                    "           intersectBLAS(v01.x, localPos, localDir, localInvDir, localNormal, localDistance, nodeCtr);\n" +
                    "           if(localDistance < localDistanceOld){\n" + // we hit something
                    "               vec4 d20 = imageLoad(tlasNodes, ivec2(nodeX+5,nodeY));\n" +
                    "               vec4 d21 = imageLoad(tlasNodes, ivec2(nodeX+6,nodeY));\n" +
                    "               vec4 d22 = imageLoad(tlasNodes, ivec2(nodeX+7,nodeY));\n" +
                    "               mat4x3 localToWorld = mat4x3(d20,d21,d22);\n" +
                    // transform result into global coordinates
                    // theoretically we could get z-fighting here
                    "               float worldDistance1 = localDistance * length(localToWorld * vec4(localDir, 0.0));\n" +
                    "               if(worldDistance1 < worldDistance){\n" + // could be false by numerical errors
                    // transform hit normal into world coordinates
                    "                   worldDistance = worldDistance1;\n" +
                    "                   worldNormal = localToWorld * vec4(localNormal, 0.0);\n" +
                    "               }\n" +
                    "           }\n" + // end of blas; get next tlas node*/
                    "           if(stackIndex < 1) break;\n" +
                    "           nodeIndex = nodeStack[--stackIndex];\n" +
                    "       }\n" +
                    "   } else {\n" + // next tlas node
                    "       if(stackIndex < 1) break;\n" +
                    "       nodeIndex = nodeStack[--stackIndex];\n" +
                    "   }\n" +
                    "}\n" + // end of tlas
                    // save result to texture
                    "       vec3 result = vec3(float(nodeCtr)*0.01);\n" +
                    "       if(drawMode == 0) if(dot(worldNormal,worldNormal)>0.0){\n" +
                    "           worldNormal = normalize(worldNormal);\n" +
                    "           result = worldNormal*.5+.5;\n" +
                    "       } else {\n" +
                    // compute sky color
                    "           float f = length(worldDir.xy);\n" +
                    "           result = sky1;//sqrt(mix(sky0*sky0,sky1*sky1,f));\n" +
                    "       }\n" +
                    "       imageStore(dst, pos, vec4(result, 1.0));\n" +
                    "   }\n" +
                    "}\n"
        ), triangles, blasNodes, tlasNodes
    )

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

        val (shader, tri, blas, tlas2) = createShader(tlas)
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
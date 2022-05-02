package me.anno.maths.bvh

import me.anno.Engine
import me.anno.ecs.Entity
import me.anno.ecs.components.cache.MeshCache
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ECSRegistry
import me.anno.engine.raycast.RayHit
import me.anno.engine.raycast.Raycast
import me.anno.gpu.copying.FramebufferToMemory
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.ComputeShader
import me.anno.gpu.shader.ComputeTextureMode
import me.anno.gpu.texture.Texture2D
import me.anno.image.ImageWriter
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.length
import me.anno.maths.Maths.mixARGB2
import me.anno.maths.bvh.BVHBuilder.Companion.createNodeTexture
import me.anno.maths.bvh.BVHBuilder.Companion.createTriangleTexture
import me.anno.maths.bvh.RayTracing.glslIntersections
import me.anno.utils.Clock
import me.anno.utils.LOGGER
import me.anno.utils.OS.desktop
import me.anno.utils.OS.downloads
import me.anno.utils.hpc.ThreadLocal2
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.AABBs.avgX
import me.anno.utils.types.AABBs.avgY
import me.anno.utils.types.AABBs.avgZ
import org.joml.Quaternionf
import org.joml.Vector2i
import org.joml.Vector3d
import org.joml.Vector3f
import org.lwjgl.opengl.GL11C.glFinish
import org.lwjgl.opengl.GL11C.glFlush

val localResult = ThreadLocal2 { RayHit() }
val sky0 = 0x2f5293
val sky1 = 0x5c729b

fun renderOnCPU(
    w: Int, h: Int, name: String, cx: Double, cy: Double, fovZ: Double,
    start2: Vector3f, rotation: Quaternionf, bvh: BVHBuilder,
) {
    val dir0 = JomlPools.vec3f.create()
    dir0.set(cx, cy, fovZ).normalize()
    val maxZ = length(dir0.x, dir0.y)
    ImageWriter.writeImageInt(
        w, h, false, name, 64
    ) { x, y, _ ->
        val direction = JomlPools.vec3f.create()
        direction.set(x - cx, cy - y, fovZ).normalize()
        val dirX = direction.x
        val dirY = direction.y
        rotation.transform(direction)
        val result = localResult.get()
        val maxDistance = 1e20
        result.distance = maxDistance
        bvh.intersect(start2, direction, result)
        val color = if (result.distance < maxDistance) {
            val normal = result.normalWS
            normal.normalize()
            (normal.x * 60 + 150).toInt() * 0x10101
        } else mixARGB2(sky0, sky1, length(dirX, dirY) / maxZ)
        JomlPools.vec3f.sub(1)
        color
    }
}

fun main() {

    // done render bunny with and without bvh, and measure time difference
    // done execute on cpu, then on gpu :)

    // with max node size (all on 2048 x 1536) = 256 -> 4, bunny
    val cpuRaw = false // 21k ns/pixel
    val cpuBVH = true // 910 -> 350 ns/pixel
    val gpuRaw = false // 240-210 ns/pixel
    val gpuBVH = true // 128 -> 2 🤯 ns/pixel

    val maxNodeSize = 4

    val w = 2048
    val h = w * 3 / 4

    if (gpuRaw || gpuBVH) {
        ECSRegistry.initWithGFX(w, h)
    } else {
        ECSRegistry.initNoGFX()
    }

    val source = downloads.getChild("3d/bunny.obj")
    val mesh = MeshCache[source]!!

    LOGGER.info("model has ${mesh.numTriangles} triangles")

    val meshComponent = MeshComponent()
    meshComponent.mesh = source
    val entity = Entity()

    mesh.ensureBuffer()

    val fovZ = -w * 5.0
    val start = Vector3d(0.0, 0.0, 1.2).add(
        mesh.aabb.avgX().toDouble(), mesh.aabb.avgY().toDouble(), mesh.aabb.avgZ().toDouble()
    )
    val start2 = Vector3f().set(start)

    val maxDistance = 1e20

    val cx = (w - 1) * 0.5
    val cy = (h - 1) * 0.5

    val clock = Clock()

    if (cpuRaw) {
        ImageWriter.writeImageInt(
            w, h, false, "bvh/cpu-raw.png", 64
        ) { x, y, _ ->
            val direction = JomlPools.vec3d.create()
            val end = JomlPools.vec3d.create()
            direction.set(x - cx, cy - y, fovZ).normalize()
            end.set(direction).mul(maxDistance).add(start)
            val result = localResult.get()
            result.distance = maxDistance
            val color = if (Raycast.raycastTriangleMesh(
                    entity, meshComponent, mesh, start, direction, end, 0.0, 0.0, result
                )
            ) {
                val normal = result.normalWS
                normal.normalize()
                (normal.x * 60 + 150).toInt() * 0x10101
            } else mixARGB2(sky0, sky1, 10f * length(direction.x.toFloat(), direction.y.toFloat()))
            JomlPools.vec3d.sub(2)
            color
        }
        clock.stop("cpu-raw", w * h)
    }

    val bvh = BVHBuilder.buildBLAS(mesh, SplitMethod.MEDIAN, maxNodeSize)!!
    if (cpuBVH) {
        // bvh.print()
        val rotation = Quaternionf()
        clock.start()
        renderOnCPU(w, h, "bvh/cpu-bvh.png", cx, cy, fovZ, start2, rotation, bvh)
        clock.stop("cpu-bvh", w * h)
    }

    // we could use https://www.khronos.org/opengl/wiki/Shader_Storage_Buffer_Object SSBOs instead of textures
    if (gpuRaw || gpuBVH) {
        val triangles = createTriangleTexture(bvh)
        val nodes = createNodeTexture(bvh)
        val result = Texture2D("colors", w, h, 1)
        result.create(TargetType.FloatTarget4)
        val maxDepth = bvh.maxDepth()
        fun render(useBVH: Boolean, name: String) {
            val shader = ComputeShader(
                "bvh-traversal", Vector2i(32), "" +
                        "layout(rgba32f, binding = 0) uniform image2D triangles;\n" +
                        "layout(rgba32f, binding = 1) uniform image2D nodes;\n" +
                        "layout(rgba32f, binding = 2) uniform image2D dst;\n" +
                        "uniform ivec2 size;\n" +
                        "uniform vec3 cameraPosition;\n" +
                        "uniform vec3 cameraOffset;\n" +
                        "uniform vec3 sky0, sky1;\n" +
                        "#define Infinity 1e20\n" +
                        glslIntersections +
                        "void main(){\n" +
                        "   ivec2 pos = ivec2(gl_GlobalInvocationID.xy);\n" +
                        "   if(all(lessThan(pos,size))){\n" +
                        "#define stackLimit $maxDepth\n" +
                        "       uint nextNodeStack[$maxDepth];\n" +
                        "       uint nodeIndex = 0;\n" + // root
                        "       uint stackIndex = 0;\n" +
                        "       vec3 normal = vec3(0.0);\n" +
                        "       float distance = Infinity;\n" +
                        "       ivec2 nodeTexSize = imageSize(nodes);\n" +
                        "       ivec2 triTexSize = imageSize(triangles);\n" +
                        "       vec3 dir = normalize(vec3(vec2(pos)-cameraOffset.xy, cameraOffset.z));\n" +
                        "       vec3 invDir = 1.0 / dir;\n" +
                        (if (useBVH) {
                            "" +
                                    "uint k=0;\n" +
                                    "while(k++<1024){\n" + // could be k<bvh.count() or true
                                    // fetch node data
                                    "    uint pixelIndex = nodeIndex * 2;\n" + // 2 = pixels per node
                                    "    uint nodeX = pixelIndex % nodeTexSize.x;\n" +
                                    "    uint nodeY = pixelIndex / nodeTexSize.x;\n" +
                                    "    vec4 d0 = imageLoad(nodes, ivec2(nodeX,nodeY));\n" +
                                    "    vec4 d1 = imageLoad(nodes, ivec2(nodeX+1,nodeY));\n" +
                                    // bounds check
                                    "    if(intersectAABB(cameraPosition,invDir,d0.xyz,d1.xyz,distance)){\n" +
                                    "        uvec2 v01 = floatBitsToUint(vec2(d0.a,d1.a));\n" +
                                    "        if(v01.y == 0){\n" +
                                    // to do: check closest one first like in https://github.com/mmp/pbrt-v3/blob/master/src/accelerators/bvh.h ?
                                    "            nextNodeStack[stackIndex++] = v01.x + nodeIndex;\n" + // mark other child for later
                                    "            nodeIndex++;\n" + // search child next
                                    "        } else {\n" +
                                    // this node is a leaf
                                    // check all triangles for intersections
                                    "            uint start = v01.x, end = min(start+v01.y,${mesh.numTriangles});\n" +
                                    "            for(uint triangleIndex=start;triangleIndex<end;triangleIndex++){\n" + // triangle index -> load triangle data
                                    "                pixelIndex = triangleIndex * 3;\n" + // 3 = pixels per triangle
                                    "                uint triX = pixelIndex % triTexSize.x;\n" +
                                    "                uint triY = pixelIndex / triTexSize.x;\n" +
                                    "                vec3 p0 = imageLoad(triangles, ivec2(triX,triY)).rgb;\n" +
                                    "                vec3 p1 = imageLoad(triangles, ivec2(triX+1,triY)).rgb;\n" +
                                    "                vec3 p2 = imageLoad(triangles, ivec2(triX+2,triY)).rgb;\n" +
                                    "                intersectTriangle(cameraPosition, dir, p0, p1, p2, normal, distance);\n" +
                                    "            }\n" + // next node
                                    "            if(stackIndex < 1) break;\n" +
                                    "            nodeIndex = nextNodeStack[--stackIndex];\n" +
                                    "        }\n" +
                                    "    } else {\n" + // next node
                                    "        if(stackIndex < 1) break;\n" +
                                    "        nodeIndex = nextNodeStack[--stackIndex];\n" +
                                    "    }\n" +
                                    "}\n"
                        } else {
                            "" +
                                    "for(uint triangleIndex=0;triangleIndex<${mesh.numTriangles};triangleIndex++){\n" + // triangle index
                                    // load triangle data
                                    "    uint pixelIndex = triangleIndex * 3;\n" + // 3 = pixels per triangle
                                    "    uint triX = pixelIndex % triTexSize.x;\n" +
                                    "    uint triY = pixelIndex / triTexSize.x;\n" +
                                    "    vec3 p0 = imageLoad(triangles, ivec2(triX,triY)).rgb;\n" +
                                    "    vec3 p1 = imageLoad(triangles, ivec2(triX+1,triY)).rgb;\n" +
                                    "    vec3 p2 = imageLoad(triangles, ivec2(triX+2,triY)).rgb;\n" +
                                    "    intersectTriangle(cameraPosition, dir, p0, p1, p2, normal, distance);\n" +
                                    "}\n"
                        }) +
                        // save result to texture
                        "       vec3 result = vec3(0.0);\n" +
                        "       if(distance < Infinity){\n" +
                        "           normal = normalize(normal);\n" +
                        "           result = vec3(normal.x * ${60 / 255f} + ${150 / 255f});\n" +
                        "       } else {\n" +
                        // compute sky color
                        "           float f = 10.0 * length(dir.xy);\n" +
                        "           result = sqrt(mix(sky0*sky0,sky1*sky1,f));\n" +
                        "       }\n" +
                        "       imageStore(dst, pos, vec4(result, 1.0));\n" +
                        "   }\n" +
                        "}\n"
            )
            shader.use()
            shader.v2i("size", w, h)
            shader.v3f("cameraPosition", start2)
            shader.v3f("cameraOffset", cx.toFloat(), cy.toFloat(), fovZ.toFloat())
            shader.v3f("sky0", sky0)
            shader.v3f("sky1", sky1)
            shader.bindTexture(0, triangles, ComputeTextureMode.READ)
            shader.bindTexture(1, nodes, ComputeTextureMode.READ)
            shader.bindTexture(2, result, ComputeTextureMode.WRITE)
            shader.runBySize(w, h) // test it + warm up
            glFlush()
            glFinish()
            clock.start()
            shader.runBySize(w, h)
            glFlush()
            glFinish()
            clock.stop(name, w * h)
            // save result to disk
            FramebufferToMemory.createImage(result, flipY = true, withAlpha = false)
                .write(desktop.getChild("bvh/$name.png"))
        }
        if (gpuRaw) render(false, "gpu-raw")
        if (gpuBVH) render(true, "gpu-bvh")
    }

    Engine.requestShutdown()

}
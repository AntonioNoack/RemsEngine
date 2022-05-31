package me.anno.maths.bvh

import me.anno.Engine
import me.anno.ecs.Entity
import me.anno.ecs.components.cache.MeshCache
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.sdf.SDFComponent.Companion.quatRot
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
import me.anno.maths.Maths.mixARGB
import me.anno.maths.Maths.mixARGB2
import me.anno.maths.bvh.BLASNode.Companion.createBLASTexture
import me.anno.maths.bvh.BLASNode.Companion.createTriangleTexture
import me.anno.maths.bvh.RayTracing.glslBLASIntersection
import me.anno.maths.bvh.RayTracing.glslIntersections
import me.anno.maths.bvh.RayTracing.loadMat4x3
import me.anno.utils.Clock
import me.anno.utils.Color.toRGB
import me.anno.utils.OS.desktop
import me.anno.utils.OS.downloads
import me.anno.utils.hpc.ThreadLocal2
import me.anno.utils.pooling.JomlPools
import org.apache.logging.log4j.LogManager
import org.joml.Quaternionf
import org.joml.Vector2i
import org.joml.Vector3d
import org.joml.Vector3f
import org.lwjgl.opengl.GL11C.glFinish
import org.lwjgl.opengl.GL11C.glFlush
import java.io.IOException

val localResult = ThreadLocal2 { RayHit() }
const val sky0 = 0x2f5293
const val sky1 = 0x5c729b

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
        val dir = JomlPools.vec3f.create()
        dir.set(x - cx, cy - y, fovZ).normalize()
        val dirX = dir.x
        val dirY = dir.y
        rotation.transform(dir)
        val hit = localResult.get()
        hit.ctr = 0
        hit.normalWS.set(0.0)
        val maxDistance = 1e15
        hit.distance = maxDistance
        try {
            bvh.intersect(start2, dir, hit)
            val color = if (dir.x < dir.y) {
                mixARGB(0, -1, clamp(hit.ctr * 0.1f))
            } else if (hit.normalWS.lengthSquared() > 0.0) {
                val normal = hit.normalWS
                normal.normalize()
                (normal.x * 60 + 150).toInt() * 0x10101
            } else mixARGB2(sky0, sky1, length(dirX, dirY) / maxZ)
            JomlPools.vec3f.sub(1)
            color
        } catch (e: IOException) {
            hit.normalWS.toRGB()
        }
    }
}

fun createComputeShader(useBVH: Boolean, maxDepth: Int, mesh: Mesh?): ComputeShader {
    return ComputeShader(
        "bvh-traversal", Vector2i(16), "" +
                "layout(rgba32f, binding = 0) uniform image2D triangles;\n" +
                "layout(rgba32f, binding = 1) uniform image2D nodes;\n" +
                "layout(rgba32f, binding = 2) uniform image2D dst;\n" +
                "uniform ivec2 size;\n" +
                "uniform vec3 worldPos;\n" +
                "uniform vec4 worldRot;\n" +
                "uniform vec3 cameraOffset;\n" +
                "uniform vec3 sky0, sky1;\n" +
                "uniform int drawMode;\n" +
                "#define Infinity 1e15\n" +
                glslIntersections +
                quatRot +
                "#define BLAS_DEPTH $maxDepth\n" +
                loadMat4x3 +
                glslBLASIntersection +
                "void main(){\n" +
                "   uint nodeCtr=0;\n" +
                "   ivec2 pos = ivec2(gl_GlobalInvocationID.xy);\n" +
                "   if(all(lessThan(pos,size))){\n" +
                "       float distance = Infinity;\n" +
                "       vec3 normal = vec3(0.0);\n" +
                "       vec3 worldDir = vec3(vec2(pos)-cameraOffset.xy, cameraOffset.z);\n" +
                "       worldDir = quatRot(worldDir, worldRot);\n" +
                "       worldDir = normalize(worldDir);\n" +
                (if (useBVH) {
                    "" +
                            "vec3 invDir = 1.0 / worldDir;\n" +
                            "intersectBLAS(0, worldPos, worldDir, invDir, normal, distance, nodeCtr);\n"
                } else {
                    "" +
                            "ivec2 triTexSize = imageSize(triangles);\n" +
                            "for(uint triangleIndex=0;triangleIndex<${mesh!!.numTriangles};triangleIndex++){\n" + // triangle index
                            // load triangle data
                            "    uint pixelIndex = triangleIndex * 3;\n" + // 3 = pixels per triangle
                            "    uint triX = pixelIndex % triTexSize.x;\n" +
                            "    uint triY = pixelIndex / triTexSize.x;\n" +
                            "    vec3 p0 = imageLoad(triangles, ivec2(triX,triY)).rgb;\n" +
                            "    vec3 p1 = imageLoad(triangles, ivec2(triX+1,triY)).rgb;\n" +
                            "    vec3 p2 = imageLoad(triangles, ivec2(triX+2,triY)).rgb;\n" +
                            "    intersectTriangle(worldPos, worldDir, p0, p1, p2, normal, distance);\n" +
                            "}\n"
                }) +
                // save result to texture
                "       vec3 result = vec3(float(nodeCtr)*0.01);\n" +
                "       if(drawMode == 0) if(distance < Infinity){\n" +
                "           normal = normalize(normal);\n" +
                "           result = vec3(normal.x * ${60 / 255f} + ${150 / 255f});\n" +
                "       } else {\n" +
                // compute sky color
                "           float f = 10.0 * length(worldDir.xy);\n" +
                "           result = sky1;//sqrt(mix(sky0*sky0,sky1*sky1,f));\n" +
                "       }\n" +
                "       imageStore(dst, pos, vec4(result, 1.0));\n" +
                "   }\n" +
                "}\n"
    )
}

fun main() {

    val logger = LogManager.getLogger("BLASTest")

    // done render bunny with and without bvh, and measure time difference
    // done execute on cpu, then on gpu :)

    // with max node size (all on 2048 x 1536) = 256 -> 4, bunny
    val cpuRaw = false // 21k ns/pixel
    val cpuBVH = true // 910 -> 350 ns/pixel
    val gpuRaw = false // 240-210 ns/pixel
    val gpuBVH = true // 128 -> 2 ns/pixel, nice :D

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

    logger.info("model has ${mesh.numTriangles} triangles")

    val meshComponent = MeshComponent()
    meshComponent.mesh = source
    val entity = Entity()

    mesh.ensureBuffer()

    val fovZ = -w * 5.0
    val start = Vector3d(
        mesh.aabb.avgX().toDouble(),
        mesh.aabb.avgY().toDouble(),
        mesh.aabb.avgZ().toDouble() + 1.2
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
        val nodes = createBLASTexture(bvh)
        triangles.write(desktop.getChild("bvh/tri.png"), flipY = false, withAlpha = false)
        nodes.write(desktop.getChild("bvh/blas.png"), flipY = false, withAlpha = false)
        val result = Texture2D("colors", w, h, 1)
        result.create(TargetType.FloatTarget4)
        val maxDepth = bvh.maxDepth()
        fun render(useBVH: Boolean, name: String) {
            val shader = createComputeShader(useBVH, maxDepth, mesh)
            shader.use()
            shader.v2i("size", w, h)
            shader.v3f("worldPos", start2)
            shader.v4f("worldDir", Quaternionf())
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
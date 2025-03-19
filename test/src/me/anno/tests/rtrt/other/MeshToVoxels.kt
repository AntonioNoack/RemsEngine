package me.anno.tests.rtrt.other

import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshCache
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Materials
import me.anno.ecs.components.mesh.material.Texture3DBTv2Material
import me.anno.engine.WindowRenderFlags
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.ECSShaderLib.pbrModelShader
import me.anno.engine.ui.render.Renderers.attributeRenderers
import me.anno.engine.ui.render.SceneView.Companion.testScene
import me.anno.gpu.DepthMode
import me.anno.gpu.GFXState
import me.anno.gpu.deferred.DeferredLayerType
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.Framebuffer3D
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.ComputeShader
import me.anno.gpu.shader.ComputeTextureMode
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.renderer.Renderer
import me.anno.gpu.texture.Texture3D
import me.anno.image.thumbs.AssetThumbHelper
import me.anno.image.thumbs.AssetThumbHelper.doneCondition
import me.anno.image.thumbs.AssetThumbHelper.getEndTime
import me.anno.image.thumbs.AssetThumbHelper.removeMissingFiles
import me.anno.io.files.FileReference
import me.anno.maths.Maths.PIf
import me.anno.maths.Maths.max
import me.anno.maths.Maths.min
import me.anno.maths.Maths.mix
import me.anno.mesh.Shapes.smoothCube
import me.anno.ui.debug.TestEngine.Companion.testUI
import me.anno.utils.Clock
import me.anno.utils.OS.downloads
import me.anno.utils.Sleep
import org.joml.AABBf
import org.joml.Matrix4f
import org.joml.Matrix4x3
import org.joml.Vector3f
import org.joml.Vector3i
import kotlin.math.cbrt
import kotlin.math.ceil
import kotlin.math.pow

// interesting applications for oct-trees:
// https://developer.nvidia.com/gpugems/gpugems2/part-v-image-oriented-computing/chapter-37-octree-textures-gpu

fun meshToVoxels(
    mesh: Mesh, renderer: Renderer, blocksX: Int, blocksY: Int, blocksZ: Int,
    waitForTextures: Boolean
): Texture3D {
    val dataXYZ = Framebuffer3D("3d", blocksX, blocksY, blocksZ, listOf(TargetType.UInt8x4), DepthBufferType.NONE)
    val dataZYX = Framebuffer3D("3d", blocksZ, blocksY, blocksX, listOf(TargetType.UInt8x4), DepthBufferType.NONE)
    val dataXZY = Framebuffer3D("3d", blocksX, blocksZ, blocksY, listOf(TargetType.UInt8x4), DepthBufferType.NONE)
    meshToSeparatedVoxels(mesh, renderer, blocksX, blocksY, blocksZ, dataXYZ, dataZYX, dataXZY, waitForTextures)
    val tex = mergeChannels(dataXYZ, dataZYX, dataXZY)
    dataZYX.destroy()
    dataXZY.destroy()
    dataXYZ.destroyExceptTextures(false)
    return tex
}

val mergeChannelsShader = ComputeShader(
    "merge-channels", Vector3i(8, 8, 8), listOf(
        Variable(GLSLType.V3I, "size")
    ), "" +
            "layout(rgba8, binding = 0) uniform image3D xyz;\n" +
            "layout(rgba8, binding = 1) uniform image3D zyx;\n" +
            "layout(rgba8, binding = 2) uniform image3D xzy;\n" +
            "layout(rgba8, binding = 3) uniform image3D dst;\n" +
            // better max function: use max by alpha, not combine all colors:
            //  just max() could introduce wrong colors, e.g. max(red, blue) should not become violet
            "vec4 maxByAlpha(vec4 a, vec4 b){\n" +
            "   return a.a >= b.a ? a : b;\n" +
            "}\n" +
            "void main(){\n" +
            "   ivec3 uvw = ivec3(gl_GlobalInvocationID);\n" +
            "   if(all(lessThan(uvw, size))) {\n" +
            "       imageStore(dst, uvw, maxByAlpha(maxByAlpha(\n" +
            "           imageLoad(xyz, uvw),\n" +
            "           imageLoad(zyx, ivec3(size.z-1-uvw.z,uvw.y,uvw.x))),\n" +
            "           imageLoad(xzy, ivec3(uvw.x,size.z-1-uvw.z,size.y-1-uvw.y))\n" +
            "       ));\n" +
            "   }\n" +
            "}\n"
)

fun waitForTextures(textures: Collection<FileReference>) {
    val endTime = getEndTime()
    Sleep.waitUntil(true) {
        doneCondition(textures, endTime)
    }
}

fun meshToSeparatedVoxels(
    mesh: Mesh, renderer: Renderer,
    blocksX: Int, blocksY: Int, blocksZ: Int,
    dataXYZ: Framebuffer3D,
    dataZYX: Framebuffer3D,
    dataXZY: Framebuffer3D,
    waitForTextures: Boolean
) {

    val bounds = mesh.getBounds()
    bounds.addMargin(max(bounds.deltaX, max(bounds.deltaY, bounds.deltaZ)) / max(blocksX, max(blocksY, blocksZ)))

    val transform = Matrix4f()

    // goal: rasterize into cubes quickly
    // strategy: draw slices onto textures, which then are part of a 3d texture
    // https://stackoverflow.com/questions/17504750/opengl-how-to-render-object-to-3d-texture-as-a-volumetric-billboard
    // draw mesh for XYZ, YZX, ZXY
    // sample code for xyz:
    fun drawMesh() {
        for (i in 0 until mesh.numMaterials) {
            // find shader
            val material = Materials.getMaterial(null, mesh.materials, i)
            val shader = (material.shader ?: pbrModelShader).value
            shader.use()
            // bind & prepare shader
            shader.m4x4("transform", transform)
            shader.m4x3("localTransform", null as Matrix4x3?)
            shader.m4x3("invLocalTransform", null as Matrix4x3?)
            material.bind(shader)
            // draw mesh
            mesh.draw(null, shader, i)
        }
    }
    if (waitForTextures) {
        val materials = mesh.materials
        val textures = HashSet(materials.flatMap(AssetThumbHelper::listTextures))
        removeMissingFiles(textures, mesh.ref)
        waitForTextures(textures)
    }
    GFXState.depthMode.use(DepthMode.FORWARD_ALWAYS) {
        val clock = Clock("MeshToVoxels")
        val invX = 1f / blocksX
        val invY = 1f / blocksY
        val invZ = 1f / blocksZ
        dataXYZ.draw(renderer) { z ->
            dataXYZ.clearColor(0)
            val min = -mix(bounds.minZ, bounds.maxZ, z * invZ)
            val max = -mix(bounds.minZ, bounds.maxZ, (z + 1) * invZ)
            transform
                .identity()
                .ortho(bounds.minX, bounds.maxX, bounds.minY, bounds.maxY, min, max)
            drawMesh()
        }
        dataZYX.draw(renderer) { x ->
            dataZYX.clearColor(0)
            val min = mix(bounds.minX, bounds.maxX, x * invX)
            val max = mix(bounds.minX, bounds.maxX, (x + 1) * invX)
            transform
                .identity()
                .rotateY(PIf / 2f)
            transform.ortho(min, max, bounds.minY, bounds.maxY, -bounds.maxZ, -bounds.minZ)
            drawMesh()
        }
        dataXZY.draw(renderer) { y ->
            dataXZY.clearColor(0)
            val min = mix(bounds.maxY, bounds.minY, y * invY)
            val max = mix(bounds.maxY, bounds.minY, (y + 1) * invY)
            transform
                .identity()
                .rotateX(PIf / 2f)
                .ortho(bounds.minX, bounds.maxX, min, max, -bounds.minZ, -bounds.maxZ)
            drawMesh()
        }
        clock.stop("Mesh -> Dense Voxels")
    }
}

val skipDistanceShader = ComputeShader(
    "skip-distances", Vector3i(8, 8, 8), listOf(
        Variable(GLSLType.V3I, "size"),
    ), "" +
            "layout(rgba8, binding = 0) uniform image3D data;\n" +
            "#define s 0.003921569\n" +
            "float read(ivec3 coords){\n" +
            "   if(all(greaterThanEqual(coords,ivec3(0))) && all(lessThan(coords,size))){\n" +
            "       vec4 v = imageLoad(data, coords);\n" +
            "       if(v.a > 0.0){\n" +
            "           return -s;\n" +
            "       } else {\n" +
            "           return v.x;\n" +
            "       }\n" +
            "   } else return 1.0;\n" +
            "}\n" +
            "void main(){\n" +
            "   ivec3 uvw = ivec3(gl_GlobalInvocationID);\n" +
            "   if(all(lessThan(uvw, size))) {\n" +
            "       vec4 v = imageLoad(data, uvw);\n" +
            "       if(v.a <= 0.0){\n" +
            "           float w = v.x;\n" +
            "           w = min(w, read(uvw+ivec3(1,0,0)));\n" +
            "           w = min(w, read(uvw-ivec3(1,0,0)));\n" +
            "           w = min(w, read(uvw+ivec3(0,1,0)));\n" +
            "           w = min(w, read(uvw-ivec3(0,1,0)));\n" +
            "           w = min(w, read(uvw+ivec3(0,0,1)));\n" +
            "           w = min(w, read(uvw-ivec3(0,0,1)));\n" +
            "           w += s;\n" +
            "           imageStore(data, uvw, vec4(clamp(w, 0.0, 1.0), 0.0, 0.0, 0.0));\n" +
            "       }\n" +
            "   }\n" +
            "}\n"
)

fun calculateSkipDistances(data: Texture3D) {
    val shader = skipDistanceShader
    shader.use()
    shader.v3i("size", data.width, data.height, data.depth)
    shader.bindTexture(0, data, ComputeTextureMode.READ_WRITE)
    for (i in 0 until min(max(max(data.width, data.height), data.depth) / 2, 255)) {
        shader.runBySize(data.width, data.height, data.depth)
        // is this correct / needed? idk...
        // glMemoryBarrier(GL_TEXTURE_UPDATE_BARRIER_BIT)
    }
}

fun mergeChannels(
    blocksX: Int, blocksY: Int, blocksZ: Int,
    dataXYZ: Texture3D,
    dataZYX: Texture3D,
    dataXZY: Texture3D,
    dst: Texture3D = dataXYZ
): Texture3D {
    // merge all channels
    val shader = mergeChannelsShader.apply { }
    shader.use()
    shader.v3i("size", blocksX, blocksY, blocksZ)
    shader.bindTexture(0, dataXYZ, ComputeTextureMode.READ)
    shader.bindTexture(1, dataZYX, ComputeTextureMode.READ)
    shader.bindTexture(2, dataXZY, ComputeTextureMode.READ)
    shader.bindTexture(3, dst, ComputeTextureMode.WRITE)
    shader.runBySize(blocksX, blocksY, blocksZ)
    return dst
}

fun mergeChannels(
    dataXYZ: Framebuffer3D,
    dataZYX: Framebuffer3D,
    dataXZY: Framebuffer3D,
    dst: Texture3D = dataXYZ.getTexture0()
): Texture3D = mergeChannels(
    dataXYZ.width, dataXYZ.height, dataXYZ.depth,
    dataXYZ.getTexture0(), dataZYX.getTexture0(), dataXZY.getTexture0(), dst
)

/**
 * Rasterize a mesh on the GPU in 3D
 * */
fun main() {

    OfficialExtensions.initForTests()

    // convert triangle mesh into voxels quickly

    // https://developer.nvidia.com/gpugems/gpugems2/part-v-image-oriented-computing/chapter-37-octree-textures-gpu

    // first triangles -> full cuboid
    // target: 256³ = 16M voxels
    // oct-tree-node:
    // 8x [child / null]
    // oct-tree-leaf:
    // color, normal?, material type / roughness / metallic
    // time limit: 1s
    val file = downloads.getChild("3d/DamagedHelmet.glb")
    val mesh = MeshCache[file]!!

    val bounds = AABBf(mesh.getBounds())
    val resolution = 512
    // this does not work with very flat models! ->
    // maybe now...
    val dx = bounds.deltaX
    val dy = bounds.deltaY
    val dz = bounds.deltaZ
    val dm = max(max(dx, dy), dz) / resolution
    bounds.addMargin(dm)

    val volume = max(dx, dm) * max(dy, dm) * max(dz, dm)
    val budget = resolution.toFloat().pow(3)
    val scale = cbrt(budget / volume)
    val blocksX = max(1, ceil(dx * scale).toInt())
    val blocksY = max(1, ceil(dy * scale).toInt())
    val blocksZ = max(1, ceil(dz * scale).toInt())

    println("size: $blocksX x $blocksY x $blocksZ")

    /*val buildOctTree = ComputeShader(
        "oct-tree", Vector3i(8, 8, 8), "" +
                "readonly sampler3D xyz, yzx, zxy;\n" +
                "" +
                "void main(){\n" +
                "   ivec3 uvw = ivec3(gl_GlobalInvocationID);\n" +
                "   if(all(lessThan(uvw, size)){\n" +
                "       int step = step0;\n" +
                "       for(;step>=1;step >>= 1){\n" +
                // todo manage node at uvw: create or discard
                "           if((uvw & step) == ivec3(0)){\n" +
                "               " +
                "           }\n" +
                "       }\n" +
                "   }\n" +
                "}\n"
    )

    fun compress() {
        // todo then cuboids -> oct-tree using compute shaders
        val shader = buildOctTree.apply { }
        shader.use()

        shader.runBySize(blocksX, blocksY, blocksZ)
        // todo finally render using this oct-tree
    }*/

    // other optimization: use r/g/b in transparent pixels to encode distance to the closest wall -> massive speed up to traversal :)

    // draw using dense voxels
    testUI("MeshToVoxels") {

        // to measure performance
        WindowRenderFlags.enableVSync = false

        val renderer = attributeRenderers[DeferredLayerType.COLOR]

        val data = meshToVoxels(mesh, renderer, blocksX, blocksY, blocksZ, true)
        val mesh1 = smoothCube.scaled(Vector3f(data.width / 2f, data.height / 2f, data.depth / 2f)).back
        val comp = MeshComponent(mesh1)
        val mat = Texture3DBTv2Material()

        val useSDF = true
        // helmet at 512 resolution (511 x 487 x 541) ; RTX 3070 on square section of 1080p
        // false -> ~200 fps
        // true -> ~390 fps :3

        if (useSDF) {
            calculateSkipDistances(data)
        }

        mat.blocks = data
        mat.useSDF = useSDF
        comp.materials = listOf(mat.ref)

        testScene(comp)
    }
}
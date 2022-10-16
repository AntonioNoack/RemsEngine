package me.anno.tests.rtrt

import me.anno.ecs.Entity
import me.anno.ecs.components.cache.MaterialCache
import me.anno.ecs.components.cache.MeshCache
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.Mesh.Companion.defaultMaterial
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.shaders.Texture3DBTv2Material
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
import me.anno.gpu.shader.Renderer
import me.anno.gpu.texture.Texture3D
import me.anno.io.files.thumbs.ThumbsExt.waitForTextures
import me.anno.maths.Maths.PIf
import me.anno.maths.Maths.cbrt
import me.anno.maths.Maths.max
import me.anno.maths.Maths.mix
import me.anno.mesh.Shapes.smoothCube
import me.anno.ui.debug.TestStudio.Companion.testUI
import me.anno.utils.Clock
import me.anno.utils.OS.downloads
import org.joml.*
import kotlin.math.ceil
import kotlin.math.pow

fun meshToVoxels(
    mesh: Mesh, renderer: Renderer, blocksX: Int, blocksY: Int, blocksZ: Int,
    waitForTextures: Boolean
): Texture3D {
    val dataXYZ = Framebuffer3D("3d", blocksX, blocksY, blocksZ, arrayOf(TargetType.UByteTarget4), DepthBufferType.NONE)
    val dataZYX = Framebuffer3D("3d", blocksZ, blocksY, blocksX, arrayOf(TargetType.UByteTarget4), DepthBufferType.NONE)
    val dataXZY = Framebuffer3D("3d", blocksX, blocksZ, blocksY, arrayOf(TargetType.UByteTarget4), DepthBufferType.NONE)
    meshToSeparatedVoxels(mesh, renderer, blocksX, blocksY, blocksZ, dataXYZ, dataZYX, dataXZY, waitForTextures)
    val tex = mergeChannels(dataXYZ, dataZYX, dataXZY)
    dataZYX.destroy()
    dataXZY.destroy()
    dataXYZ.destroyExceptTextures(false)
    return tex
}

fun meshToSeparatedVoxels(
    mesh: Mesh, renderer: Renderer,
    blocksX: Int, blocksY: Int, blocksZ: Int,
    dataXYZ: Framebuffer3D,
    dataZYX: Framebuffer3D,
    dataXZY: Framebuffer3D,
    waitForTextures: Boolean
) {

    val bounds = mesh.ensureBounds()
    bounds.addMargin(max(bounds.deltaX(), max(bounds.deltaY(), bounds.deltaZ())) / max(blocksX, max(blocksY, blocksZ)))

    val transform = Matrix4f()

    // goal: rasterize into cubes quickly
    // strategy: draw slices onto textures, which then are part of a 3d texture
    // https://stackoverflow.com/questions/17504750/opengl-how-to-render-object-to-3d-texture-as-a-volumetric-billboard
    // draw mesh for XYZ, YZX, ZXY
    // sample code for xyz:
    fun drawMesh() {
        for (i in 0 until mesh.numMaterials) {
            // find shader
            val material = MaterialCache[mesh.materials.getOrNull(i)] ?: defaultMaterial
            val shader = (material.shader ?: pbrModelShader).value
            shader.use()
            // bind & prepare shader
            shader.m4x4("transform", transform)
            shader.m4x3("localTransform", null)
            shader.m4x3("invLocalTransform", null)
            material.bind(shader)
            // draw mesh
            mesh.draw(shader, i)
        }
    }
    GFXState.depthMode.use(DepthMode.ALWAYS11) {
        if (waitForTextures) waitForTextures(mesh, mesh.ref)
        val clock = Clock()
        val invX = 1f / blocksX
        val invY = 1f / blocksY
        val invZ = 1f / blocksZ
        dataXYZ.draw(renderer) { z ->
            dataXYZ.clearColor(0)
            val min = -mix(bounds.minZ, bounds.maxZ, z * invZ)
            val max = -mix(bounds.minZ, bounds.maxZ, (z + 1) * invZ)
            transform.identity()
            transform.ortho(bounds.minX, bounds.maxX, bounds.minY, bounds.maxY, min, max)
            drawMesh()
        }
        dataZYX.draw(renderer) { x ->
            dataZYX.clearColor(0)
            val min = mix(bounds.minX, bounds.maxX, x * invX)
            val max = mix(bounds.minX, bounds.maxX, (x + 1) * invX)
            transform.identity()
            transform.rotateY(PIf / 2f)
            transform.ortho(min, max, bounds.minY, bounds.maxY, -bounds.maxZ, -bounds.minZ)
            drawMesh()
        }
        dataXZY.draw(renderer) { y ->
            dataXZY.clearColor(0)
            val min = mix(bounds.maxY, bounds.minY, y * invY)
            val max = mix(bounds.maxY, bounds.minY, (y + 1) * invY)
            transform.identity()
            transform.rotateX(PIf / 2f)
            transform.ortho(bounds.minX, bounds.maxX, min, max, -bounds.minZ, -bounds.maxZ)
            drawMesh()
        }
        clock.stop("Mesh -> Dense Voxels")
    }

}

val mergeChannels = ComputeShader(
    "oct-tree", Vector3i(8, 8, 8), "" +
            "layout(rgba8, binding = 0) uniform image3D xyz;\n" +
            "layout(rgba8, binding = 1) uniform image3D zyx;\n" +
            "layout(rgba8, binding = 2) uniform image3D xzy;\n" +
            "layout(rgba8, binding = 3) uniform image3D dst;\n" +
            "uniform ivec3 size;\n" +
            "void main(){\n" +
            "   ivec3 uvw = ivec3(gl_GlobalInvocationID);\n" +
            "   if(all(lessThan(uvw, size))) {\n" +
            "       imageStore(dst, uvw, max(max(\n" +
            "           imageLoad(xyz, uvw),\n" +
            "           imageLoad(zyx, ivec3(size.z-1-uvw.z,uvw.y,uvw.x))),\n" +
            "           imageLoad(xzy, ivec3(uvw.x,size.z-1-uvw.z,size.y-1-uvw.y))\n" +
            "       ));\n" +
            "   }\n" +
            "}\n"
)

fun mergeChannels(
    blocksX: Int, blocksY: Int, blocksZ: Int,
    dataXYZ: Texture3D,
    dataZYX: Texture3D,
    dataXZY: Texture3D,
    dst: Texture3D = dataXYZ
): Texture3D {
    // merge all channels
    val shader = mergeChannels.apply { }
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
    dataXYZ.w, dataXYZ.h, dataXYZ.d,
    dataXYZ.getTexture0(), dataZYX.getTexture0(), dataXZY.getTexture0(), dst
)

fun main() {

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

    val bounds = AABBf(mesh.ensureBounds())
    val resolution = 512
    // this does not work with very flat models! ->
    // maybe now...
    val dx = bounds.deltaX()
    val dy = bounds.deltaY()
    val dz = bounds.deltaZ()
    val dm = max(max(dx, dy), dz) / resolution
    bounds.addMargin(dm)

    val volume = max(dx, dm) * max(dy, dm) * max(dz, dm)
    val budget = resolution.toFloat().pow(3)
    val scale = cbrt(budget / volume)
    val blocksX = max(1, ceil(dx * scale).toInt())
    val blocksY = max(1, ceil(dy * scale).toInt())
    val blocksZ = max(1, ceil(dz * scale).toInt())

    println("size: $blocksX x $blocksY x $blocksZ")

    val dataXYZ = Framebuffer3D("3d", blocksX, blocksY, blocksZ, arrayOf(TargetType.UByteTarget4), DepthBufferType.NONE)
    val dataZYX = Framebuffer3D("3d", blocksZ, blocksY, blocksX, arrayOf(TargetType.UByteTarget4), DepthBufferType.NONE)
    val dataXZY = Framebuffer3D("3d", blocksX, blocksZ, blocksY, arrayOf(TargetType.UByteTarget4), DepthBufferType.NONE)

    val transform = Matrix4f()

    fun rasterize() {
        // goal: rasterize into cubes quickly
        // strategy: draw slices onto textures, which then are part of a 3d texture
        // https://stackoverflow.com/questions/17504750/opengl-how-to-render-object-to-3d-texture-as-a-volumetric-billboard
        // draw mesh for XYZ, YZX, ZXY
        // sample code for xyz:
        val renderer = attributeRenderers[DeferredLayerType.COLOR]
        fun drawMesh() {
            for (i in 0 until mesh.numMaterials) {
                // find shader
                val material = MaterialCache[mesh.materials.getOrNull(i)] ?: defaultMaterial
                val shader = (material.shader ?: pbrModelShader).value
                shader.use()
                // bind & prepare shader
                shader.m4x4("transform", transform)
                shader.m4x3("localTransform", null)
                shader.m4x3("invLocalTransform", null)
                material.bind(shader)
                // draw mesh
                mesh.draw(shader, i)
            }
        }
        GFXState.depthMode.use(DepthMode.ALWAYS11) {
            waitForTextures(mesh, file)
            val clock = Clock()
            val invX = 1f / blocksX
            val invY = 1f / blocksY
            val invZ = 1f / blocksZ
            dataXYZ.draw(renderer) { z ->
                dataXYZ.clearColor(0)
                val min = -mix(bounds.minZ, bounds.maxZ, z * invZ)
                val max = -mix(bounds.minZ, bounds.maxZ, (z + 1) * invZ)
                transform.identity()
                transform.ortho(bounds.minX, bounds.maxX, bounds.minY, bounds.maxY, min, max)
                drawMesh()
            }
            dataZYX.draw(renderer) { x ->
                dataZYX.clearColor(0)
                val min = mix(bounds.minX, bounds.maxX, x * invX)
                val max = mix(bounds.minX, bounds.maxX, (x + 1) * invX)
                transform.identity()
                transform.rotateY(PIf / 2f)
                transform.ortho(min, max, bounds.minY, bounds.maxY, -bounds.maxZ, -bounds.minZ)
                drawMesh()
            }
            dataXZY.draw(renderer) { y ->
                dataXZY.clearColor(0)
                val min = mix(bounds.maxY, bounds.minY, y * invY)
                val max = mix(bounds.maxY, bounds.minY, (y + 1) * invY)
                transform.identity()
                transform.rotateX(PIf / 2f)
                transform.ortho(bounds.minX, bounds.maxX, min, max, -bounds.minZ, -bounds.maxZ)
                drawMesh()
            }
            clock.stop("Mesh -> Dense Voxels")
        }
    }

    val buildOctTree = ComputeShader(
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
    }

    // draw using dense voxels
    testUI {
        rasterize()
        val entity = Entity()
        fun add(data: Texture3D, rotation: Quaterniond?) {
            val mesh1 = smoothCube.scaled(Vector3f(data.w / 2f, data.h / 2f, data.d / 2f)).back
            val comp = MeshComponent(mesh1.ref)
            val mat = Texture3DBTv2Material()
            mat.blocks = data
            comp.materials = listOf(mat.ref)
            if (rotation != null) {
                val child = Entity()
                entity.add(child)
                child.add(comp)
                child.transform.localRotation = rotation
            } else entity.add(comp)
        }
        // add(dataXYZ.getTexture0(), null)
        // add(dataZYX.getTexture0(), Quaterniond().rotateY(PI / 2))
        // add(dataXZY.getTexture0(), Quaterniond().rotateX(PI / 2))
        add(mergeChannels(dataXYZ, dataZYX, dataXZY), null)
        testScene(entity)
    }
}
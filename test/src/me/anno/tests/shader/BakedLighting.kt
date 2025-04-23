package me.anno.tests.shader

import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.forAllComponentsInChildren
import me.anno.ecs.EntityQuery.getComponentInChildren
import me.anno.ecs.components.light.DirectionalLight
import me.anno.ecs.components.light.sky.Skybox
import me.anno.ecs.components.light.sky.SkyboxBase
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.MeshIterators.forEachPointIndex
import me.anno.ecs.components.mesh.MeshIterators.forEachTriangle
import me.anno.ecs.components.mesh.MeshIterators.forEachTriangleIndexV2
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.material.MaterialCache
import me.anno.ecs.components.mesh.material.Materials
import me.anno.ecs.components.mesh.material.utils.TypeValue
import me.anno.ecs.components.mesh.shapes.CubemapModel
import me.anno.ecs.components.mesh.shapes.IcosahedronModel
import me.anno.ecs.components.mesh.utils.MeshInstanceData
import me.anno.ecs.components.mesh.utils.MeshVertexData
import me.anno.engine.DefaultAssets
import me.anno.engine.Events.addEvent
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.render.ECSMeshShader
import me.anno.engine.ui.render.RendererLib.getReflectivity
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.DitherMode
import me.anno.gpu.GFX
import me.anno.gpu.GFXState.renderPurely
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.GPUTasks.addGPUTask
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib.brightness
import me.anno.gpu.shader.ShaderLib.coordsUVVertexShader
import me.anno.gpu.shader.ShaderLib.coordsVertexShader
import me.anno.gpu.shader.ShaderLib.parallaxMapping
import me.anno.gpu.shader.ShaderLib.quatRot
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.shader.renderer.Renderer
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureCache
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.image.Image
import me.anno.image.ImageCache
import me.anno.image.raw.FloatImage
import me.anno.image.raw.IntImage
import me.anno.maths.Maths
import me.anno.maths.Maths.length
import me.anno.maths.Maths.max
import me.anno.maths.Maths.min
import me.anno.maths.Maths.mix
import me.anno.maths.Maths.sq
import me.anno.maths.bvh.BLASNode
import me.anno.maths.bvh.BLASTexture.createBLASTexture
import me.anno.maths.bvh.BVHBuilder
import me.anno.maths.bvh.BVHBuilder.buildBLAS
import me.anno.maths.bvh.BVHBuilder.createTLASLeaf
import me.anno.maths.bvh.BVHBuilder.createTexture
import me.anno.maths.bvh.RayTracing.glslGraphicsDefines
import me.anno.maths.bvh.SplitMethod
import me.anno.maths.bvh.TLASLeaf
import me.anno.maths.bvh.TLASNode
import me.anno.maths.bvh.TLASTexture.PIXELS_PER_TLAS_NODE
import me.anno.maths.bvh.TLASTexture.createTLASTexture
import me.anno.maths.bvh.TriangleTexture.PIXELS_PER_TRIANGLE
import me.anno.maths.bvh.TriangleTexture.PIXELS_PER_VERTEX
import me.anno.maths.bvh.TrisFiller.Companion.fillTris
import me.anno.maths.bvh.shader.TextureRTShaderLib
import me.anno.maths.geometry.Rasterizer
import me.anno.sdf.random.SDFRandom.Companion.randLib
import me.anno.tests.rtrt.engine.commonFunctions
import me.anno.tests.rtrt.engine.commonUniforms
import me.anno.ui.base.progress.ProgressBar
import me.anno.ui.editor.color.spaces.HSLuv
import me.anno.utils.Clock
import me.anno.utils.Color.b01
import me.anno.utils.Color.g01
import me.anno.utils.Color.r01
import me.anno.utils.OS.desktop
import me.anno.utils.OS.pictures
import me.anno.utils.OS.res
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertTrue
import me.anno.utils.pooling.Pools
import me.anno.utils.structures.lists.Lists.createList
import me.anno.utils.types.Booleans.hasFlag
import me.anno.utils.types.Floats.formatPercent
import me.anno.utils.types.Triangles.getBarycentrics
import me.anno.utils.types.Triangles.getTriangleArea
import org.apache.logging.log4j.LogManager
import org.joml.AABBf
import org.joml.AABBi
import org.joml.Matrix3x2f
import org.joml.Matrix4x3f
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.round
import kotlin.random.Random

// bake lighting:
//  - find out where sun shines directly (0..1)
//  - bake diffuse light using raytracing, maybe CPU, maybe GPU
//  - combine multiple UV sets onto one texture, weighted by actual surface area
//    -> each mesh instance needs to save its rectangle for illumination
//  - todo only for "static" geometry
//  - one texture for all geometry
//  - bind that texture, and use it; maybe set emissive = diffuse * lightMap; diffuse = 0;

private var debugImages = false

private val LOGGER = LogManager.getLogger("BakedLighting")

object BakedLightingShader : ECSMeshShader("BakedLighting") {

    var bakedIllumTex: ITexture2D? = null

    override fun createFragmentStages(key: ShaderKey): List<ShaderStage> {
        return key.vertexData.onFragmentShader + listOf(
            ShaderStage(
                "material", createFragmentVariables(key) +
                        listOf(
                            // todo for instanced rendering, this would have to be transferred, too
                            Variable(GLSLType.V4F, "bakedRect"),
                            Variable(GLSLType.S2D, "bakedIllumTex"),
                        ),
                concatDefines(key).toString() +
                        discardByCullingPlane +
                        // step by step define all material properties
                        baseColorCalculation +
                        (if (key.flags.hasFlag(NEEDS_COLORS)) {
                            "" +
                                    normalTanBitanCalculation +
                                    normalMapCalculation +
                                    emissiveCalculation +
                                    occlusionCalculation +
                                    metallicCalculation +
                                    roughnessCalculation +
                                    "vec2 bakedUV = bakedRect.xy + vec2(uv.x,1.0-uv.y) * bakedRect.zw;\n" +
                                    "vec4 bakedIllumination = max(texture(bakedIllumTex, bakedUV),vec4(0.0));\n" +
                                    "float diffuseness = 1.0 - finalMetallic;\n" +
                                    "finalEmissive += diffuseness * finalColor * sqrt(bakedIllumination.xyz);\n" +
                                    "finalColor *= 1.0 - diffuseness;\n" +
                                    v0 + sheenCalculation +
                                    clearCoatCalculation +
                                    reflectionCalculation
                        } else "") +
                        finalMotionCalculation
            ).add(quatRot).add(brightness).add(parallaxMapping).add(getReflectivity)
        )
    }

    val tex = pictures.getChild("4k.jpg")
    override fun bind(shader: Shader, renderer: Renderer, instanced: Boolean) {
        super.bind(shader, renderer, instanced)
        val light = bakedIllumTex ?: TextureCache[tex, true] ?: whiteTexture
        light.bind(shader, "bakedIllumTex", Filtering.LINEAR, Clamping.CLAMP)
    }
}

fun createSampleScene(): Entity {

    // define a simple sample scene
    val random = Random(1234)
    val scene = Entity()

    val floorMaterial = Material()
    floorMaterial.shader = BakedLightingShader
    floorMaterial.diffuseMap = res.getChild("textures/UVChecker.png")
    Entity("Floor", scene)
        .add(MeshComponent(DefaultAssets.plane, floorMaterial))
        .setScale(50f)

    val cubeMesh = CubemapModel.model.front

    val floorMaterial1 = Material()
    floorMaterial1.shader = BakedLightingShader
    Entity("Root", scene)
        .add(MeshComponent(cubeMesh, floorMaterial1))
        .setPosition(0.0, 7.0, 0.0)
        .setScale(30f, 0.3f, 30f)

    // add a few boxes
    val cubes = Entity("Spheres", scene)
    for (i in 0 until 7) {
        val radius = random.nextDouble() * 5.0 + 2.5
        val material = Material()
        material.shader = BakedLightingShader
        val hsluv = Vector3f(
            random.nextFloat(),
            random.nextFloat(),
            random.nextFloat() * .5f + .5f
        )
        material.diffuseBase.set(HSLuv.toRGB(hsluv), 1f)
        if (random.nextFloat() < 0.2f) {
            material.metallicMinMax.set(1f)
            material.roughnessMinMax.set(0f)
        }
        Entity(cubes).add(MeshComponent(cubeMesh, material))
            .setPosition(random.nextDouble() * 70 - 35, radius, random.nextDouble() * 70 - 35)
            .setScale(radius.toFloat())
    }

    // add a few spheres
    val spheres = Entity("Spheres", scene)
    val sphereMesh = IcosahedronModel.createIcosphere(3)

    for (i in 0 until 50) {
        val radius = random.nextDouble() * 2.0 + 1.0
        val material = Material()
        material.shader = BakedLightingShader
        val hsluv = Vector3f(
            random.nextFloat(),
            random.nextFloat(),
            random.nextFloat() * .5f + .5f
        )
        material.diffuseBase.set(HSLuv.toRGB(hsluv), 1f)
        if (random.nextFloat() < 0.2f) {
            material.metallicMinMax.set(1f)
            material.roughnessMinMax.set(0f)
        } else if (random.nextFloat() < 0.05f) {
            material.emissiveBase
                .set(material.diffuseBase)
                .safeNormalize(50f)
            material.diffuseBase.set(0f, 0f, 0f, 1f)
        }
        Entity(spheres).add(MeshComponent(sphereMesh, material))
            .setPosition(random.nextDouble() * 70 - 35, radius, random.nextDouble() * 70 - 35)
            .setScale(radius.toFloat())
    }

    val sun = DirectionalLight()
    sun.shadowMapCascades = 1
    val sunEntity = Entity("Sun", scene).add(sun)
        .setScale(50f)
    val skybox = Skybox()
    scene.add(skybox)
    skybox.applyOntoSun(sunEntity, sun, 20f)

    return scene
}

// todo automatic, optimized UV unwrapping???
// todo secondary UV coordinates especially for lightmaps?

val dst = desktop.getChild("LightBaking")
fun main() {

    OfficialExtensions.initForTests()

    val scene = createSampleScene()
    scene.validateTransform()
    val weights = HashMap<MeshComponent, Double>()
    scene.forAllComponentsInChildren(MeshComponent::class) {
        weights[it] = calculateSurfaceArea(it.entity!!, it)
    }

    val resolution = 1024
    splitArea(weights, resolution)
    val rasterizedScene = RaytracingInput(resolution, resolution)
    scene.forAllComponentsInChildren(MeshComponent::class) { comp ->
        rasterizeMeshOntoUVs(comp, rasterizedScene, resolution)
    }

    for (i in 0 until 3) {
        spread(rasterizedScene)
    }

    if (true) {
        dst.tryMkdirs()
        rasterizedScene.positions.clone().normalize01().write(dst.getChild("positions.png"))
        rasterizedScene.normals.clone().normalize01().write(dst.getChild("normals.png"))
        rasterizedScene.diffuse.write(dst.getChild("diffuse.png"))
        rasterizedScene.emissive.clone().normalize01().write(dst.getChild("emissive.png"))
    }

    val bvh = buildTLAS(scene, Vector3d(), SplitMethod.MEDIAN_APPROX, 16)!!
    val skybox = scene.getComponentInChildren(SkyboxBase::class) ?: Skybox.defaultSky
    addGPUTask("illum", 1000) {
        bakeIllumination(bvh, rasterizedScene, skybox)
    }
    testSceneWithUI("Baked Lighting", scene)
}

fun buildTLAS(
    scene: Entity,
    cameraPosition: Vector3d,
    splitMethod: SplitMethod, maxNodeSize: Int
): TLASNode? {
    val clock = Clock(LOGGER)
    val objects = ArrayList<TLASLeaf>()
    scene.forAllComponentsInChildren(MeshComponent::class) { comp ->
        val mesh = comp.getMesh()
        if (mesh is Mesh) {
            val blas = mesh.raycaster ?: buildBLAS(mesh, splitMethod, maxNodeSize)
            if (blas != null) {
                mesh.raycaster = blas
                objects.add(createTLASLeaf(mesh, blas, comp.transform!!, comp, cameraPosition))
            }
        }
    }
    clock.stop("Creating BLASes")
    LOGGER.info("Building TLAS from ${objects.size} objects")
    if (objects.isEmpty()) return null
    val tlas = BVHBuilder.buildTLAS(objects, splitMethod)
    clock.stop("Creating TLAS")
    return tlas
}

class RaytracingInput(w: Int, h: Int) {
    val positions = FloatImage(w, h, 3)
    val normals = FloatImage(w, h, 3)
    val diffuse = FloatImage(w, h, 3)
    val emissive = FloatImage(w, h, 3)
    val tmp = FloatImage(w, h, 3)
}

data class SimpleMaterial(
    val diffuseTexture: Image,
    val diffuseColor: Vector3f,
    val emissiveTexture: Image,
    val emissiveColor: Vector3f,
    val filtering: Filtering,
    val clamping: Clamping
)

val whiteImage = IntImage(1, 1, intArrayOf(-1), false)
fun rasterizeMeshOntoUVs(component: MeshComponent, dst: RaytracingInput, resolution: Int) {

    val transform = Matrix4x3f()
        .set(component.transform!!.globalTransform)

    val mesh = component.getMesh() as? Mesh ?: return
    mesh.ensureNorTanUVs()
    val pos = mesh.positions ?: return
    val nor = mesh.normals ?: return
    val uvs = mesh.uvs ?: return // todo if missing, calculate single pixel-value for it
    val region = MaterialCache[component.materials[0]]!!.shaderOverrides["bakedRect"]!!.value as Vector4f

    // we could find UV-AABB, and unproject it to remove fract() from shader, and make spheres work out of the box
    val uvBounds = AABBf()
    mesh.forEachPointIndex(false) { pi ->
        uvBounds.union(uvs[pi * 2], uvs[pi * 2 + 1], 0f)
        false
    }

    // apply uvBounds-unmapping here
    val dx = region.z * resolution
    val dy = region.w * resolution
    val dxi = dx / uvBounds.deltaX
    val dyi = dy / uvBounds.deltaY
    val x0 = region.x * resolution
    val y0 = region.y * resolution

    // UV-unmapping means that we need to rewrite the region, too
    val rdx = region.z / uvBounds.deltaX
    val rdy = region.w / uvBounds.deltaY
    region.set(
        region.x - uvBounds.minX * rdx,
        region.y - uvBounds.minY * rdy,
        rdx, rdy
    )

    val uvTransform = Matrix3x2f().set(
        dxi, 0f,
        0f, dyi,
        (x0 - uvBounds.minX * dxi),
        (y0 - uvBounds.minY * dyi)
    )

    val bounds = AABBf()
        .setMin(x0, y0, 0f)
        .setMax(x0 + dx, y0 + dy, 0f)

    val bary = Vector3f()
    val uvi = Vector2f()
    val a = Vector2f()
    val b = Vector2f()
    val c = Vector2f()

    val materials = createList(mesh.numMaterials) {
        val src = Materials.getMaterial(component.materials, mesh.materials, it)
        SimpleMaterial(
            ImageCache[src.diffuseMap, false] ?: whiteImage, Vector3f(src.diffuseBase),
            ImageCache[src.emissiveMap, false] ?: whiteImage, src.emissiveBase,
            if (src.linearFiltering) Filtering.TRULY_LINEAR else Filtering.TRULY_NEAREST,
            src.clamping
        )
    }

    val materialIds = mesh.materialIds
    mesh.forEachTriangleIndexV2 { ai, bi, ci, faceIndex ->
        // get UVs
        val ai2 = ai * 2
        val bi2 = bi * 2
        val ci2 = ci * 2
        uvTransform.transformPosition(a.set(uvs, ai2))
        uvTransform.transformPosition(b.set(uvs, bi2))
        uvTransform.transformPosition(c.set(uvs, ci2))

        // find out diffuseTexture and diffuseColor,
        //  emissiveTexture and emissiveColor
        val material = materials[min(materialIds?.getOrNull(faceIndex) ?: 0, materials.lastIndex)]

        val ai3 = ai2 + ai
        val bi3 = bi2 + bi
        val ci3 = ci2 + ci
        // rasterize between UVs
        Rasterizer.rasterize(a, b, c, bounds) { minX, maxX, y ->
            uvi.y = y.toFloat()
            for (x in minX..maxX) {
                uvi.x = x.toFloat()

                // calculate barycentric coordinates
                getBarycentrics(a, b, c, uvi, bary)

                // calculate position, normal
                val px = bary.dot(pos[ai3 + 0], pos[bi3 + 0], pos[ci3 + 0])
                val py = bary.dot(pos[ai3 + 1], pos[bi3 + 1], pos[ci3 + 1])
                val pz = bary.dot(pos[ai3 + 2], pos[bi3 + 2], pos[ci3 + 2])
                val nx = bary.dot(nor[ai3 + 0], nor[bi3 + 0], nor[ci3 + 0])
                val ny = bary.dot(nor[ai3 + 1], nor[bi3 + 1], nor[ci3 + 1])
                val nz = bary.dot(nor[ai3 + 2], nor[bi3 + 2], nor[ci3 + 2])

                // calculate UVs
                val u = bary.dot(uvs[ai2 + 0], uvs[bi2 + 0], uvs[ci2 + 0])
                val v = bary.dot(uvs[ai2 + 1], uvs[bi2 + 1], uvs[ci2 + 1])

                fun write(dst: FloatImage, col3: Vector3f) {
                    val index = dst.getIndex(x, y)
                    dst.setValue(index, 0, col3.x)
                    dst.setValue(index, 1, col3.y)
                    dst.setValue(index, 2, col3.z)
                }

                transform.transformPosition(bary.set(px, py, pz))
                write(dst.positions, bary)

                transform.transformDirection(bary.set(nx, ny, nz)).safeNormalize()
                write(dst.normals, bary)

                fun write(dst: FloatImage, texture: Image, tint: Vector3f) {
                    // sample color and multiply by tint
                    val index = dst.getIndex(x, y)
                    if (texture !== whiteImage) {
                        val rgb = texture.sampleRGB(
                            u * texture.width,
                            v * texture.height,
                            material.filtering,
                            material.clamping
                        )
                        dst.setValue(index, 0, sq(tint.x * rgb.r01()))
                        dst.setValue(index, 1, sq(tint.y * rgb.g01()))
                        dst.setValue(index, 2, sq(tint.z * rgb.b01()))
                    } else {
                        dst.setValue(index, 0, sq(tint.x))
                        dst.setValue(index, 1, sq(tint.y))
                        dst.setValue(index, 2, sq(tint.z))
                    }
                }

                write(dst.diffuse, material.diffuseTexture, material.diffuseColor)
                write(dst.emissive, material.emissiveTexture, material.emissiveColor)
            }
        }
        false
    }
}

fun spread(rti: RaytracingInput) {
    // todo why isn't this getting rid of the black edges on the spheres???
    rti.normals.data.copyInto(rti.tmp.data)
    spread(rti.diffuse, rti.tmp, false)
    spread(rti.emissive, rti.tmp, false)
    spread(rti.normals, rti.tmp, true)
    spread(rti.positions, rti.tmp, false)
}

fun FloatImage.hasValue(idx: Int): Boolean {
    return getValue(idx, 0) != 0f ||
            getValue(idx, 1) != 0f ||
            getValue(idx, 2) != 0f
}

fun spread(image: FloatImage, tmp: FloatImage, normalize: Boolean) {
    image.forEachPixel { x, y ->
        spreadPixelIfNeeded(image, tmp, x, y, normalize)
    }
}

fun spreadPixelIfNeeded(image: FloatImage, tmp: FloatImage, x: Int, y: Int, normalize: Boolean) {
    val idx0 = image.getIndex(x, y)
    if (tmp.hasValue(idx0)) return
    // check if any neighbor is defined, and if so, copy it
    var sx = 0f
    var sy = 0f
    var sz = 0f
    var sw = 0f
    for (dy in -1..1) {
        val ny = y + dy
        if (ny !in 0 until image.height) continue
        for (dx in -1..1) {
            val nx = x + dx
            if (nx !in 0 until image.width) continue
            val idx = image.getIndex(nx, ny)
            if (tmp.hasValue(idx)) {
                sx += image.getValue(idx, 0)
                sy += image.getValue(idx, 1)
                sz += image.getValue(idx, 2)
                sw++
            }
        }
    }
    if (sw > 0f) {
        // if is normals, actually normalize the value
        sw = if (normalize) 1f / max(length(sx, sy, sz), 1e-9f) else 1f / sw
        image.setValue(idx0, 0, sx * sw)
        image.setValue(idx0, 1, sy * sw)
        image.setValue(idx0, 2, sz * sw)
    }
}

val blurShader = Shader(
    "blur", emptyList(), coordsVertexShader, emptyList(), listOf(
        Variable(GLSLType.S2D, "bakedIllum"),
        Variable(GLSLType.S2D, "norTex"),
        Variable(GLSLType.V4F, "result", VariableMode.OUT)
    ), "" +
            "vec4 getValue(ivec2 uv, ivec2 size, float weight, vec3 normal0){\n" +
            "   if(uv.x < 0 || uv.y < 0 || uv.x >= size.x || uv.y >= size.y) return vec4(0.0);\n" +
            "   vec3 normal1 = texelFetch(norTex,uv,0).xyz;\n" +
            "   weight *= dot(normal0,normal1)-0.9;\n" +
            "   if(weight <= 0.0) return vec4(0.0);\n" +
            "   vec3 v = texelFetch(bakedIllum,uv,0).xyz;\n" +
            "   return vec4(v * weight, weight);\n" +
            "}\n" +
            "void main(){\n" +
            "   ivec2 size = textureSize(bakedIllum,0);\n" +
            "   ivec2 uv = ivec2(gl_FragCoord.xy);\n" +
            "   vec3 normal0 = texelFetch(norTex,uv,0).xyz;\n" +
            "   vec4 value = vec4(texelFetch(bakedIllum,uv,0).xyz, 1.0);\n" +
            "   value += getValue(uv+ivec2(1,0),size,1.0,normal0);\n" +
            "   value += getValue(uv-ivec2(1,0),size,1.0,normal0);\n" +
            "   value += getValue(uv+ivec2(0,1),size,1.0,normal0);\n" +
            "   value += getValue(uv-ivec2(0,1),size,1.0,normal0);\n" +
            "   result = vec4(value.rgb / value.a, 1.0);\n" +
            "}\n"
)

fun createTriangleTexture(blasList: List<BLASNode>): Texture2D {
    // to do if there are too many triangles, use a texture array?
    // 8k x 8k = 64M pixels = 64M vertices = 21M triangles
    // but that'd also need 64M * 16byte/vertex = 1GB of VRAM
    // to do most meshes don't need such high precision, maybe use u8 or u16 or fp16
    GFX.checkIsGFXThread()
    val buffers = blasList.map { it.findGeometryData() } // positions without index
    // RGB is not supported by compute shaders (why ever...), so use RGBA
    val numTriangles = buffers.sumOf { it.indices.size / 3 }
    val texture = createTexture("triangles", numTriangles, PIXELS_PER_TRIANGLE)
    val bytesPerPixel = 16 // 4 channels * 4 bytes / float
    val buffer = Pools.byteBufferPool[texture.width * texture.height * bytesPerPixel, false, false]
    val data = buffer.asFloatBuffer()
    fillTris(blasList, buffers) { geometry, i ->
        val uvs = geometry.uvs
        if (uvs != null && i * 2 + 1 < uvs.size) {
            data.put(geometry.positions, i * 3, 3)
            data.put(uvs[i * 2])
            data.put(geometry.normals, i * 3, 3)
            data.put(1f - uvs[i * 2 + 1])
        } else {
            data.put(geometry.positions, i * 3, 3)
            data.put(0f)
            data.put(geometry.normals, i * 3, 3)
            data.put(0f)
        }
    }
    LOGGER.info("Filled triangles ${(data.position().toFloat() / data.capacity()).formatPercent()}%, $texture")
    texture.createRGBA(data, buffer, false)
    Pools.byteBufferPool.returnBuffer(buffer)
    return texture
}

// todo next-event estimation or sth like that for much lower noise ^^, and maybe no need to blur :)
fun bakeIllumination(bvh: TLASNode, input: RaytracingInput, skybox: SkyboxBase) {

    val numIterations = 5000
    val progress = GFX.someWindow.addProgressBar(
        object : ProgressBar("Baking", "Samples", numIterations + 1.0) { // 1.0 for blurring at the end
            override fun formatProgress(): String {
                return "${progress.toInt()}/$numIterations $unit"
            }
        })

    val imageForSize = input.positions
    var srcI = Framebuffer("illum0", imageForSize.width, imageForSize.height, TargetType.Float32x3)
    var dstI = Framebuffer("illum1", imageForSize.width, imageForSize.height, TargetType.Float32x3)

    fun toggle() {
        val tmp = srcI
        srcI = dstI
        dstI = tmp
    }

    val posTex = Texture2D(input.positions, false)
    val norTex = Texture2D(input.normals, false)
    val difTex = Texture2D(input.diffuse, false)
    val emmTex = Texture2D(input.emissive, false)

    val skyKey = BaseShader.ShaderKey(
        Renderer.copyRenderer,
        MeshVertexData.DEFAULT, MeshInstanceData.DEFAULT,
        DitherMode.DRAW_EVERYTHING,
        null, null,
        0
    )
    val skyFragments = skybox.shader!!.createFragmentStages(skyKey)
    assertEquals(1, skyFragments.size)
    val skyFragment = skyFragments[0]

    val uniqueMeshes = HashSet<BLASNode>(bvh.countTLASLeaves())
    bvh.collectMeshes(uniqueMeshes)

    val maxTLASDepth = bvh.maxDepth()
    val maxBLASDepth = uniqueMeshes.maxOfOrNull { it.maxDepth() } ?: 0

    val meshes = uniqueMeshes.toList()
    assertEquals(2, PIXELS_PER_VERTEX)
    assertEquals(8, PIXELS_PER_TLAS_NODE)
    val lib = TextureRTShaderLib(2, 9)
    val rtShader = Shader(
        "illumBaking", emptyList(), coordsUVVertexShader, uvList, commonUniforms + listOf(
            Variable(GLSLType.S2D, "triangles"),
            Variable(GLSLType.S2D, "blasNodes"),
            Variable(GLSLType.S2D, "tlasNodes"),
            Variable(GLSLType.S2D, "posTex"),
            Variable(GLSLType.S2D, "norTex"),
            Variable(GLSLType.S2D, "difTex"),
            Variable(GLSLType.S2D, "emmTex"),
            Variable(GLSLType.S2D, "bakedIllum"),
            Variable(GLSLType.V1I, "bakedUVHeightM1"),
            Variable(GLSLType.V1F, "resultWeight"),
            Variable(GLSLType.V1I, "randomSeed"),
            Variable(GLSLType.V4F, "dst", VariableMode.OUT)
        ) + skyFragment.variables.filter { it.inOutMode == VariableMode.IN }, "" +
                "#define Infinity 1e15\n" +
                commonFunctions
                    .replace("step(dnn,0.0)", "1.0") +
                "#define nodes blasNodes\n" +
                "#define BLAS_DEPTH $maxBLASDepth\n" +
                "#define TLAS_DEPTH $maxTLASDepth\n" +
                glslGraphicsDefines +
                randLib +
                lib.glslBLASIntersection(false)
                    .replace("out vec3 normal", "out vec3 normal, vec4 bakedRect, inout vec2 bakedUV")
                    .replace(
                        "intersectTriangle(pos, dir, p0.rgb, p1.rgb, p2.rgb, n0.rgb, n1.rgb, n2.rgb, normal, distance);",
                        "" +
                                "vec4 hit = intersectTriangle(pos, dir, p0.rgb, p1.rgb, p2.rgb, n0.rgb, n1.rgb, n2.rgb, normal, distance);\n" +
                                "if(hit.w == 1.0) {\n" +
                                "   vec3 uvw = hit.xyz;\n" +
                                "   vec2 uv = vec2(dot(uvw,vec3(p0.w,p1.w,p2.w)),dot(uvw,vec3(n0.w,n1.w,n2.w)));\n" +
                                "   bakedUV = bakedRect.xy + uv * bakedRect.zw;\n" +
                                "}"
                    ) +
                lib.glslTLASIntersection(false)
                    .replace("out vec3 worldNormal", "out vec3 worldNormal, inout vec2 bakedUV")
                    .replace(", localDistance, localNormal", ", localDistance, localNormal, bakedRect, bakedUV")
                    .replace(
                        "intersectBLAS(", "" +
                                "vec4 bakedRect = LOAD_PIXEL(tlasNodes, ivec2(nodeX+8u,nodeY));\n" +
                                "intersectBLAS("
                    ) +
                skyFragment.functions.map { it.body }
                    .filter { it != quatRot } // already included
                    .joinToString("\n") +
                "void main(){\n" +
                "   ivec2 uv = ivec2(gl_FragCoord.xy);\n" +
                "   vec3 position = texelFetch(posTex,uv,0).xyz;\n" +
                "   vec3 normal = texelFetch(norTex,uv,0).xyz;\n" +
                "   vec3 color = vec3(0.0);\n" +
                "   if (dot(normal,normal) > 0.5){\n" +
                "       vec3 tint = vec3(1.0);\n" +
                "       int seed = threeInputRandom(uv.x,uv.y,randomSeed);\n" +
                "       for(int j=ZERO;j<3;j++){\n" +
                "           vec3 randomDir = vec3(0.0);\n" +
                "           for(int i=0;i<5;i++){\n" +
                "               randomDir = nextRandF3(seed) * 2.0 - 1.0;\n" +
                "               if(dot(randomDir,randomDir) <= 1.0) break;\n" +
                "           }\n" +
                "           vec3 dir = normalize(normal + randomDir);\n" +
                "           position += dir * (length(position) * 0.00001);\n" +
                // invoke tracing
                "           float distance = 1e20, distance0 = distance;\n" +
                "           vec2 bakedUV = vec2(0.0); vec3 normalOut = vec3(0.0);\n" +
                "           intersectTLAS(position,dir,distance,normalOut,bakedUV);\n" +
                "           if (distance < distance0){\n" +
                // write emission[bakedUV] + diffuse[bakedUV] * bakedIllum[bakedUV]
                "               if(dot(normal,normalOut) < 0.0) {\n" +
                "                   float weight = 0.75;\n" + // mix half-n-half with what's already there
                "                   color += tint * (texture(emmTex,bakedUV,0).rgb + (1.0-weight) * " +
                "                       texture(bakedIllum,ivec2(bakedUV.x,bakedUVHeightM1-bakedUV.y),0).rgb);\n" +
                "                   tint *= weight * texture(difTex,bakedUV,0).rgb;\n" +
                "                   position += dir * distance;\n" +
                "                   normal = reflect(dir,normalOut);\n" +
                "               } else {\n" +
                // hit inside, just make it black
                "                   break;\n" +
                "               }\n" +
                "           } else {\n" +
                // sky -> write sky color
                "               color += tint * getSkyColor(dir);\n" +
                "               break;\n" +
                "           }\n" +
                "       }\n" +
                "   }\n" +
                "   vec3 oldDst = texelFetch(bakedIllum,uv,0).xyz;\n" +
                "   dst = vec4(mix(oldDst, color, resultWeight), 1.0);\n" +
                "}\n"
    )
    // todo implement pass to add all lights:
    //  - split into sampled and simple lights
    //  - sampled lights - as the name says, need to be sampled : could be represented by a 3d mesh
    //  - simple lights can be baked once (ray-traced visibility check), and just added like a constant
    rtShader.glslVersion = 330 // for floatBitsToUint
    rtShader.setTextureIndices(
        "triangles", "tlasNodes", "blasNodes",
        "posTex", "norTex", "difTex", "emmTex",
        "bakedIllum"
    )

    val triangles = createTriangleTexture(meshes)
    val blasNodes = createBLASTexture(meshes, 2 * 3)
    val tlasNodes = createTLASTexture(bvh, 9) { node, data ->
        val bakedRect = if (node is TLASLeaf) {
            val material = MaterialCache[(node.component as? MeshComponent)?.materials?.getOrNull(0)]
            material?.shaderOverrides?.get("bakedRect")?.value as? Vector4f
        } else null
        if (bakedRect != null) {
            data.put(bakedRect.x).put(bakedRect.y)
                .put(bakedRect.z).put(bakedRect.w)
        } else {
            data.position(data.position() + 4)
        }
    }

    if (debugImages) {
        triangles.write(dst.getChild("triangles.png"))
        blasNodes.write(dst.getChild("blasNodes.png"))
        tlasNodes.write(dst.getChild("tlasNodes.png"))
    }

    srcI.ensure()

    // apply soft blur when normals are similar
    fun blur() {
        useFrame(dstI, Renderer.copyRenderer) {
            blurShader.use()
            norTex.bindTrulyNearest(blurShader, "norTex")
            srcI.getTexture0MS().bindTrulyNearest(blurShader, "bakedIllum")
            flat01.draw(blurShader)
        }
        toggle()
    }

    var iteration = 0
    fun nextFrame() {
        val i = iteration++
        if (i < numIterations) {
            renderPurely {
                useFrame(dstI, Renderer.copyRenderer) {
                    rtShader.use()
                    skybox.material.bind(rtShader)
                    srcI.getTexture0MS().bindTrulyNearest(rtShader, "bakedIllum")
                    rtShader.v1f("resultWeight", 1f / (i + 1f))
                    rtShader.v1i("randomSeed", Maths.randomInt())
                    rtShader.v1i("bakedUVHeightM1", srcI.height - 1)
                    // bind input textures
                    posTex.bindTrulyNearest(rtShader, "posTex")
                    norTex.bindTrulyNearest(rtShader, "norTex")
                    difTex.bindTrulyNearest(rtShader, "difTex")
                    emmTex.bindTrulyNearest(rtShader, "emmTex")
                    // bind RTAS
                    triangles.bindTrulyNearest(rtShader, "triangles")
                    blasNodes.bindTrulyNearest(rtShader, "blasNodes")
                    tlasNodes.bindTrulyNearest(rtShader, "tlasNodes")
                    flat01.draw(rtShader)
                }
                toggle()
            }
            progress.progress++
            BakedLightingShader.bakedIllumTex = srcI.getTexture0()
            addEvent(0) { // delay it a little
                addGPUTask("bakeIllum", 100, ::nextFrame)
            }
        } else {

            renderPurely {
                for (j in 0 until 10) {
                    blur()
                }
            }

            progress.finish()

            triangles.destroy()
            blasNodes.destroy()
            tlasNodes.destroy()
            posTex.destroy()
            norTex.destroy()
            difTex.destroy()
            emmTex.destroy()
            dstI.destroy()
            LOGGER.info("Destroyed textures")

            val result = srcI.getTexture0()
            srcI.destroyExceptTextures(true)
            BakedLightingShader.bakedIllumTex = result

            result.write(dst.getChild("bakedIllum.png"))
        }
    }
    nextFrame()
}

data class WeightedValue(val value: MeshComponent, val weight: Double)

val padding = 2
val halfPadding = padding / 2

fun splitArea(areaMap: Map<MeshComponent, Double>, resolution: Int) {
    // split [0,1]² region into areas weighted by areaMap/totalArea, and fractional pixels don't make any sense,
    //  plus padding would be nice
    val totalArea = areaMap.values.sum()
    val paddingPortion = padding.toDouble() / resolution
    val entries = areaMap.entries.sortedByDescending { it.value }.map { (comp, area) ->
        val weight = mix(area / totalArea, 1.0 / areaMap.size, paddingPortion)
        WeightedValue(comp, weight)
    }
    splitRegion(entries, AABBi(0, 0, 0, resolution, resolution, 0), resolution)
}

// https://en.wikipedia.org/wiki/Guillotine_cutting
// https://github.com/nical/guillotiere?tab=readme-ov-file
// https://docs.rs/guillotiere/latest/guillotiere/struct.AtlasAllocator.html
fun splitRegion(entries: List<WeightedValue>, remainingRegion: AABBi, resolution: Int) {
    assertTrue(entries.isNotEmpty())
    if (entries.size == 1) {
        val value = Vector4f(
            remainingRegion.minX.toFloat(),
            remainingRegion.minY.toFloat(),
            remainingRegion.deltaX.toFloat(),
            remainingRegion.deltaY.toFloat()
        ).div(resolution.toFloat())
        val comp = entries[0].value
        if (comp.materials.isEmpty()) {
            comp.materials = comp.getMesh()!!.materials
            for (mat in comp.materials) {
                val matI = MaterialCache[mat] ?: continue
                matI.shader = BakedLightingShader
            }
        }
        for (mat in comp.materials) {
            MaterialCache[mat]!!.shaderOverrides["bakedRect"] = TypeValue(GLSLType.V4F, value)
        }
    } else {
        val left = ArrayList<WeightedValue>(entries.size)
        val right = ArrayList<WeightedValue>(entries.size)
        // try to split left and right as evenly as possible
        var leftWeight = 0.0
        var rightWeight = 0.0
        for (i in entries.indices) { // to do this probably could be done in-place, how though?
            val entry = entries[i]
            if (leftWeight + entry.weight / 2f <= rightWeight) {
                left += entry
                leftWeight += entry.weight
            } else {
                right += entry
                rightWeight += entry.weight
            }
        }
        val splitAxis = if (remainingRegion.deltaX >= remainingRegion.deltaY) 0 else 1
        val splitValue = mix(
            remainingRegion.getMin(splitAxis),
            remainingRegion.getMax(splitAxis),
            leftWeight / (leftWeight + rightWeight)
        )
        val minX = remainingRegion.getComp(splitAxis)
        val maxX = remainingRegion.getComp(splitAxis + 3)
        remainingRegion.setComp(splitAxis, splitValue + halfPadding)
        splitRegion(right, remainingRegion, resolution)
        remainingRegion.setComp(splitAxis, minX)
        remainingRegion.setComp(3 + splitAxis, splitValue - halfPadding)
        splitRegion(left, remainingRegion, resolution)
        remainingRegion.setComp(3 + splitAxis, maxX)
    }
}

fun calculateSurfaceArea(entity: Entity, component: MeshComponent): Double {
    val mesh = component.getMesh() as? Mesh ?: return 0.0
    mesh.uvs ?: return 0.0
    entity.validateTransform()
    var area = 0.0
    val transform = entity.transform.globalTransform
    // could be optimized for uniform scale: transform only needed once, and result could be cached by mesh-ref
    // could be optimized for instanced geometry: each point only needs to be transformed once
    mesh.forEachTriangle { a, b, c ->
        transform.transformPosition(a)
        transform.transformPosition(b)
        transform.transformPosition(c)
        area += getTriangleArea(a, b, c)
        false
    }
    return area
}
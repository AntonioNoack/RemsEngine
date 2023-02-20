package me.anno.tests.mesh.hexagons

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.anim.AnimTexture
import me.anno.ecs.components.chunks.spherical.Hexagon
import me.anno.ecs.components.chunks.spherical.HexagonSphere
import me.anno.ecs.components.light.AmbientLight
import me.anno.ecs.components.light.DirectionalLight
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.sdf.SDFComponent
import me.anno.ecs.components.physics.BulletPhysics
import me.anno.ecs.components.shaders.SkyBox
import me.anno.engine.ui.control.ControlScheme
import me.anno.engine.ui.render.ECSMeshShader
import me.anno.engine.ui.render.PlayMode
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Renderer
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib.positionPostProcessing
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.Texture2DArray
import me.anno.image.ImageCPUCache
import me.anno.maths.Maths.TAUf
import me.anno.utils.OS.pictures
import me.anno.utils.hpc.ThreadLocal2
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.arrays.ExpandingFloatArray
import me.anno.utils.structures.arrays.ExpandingIntArray
import me.anno.utils.types.Floats.toRadians
import me.anno.utils.types.Triangles
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.cos
import kotlin.math.sin

// create a Minecraft world on a hex sphere :3
// use chunks

val air: Byte = 0
val stone: Byte = 1
val dirt: Byte = 2
val grass: Byte = 3
val log: Byte = 4
val leaves: Byte = 5
val gravel: Byte = 6
val sand: Byte = 7
val water: Byte = 8

val texIdsXZ = intArrayOf(-1, 16, 32, 48, 65, 83, 17, 33, 253)
val texIdsPY = intArrayOf(-1, 16, 32, 0, 81, 83, 17, 33, 253)
val texIdsNY = intArrayOf(-1, 16, 32, 32, 81, 83, 17, 33, 253)

object HSMCShader : ECSMeshShader("hexagons") {

    val texture by lazy {
        val image = ImageCPUCache[pictures.getChild("atlas.webp"), false]!!
        val images = image.split(16, 1) // create stripes
        val texture = Texture2DArray("atlas", 16, 16, 256)
        texture.create(images, true)
        texture
    }

    override fun bind(shader: Shader, renderer: Renderer, instanced: Boolean) {
        super.bind(shader, renderer, instanced)
        val ti = shader.getTextureIndex("diffuseMapStack")
        if (ti >= 0) texture.bind(ti, GPUFiltering.NEAREST, Clamping.REPEAT)
    }

    override fun createVertexStage(
        isInstanced: Boolean,
        isAnimated: Boolean,
        colors: Boolean,
        motionVectors: Boolean,
        limitedTransform: Boolean
    ): ShaderStage {
        val defines = createDefines(isInstanced, isAnimated, colors, motionVectors, limitedTransform)
        val variables = createVertexVariables(isInstanced, isAnimated, colors, motionVectors, limitedTransform)
            .filter {
                when (it.name) {
                    "uvs", "tangents" -> false
                    else -> true
                }
            }
        val stage = ShaderStage(
            "vertex",
            variables, defines +
                    "localPosition = coords;\n" + // is output, so no declaration needed
                    motionVectorInit +
                    instancedInitCode +
                    "#define tangents vec4(0,0,0,1)\n" +
                    normalInitCode +
                    // calculate uv from color to save memory and bandwidth
                    "#ifdef COLORS\n" +
                    "   int idx = int(colors0.g*255.0+0.5);\n" +
                    "   vertexColor0 = colors0;\n" +
                    "   const vec2 uvArray[11] = vec2[](" +
                    "${uv6.joinToString { "vec2(${it.x},${1f - it.y})" }}, " +
                    "${uv5.joinToString { "vec2(${it.x},${1f - it.y})" }});\n" +
                    "   uv = idx < 11 ? uvArray[idx] : vec2(bool(idx&1) ? 0.0 : 0.5, -float((idx-11)>>1));\n" +
                    "#endif\n" +
                    applyTransformCode +
                    // colorInitCode +
                    "gl_Position = transform * vec4(finalPosition, 1.0);\n" +
                    motionVectorCode +
                    positionPostProcessing
        )
        if (isAnimated && AnimTexture.useAnimTextures) stage.add(getAnimMatrix)
        if (limitedTransform) stage.add(SDFComponent.quatRot)
        return stage
    }

    override fun createFragmentStage(
        isInstanced: Boolean,
        isAnimated: Boolean,
        motionVectors: Boolean
    ): ShaderStage {
        return ShaderStage(
            "material",
            createFragmentVariables(isInstanced, isAnimated, motionVectors)
                .filter {
                    when (it.name) {
                        "emissiveMap", "diffuseMap", "normalMap", "metallicMap",
                        "roughnessMap" -> false
                        else -> true
                    }
                } + listOf(
                Variable(GLSLType.S2DA, "diffuseMapStack"),
                Variable(GLSLType.V3F, "localPosition"),
            ),
            discardByCullingPlane +
                    // step by step define all material properties
                    // baseColorCalculation +
                    "ivec2 uv2;\n" +
                    "ivec3 ts = textureSize(diffuseMapStack,0);\n" +
                    "float uvW = int(vertexColor0.b*255.0+0.5);\n" +
                    "vec4 color;\n" +
                    // todo optional edge smoothing like in Rem's Studio
                    "bool up = abs(dot(normal,normalize(localPosition)))>0.9;\n" +
                    "if(dot(abs(vec4(dFdx(uv),dFdy(uv))),vec4(1.0)) * ts.x < 1.41) {\n" +
                    "   if(up) {\n" +
                    // looks great :3, but now the quads are too large, even if they match
                    "      vec2 uv1 = fract(uv) * ts.x;\n" +
                    "      uv2 = ivec2(uv1);\n" +
                    "      if(dot(fract(uv1),vec2(1.0))>=1.0) uv2.x = (uv2.x + (ts.x>>1)) % ts.x;\n" +
                    "   } else {\n" +
                    "      vec2 uv1 = fract(uv) * ts.xy;\n" +
                    "      vec2 uvl = mod(uv1, 2.0) - 1.0;\n" +
                    "      if(abs(uvl.x)+abs(uvl.y)>=1.0) uv1 -= 2.0 * uvl;\n" +
                    "      uv2 = ivec2(uv1);\n" +
                    "   }\n" +
                    "   color = texelFetch(diffuseMapStack, ivec3(uv2, uvW), 0);\n" +
                    "} else if(up) {\n" +
                    // anisotropic filtering or similar for the distance ^^
                    "   color = (texture(diffuseMapStack,vec3(uv,uvW)) + texture(diffuseMapStack,vec3(uv+vec2(0.5,0.0),uvW))) * 0.5;\n" +
                    "} else {\n" +
                    "   color = texture(diffuseMapStack,vec3(uv,uvW));\n" +
                    "}\n" +
                    // "if(color.a < ${1f / 255f}) discard;\n" +
                    "finalColor = color.rgb;\n" +
                    "finalAlpha = color.a;\n" +
                    normalTanBitanCalculation +
                    // normalMapCalculation +
                    // emissiveCalculation +
                    // occlusionCalculation +
                    // metallicCalculation +
                    // roughnessCalculation +
                    reflectionPlaneCalculation +
                    // v0 + sheenCalculation +
                    // clearCoatCalculation +
                    (if (motionVectors) finalMotionCalculation else "")
        )
    }
}

val material = Material().apply {
    shader = HSMCShader
}

fun interface IndexMap {
    operator fun get(index: Long): Int
}

var useMeshPools = false

val positions = ThreadLocal2 { ExpandingFloatArray(8192, Texture2D.floatArrayPool) }
val normals = ThreadLocal2 { ExpandingFloatArray(8192, Texture2D.floatArrayPool) }
val colors = ThreadLocal2 { ExpandingIntArray(8192, Texture2D.intArrayPool) }

val uv6 = arrayOf(
    Vector2f(1f, 0.75f),
    Vector2f(0.5f, 1f),
    Vector2f(0f, 0.75f),
    Vector2f(0f, 0.25f),
    Vector2f(0.5f, 0f),
    Vector2f(1f, 0.25f)
).onEach {
    it.y += it.x * 0.5f
}

val uv5 = Array(5) {
    val a = it * TAUf / 5f
    val x = cos(a) * .5f + .5f
    val y = sin(a) * .5f + .5f
    Vector2f(x, y + x * 0.5f)
}

fun generateMesh(
    hexagons: List<Hexagon>, size: Int,
    mapping: IndexMap, world1: ByteArray,
    world: HexagonSphereMCWorld
): Mesh {

    val sy = world.sy

    val positions = positions.get()
    val normals = normals.get()
    val colors = colors.get()
    // val uvs = uvs.get()

    val uv6 = IntArray(6)
    val uv5 = IntArray(5)

    for (i in uv6.indices) uv6[i] = i.shl(8)
    for (i in uv5.indices) uv5[i] = (i+6).shl(8)

    val normal = Vector3f()
    val c2v = Vector3f()
    for (i in 0 until size) {
        val hex = hexagons[i]
        // val i0 = mapping[hex.index] * sy
        val i0 = i * sy // the same, just faster
        for (y in 0 until sy) {
            val here = world1[i0 + y]
            if (here != air) {
                fun addLayer(fy: Float, di0: Int, di1: Int, color: Int) {
                    val uvi = if (hex.corners.size == 6) uv6 else uv5
                    val c0 = hex.corners[0]
                    val uv0 = uvi[0]
                    for (j in 2 until hex.corners.size) {
                        positions.add(c0.x * fy, c0.y * fy, c0.z * fy)
                        normals.add(c0)
                        val c2 = hex.corners[j + di0]
                        positions.add(c2.x * fy, c2.y * fy, c2.z * fy)
                        normals.add(c2)
                        val c1 = hex.corners[j + di1]
                        positions.add(c1.x * fy, c1.y * fy, c1.z * fy)
                        normals.add(c1)
                        colors.add(color or uv0)
                        colors.add(color or uvi[j + di0])
                        colors.add(color or uvi[j + di1])
                    }
                }
                // add top/bottom
                if (y > 0 && world1[i0 + y - 1] == air) { // lowest floor is invisible
                    // add bottom
                    addLayer(world.h(y), 0, -1, texIdsNY[here.toInt().and(255)])
                    // flip normals
                    val len1 = (hex.corners.size - 2) * 3 * 3
                    for (j in normals.size - len1 until normals.size) {
                        normals[j] = -normals[j]
                    }
                }
                if (y + 1 >= sy || world1[i0 + y + 1] == air) {
                    // add top
                    addLayer(world.h(y + 1), -1, 0, texIdsPY[here.toInt().and(255)])
                }
            }
        }
        for (k in hex.neighbors.indices) {
            val neighbor = hex.neighbors[k]!!
            val i1 = mapping[neighbor.index] * sy
            if (i1 < 0) throw IllegalStateException("Missing neighbor[$k]=${neighbor.index} of list[$i]=${hex.index}")

            // sideways
            fun addSide(block: Byte, y0: Int, y1: Int) {
                val c0 = hex.corners[k]
                val c1 = hex.corners[(k + 1) % hex.corners.size]
                val color = texIdsXZ[block.toInt().and(255)]
                val h0 = world.h(y0)
                val h1 = world.h(y1)
                val v1 = ((y1 - y0) * 2 + 11).shl(8)//.toFloat()
                c2v.set(c0).mul(1.1f)
                Triangles.subCross(c0, c1, c2v, normal).normalize()
                positions.add(c0.x * h0, c0.y * h0, c0.z * h0)
                positions.add(c1.x * h1, c1.y * h1, c1.z * h1)
                positions.add(c0.x * h1, c0.y * h1, c0.z * h1)
                colors.add(color or (11.shl(8)))
                colors.add(color or (v1 + 256))
                colors.add(color or v1)
                positions.add(c0.x * h0, c0.y * h0, c0.z * h0)
                positions.add(c1.x * h0, c1.y * h0, c1.z * h0)
                positions.add(c1.x * h1, c1.y * h1, c1.z * h1)
                colors.add(color or (11.shl(8)))
                colors.add(color or (12.shl(8)))
                colors.add(color or (v1 + 256))
                for (j in 0 until 6) {
                    normals.add(normal)
                }
            }

            var lastType: Byte = 0
            var lastAir = true
            var lastY0 = 0
            for (y in 0 until sy) {
                val currType = world1[i0 + y]
                val currAir = world1[i1 + y] == air
                if (currType != lastType || currAir != lastAir) {
                    if (lastType > 0 && lastAir) addSide(lastType, lastY0, y)
                    lastY0 = y
                    lastType = currType
                    lastAir = currAir
                }
            }
            if (lastType > 0 && lastAir) {
                addSide(lastType, lastY0, sy)
            }

        }
    }

    val mesh = Mesh()
    val exact = !useMeshPools
    mesh.positions = positions.toFloatArray(canReturnSelf = false, exact)
    mesh.normals = normals.toFloatArray(canReturnSelf = false, exact)
    mesh.color0 = colors.toIntArray(canReturnSelf = false, exact)
    mesh.materials = listOf(material.ref)
    mesh.invalidateGeometry()

    positions.clear()
    normals.clear()
    colors.clear()

    return mesh

}

fun createMesh(visualList: ArrayList<Hexagon>, world: HexagonSphereMCWorld): Mesh {
    val size = visualList.size
    val (world1, indexMap) = world.generateWorld(visualList, true)
    val mesh = generateMesh(visualList, size, indexMap, world1, world)
    Texture2D.byteArrayPool.returnBuffer(world1)
    return mesh
}

fun destroyMesh(mesh: Mesh) {
    if (useMeshPools) {
        Texture2D.floatArrayPool.returnBuffer(mesh.positions)
        Texture2D.floatArrayPool.returnBuffer(mesh.normals)
        Texture2D.floatArrayPool.returnBuffer(mesh.tangents)
        Texture2D.intArrayPool.returnBuffer(mesh.color0)
    }
    mesh.destroy()
}

fun main() {

    val n = 100
    val hexagons = createHexSphere(n)
    val world = HexagonSphereMCWorld(HexagonSphere(100, 1))

    var i0 = 0
    val scene = Entity()
    for (chunkId in 0 until chunkCount) {
        val i1 = calculateChunkEnd(chunkId, n)
        scene.add(Entity().apply {
            val mesh = createMesh(ArrayList(hexagons.subList(i0, i1)), world)
            add(MeshComponent(mesh))
            // add(MeshCollider(mesh).apply { isConvex = false }) // much too expensive
            // add(Rigidbody().apply { isStatic = true })
        })
        i0 = i1
    }

    val sky = SkyBox()
    sky.spherical = true
    scene.add(sky)

    val physics = BulletPhysics()
    scene.add(physics)
    physics.gravity.set(0.0)

    val sun = DirectionalLight()
    sun.shadowMapCascades = 1
    sun.shadowMapResolution = 4096
    sun.autoUpdate = true
    sun.color.set(5f)
    val sunEntity = Entity("Sun")
    sunEntity.add(sun)
    sunEntity.scale = Vector3d(2.0)
    sunEntity.rotation = sunEntity.rotation
        .identity()
        .set(sky.sunRotation)

    sunEntity.add(object : Component() {
        override fun clone() = throw NotImplementedError()
        override fun onUpdate(): Int {
            sky.applyOntoSun(sunEntity, sun, 5f)
            return 1
        }
    })


    val ambient = AmbientLight()
    ambient.color.set(0.5f)
    scene.add(ambient)
    scene.add(sunEntity)

    // todo add sphere as a test object for collisions

    testSceneWithUI(scene) {
        if (false) {
            it.renderer.playMode = PlayMode.PLAYING // remove grid
            it.renderer.enableOrbiting = false
            it.renderer.radius = 0.1
            it.playControls = ControllerOnSphere(it.renderer, sky)
        }
    }

}

// todo change light direction to come from sun

open class ControllerOnSphere(
    rv: RenderView,
    val sky: SkyBox?
) : ControlScheme(rv) {

    // todo walk and jump with physics
    // todo set and destroy blocks

    val forward = Vector3d(0.0, 0.0, -1.0)
    val right = Vector3d(1.0, 0.0, 0.0)
    val position = rv.position
    val up = Vector3d()

    init {
        if (position.length() < 1e-16) {
            position.set(0.0, 1.52, 0.0)
        }// todo else find axes :)
        position.normalize(up)
    }

    override fun rotateCamera(vx: Float, vy: Float, vz: Float) {
        val axis = up
        val s = 1.0.toRadians()
        forward.rotateAxis(vy * s, axis.x, axis.y, axis.z)
        right.rotateAxis(vy * s, axis.x, axis.y, axis.z)
        val dx = vx * s
        // todo clamp angle
        // val currAngle = forward.angleSigned(up, right)
        // val dx2 = clamp(currAngle + dx, 1.0.toRadians(), 179.0.toRadians()) - currAngle
        forward.rotateAxis(dx, right.x, right.y, right.z)
        correctAxes()
    }

    fun correctAxes() {
        val dirY = up
        val er1 = right.dot(dirY)
        right.sub(er1 * dirY.x, er1 * dirY.y, er1 * dirY.z)
        val dirZ = forward
        val er2 = right.dot(dirZ)
        right.sub(er2 * dirZ.x, er2 * dirZ.y, er2 * dirZ.z)
        right.normalize()
    }

    override fun updateViewRotation() {
        view.rotation.identity()
            .lookAlong(forward, up)
            .invert()
        view.updateEditorCameraTransform()
        invalidateDrawing()
    }

    override fun moveCamera(dx: Double, dy: Double, dz: Double) {
        val height = position.length()
        position.add(dx * right.x, dx * right.y, dx * right.z)
        position.sub(dz * forward.x, dz * forward.y, dz * forward.z)
        position.normalize(height + dy)
        onChangePosition()
    }

    fun onChangePosition() {
        val rot = JomlPools.quat4d.borrow().rotationTo(up, position)
        position.normalize(up)
        rot.transform(forward)
        rot.transform(right)
        sky?.worldRotation?.mul(-rot.x.toFloat(), -rot.y.toFloat(), -rot.z.toFloat(), rot.w.toFloat())
        correctAxes()
    }

}

// todo free: one small world, basic MC-like mechanics
// todo cost: unlimited worlds
// todo cost: multiplayer
// todo cost: chemistry dlc, mineral dlc, gun play dlc...
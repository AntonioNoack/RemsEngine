package me.anno.tests.mesh

import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.chunks.spherical.Hexagon
import me.anno.ecs.components.chunks.spherical.HexagonSphere
import me.anno.ecs.components.chunks.spherical.HexagonSphere.calculateChunkEnd
import me.anno.ecs.components.chunks.spherical.HexagonSphere.chunkCount
import me.anno.ecs.components.chunks.spherical.HexagonSphere.findLength
import me.anno.ecs.components.light.AmbientLight
import me.anno.ecs.components.light.DirectionalLight
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
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
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.Texture2DArray
import me.anno.image.ImageCPUCache
import me.anno.maths.Maths.TAUf
import me.anno.maths.Maths.pow
import me.anno.maths.noise.FullNoise
import me.anno.maths.noise.PerlinNoise
import me.anno.utils.OS.pictures
import me.anno.utils.hpc.ThreadLocal2
import me.anno.utils.pooling.JomlPools
import me.anno.utils.structures.arrays.ExpandingFloatArray
import me.anno.utils.structures.arrays.ExpandingIntArray
import me.anno.utils.types.Arrays.subList
import me.anno.utils.types.Floats.toRadians
import me.anno.utils.types.Triangles
import org.joml.Vector2f
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.sin

// create a Minecraft world on a hex sphere :3
// use chunks

val air: Byte = 0
val stone: Byte = 1
val dirt: Byte = 2
val grass: Byte = 3
val log: Byte = 4
val leaves: Byte = 5

val texIdsXZ = intArrayOf(-1, 16, 32, 48, 65, 83)
val texIdsPY = intArrayOf(-1, 16, 32, 0, 81, 83)
val texIdsNY = intArrayOf(-1, 16, 32, 32, 81, 83)

val minHeight = -64
val maxHeight = 64
val sy = maxHeight - minHeight

val material = Material().apply {
    shader = object : ECSMeshShader("hexagons") {

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
            if (ti >= 0) texture.bind(ti)
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
                        // todo anisotropic filtering or similar for the distance
                        // todo optional edge smoothing like in Rem's Studio
                        "if(abs(dot(normal,normalize(localPosition)))>0.9){\n" +
                        // looks great :3, but now the quads are too large, even if they match
                        "   vec2 uv1 = fract(vec2(uv.x,uv.y-0.5*uv.x)) * ts.x;\n" +
                        "   uv2 = ivec2(uv1);\n" +
                        "   if(dot(fract(uv1),vec2(1.0))>=1.0) uv2.x = (uv2.x + 8) % ts.x;\n" +
                        "} else {\n" +
                        "   vec2 uv1 = fract(uv) * ts.xy;\n" +
                        "   vec2 uvl = mod(uv1, 2.0) - 1.0;\n" +
                        "   if(abs(uvl.x)+abs(uvl.y)>=1.0) uv1 -= 2.0 * uvl;\n" +
                        "   uv2 = ivec2(uv1);\n" +
                        "}\n" +
                        "vec4 texDiffuseMap = texelFetch(diffuseMapStack, ivec3(uv2, int(vertexColor0.b*255.0+0.5)), 0);\n" +
                        "vec4 color = diffuseBase * texDiffuseMap;\n" +
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
}

fun interface IndexMap {
    fun map(index: Long): Int
}

val rnd = FullNoise(2345L)

fun generateWorld(hexagons: List<Hexagon>, n: Int): ByteArray {
    val idMap = hexagons.withIndex().associate { it.value.index to it.index }
    return generateWorld(hexagons, { idMap[it] ?: -1 }, n)
}

fun generateWorld(hexagons: List<Hexagon>, mapping: IndexMap, n: Int): ByteArray {

    val size = hexagons.size * sy
    val world = ByteArray(size)// Texture2D.byteArrayPool[size, false, false]
    // world.fill(air, 0, size)

    val perlin = PerlinNoise(1234L, 8, 0.5f, -63f, 56f, Vector4f(n / 100f))
    for (i in hexagons.indices) {
        val wi = mapping.map(hexagons[i].index) * sy
        val cen = hexagons[i].center
        val hi = (perlin[cen.x, cen.y, cen.z] - minHeight).toInt()
        for (y in 0 until hi - 3) world[wi + y] = stone
        for (y in hi - 3 until hi - 1) world[wi + y] = dirt
        for (y in hi - 1 until hi) world[wi + y] = grass
    }

    // generate random trees :3
    for (i in hexagons.indices) {
        val hex0 = hexagons[i]
        if (rnd[hex0.index.toInt()] < 0.03f) {
            val wi = mapping.map(hex0.index) * sy
            val cen = hex0.center
            val hi = (perlin[cen.x, cen.y, cen.z] - minHeight).toInt()
            for (y in hi + 3 until hi + 6) world[wi + y] = leaves
            for (neighbor0 in hex0.neighbors) {
                neighbor0 ?: continue
                val i1 = mapping.map(neighbor0.index) * sy
                if (i1 >= 0) for (y in hi + 2 until hi + 5) world[i1 + y] = leaves
                for (neighbor1 in neighbor0.neighbors) {
                    neighbor1 ?: continue
                    val i2 = mapping.map(neighbor1.index) * sy
                    if (i2 >= 0) for (y in hi + 2 until hi + 4) world[i2 + y] = leaves
                }
            }
            for (y in hi until hi + 3) world[wi + y] = log
        }
    }

    return world

}

var useMeshPools = false

val positions = ThreadLocal2 { ExpandingFloatArray(8192, Texture2D.floatArrayPool) }
val normals = ThreadLocal2 { ExpandingFloatArray(8192, Texture2D.floatArrayPool) }
val colors = ThreadLocal2 { ExpandingIntArray(8192, Texture2D.intArrayPool) }
val uvs = ThreadLocal2 { ExpandingFloatArray(8192, Texture2D.floatArrayPool) }

val uv6 = arrayOf(
    Vector2f(1f, 0.75f),
    Vector2f(0.5f, 1f),
    Vector2f(0f, 0.75f),
    Vector2f(0f, 0.25f),
    Vector2f(0.5f, 0f),
    Vector2f(1f, 0.25f)
)

val uv5 = Array(5) {
    val a = it * TAUf / 5f
    Vector2f(cos(a) * .5f + .5f, sin(a) * .5f + .5f)
}

fun h(yi: Int, len: Float): Float {
    val y = yi + minHeight
    val base = 1f + 0.866f * len
    return pow(base, y.toFloat())
}

fun yi(h: Float, len: Float): Float {
    val base = 1f + 0.866f * len
    return ln(h) / ln(base) - minHeight
}

fun generateMesh(hexagons: List<Hexagon>, mapping: IndexMap, world: ByteArray, len: Float, fillVoid: Boolean): Mesh {

    val positions = positions.get()
    val normals = normals.get()
    val colors = colors.get()
    val uvs = uvs.get()

    val normal = Vector3f()
    val c2v = Vector3f()
    for (i in hexagons.indices) {
        val hex = hexagons[i]
        val i0 = mapping.map(hex.index) * sy
        for (y in 0 until sy) {
            val here = world[i0 + y]
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
                        uvs.add(uv0)
                        uvs.add(uvi[j + di0])
                        uvs.add(uvi[j + di1])
                        colors.add(color)
                        colors.add(color)
                        colors.add(color)
                    }
                }
                // add top/bottom
                if (y > 0 && world[i0 + y - 1] == air) { // lowest floor is invisible
                    // add bottom
                    addLayer(h(y, len), 0, -1, texIdsNY[here.toInt().and(255)])
                    // flip normals
                    val len1 = (hex.corners.size - 2) * 3 * 3
                    for (j in normals.size - len1 until normals.size) {
                        normals[j] = -normals[j]
                    }
                }
                if (y + 1 >= sy || world[i0 + y + 1] == air) {
                    // add top
                    addLayer(h(y + 1, len), -1, 0, texIdsPY[here.toInt().and(255)])
                }
            }
        }
        for (k in hex.neighbors.indices) {
            val neighbor = hex.neighbors[k] ?: break
            val i1 = mapping.map(neighbor.index) * sy

            // sideways
            fun addSide(block: Byte, y0: Int, y1: Int) {
                val c0 = hex.corners[k]
                val c1 = hex.corners[(k + 1) % hex.corners.size]
                val color = texIdsXZ[block.toInt().and(255)]
                val h0 = h(y0, len)
                val h1 = h(y1, len)
                val v1 = (y1 - y0).toFloat()
                c2v.set(c0).mul(1.1f)
                Triangles.subCross(c0, c1, c2v, normal).normalize()
                positions.add(c0.x * h0, c0.y * h0, c0.z * h0)
                positions.add(c1.x * h1, c1.y * h1, c1.z * h1)
                positions.add(c0.x * h1, c0.y * h1, c0.z * h1)
                uvs.add(0.00f, 0f)
                uvs.add(0.50f, v1)
                uvs.add(0.00f, v1)
                positions.add(c0.x * h0, c0.y * h0, c0.z * h0)
                positions.add(c1.x * h0, c1.y * h0, c1.z * h0)
                positions.add(c1.x * h1, c1.y * h1, c1.z * h1)
                uvs.add(0.00f, 0f)
                uvs.add(0.50f, 0f)
                uvs.add(0.50f, v1)
                for (j in 0 until 6) {
                    normals.add(normal)
                    colors.add(color)
                }
            }

            if (i1 >= 0) {
                // todo can be made more efficient by joining sides
                for (y in 0 until sy) {
                    val here = world[i0 + y]
                    if (here != air && world[i1 + y] == air) {
                        // add side
                        addSide(here, y, y + 1)
                    }
                }
            } else if (fillVoid) { // unknown (chunk border)
                var lastType: Byte = 0
                var lastY0 = 0
                for (y in 0 until sy) {
                    val currType = world[i0 + y]
                    if (currType != lastType) {
                        if (lastType > 0) addSide(lastType, lastY0, y)
                        lastY0 = y
                        lastType = currType
                    }
                }
                if (lastType > 0) {
                    addSide(lastType, lastY0, sy)
                }
            }
        }
    }

    val mesh = Mesh()
    val exact = !useMeshPools
    mesh.positions = positions.toFloatArray(canReturnSelf = false, exact)
    mesh.normals = normals.toFloatArray(canReturnSelf = false, exact)
    mesh.color0 = colors.toIntArray(canReturnSelf = false, exact)
    mesh.uvs = uvs.toFloatArray(canReturnSelf = false, exact)
    mesh.materials = listOf(material.ref)
    mesh.invalidateGeometry()

    positions.clear()
    normals.clear()
    colors.clear()
    uvs.clear()

    return mesh

}

class MeshBuildHelper(n: Int) {
    val size = n * (n + 1)
    val set0 = HashSet<Hexagon>(size)
    val set1 = HashSet<Hexagon>(size)
    val extraIds = HashMap<Long, Int>(size)
    val genList = ArrayList<Hexagon>(size)
    val primaryIds = HashMap<Long, Int>(size)
}

fun createMesh(visualList: List<Hexagon>, n: Int, helper: MeshBuildHelper): Mesh {

    val primaryIds = helper.primaryIds
    primaryIds.clear()
    for (i in visualList.indices) {
        primaryIds[visualList[i].index] = i
    }

    fun add(hex: Hexagon, dst: HashSet<Hexagon>) {
        val nei = hex.neighbors
        for (hex1 in nei) {
            if (hex1 != null && hex1.index !in primaryIds)
                dst.add(hex1)
        }
    }

    val set0 = helper.set0
    val set1 = helper.set1
    set1.clear()
    for (hex in visualList) {
        add(hex, set1)
    }
    set0.clear()
    for (hex in set1) {
        add(hex, set0)
    }
    set1.addAll(set0)

    val worldList = helper.genList
    worldList.clear()
    worldList.addAll(visualList)
    worldList.addAll(set1)

    val borderIds = helper.extraIds
    borderIds.clear()
    for (i in visualList.size until worldList.size) {
        borderIds[worldList[i].index] = i
    }

    val map0 = IndexMap { id -> primaryIds[id] ?: borderIds[id] ?: -1 }
    val world = generateWorld(worldList, map0, n)
    val map1 = IndexMap { id -> primaryIds[id] ?: -1 }
    val mesh = generateMesh(visualList, map1, world, findLength(n), true)
    Texture2D.byteArrayPool.returnBuffer(world)
    return mesh
}

fun destroyMesh(mesh: Mesh) {
    if (useMeshPools) {
        Texture2D.floatArrayPool.returnBuffer(mesh.positions)
        Texture2D.floatArrayPool.returnBuffer(mesh.normals)
        Texture2D.floatArrayPool.returnBuffer(mesh.uvs)
        Texture2D.floatArrayPool.returnBuffer(mesh.tangents)
        Texture2D.intArrayPool.returnBuffer(mesh.color0)
    }
    mesh.destroy()
}

fun main() {

    val n = 100
    val hexagons = HexagonSphere.createHexSphere(n)

    var i0 = 0
    val scene = Entity()
    val helper = MeshBuildHelper(n)
    for (chunkId in 0 until chunkCount) {
        val i1 = calculateChunkEnd(chunkId, n)
        scene.add(Entity().apply {
            val mesh = createMesh(hexagons.subList(i0, i1), n, helper)
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

open class ControllerOnSphere(rv: RenderView, val sky: SkyBox?) : ControlScheme(rv) {

    // todo walk and jump with physics
    // todo set and destroy blocks

    val forward = Vector3d(0.0, 0.0, -1.0)
    val right = Vector3d(1.0, 0.0, 0.0)
    val position = rv.position
    val up = Vector3d()

    init {
        if (position.length() < 1e-16) {
            position.set(0.0, 1.52, 0.0) // 1.52 is a good start
        }// todo else find axes :)
        position.normalize(up)
    }

    override fun rotateCamera(vx: Float, vy: Float, vz: Float) {
        val axis = up
        val s = 1.0.toRadians()
        forward.rotateAxis(vy * s, axis.x, axis.y, axis.z)
        right.rotateAxis(vy * s, axis.x, axis.y, axis.z)
        val dx = vx * s
        // val currAngle = forward.angleSigned(up, right)
        // val dx2 = clamp(currAngle + dx, 1.0.toRadians(), 179.0.toRadians()) - currAngle
        forward.rotateAxis(dx, right.x, right.y, right.z) // todo clamp angle (how?)
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
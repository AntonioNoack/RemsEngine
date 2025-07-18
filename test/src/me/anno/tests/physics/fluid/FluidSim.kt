package me.anno.tests.physics.fluid

import me.anno.Time
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshCache
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.material.MaterialCache
import me.anno.ecs.systems.OnUpdate
import me.anno.engine.WindowRenderFlags
import me.anno.engine.OfficialExtensions
import me.anno.engine.ui.control.DraggingControls
import me.anno.engine.ui.render.ECSMeshShader
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.renderPurely
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.shader.renderer.Renderer
import me.anno.gpu.shader.renderer.Renderer.Companion.copyRenderer
import me.anno.gpu.texture.Texture2D
import me.anno.input.Input
import me.anno.input.Key
import me.anno.maths.Maths.PIf
import me.anno.maths.Maths.max
import me.anno.maths.Maths.mix
import me.anno.tests.physics.fluid.FluidMeshShader.createFluidMesh
import me.anno.tests.physics.fluid.FluidSimulator.splashShader
import me.anno.tests.physics.fluid.FluidSimulator.splatShader
import me.anno.ui.Panel
import me.anno.utils.OS.downloads
import org.joml.AABBd
import org.joml.Matrix4x3
import kotlin.math.pow
import kotlin.random.Random

var mx = 0f
var my = 0f

val cellSize = 0.03f
val waveHeight = 5f

fun step(it: Panel, lx: Float, ly: Float, s: Float, sim: FluidSimulation) {

    val dt = Time.deltaTime
    renderPurely {

        useFrame(copyRenderer) {

            // when the user clicks, we spawn an outgoing circle
            val pressForce = when {
                Input.wasKeyPressed(Key.BUTTON_LEFT) -> +1
                Input.wasKeyReleased(Key.BUTTON_LEFT) -> -1
                else -> 0
            }
            if (pressForce != 0) {
                GFXState.blendMode.use(BlendMode.PURE_ADD) {
                    splashShader.apply {
                        useFrame(sim.velocity.read) {
                            use()
                            v1f("strength", 10f * s * pressForce)
                            v4f("posSize", lx, ly, s, s)
                            v4f("tiling", 1f, 1f, 0f, 0f)
                            m4x4("transform")
                            flat01.draw(splashShader)
                        }
                    }
                }
            }

            // user interaction
            val force = 100f * s / it.height
            val dx = mx * force
            val dy = my * force
            if (dx != 0f || dy != 0f) {
                GFXState.blendMode.use(BlendMode.PURE_ADD) {
                    splatShader.apply {
                        useFrame(sim.velocity.read) {
                            use()
                            v4f("color", dx, dy, 0f, 0f)
                            v4f("posSize", lx, ly, s, s)
                            v4f("tiling", 1f, 1f, 0f, 0f)
                            m4x4("transform")
                            flat01.draw(splatShader)
                        }
                    }
                }
                mx = 0f
                my = 0f
            }

            sim.step(dt.toFloat())

        }
    }
}

class ParticleShader(val sim: FluidSimulation) : ECSMeshShader("particles") {
    override fun createVertexStages(key: ShaderKey): List<ShaderStage> {
        val variables = createAnimVariables(key) + listOf(
            Variable(GLSLType.V3F, "areaSize"),
            Variable(GLSLType.S2D, "positionTex"),
            Variable(GLSLType.S2D, "rotationTex"),
            Variable(GLSLType.S2D, "metadataTex"),
            Variable(GLSLType.V3F, "localPosition", VariableMode.OUT)
        )
        val vertexPosOverrideStage = ShaderStage(
            "vertex",
            variables, "" +
                    "ivec2 texSize = textureSize(positionTex,0);\n" +
                    "ivec2 particleUV = ivec2(gl_InstanceID % texSize.x, gl_InstanceID / texSize.x);\n" +
                    "vec3 particlePos = texelFetch(positionTex, particleUV, 0).xyz - vec3(0.5, 0.0, 0.5);\n" +
                    "localPosition = particlePos * areaSize + positions;\n"
        )
        return createDefines(key) +
                loadVertex(key) +
                vertexPosOverrideStage +
                animateVertex(key) +
                transformVertex(key) +
                finishVertex(key)
    }

    override fun bind(shader: Shader, renderer: Renderer, instanced: Boolean) {
        super.bind(shader, renderer, instanced)
        val data = sim.particles.read
        data.getTextureI(0).bindTrulyLinear(shader, "positionTex")
        data.getTextureI(2).bindTrulyLinear(shader, "rotationTex")
        data.getTextureI(3).bindTrulyLinear(shader, "metadataTex")
        shader.v3f("areaSize", sim.width * cellSize, waveHeight, sim.height * cellSize)
    }
}

class DuckComponent : MeshComponent() {
    lateinit var sim: FluidSimulation

    // extend theoretical bounds
    override fun fillSpace(globalTransform: Matrix4x3, dstUnion: AABBd) {
        localAABB.setMin(-sim.width * 0.5 * cellSize, 0.0, -sim.height * 0.5 * cellSize)
        localAABB.setMax(+sim.width * 0.5 * cellSize, waveHeight.toDouble(), +sim.height * 0.5 * cellSize)
        localAABB.transform(globalTransform, globalAABB)
        dstUnion.union(globalAABB)
    }
}

/**
 * small gpu fluid simulation
 *
 * adapted from https://github.com/PavelDoGreat/WebGL-Fluid-Simulation/blob/master/script.js
 *
 * todo add terrain (2.5d, without overhangs)
 * done: add small bodies like boats/ducks/... on the surface
 * */
fun main() {

    OfficialExtensions.initForTests()

    val w = 1024
    val h = 1024
    val p = 1024
    val sim = FluidSimulation(w, h, p)

    val duckModel = downloads.getChild("3d/Rubber Duck.glb")

    val init = lazy {
        // initialize textures
        sim.velocity.read.clearColor(0f, 0f, 0f, 1f) // 2d, so -1 = towards bottom right
        sim.pressure.read.clearColor(1f, 0f, 0f, 1f) // 1d, so max level
        // initialize sim.particles
        sim.particles.read.clearColor(0) // set everything zero
        // initialize positions randomly
        val particleData = sim.particles.read
        val wi = particleData.width
        val hi = particleData.height
        val positions = FloatArray(wi * hi * 3)
        val random = Random(1234)
        for (i in 0 until p) {
            positions[i * 3 + 0] = mix(0.01f, 0.99f, random.nextFloat())
            positions[i * 3 + 2] = mix(0.01f, 0.99f, random.nextFloat())
            positions[i * 3 + 1] = 1f
        }
        (particleData.getTextureI(0) as Texture2D).createRGB(positions, false)
        // initialize metadata randomly
        val metadata = FloatArray(wi * hi * 4) // min-fluid-height, radius, mass
        for (i in 0 until wi * hi) {
            val radius = 0.05f / waveHeight // mix(0.1f, 1f, random.nextFloat())
            val density = 0.2f
            val volume = 4f / 3f * PIf * radius.pow(3)
            metadata[i * 4] = radius
            metadata[i * 4 + 1] = radius
            metadata[i * 4 + 2] = density * volume // mass
            metadata[i * 4 + 3] = density
        }
        (particleData.getTextureI(3) as Texture2D).createRGBA(metadata, false)
        // todo set rotation randomly
    }

    val scene = Entity("Scene")
    val ducks = Entity("Ducks", scene)
    val materials = MeshCache.getEntry(duckModel).waitFor()!!.materials
    val shader = ParticleShader(sim)
    val newMaterials = materials.map {
        println(MaterialCache.getEntry(it).waitFor()!!.run { "$name: $diffuseBase, $this" })
        val material = MaterialCache.getEntry(it).waitFor()!!.clone() as Material
        material.shader = shader
        material.ref
    }
    for (i in 0 until p) {
        val duck = DuckComponent()
        duck.sim = sim
        duck.meshFile = duckModel
        duck.isInstanced = true
        duck.materials = newMaterials
        ducks.add(duck)
    }

    val mesh = createFluidMesh(sim, waveHeight)
    val comp = object : MeshComponent(mesh), OnUpdate {

        override fun onUpdate() {
            val ci = RenderView.currentInstance ?: return
            // calculate interaction coordinates
            val rayPos = ci.mousePosition
            val rayDir = ci.mouseDirection
            val dist = (waveHeight - rayPos.y) / rayDir.y
            val gx = 0.5f + (rayPos.x + dist * rayDir.x) / (w * cellSize)
            val gz = 0.5f + (rayPos.z + dist * rayDir.z) / (h * cellSize)
            val lx = if (dist > 0f) gx.toFloat() else 0f
            val ly = if (dist > 0f) gz.toFloat() else 0f
            // initialize, if needed
            init.value
            // step physics
            step(ci, lx, ly, 0.2f * dist.toFloat() / (max(w, h) * cellSize), sim)
        }

        override fun fillSpace(globalTransform: Matrix4x3, dstUnion: AABBd) {
            localAABB.set(mesh.getBounds())
            localAABB.minY = -50.0
            localAABB.maxY = +50.0
            localAABB.transform(globalTransform, globalAABB)
            dstUnion.union(globalAABB)
        }
    }
    scene.add(comp)

    // we handle collisions ourselves
    comp.collisionMask = 0
    testSceneWithUI("FluidSim", scene) {
        WindowRenderFlags.enableVSync = true
        it.editControls = object : DraggingControls(it.renderView) {
            override fun onMouseClicked(x: Float, y: Float, button: Key, long: Boolean) {}
            override fun onMouseMoved(x: Float, y: Float, dx: Float, dy: Float) {
                super.onMouseMoved(x, y, dx, dy)
                if (Input.isLeftDown) {
                    mx += dx
                    my += dy
                }
            }
        }
    }
}
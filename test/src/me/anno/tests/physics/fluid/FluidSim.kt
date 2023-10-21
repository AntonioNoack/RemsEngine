package me.anno.tests.physics.fluid

import me.anno.Time
import me.anno.ecs.Entity
import me.anno.ecs.components.anim.AnimTexture
import me.anno.ecs.components.mesh.*
import me.anno.ecs.components.mesh.terrain.TerrainUtils
import me.anno.engine.ui.control.DraggingControls
import me.anno.engine.ui.render.ECSMeshShader
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.GFX.flat01
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.renderPurely
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.pipeline.PipelineStage.Companion.TRANSPARENT_PASS
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Renderer.Companion.copyRenderer
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.input.Input
import me.anno.input.Key
import me.anno.maths.Maths.hasFlag
import me.anno.maths.Maths.max
import me.anno.tests.physics.fluid.FluidSimulator.splashShader
import me.anno.tests.physics.fluid.FluidSimulator.splatShader
import me.anno.ui.Panel
import me.anno.utils.types.Arrays.resize
import org.joml.AABBd
import org.joml.Matrix4x3d

var mx = 0f
var my = 0f

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
                splashShader.apply {
                    useFrame(sim.velocity.read) {
                        GFXState.blendMode.use(BlendMode.PURE_ADD) {
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
            //  velocity.read -> velocity.write
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

val fluidMeshShader = object : ECSMeshShader("fluid") {
    override fun createVertexStages(flags: Int): List<ShaderStage> {
        val defines = createDefines(flags)
        val variables = createVertexVariables(flags)
        val stage = ShaderStage(
            "vertex",
            variables + listOf(
                Variable(GLSLType.S2D, "heightTex"),
                Variable(GLSLType.V1F, "waveHeight")
            ), defines.toString() +
                    "localPosition = coords + vec3(0,waveHeight * texture(heightTex,uvs).x,0);\n" + // is output, so no declaration needed
                    motionVectorInit +

                    instancedInitCode +

                    // normalInitCode +
                    "#ifdef COLORS\n" +
                    "   vec2 texSize = textureSize(heightTex,0);\n" +
                    "   vec2 du = vec2(1.0/texSize.x,0.0), dv = vec2(0.0,1.0/texSize.y);\n" +
                    "   float dx = texture(heightTex,uvs+du).x - texture(heightTex,uvs-du).x;\n" +
                    "   float dz = texture(heightTex,uvs+dv).x - texture(heightTex,uvs-dv).x;\n" +
                    "   normal = normalize(vec3(dx*waveHeight, 1.0, dz*waveHeight));\n" +
                    "   tangent = tangents;\n" +
                    "#endif\n" +

                    applyTransformCode +
                    colorInitCode +
                    "gl_Position = matMul(transform, vec4(finalPosition, 1.0));\n" +
                    motionVectorCode +
                    ShaderLib.positionPostProcessing
        )
        if (flags.hasFlag(IS_ANIMATED) && AnimTexture.useAnimTextures) stage.add(getAnimMatrix)
        if (flags.hasFlag(USES_PRS_TRANSFORM)) stage.add(ShaderLib.quatRot)
        return listOf(stage)
    }
}

fun createFluidMesh(sim: FluidSimulation, waveHeight: Float): Mesh {

    // todo use procedural mesh instead?

    val w = sim.width
    val h = sim.height
    val mesh = Mesh()
    TerrainUtils.generateRegularQuadHeightMesh(
        w, h, 0, w, false, 1f, mesh,
        { 0f }, { -1 }
    )
    // generate UVs
    val pos = mesh.positions!!
    val uvs = mesh.uvs.resize(pos.size / 3 * 2)
    for (i in uvs.indices step 2) {
        uvs[i] = 0.5f + pos[i / 2 * 3] / w
        uvs[i + 1] = 0.5f - pos[i / 2 * 3 + 2] / h
    }
    mesh.uvs = uvs
    mesh.invalidateGeometry()

    val fluidMaterial = Material()
    fluidMaterial.shader = fluidMeshShader
    fluidMaterial.shaderOverrides["heightTex"] = TypeValueV2(GLSLType.S2D) { sim.pressure.read }
    fluidMaterial.shaderOverrides["waveHeight"] = TypeValue(GLSLType.V1F, waveHeight)
    fluidMaterial.pipelineStage = TRANSPARENT_PASS
    fluidMaterial.metallicMinMax.set(1f)
    fluidMaterial.roughnessMinMax.set(0f)
    fluidMaterial.diffuseBase.w = 1f
    fluidMaterial.indexOfRefraction = 1.33f // water
    mesh.materials = listOf(fluidMaterial.ref)
    return mesh
}

/**
 * small gpu fluid simulation
 *
 * adapted from https://github.com/PavelDoGreat/WebGL-Fluid-Simulation/blob/master/script.js
 *
 * todo add terrain (2.5d, without overhangs)
 * todo add small bodies like boats/ducks/... on the surface
 * */
fun main() {

    val w = 1024
    val h = 1024
    val p = 20
    val sim = FluidSimulation(w, h, p)

    val init = lazy {
        // initialize textures
        sim.velocity.read.clearColor(0f, 0f, 0f, 1f) // 2d, so -1 = towards bottom right
        sim.pressure.read.clearColor(0.5f, 0f, 0f, 1f) // 1d, so max level
        // todo initialize sim.particles randomly
        sim.particles.read.clearColor(0)
    }

    val waveHeight = 50f
    val mesh = createFluidMesh(sim, waveHeight)
    val comp = object : MeshComponent(mesh) {

        fun update(ci: RenderView) {
            // calculate interaction coordinates
            val rayDir = ci.getMouseRayDirection()
            val rayPos = ci.cameraPosition
            val dist = (waveHeight - rayPos.y) / rayDir.y
            val gx = 0.5f + (rayPos.x + dist * rayDir.x) / w
            val gz = 0.5f + (rayPos.z + dist * rayDir.z) / h
            val lx = if (dist > 0f) gx.toFloat() else 0f
            val ly = if (dist > 0f) gz.toFloat() else 0f
            // initialize, if needed
            init.value
            // step physics
            step(ci, lx, ly, 0.2f * dist.toFloat() / max(w, h), sim)
        }

        override fun onUpdate(): Int {
            super.onUpdate()
            val ci = RenderView.currentInstance
            if (ci != null) update(ci)
            return 1
        }

        override fun fillSpace(globalTransform: Matrix4x3d, aabb: AABBd): Boolean {
            localAABB.set(mesh.getBounds())
            localAABB.minY = -50.0
            localAABB.maxY = +50.0
            localAABB.transform(globalTransform, globalAABB)
            aabb.union(globalAABB)
            return true
        }
    }
    val scene = Entity(comp)
    // we handle collisions ourselves
    comp.collisionMask = 0
    testSceneWithUI("FluidSim", scene) {
        it.editControls = object : DraggingControls(it.renderer) {
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
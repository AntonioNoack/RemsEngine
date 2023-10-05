package me.anno.tests.physics

import me.anno.Time
import me.anno.ecs.Entity
import me.anno.ecs.components.anim.AnimTexture
import me.anno.ecs.components.mesh.*
import me.anno.ecs.components.mesh.terrain.TerrainUtils
import me.anno.engine.ui.control.DraggingControls
import me.anno.engine.ui.render.ECSMeshShader
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.GFX
import me.anno.gpu.GFX.clip
import me.anno.gpu.GFX.flat01
import me.anno.gpu.GFXState
import me.anno.gpu.GFXState.renderPurely
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.blending.BlendMode
import me.anno.gpu.drawing.DrawTexts.drawSimpleTextCharByChar
import me.anno.gpu.drawing.GFXx2D
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.pipeline.PipelineStage.Companion.TRANSPARENT_PASS
import me.anno.gpu.shader.BaseShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Renderer.Companion.copyRenderer
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.ShaderLib.coordsList
import me.anno.gpu.shader.ShaderLib.coordsVShader
import me.anno.gpu.shader.ShaderLib.simpleVertexShader
import me.anno.gpu.shader.ShaderLib.simpleVertexShaderList
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.gpu.texture.ITexture2D
import me.anno.gpu.texture.TextureLib.blackTexture
import me.anno.input.Input
import me.anno.input.Key
import me.anno.maths.Maths.hasFlag
import me.anno.maths.Maths.max
import me.anno.ui.Panel
import me.anno.ui.base.constraints.AxisAlignment
import me.anno.ui.debug.TestDrawPanel.Companion.testDrawing
import me.anno.utils.types.Arrays.resize
import org.joml.AABBd
import org.joml.Matrix4x3d
import org.joml.Vector2f
import kotlin.math.exp

/**
 * small gpu fluid simulation
 *
 * adapted from https://github.com/PavelDoGreat/WebGL-Fluid-Simulation/blob/master/script.js
 * */
fun main() {

    val w = 1024
    val h = 1024

    class State(init: () -> Framebuffer) {
        var read = init()
        var write = init()
        fun swap() {
            val tmp = read
            read = write
            write = tmp
        }
    }

    val uvs1 = "" +
            "   vec2 vL = uv - vec2(texelSize.x, 0.0);\n" +
            "   vec2 vR = uv + vec2(texelSize.x, 0.0);\n" +
            "   vec2 vT = uv - vec2(0.0, texelSize.y);\n" +
            "   vec2 vB = uv + vec2(0.0, texelSize.y);\n"

    val velocity = State { Framebuffer("velocity", w, h, TargetType.FloatTarget2) }
    val divergence = Framebuffer("divergence", w, h, TargetType.FloatTarget1)
    val pressure = State { Framebuffer("pressure", w, h, TargetType.FloatTarget1) }

    val splatShader = Shader(
        "splat", simpleVertexShaderList, simpleVertexShader, uvList,
        listOf(
            Variable(GLSLType.V4F, "color"),
            Variable(GLSLType.V4F, "result", VariableMode.OUT)
        ), "" +
                "void main(){\n" +
                "   vec2 uv1 = uv*2.0-1.0;\n" +
                "   float effect = max(1.0-length(uv1),0.0);\n" +
                "   result = effect * color;\n" +
                "}\n"
    )

    val splashShader = Shader(
        "splash", simpleVertexShaderList, simpleVertexShader, uvList,
        listOf(
            Variable(GLSLType.V1F, "strength"),
            Variable(GLSLType.V4F, "result", VariableMode.OUT)
        ), "" +
                "void main() {\n" +
                "   vec2 uv1 = uv*2.0-1.0;\n" +
                "   float dist = dot(uv1,uv1);\n" +
                "   result = vec4(uv1 * exp(-dist*10.0) * strength, 0.0, 0.0);\n" +
                "}\n"
    )

    val divergenceShader = Shader(
        "divergence", coordsList, coordsVShader, uvList, listOf(
            Variable(GLSLType.V4F, "result", VariableMode.OUT),
            Variable(GLSLType.V2F, "texelSize"),
            Variable(GLSLType.S2D, "velocityTex")
        ), "" +
                "void main(){\n" +
                uvs1 +
                "   float L = texture(velocityTex, vL).x;\n" +
                "   float R = texture(velocityTex, vR).x;\n" +
                "   float T = texture(velocityTex, vT).y;\n" +
                "   float B = texture(velocityTex, vB).y;\n" +
                "   vec2 C = texture(velocityTex, uv).xy;\n" +
                // if on edge, set value to negative of center (?)
                "   if(vL.x < 0.0) { L = -C.x; }\n" +
                "   if(vR.x > 1.0) { R = -C.x; }\n" +
                "   if(vT.y > 1.0) { T = -C.y; }\n" +
                "   if(vB.y < 0.0) { B = -C.y; }\n" +
                "   result = vec4(0.5*(R-L+T-B),0.0,0.0,1.0);\n" +
                "}"
    )

    val clearProgram = Shader(
        "scale", coordsList, coordsVShader, uvList, listOf(
            Variable(GLSLType.V4F, "result", VariableMode.OUT),
            Variable(GLSLType.V1F, "pressure"),
            Variable(GLSLType.S2D, "pressureTex")
        ), "" +
                "void main(){\n" +
                "   result = pressure * texture(pressureTex,uv);\n" +
                "}"
    )

    val pressureProgram = Shader(
        "scale", coordsList, coordsVShader, uvList, listOf(
            Variable(GLSLType.V4F, "result", VariableMode.OUT),
            Variable(GLSLType.V2F, "texelSize"),
            Variable(GLSLType.V1F, "pressure"),
            Variable(GLSLType.S2D, "pressureTex"),
            Variable(GLSLType.S2D, "divergenceTex")
        ), "" +
                "void main() {\n" +
                uvs1 +
                "   float L = texture2D(pressureTex, vL).x;\n" +
                "   float R = texture2D(pressureTex, vR).x;\n" +
                "   float T = texture2D(pressureTex, vT).x;\n" +
                "   float B = texture2D(pressureTex, vB).x;\n" +
                "   float C = texture2D(pressureTex, uv).x;\n" +
                "   float divergence = texture2D(divergenceTex, uv).x;\n" +
                "   float pressure =(L + R + B + T - divergence) * 0.25;\n" +
                "   result = vec4(pressure, 0.0, 0.0, 1.0);\n" +
                "}"
    )

    val gradientSubProgram = Shader(
        "gradientSub", coordsList, coordsVShader, uvList, listOf(
            Variable(GLSLType.V4F, "result", VariableMode.OUT),
            Variable(GLSLType.V2F, "texelSize"),
            Variable(GLSLType.S2D, "velocityTex"),
            Variable(GLSLType.S2D, "pressureTex"),
        ), "" +
                "void main() {\n" +
                uvs1 +
                "   float L = texture2D(pressureTex, vL).x;\n" +
                "   float R = texture2D(pressureTex, vR).x;\n" +
                "   float T = texture2D(pressureTex, vT).x;\n" +
                "   float B = texture2D(pressureTex, vB).x;\n" +
                "   vec2 velocity = texture2D(velocityTex, uv).xy - vec2(R - L, T - B);\n" +
                "   result = vec4(velocity, 0.0, 1.0);\n" +
                "}"
    )

    val advectionProgram = Shader(
        "advection", coordsList, coordsVShader, uvList, listOf(
            Variable(GLSLType.V4F, "result", VariableMode.OUT),
            Variable(GLSLType.V2F, "texelSize"),
            Variable(GLSLType.V1F, "dt"),
            Variable(GLSLType.V1F, "scale"),
            Variable(GLSLType.S2D, "velocityTex"),
            Variable(GLSLType.S2D, "sourceTex"),
        ), "" +
                "void main() {\n" +
                "   vec2 coord = uv - dt * texture2D(velocityTex, uv).xy * texelSize;\n" +
                "   result = texture2D(sourceTex, coord) * scale;\n" +
                "}"
    )

    val ts = Vector2f(1f / w, 1f / h)

    val texture1dShader = BaseShader(
        "flatShaderTexture",
        ShaderLib.simpleVertexShaderV2List,
        ShaderLib.simpleVertexShaderV2, uvList,
        listOf(Variable(GLSLType.S2D, "tex")), "" +
                "void main(){\n" +
                "   float col = texture(tex, uv).x;\n" +
                "   if(!(col >= -1e38 && col <= 1e38)) {\n" +
                "       gl_FragColor = vec4(1.0,0.0,1.0,1.0);\n" +
                "   } else {\n" +
                "       gl_FragColor = vec4(abs(col) * (col < 0.0 ? vec3(1,0,0) : vec3(1,1,1)), 1.0);\n" +
                "   }\n" +
                "}"
    )

    val texture2dShader = BaseShader(
        "flatShaderTexture",
        ShaderLib.simpleVertexShaderV2List,
        ShaderLib.simpleVertexShaderV2, uvList,
        listOf(Variable(GLSLType.S2D, "tex")), "" +
                "void main(){\n" +
                "   vec2 col = texture(tex, uv).xy;\n" +
                "   if(!(col.x >= -1e38 && col.x <= 1e38)) {\n" +
                "       gl_FragColor = vec4(1.0,0.0,1.0,1.0);\n" +
                "   } else {\n" +
                "       gl_FragColor = vec4(col.x*.5+.5,col.y*.5+.5,.5, 1.0);\n" +
                "   }\n" +
                "}"
    )

    fun displayTexture1d(x: Int, y: Int, w: Int, h: Int, texture: ITexture2D) {
        if (w == 0 || h == 0) return
        GFX.check()
        val shader = texture1dShader.value
        shader.use()
        GFXx2D.posSize(shader, x, y, w, h)
        GFXx2D.defineAdvancedGraphicalFeatures(shader)
        GFXx2D.tiling(shader, null)
        texture.bind(
            0,
            GPUFiltering.NEAREST,
            Clamping.CLAMP
        )
        flat01.draw(shader)
        GFX.check()
    }

    fun displayTexture2d(x: Int, y: Int, w: Int, h: Int, texture: ITexture2D) {
        if (w == 0 || h == 0) return
        GFX.check()
        val shader = texture2dShader.value
        shader.use()
        GFXx2D.posSize(shader, x, y, w, h)
        GFXx2D.defineAdvancedGraphicalFeatures(shader)
        GFXx2D.tiling(shader, null)
        texture.bind(
            0,
            GPUFiltering.NEAREST,
            Clamping.CLAMP
        )
        flat01.draw(shader)
        GFX.check()
    }

    val init = lazy {
        // initialize textures
        velocity.read.clearColor(0f, 0f, 0f, 1f) // 2d, so -1 = towards bottom right
        pressure.read.clearColor(0.5f, 0f, 0f, 1f) // 1d, so max level
    }

    var mx = 0f
    var my = 0f

    val visuals = listOf(
        divergence to "Div",
        blackTexture.wrapAsFramebuffer() to "",
        pressure.read to "PressureR",
        pressure.write to "PressureW",
        velocity.read to "VelocityR",
        velocity.write to "VelocityW"
    )

    fun step(it: Panel, lx: Float, ly: Float, s: Float) {

        init.value

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
                        useFrame(velocity.read) {
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
                            useFrame(velocity.read) {
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

                divergenceShader.apply {
                    useFrame(divergence) {
                        use()
                        v2f("texelSize", ts)
                        velocity.read.getTexture0().bindTrulyNearest(this, "velocityTex")
                        flat01.draw(this)
                    }
                }

                clearProgram.apply { // scales the pressure down... why ever...
                    useFrame(pressure.write) {
                        use()
                        pressure.read.getTexture0().bindTrulyNearest(this, "pressureTex")
                        v1f("pressure", 1f) // config
                        flat01.draw(this)
                    }
                    pressure.swap()
                }

                pressureProgram.apply {
                    use()
                    v2f("texelSize", ts)
                    divergence.getTexture0().bindTrulyNearest(this, "divergenceTex")
                    for (i in 0 until 20) {
                        useFrame(pressure.write) {
                            pressure.read.getTexture0().bindTrulyNearest(this, "pressureTex")
                            flat01.draw(this)
                        }
                        pressure.swap()
                    }
                }

                gradientSubProgram.apply {
                    useFrame(velocity.write) {
                        use()
                        v2f("texelSize", ts)
                        pressure.read.getTexture0().bindTrulyNearest(this, "pressureTex")
                        velocity.read.getTexture0().bindTrulyNearest(this, "velocityTex")
                        flat01.draw(this)
                    }
                    velocity.swap()
                }

                advectionProgram.apply {
                    use()
                    v2f("texelSize", ts)
                    v1f("dt", dt)
                    val dissipation = 0.2f
                    v1f("scale", exp(-dt * dissipation))
                    useFrame(velocity.write) {
                        // correct source? yes, both are bound to the same target
                        velocity.read.getTexture0().bind(this, "sourceTex", GPUFiltering.TRULY_LINEAR, Clamping.CLAMP)
                        velocity.read.getTexture0().bind(this, "velocityTex", GPUFiltering.TRULY_LINEAR, Clamping.CLAMP)
                        flat01.draw(this)
                    }
                    velocity.swap()
                    // if there are more attributes, like color, they should be transferred using the same program
                    // set their dissipation to 1.0
                }


            }

        }
    }

    fun draw(it: Panel) {
        for (i in visuals.indices) {
            val (tex, title) = visuals[i]
            val j = i / 2
            val k = i and 1
            val x0 = it.x + j * it.width / 3
            val x1 = it.x + (j + 1) * it.width / 3
            val y0 = it.y + k * it.height / 2
            val y1 = it.y + (k + 1) * it.height / 2
            clip(x0, y0, x1 - x0, y1 - y0) {
                if (i < 4) {
                    displayTexture1d(x0, y0, x1 - x0, y1 - y0, tex.getTexture0())
                } else {
                    displayTexture2d(x0, y0, x1 - x0, y1 - y0, tex.getTexture0())
                }
                drawSimpleTextCharByChar(
                    (x0 + x1) / 2, (y0 + y1) / 2, 2, title,
                    AxisAlignment.CENTER, AxisAlignment.CENTER
                )
            }
        }
    }

    if (false) {
        testDrawing("Fluid Sim") {

            it.allowLeft = true
            it.allowRight = false

            val window = it.window!!
            step(
                it, (window.mouseX - it.x) / it.width,
                (window.mouseY - it.y) / it.height, 0.2f
            )
            draw(it)

        }
    } else {
        // use GPU-generated mesh instead?
        val mesh = Mesh()
        TerrainUtils.generateRegularQuadHeightMesh(
            w, h, 0, w, false, 1f, mesh,
            { 0f }, { -1 }
        )
        // generate UVs
        val pos = mesh.positions!!
        val uvs = mesh.uvs.resize(pos.size / 3 * 2)
        val bdx = mesh.getBounds()
        for (i in uvs.indices step 2) {
            uvs[i] = (pos[i / 2 * 3] - bdx.minX) / bdx.deltaX
            uvs[i + 1] = (pos[i / 2 * 3 + 2] - bdx.minZ) / bdx.deltaZ
        }
        mesh.uvs = uvs
        mesh.invalidateGeometry()
        val shader = object : ECSMeshShader("fluid") {
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
        val waveHeight = 50f
        val material = Material()
        material.shader = shader
        material.shaderOverrides["heightTex"] = TypeValueV2(GLSLType.S2D) { pressure.read }
        material.shaderOverrides["waveHeight"] = TypeValue(GLSLType.V1F, waveHeight)
        material.pipelineStage = TRANSPARENT_PASS
        material.metallicMinMax.set(1f)
        material.roughnessMinMax.set(0f)
        material.diffuseBase.w = 1f
        material.indexOfRefraction = 1.33f // water
        mesh.materials = listOf(material.ref)
        val comp = object : MeshComponent(mesh) {
            override fun onUpdate(): Int {
                super.onUpdate()
                val ci = RenderView.currentInstance
                if (ci != null) {
                    val rayDir = ci.getMouseRayDirection()
                    val rayPos = ci.cameraPosition
                    val dist = (waveHeight - rayPos.y) / rayDir.y
                    val gx = 0.5f + (rayPos.x + dist * rayDir.x) / w
                    val gz = 0.5f - (rayPos.z + dist * rayDir.z) / h
                    val lx = if (dist > 0f) gx.toFloat() else 0f
                    val ly = if (dist > 0f) gz.toFloat() else 0f
                    step(ci, lx, ly, 0.2f * dist.toFloat() / max(w, h))
                }
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
        // we handle collisions ourselves
        comp.collisionMask = 0
        testSceneWithUI("FluidSim", Entity(comp)) {
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
}
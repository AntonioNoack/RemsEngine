package me.anno.tests.physics

import me.anno.Engine
import me.anno.gpu.GFX.clip
import me.anno.gpu.GFX.flat01
import me.anno.gpu.GFXState.renderPurely
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.drawing.DrawTextures.drawTexture
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Renderer.Companion.copyRenderer
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib.coordsList
import me.anno.gpu.shader.ShaderLib.coordsVShader
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.GPUFiltering
import me.anno.ui.debug.TestDrawPanel.Companion.testDrawing
import org.joml.Vector2f
import kotlin.math.exp

fun main() {

    // todo small gpu fluid sim
    // https://github.com/PavelDoGreat/WebGL-Fluid-Simulation/blob/master/script.js

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

    val uvs = "" +
            "   vec2 vL = uv - vec2(texelSize.x, 0.0);\n" +
            "   vec2 vR = uv + vec2(texelSize.x, 0.0);\n" +
            "   vec2 vT = uv - vec2(0.0, texelSize.y);\n" +
            "   vec2 vB = uv + vec2(0.0, texelSize.y);\n"

    val velocity = State { Framebuffer("velocity", w, h, TargetType.FloatTarget2) }
    val divergence = Framebuffer("divergence", w, h, TargetType.FloatTarget1)
    val curl = Framebuffer("curl", w, h, TargetType.FloatTarget1)
    val pressure = State { Framebuffer("pressure", w, h, TargetType.FloatTarget1) }

    val curlShader = Shader(
        "curl", coordsList, coordsVShader, uvList, listOf(
            Variable(GLSLType.V4F, "result", VariableMode.OUT),
            Variable(GLSLType.V2F, "texelSize"),
            Variable(GLSLType.S2D, "velocityTex")
        ), "" +
                "void main() {\n" +
                uvs +
                "   float L = texture2D(velocityTex, vL).y;\n" +
                "   float R = texture2D(velocityTex, vR).y;\n" +
                "   float T = texture2D(velocityTex, vT).x;\n" +
                "   float B = texture2D(velocityTex, vB).x;\n" +
                "   result = vec4(0.5*(R-L-T+B),0.0,0.0,1.0);\n" +
                "}"
    )

    val vorticityShader = Shader(
        "vorticity", coordsList, coordsVShader, uvList, listOf(
            Variable(GLSLType.V4F, "result", VariableMode.OUT),
            Variable(GLSLType.V1F, "curl"),
            Variable(GLSLType.V1F, "dt"),
            Variable(GLSLType.V2F, "texelSize"),
            Variable(GLSLType.S2D, "curlTex"),
            Variable(GLSLType.S2D, "velocityTex")
        ), "" +
                "void main() {\n" +
                uvs +
                "    float L = texture2D(curlTex, vL).x;\n" +
                "    float R = texture2D(curlTex, vR).x;\n" +
                "    float T = texture2D(curlTex, vT).x;\n" +
                "    float B = texture2D(curlTex, vB).x;\n" +
                "    float C = texture2D(curlTex, uv).x;\n" +

                "    vec2 force = 0.5 * vec2(abs(T) - abs(B), abs(R) - abs(L));\n" +
                "    force /= length(force) + 0.0001;\n" +
                "    force *= curl * C;\n" +
                "    force.y = -force.y;\n" +

                "    vec2 velocity = texture2D(velocityTex, uv).xy;\n" +
                "    velocity += force * dt;\n" +
                "    velocity = min(max(velocity, -1000.0), 1000.0);\n" +
                "    result = vec4(velocity, 0.0, 1.0);\n" +
                "}"
    )

    val divergenceShader = Shader(
        "divergence", coordsList, coordsVShader, uvList, listOf(
            Variable(GLSLType.V4F, "result", VariableMode.OUT),
            Variable(GLSLType.V2F, "texelSize"),
            Variable(GLSLType.S2D, "velocityTex")
        ), "" +
                "void main(){\n" +
                uvs +
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

    val scaleProgram = Shader(
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
                uvs +
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
                uvs +
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

    val init = lazy {
        // initialize textures
        velocity.read.clearColor(-1)
        pressure.read.clearColor(-1)
    }

    testDrawing("Fluid Sim") {

        init.value

        val dt = Engine.deltaTime

        renderPurely {

            curlShader.apply {
                useFrame(curl, copyRenderer) {
                    use()
                    v2f("texelSize", ts)
                    velocity.read.getTexture0().bindTrulyNearest(this, "velocity")
                    flat01.draw(this)
                }
            }

            vorticityShader.apply {
                useFrame(velocity.write, copyRenderer) {
                    use()
                    v2f("texelSize", ts)
                    velocity.read.getTexture0().bindTrulyNearest(this, "velocityTex")
                    curl.getTexture0().bindTrulyNearest(this, "curlTex")
                    v1f("curl", 1f)
                    v1f("dt", dt)
                    flat01.draw(this)
                }
                velocity.swap()
            }

            divergenceShader.apply {
                useFrame(divergence, copyRenderer) {
                    use()
                    v2f("texelSize", ts)
                    velocity.read.getTexture0().bindTrulyNearest(this, "velocityTex")
                    flat01.draw(this)
                }
            }

            scaleProgram.apply {
                useFrame(pressure.write, copyRenderer) {
                    use()
                    pressure.read.getTexture0().bindTrulyNearest(this, "pressureTex")
                    v1f("pressure", 0.8f) // config
                    flat01.draw(this)
                }
                pressure.swap()
            }

            pressureProgram.apply {
                use()
                v2f("texelSize", ts)
                divergence.getTexture0().bindTrulyNearest(this, "divergenceTex")
                for (i in 0 until 20) {
                    useFrame(pressure.write, copyRenderer) {
                        pressure.read.getTexture0().bindTrulyNearest(this, "pressureTex")
                        flat01.draw(this)
                    }
                    pressure.swap()
                }
            }

            gradientSubProgram.apply {
                use()
                v2f("texelSize", ts)
                pressure.read.getTexture0().bindTrulyNearest(this, "pressureTex")
                velocity.read.getTexture0().bindTrulyNearest(this, "velocityTex")
                flat01.draw(this)
            }

            advectionProgram.apply {
                use()
                v2f("texelSize", ts)
                v1f("dt", dt)
                val dissipation = 0.2f
                v1f("scale", exp(-dt * dissipation))
                useFrame(velocity.write, copyRenderer) {
                    velocity.read.getTexture0().bind(this, "sourceTex", GPUFiltering.TRULY_LINEAR, Clamping.CLAMP)
                    velocity.read.getTexture0().bind(this, "velocityTex", GPUFiltering.TRULY_LINEAR, Clamping.CLAMP)
                    flat01.draw(this)
                }
                velocity.swap()
                // if there are more attributes, like color, they should be transferred using the same program
                // set their dissipation to 1.0
            }

            // todo draw into it...

            // depending on mode/x, draw all states
            val y0 = it.y
            val y1 = it.y + it.height
            val visuals = listOf(curl, divergence, pressure.read, velocity.read)
            for (i in visuals.indices) {
                val tex = visuals[i]
                val x0 = it.x + i * it.width / visuals.size
                val x1 = it.x + (i + 1) * it.width / visuals.size
                clip(x0, y0, x1 - x0, y1 - y0) {
                    drawTexture(it.x, it.y, it.width, it.height, tex.getTexture0())
                }
            }

        }

    }

}
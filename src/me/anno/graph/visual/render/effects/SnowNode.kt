package me.anno.graph.visual.render.effects

import me.anno.ecs.systems.GlobalSettings
import me.anno.engine.ui.render.RenderState
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.deferred.BufferQuality
import me.anno.gpu.framebuffer.DepthBufferType
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.shader.DepthTransforms
import me.anno.gpu.shader.DepthTransforms.depthToPosition
import me.anno.gpu.shader.DepthTransforms.rawToDepth
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderFuncLib.randomGLSL
import me.anno.gpu.shader.ShaderLib.coordsUVVertexShader
import me.anno.gpu.shader.ShaderLib.quatRot
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.shader.renderer.Renderer
import me.anno.graph.visual.actions.ActionNode
import me.anno.graph.visual.render.Texture

/**
 * To configure this node, add a SnowControl instance to your scene.
 * todo do the same for DepthOfField, so we don't have to hack it
 * */
class SnowNode : ActionNode(
    "Snow",
    listOf("Texture", "Illuminated", "Texture", "Depth"),
    listOf("Texture", "Illuminated")
) {
    override fun executeAction() {
        val colorTex = (getInput(1) as Texture).tex
        val depthTex = (getInput(2) as Texture).tex
        val result = FBStack["snow", colorTex.width, colorTex.height, 4, BufferQuality.FP_16, 1, DepthBufferType.NONE]
        val snowSettings = GlobalSettings[SnowSettings::class]
        useFrame(result, Renderer.copyRenderer) {
            val shader = snowShader
            shader.use()
            colorTex.bindTrulyNearest(shader, "colorTex")
            depthTex.bindTrulyNearest(shader, "depthTex")
            shader.v3f("cameraPosition", RenderState.cameraPosition)
            shader.v4f("cameraRotation", RenderState.cameraRotation)
            shader.v3f("snowPosition", snowSettings.position)
            shader.v3f("snowColor", snowSettings.color)
            shader.v1f("flakeSize", snowSettings.flakeSize)
            shader.v1f("density", snowSettings.density)
            shader.v1f("elongation", 1f / snowSettings.elongation)
            shader.v1f("invFogDistance", 1f / snowSettings.fogDistance)
            shader.v1f("skySnowiness", snowSettings.skySnowiness)
            shader.v4f("worldRotation", snowSettings.worldRotation)
            DepthTransforms.bindDepthUniforms(shader)
            flat01.draw(shader)
        }
        setOutput(1, Texture(result.getTexture0()))
    }

    companion object {
        val snowShader = Shader(
            "Snow", emptyList(), coordsUVVertexShader, uvList, listOf(
                Variable(GLSLType.V3F, "cameraPosition"),
                Variable(GLSLType.V4F, "cameraRotation"),
                Variable(GLSLType.S2D, "colorTex"),
                Variable(GLSLType.S2D, "depthTex"),
                Variable(GLSLType.V1F, "density"),
                Variable(GLSLType.V1F, "flakeSize"),
                Variable(GLSLType.V1F, "invFogDistance"),
                Variable(GLSLType.V1F, "skySnowiness"),
                Variable(GLSLType.V3F, "snowPosition"),
                Variable(GLSLType.V3F, "snowColor"),
                Variable(GLSLType.V4F, "worldRotation"),
                Variable(GLSLType.V1F, "elongation"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT)
            ) + DepthTransforms.depthVars, "" +
                    // traverse field of snowflakes
                    // check each snowflake for overlap
                    // if so, make the pixel white
                    quatRot +
                    rawToDepth +
                    depthToPosition +
                    "float sddSphere2(vec3 pos, vec3 dir, float radius){\n" +
                    "   vec3 closest = pos - dir * dot(pos,dir);\n" +
                    "   return length(closest) - radius;\n" +
                    "}\n" +
                    randomGLSL +
                    "void main(){\n" +
                    "   vec4 color = texture(colorTex,uv);\n" +
                    "   float depth = density * rawToDepth(texture(depthTex,uv).r);\n" +
                    // calculate position and direction in world space
                    "   vec3 pos = cameraPosition;\n" +
                    "   pos = quatRot(pos, worldRotation);\n" +
                    "   pos += snowPosition;\n" + // snow falling = camera rising
                    "   pos *= density;\n" +
                    "   vec3 dir = normalize(rawCameraDirection(uv) * density);\n" +
                    "   dir = quatRot(dir, worldRotation);\n" +
                    "   vec2 dirSign = sign(dir.xz);\n" +
                    "   vec2 blockPosition = floor(pos.xz);\n" +
                    "   vec2 dist3 = (dirSign*.5+.5 + blockPosition - pos.xz)/dir.xz;\n" +
                    "   vec2 invUStep = dirSign/dir.xz;\n" +
                    "   float maxDist = 30.0 * min(density, 1.0);\n" +
                    "   int maxSteps = int(maxDist * 3.0);\n" + // if low density, use less steps
                    "   float dist = 0.0;\n" +
                    "   float snowiness = 0.0;\n" +
                    "   if (depth < 1e16) {\n" + // fog
                    "       snowiness = 1.0 - exp(-depth * invFogDistance);\n" +
                    "   } else {\n" +
                    "       snowiness = skySnowiness;\n" +
                    "   }\n" +
                    // fade them out in the distance (make them blurry)
                    "   for (int i=0; i<maxSteps && snowiness < 0.99; i++) {\n" +
                    "       float nextDist = min(dist3.x, dist3.y);\n" +
                    "       float distGuess = (dist + nextDist) * 0.5;\n" +
                    "       float cellY = floor(pos.y + dir.y * distGuess);\n" +
                    "       float flakeD = flakeSize * (1.0 + max(dist - 7.0, 0.0) * 0.25), flakeR = 0.5 * flakeD;" +
                    "       float flakeA = clamp(distGuess, 0.0, 1.0) / (1.0 + distGuess * distGuess);\n" +
                    "       vec2 seed = blockPosition + vec2(0.03 * cellY, 0.0);\n" +
                    "       float flakeY = flakeR+(1.0-flakeD)*random(seed+0.3);\n" +
                    // check for colliding snowflakes
                    // todo random xz swaying
                    "       float flakeX = flakeR + (1.0-flakeD) * random(seed);\n" +
                    "       float flakeZ = flakeR + (1.0-flakeD) * random(seed+0.5);\n" +
                    "       vec3 spherePos = vec3(blockPosition + vec2(flakeX,flakeZ), cellY + flakeY - 0.5).xzy - pos;\n" +
                    "       vec3 dir1 = dir;\n" +
                    // elongation is for rain
                    "       if (elongation != 1.0) {\n" +
                    "           dir1.y *= elongation;\n" +
                    "           dir1 = normalize(dir1);\n" +
                    "           spherePos.y *= elongation;\n" +
                    "       }\n" +
                    "       if(dot(spherePos,dir1) > min(depth, maxDist)) break;\n" +
                    "       float dist1 = sddSphere2(spherePos,dir1,flakeD*0.5*density);\n" +
                    // antialiasing could be skipped on weak devices
                    "       dist1 /= fwidth(dist1);\n" +
                    "       if (dist1 < 0.0) {\n" +
                    "           dist1 = min(-dist1, 1.0);\n" +
                    "           snowiness += (1.0 - snowiness) * flakeA * dist1;\n" +
                    "       }\n" +
                    // continue tracing
                    "       if (nextDist == dist3.x){\n" +
                    "           blockPosition.x += dirSign.x; dist3.x += invUStep.x;\n" +
                    "       } else {\n" +
                    "           blockPosition.y += dirSign.y; dist3.y += invUStep.y;\n" +
                    "       }\n" +
                    "       dist = nextDist;\n" +
                    "   }\n" +
                    "   color.rgb = mix(color.rgb, snowColor, snowiness);\n" +
                    "   result = color;\n" +
                    "}\n"
        )
    }
}

package me.anno.tests.shader

import me.anno.Time
import me.anno.config.DefaultConfig.style
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.annotations.Range
import me.anno.ecs.annotations.Type
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.RenderState
import me.anno.engine.ui.render.SceneView
import me.anno.gpu.GFX.flat01
import me.anno.gpu.GFXState.useFrame
import me.anno.gpu.deferred.BufferQuality
import me.anno.gpu.framebuffer.FBStack
import me.anno.gpu.shader.DepthTransforms
import me.anno.gpu.shader.DepthTransforms.depthToPosition
import me.anno.gpu.shader.DepthTransforms.rawToDepth
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Renderer
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderFuncLib.noiseFunc
import me.anno.gpu.shader.ShaderLib.coordsList
import me.anno.gpu.shader.ShaderLib.coordsVShader
import me.anno.gpu.shader.ShaderLib.quatRot
import me.anno.gpu.shader.ShaderLib.uvList
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.graph.render.QuickPipeline
import me.anno.graph.render.Texture
import me.anno.graph.render.effects.*
import me.anno.graph.render.scene.CombineLightsNode
import me.anno.graph.render.scene.RenderLightsNode
import me.anno.graph.render.scene.RenderSceneNode
import me.anno.graph.types.FlowGraph
import me.anno.graph.types.flow.actions.ActionNode
import me.anno.io.ISaveable.Companion.registerCustomClass
import me.anno.mesh.Shapes.flatCube
import me.anno.sdf.shapes.SDFSphere.Companion.sdSphere
import me.anno.ui.custom.CustomList
import me.anno.ui.debug.TestDrawPanel
import me.anno.ui.debug.TestStudio.Companion.testUI
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f

// get snow effect working like in https://www.glslsandbox.com/e#36547.0
// done get this and rain working in 3d
// todo render snow as SDF instead (?), so we can apply lighting to it without extra cost.
// our snow is transparent though... so that's a little more complicated...

val snowShader = Shader(
    "Snow", coordsList, coordsVShader, uvList, listOf(
        Variable(GLSLType.V3F, "cameraPosition"),
        Variable(GLSLType.V4F, "cameraRotation"),
        Variable(GLSLType.V1F, "invWorldScale"),
        Variable(GLSLType.S2D, "colorTex"),
        Variable(GLSLType.S2D, "depthTex"),
        Variable(GLSLType.V1F, "density"),
        Variable(GLSLType.V1F, "flakeSize"),
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
            sdSphere +
            noiseFunc +
            "void main(){\n" +
            "   vec4 color = texture(colorTex,uv);\n" +
            "   float depth = density * invWorldScale * rawToDepth(texture(depthTex,uv).r);\n" +
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
            // fade them out in the distance (make them blurry)
            "   for(int i=0;i<maxSteps;i++){\n" +
            "       float nextDist = min(dist3.x, dist3.y);\n" +
            "       float distGuess = (dist + nextDist) * 0.5;\n" +
            "       float cellY = floor(pos.y + dir.y * distGuess);\n" +
            "       float flakeD = flakeSize * (1.0 + max(dist - 7.0, 0.0) * 0.25), flakeR = 0.5 * flakeD, flakeA = min(distGuess, 1.0) / (1.0 + distGuess * distGuess);\n" +
            "       vec2 seed = blockPosition + vec2(0.03 * cellY, 0.0);\n" +
            "       float flakeY = flakeR+(1.0-flakeD)*random(seed+0.3);\n" +
            // check for colliding snowflakes
            // todo random xz swaying
            "       float flakeX = flakeR+(1.0-flakeD)*random(seed);\n" +
            "       float flakeZ = flakeR+(1.0-flakeD)*random(seed+0.5);\n" +
            "       vec3 spherePos = (vec3(blockPosition + vec2(flakeX,flakeZ), cellY + flakeY - 0.5).xzy - pos);\n" +
            "       vec3 dir1 = dir;\n" +
            // elongation is for rain
            "       if(elongation != 1.0) {\n" +
            "           dir1.y *= elongation;\n" +
            "           dir1=normalize(dir1);\n" +
            "           spherePos.y *= elongation;\n" +
            "       }\n" +
            "       if(dot(spherePos,dir1) > min(depth, maxDist)) break;\n" +
            "       if(sddSphere(spherePos,dir1,flakeD*0.5*density) < 0.0) color.rgb = mix(color.rgb, snowColor, flakeA);\n" +
            // continue tracing
            "       if(nextDist == dist3.x){\n" +
            "           blockPosition.x += dirSign.x; dist3.x += invUStep.x;\n" +
            "       } else {\n" +
            "           blockPosition.y += dirSign.y; dist3.y += invUStep.y;\n" +
            "       }\n" +
            "       dist = nextDist;\n" +
            "   }\n" +
            "   if(depth < 1e16) color.rgb = mix(snowColor, color.rgb, exp(-depth * 0.01));\n" +
            "   result = color;\n" +
            "}\n"
)

class SnowControl : Component() {

    @Range(0.0, 100.0)
    var density = 2f

    @Range(0.0001, 0.5)
    var flakeSize = 0.02f

    var velocity = Vector3d(0f, 0f, 0f)
    val position = Vector3d(0f, 0f, 0f)

    // where zoom-in is appearing when the density changes
    var center = Vector3d(0f, 100f, 0f)

    @Type("Color3HDR")
    var color = Vector3f(10f)

    @Range(1.0, 100.0)
    var elongation = 1f

    var worldRotation = Quaternionf()

    private var lastDensity = density
    override fun onUpdate(): Int {
        val deltaDensity = (lastDensity / density).toDouble()
        lastDensity = density
        if (deltaDensity != 1.0 && deltaDensity in 0.5..2.0) {
            position.add(center)
            position.mul(deltaDensity)
            position.sub(center)
        }
        velocity.mulAdd(-Time.deltaTime.toDouble(), position, position)
        return 1
    }
}

class SnowNode : ActionNode(
    "Snow",
    listOf("Texture", "Illuminated", "Texture", "Depth"),
    listOf("Texture", "Illuminated")
) {
    lateinit var snowControl: SnowControl
    override fun executeAction() {
        val colorTex = (getInput(1) as Texture).tex
        val depthTex = (getInput(2) as Texture).tex
        val result = FBStack["snow", colorTex.width, colorTex.height, 4, BufferQuality.HIGH_16, 1, false]
        useFrame(result, Renderer.copyRenderer) {
            val shader = snowShader
            shader.use()
            colorTex.bindTrulyNearest(shader, "colorTex")
            depthTex.bindTrulyNearest(shader, "depthTex")
            shader.v1f("invWorldScale", 1.0 / RenderState.worldScale)
            shader.v3f("cameraPosition", RenderState.cameraPosition)
            shader.v4f("cameraRotation", RenderState.cameraRotation)
            shader.v3f("snowPosition", snowControl.position)
            shader.v3f("snowColor", snowControl.color)
            shader.v1f("flakeSize", snowControl.flakeSize)
            shader.v1f("density", snowControl.density)
            shader.v1f("elongation", 1f / snowControl.elongation)
            shader.v4f("worldRotation", snowControl.worldRotation)
            DepthTransforms.bindDepthToPosition(shader)
            flat01.draw(shader)
        }
        setOutput(1, Texture(result.getTexture0()))
    }
}

fun main() {
    testUI("Snow") {
        val list = CustomList(false, style)

        // 2d
        val shader = Shader(
            "snow", coordsList, coordsVShader, uvList, listOf(
                Variable(GLSLType.V1F, "time"),
                Variable(GLSLType.V2F, "uvScale"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT)
            ), "" +
                    "float snow(vec2 uv, float scale) {\n" +
                    "   float w=smoothstep(1.,0.,-uv.y*(scale/10.));if(w<.1)return 0.;\n" +
                    "   uv+=time/scale;uv.y+=time*2./scale;uv.x+=sin(uv.y+time*.5)/scale;\n" +
                    "   uv*=scale;vec2 s=floor(uv),f=fract(uv),p;float k=3.,d;\n" +
                    "   p=.5+.35*sin(11.*fract(sin((s+p+scale)*mat2(7,3,6,5))*5.))-f;d=length(p);k=min(d,k);\n" +
                    "   k=smoothstep(0.,k,sin(f.x+f.y)*0.01);\n" +
                    "   return k*w;\n" +
                    "}\n" +
                    "void main(){\n" +
                    "   vec2 uv2 = uv * uvScale;\n" +
                    "   float c=smoothstep(1.,0.3,clamp(uv2.y*.3+.8,0.,.75));\n" +
                    "   c+=snow(uv2,30.)*.3;\n" +
                    "   c+=snow(uv2,20.)*.5;\n" +
                    "   c+=snow(uv2,15.)*.8;\n" +
                    "   c+=snow(uv2,10.);\n" +
                    "   c+=snow(uv2,8.);\n" +
                    "   c+=snow(uv2,6.);\n" +
                    "   c+=snow(uv2,5.);\n" +
                    "   result = vec4(mix(vec3(0.1,0.2,0.3),vec3(1),c),1);\n" +
                    "}"
        )
        list.add(TestDrawPanel {
            shader.use()
            shader.v1f("time", Time.gameTime)
            shader.v2f("uvScale", it.width.toFloat() / it.height, 1f)
            flat01.draw(shader)
        }, 1f)

        // 3d
        val snowControl = SnowControl()
        val snowNode = SnowNode()
        snowNode.snowControl = snowControl
        snowControl.velocity.set(-0.3f, -1.5f, 0f)
        val graph = createSnowGraph(snowNode)
        val mode = RenderMode("Snow", graph)
        val scene = Entity("Scene")
        scene.add(MeshComponent(flatCube.front))
        scene.add(snowControl)
        registerCustomClass(SnowControl())
        list.add(SceneView.testScene(scene) {
            it.renderer.renderMode = mode
        }, 2f)
        list
    }
}

fun createSnowGraph(snowNode: SnowNode): FlowGraph {
    return QuickPipeline()
        .then(RenderSceneNode())
        .then(RenderLightsNode())
        .then(SSAONode())
        .then(CombineLightsNode())
        .then(SSRNode())
        .then(snowNode)
        // .then(ChromaticAberrationNode())
        .then1(BloomNode(), mapOf("Apply Tone Mapping" to true))
        .then(FXAANode())
        .then(GizmoNode(), mapOf("Illuminated" to listOf("Color")))
        .finish()
}
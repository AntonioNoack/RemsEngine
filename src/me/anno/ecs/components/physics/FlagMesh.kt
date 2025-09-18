package me.anno.ecs.components.physics

import me.anno.Time
import me.anno.ecs.annotations.Range
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.components.mesh.material.utils.TypeValue
import me.anno.ecs.components.mesh.terrain.RectangleTerrainModel
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.ecs.systems.OnUpdate
import me.anno.engine.ui.render.ECSMeshShader
import me.anno.gpu.CullMode
import me.anno.gpu.GFXState
import me.anno.gpu.buffer.SimpleBuffer.Companion.flat01
import me.anno.gpu.framebuffer.Framebuffer
import me.anno.gpu.framebuffer.TargetType
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.Shader
import me.anno.gpu.shader.ShaderLib
import me.anno.gpu.shader.builder.ShaderStage
import me.anno.gpu.shader.builder.Variable
import me.anno.gpu.shader.builder.VariableMode
import me.anno.gpu.shader.renderer.Renderer
import me.anno.gpu.texture.Clamping
import me.anno.maths.Maths
import me.anno.maths.Maths.fract
import me.anno.maths.MinMax.max
import me.anno.maths.noise.FullNoise
import me.anno.utils.structures.maps.LazyMap
import me.anno.utils.structures.tuples.IntPair
import me.anno.utils.types.Arrays.resize
import me.anno.utils.types.Booleans.hasFlag
import me.anno.utils.types.Booleans.withoutFlag
import org.joml.AABBd
import org.joml.Matrix4x3
import org.joml.Vector2f
import org.joml.Vector3f

class FlagMesh : MeshComponent(), OnUpdate {

    var windStrength = Vector3f(10f, 0f, 0f)
    var randomnessSeed = 0f

    @Range(2.0, 200.0)
    var resolutionX = 15
        set(value) {
            field = max(2, value)
        }

    @Range(2.0, 200.0)
    var resolutionY = 10
        set(value) {
            field = max(2, value)
        }

    private var tex0 = Framebuffer("cloth-0", 1, 1, TargetType.Float32x3)
    private var tex1 = Framebuffer("cloth-1", 1, 1, TargetType.Float32x3)

    val material = Material()

    init {
        material.cullMode = CullMode.BOTH
        material.clamping = Clamping.CLAMP
        material.translucency = 0.7f
        material.shader = shader
        materials = listOf(material.ref)
    }

    private fun createTargets(key: IntPair, initial: FloatArray) {

        tex0.width = key.first
        tex0.height = key.second
        tex1.width = key.first
        tex1.height = key.second

        // upload initial data
        tex0.destroy()
        tex0.ensure()
        tex0.textures!![0].createRGB(initial, false)

        tex1.destroy()
        tex1.ensure()
        tex1.textures!![0].createRGB(initial, false)
    }

    private var prevDt = 0.01f

    fun step(dt: Float) {

        val w = resolutionX
        val h = resolutionY

        val src = tex0
        val dst = tex1

        GFXState.useFrame(dst, Renderer.copyRenderer) {
            updateShader.use()
            dst.getTexture0().bindTrulyNearest(updateShader, "prev")
            src.getTexture0().bindTrulyNearest(updateShader, "curr")
            val currDt = Maths.clamp(dt, 0.001f, 0.1f)
            updateShader.v1f("dt0", prevDt)
            updateShader.v1f("dt1", currDt)
            updateShader.v2f("duv", 1f / (w - 1f), 1f / (h - 1f))
            val time = (fract(Time.gameTime / 1e3) * 1e3).toFloat()
            val nx = noise[time, randomnessSeed] - 0.5f
            val ny = noise[time, randomnessSeed + 1f] - 0.5f
            val nz = noise[time, randomnessSeed + 2f] - 0.5f
            updateShader.v3f("acc", windStrength.x + nx, windStrength.y + ny, windStrength.z + nz)
            updateShader.v1f("maxLen", 1f / (w - 1f))
            prevDt = currDt
            flat01.draw(updateShader)
        }

        tex0 = dst
        tex1 = src
    }

    private var time = 0f
    private var fract = 0f
    var dt = 1f / 30f

    // doesn't work yet :/
    var useCustomMesh = false

    override fun fillSpace(globalTransform: Matrix4x3, dstUnion: AABBd) {
        val mesh = getMesh()
        if (mesh != null) {
            localAABB.setMin(-1.0, -1.0, -1.0).setMax(1.0, 2.0, 1.0)
            globalAABB.clear()
            localAABB.transformUnion(globalTransform, globalAABB, globalAABB)
            dstUnion.union(globalAABB)
        }
    }

    override fun onUpdate() {

        val w = resolutionX
        val h = resolutionY
        if (!useCustomMesh) {
            if (tex0.width != w || tex0.height != h || !tex0.isCreated() || !tex1.isCreated()) {
                val key = IntPair(w, h)
                val data = meshCache[key]
                createTargets(key, data.positions!!)
                meshFile = data.ref
            }
        }

        val dt0 = Time.deltaTime.toFloat()
        time += dt0
        if (time < 10f * dt) {
            while (time > dt) {
                step(dt)
                time -= dt
            }
            fract = 1f - time / dt
        } else time = 0f // else step is too large

        material.shaderOverrides["coords0Tex"] = TypeValue(GLSLType.S2D, tex0.getTexture0())
        material.shaderOverrides["coords1Tex"] = TypeValue(GLSLType.S2D, tex1.getTexture0())
        material.shaderOverrides["coordsFract"] = TypeValue(GLSLType.V1F, fract)
        material.shaderOverrides["duv"] = TypeValue(GLSLType.V2F, Vector2f(1f / (w - 1f), 1f / (h - 1f)))
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is FlagMesh) return
        dst.useCustomMesh = useCustomMesh
        dst.dt = dt
        dst.time = time
        dst.fract = fract
        dst.randomnessSeed = randomnessSeed
        dst.windStrength.set(windStrength)
    }

    override fun destroy() {
        super.destroy()
        tex0.destroy()
        tex1.destroy()
    }

    companion object {

        private val noise = FullNoise(1234L)

        val meshCache = LazyMap<IntPair, Mesh> {

            val (w, h) = it
            val mesh = Mesh()

            RectangleTerrainModel.generateQuadIndices(w, h, false, mesh)

            val aspect = 1f / (w - 1)
            val pos = mesh.positions.resize(w * h * 3)
            val uvs = mesh.uvs.resize(w * h * 2)
            for (y in 0 until h) {
                var i = y * w * 3
                for (x in 0 until w) {
                    val j = i / 3 * 2
                    uvs[j] = (w - 1 - x).toFloat() * aspect
                    uvs[j + 1] = y.toFloat() / (h - 1f)
                    pos[i++] = (w - 1 - x).toFloat() * aspect
                    pos[i++] = (h - 1 - y).toFloat() * aspect
                    i++
                }
            }
            mesh.positions = pos
            mesh.uvs = uvs

            mesh

        }

        val updateShader = Shader(
            "clothSim", emptyList(), ShaderLib.coordsUVVertexShader, ShaderLib.uvList, listOf(
                Variable(GLSLType.V1F, "dt0"),
                Variable(GLSLType.V1F, "dt1"),
                Variable(GLSLType.V1F, "maxLen"),
                Variable(GLSLType.V2F, "duv"),
                Variable(GLSLType.V3F, "acc"),
                Variable(GLSLType.S2D, "curr"),
                Variable(GLSLType.S2D, "prev"),
                Variable(GLSLType.V4F, "result", VariableMode.OUT)
            ), "" +
                    "void main() {\n" +
                    "   ivec2 uv = ivec2(gl_FragCoord.xy);\n" +
                    "   vec3 pos0 = texelFetch(prev, uv, 0).rgb;\n" +
                    "   vec3 pos1 = texelFetch(curr, uv, 0).rgb;\n" +
                    "   vec3  vel = dt0 > 0.0 ? (pos1-pos0) / dt0 : vec3(0.0);\n" +
                    "   float effect = uv.x;\n" +
                    "   ivec2 size = textureSize(curr,0);\n" +
                    "   if(uv.x < size.x-1) {\n" + // else pinned
                    "        vel += acc * dt1;\n" +
                    "       pos1 += vel * dt1;\n" +
                    // spring back by neighbors
                    "       ivec2 offsets[4] = ivec2[](ivec2(-1,0), ivec2(1,0), ivec2(0,-1), ivec2(0,1));\n" +
                    "       for(int i=0;i<4;i++){\n" +
                    "           ivec2 uv2 = uv + offsets[i];\n" +
                    "           if(uv2.x<0||uv2.y<0||uv2.x>=size.x||uv2.y>=size.y) continue;\n" +
                    "           vec3 pos2 = texelFetch(curr,uv2,0).rgb;\n" +
                    "           float len = length(pos2-pos1);\n" +
                    "           float f = 0.5 * clamp(len/maxLen-1.0,0.0,1.0);\n" +
                    "           pos1 = mix(pos1,pos2,f);\n" +
                    "       }\n" +
                    "   }\n" +
                    "   result = vec4(pos1, 1.0);\n" +
                    "}\n"
        )

        val shader = object : ECSMeshShader("cloth") {
            override fun createVertexStages(key: ShaderKey): List<ShaderStage> {
                val flagMovingStage = ShaderStage(
                    "vertex", listOf(
                        Variable(GLSLType.V2F, "duv"),
                        Variable(GLSLType.V1F, "coordsFract"),
                        Variable(GLSLType.S2D, "coords0Tex"),
                        Variable(GLSLType.S2D, "coords1Tex"),
                        Variable(GLSLType.V2F, "uvs"),
                        Variable(GLSLType.V3F, "localPosition", VariableMode.OUT),
                        Variable(GLSLType.V3F, "normal", VariableMode.OUT),
                        Variable(GLSLType.V4F, "tangent", VariableMode.OUT),
                    ), "" +
                            // todo use coordFract-dt/ds for better motion vectors
                            "localPosition = mix(texture(coords0Tex,uvs).rgb,texture(coords1Tex,uvs).rgb,coordsFract);\n" +
                            // calculate normals and tangents
                            "#ifdef COLORS\n" +
                            "    vec2 du = vec2(duv.x,0.0), dv = vec2(0.0,duv.y);\n" +
                            // is dx the correct axis for tan or bitan?
                            "    vec3 tan0 = texture(coords0Tex,uvs+du).rgb - texture(coords0Tex,uvs-du).rgb;\n" +
                            "    vec3 tan1 = texture(coords1Tex,uvs+du).rgb - texture(coords1Tex,uvs-du).rgb;\n" +
                            "    vec3 tan = normalize(mix(tan0,tan1,coordsFract));\n" +
                            "    vec3 bit0 = texture(coords0Tex,uvs+dv).rgb - texture(coords0Tex,uvs-dv).rgb;\n" +
                            "    vec3 bit1 = texture(coords1Tex,uvs+dv).rgb - texture(coords1Tex,uvs-dv).rgb;\n" +
                            "    vec3 bit = mix(bit0,bit1,coordsFract);\n" +
                            "    normal = normalize(cross(bit,tan));\n" +
                            "    tangent = vec4(tan,1.0);\n" +
                            "#endif\n"
                )
                return createDefines(key) +
                        loadVertex(key, key.flags.withoutFlag(NEEDS_MOTION_VECTORS)) +
                        flagMovingStage +
                        f(key.vertexData.loadMotionVec, key.flags.hasFlag(NEEDS_MOTION_VECTORS)) +
                        transformVertex(key) +
                        finishVertex(key)
            }
        }
    }
}
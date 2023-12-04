package me.anno.sdf.shapes

import me.anno.Time
import me.anno.ecs.annotations.Range
import me.anno.ecs.components.mesh.TypeValue
import me.anno.engine.debug.DebugLine
import me.anno.engine.debug.DebugPoint
import me.anno.engine.debug.DebugShapes.debugLines
import me.anno.engine.debug.DebugShapes.debugPoints
import me.anno.gpu.GFX
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.texture.Clamping
import me.anno.gpu.texture.Filtering
import me.anno.gpu.texture.Texture2D
import me.anno.gpu.texture.TextureLib.whiteTexture
import me.anno.graph.ui.GraphPanel.Companion.greenish
import me.anno.graph.ui.GraphPanel.Companion.red
import me.anno.image.ImageCache
import me.anno.gpu.texture.TextureCache
import me.anno.io.files.FileReference
import me.anno.io.files.InvalidRef
import me.anno.io.serialization.SerializedProperty
import me.anno.maths.Maths.length
import me.anno.maths.Maths.sq
import me.anno.sdf.SDFCombiningFunctions.sdMax
import me.anno.sdf.SDFCombiningFunctions.smoothMinCubic
import me.anno.sdf.VariableCounter
import me.anno.sdf.shapes.SDFBox.Companion.sdBox
import me.anno.utils.Color.black
import org.joml.AABBf
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.math.max

class SDFHeightMap : SDFShape() {

    companion object {

        // different ways: 2d / 1d
        // walk along path, and find the closest hit
        // to do use max-lods, to traverse the terrain quicker
        val sampleFunc = "" +
                // todo this is incorrect :/
                "float sampleHeightMapDistance(vec3 pos, vec3 dir, sampler2D image, vec3 scale, float scaI) {\n" +
                "   float dist0 = sddBox(pos,dir,1.0/scale.xzy);\n" +
                "   float skipEval = max((0.10-scaI)/(0.08),0.0);\n" +
                "   if(skipEval >= 1.0) return dist0;\n" + // skip evaluation completely
                "   vec3 pos0=pos;\n" +
                "   pos = pos.xzy * scale;\n" +
                "   dir = dir.xzy * scale;\n" +
                "   dir.y *= 2.0;\n" +
                "   pos.xy = pos.xy * 0.5 + 0.5;\n" + // center texture
                "   ivec2 size = textureSize(image,0);\n" +
                "   float dh0 = pos.z - pow(textureLod(image,pos.xy,0.0).x,2.0);\n" +
                "   float sign0 = sign(dh0);\n" +
                "   if(sign0 < 0.0) dir = -dir;\n" + // reverse the ray if inside the terrain
                "   float step = 1.0/max(1e-9,length(dir.xz)*float(max(size.x,size.y)));\n" +
                "   float distance = abs(dh0 / dir.z);\n" +
                "   float walked = 0.0, lastDh = dh0;\n" +
                "   float distToCenter = sddBox(pos.xy-vec2(0.5),dir.xy,vec2(0.5));\n" +
                "   if(distToCenter > 0.0){\n" +
                "       pos += distToCenter * dir;\n" +
                "       walked += distToCenter;\n" +
                "   }\n" +
                "   for(int i=0;i<32;i++) {\n" +
                // calculate, if we could even get a better distance
                "       pos += step * dir;\n" + // 1px forward
                "       walked += step;\n" +
                "       if(pos.x < 0.0 || pos.y < 0.0 || pos.x > 1.0 || pos.y > 1.0) break;\n" +
                // find a position, where sign(dh) != sign(dh0)
                // linear interpolation to find the hit
                "       float dh = pos.z - pow(textureLod(image,pos.xy,0.0).x,2.0);\n" +
                "       if(sign0 != sign(dh)) {\n" +
                "           float fract = step * lastDh/(lastDh-dh);\n" +
                // "           pos -= fract * dir;\n" +
                "           walked -= fract;\n" +
                "           return max(mix(sign0*walked,dist0,skipEval),dist0);\n" +
                "       } else {\n" +
                // "           float distanceI = walked + dh * step;\n" +
                // "           distance = min(distance, distanceI);\n" +
                "           lastDh = dh;\n" +
                // "           if(walked > distance || distance <= dist0) break;\n" + // we've gone too far
                "       }\n" +
                "   }\n" +
                "   return max(mix(sign0*distance,dist0,skipEval),dist0);\n" +
                "}\n"
    }

    @Range(0.0, 100.0)
    var maxHeight = 1f
        set(value) {
            field = value
            invalidateBounds()
        }

    @SerializedProperty("Image/Reference")
    var source: FileReference = InvalidRef

    private var lastImg: Texture2D? = null

    override fun buildShader(
        builder: StringBuilder,
        posIndex0: Int,
        nextVariableId: VariableCounter,
        dstIndex: Int,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>,
        seeds: ArrayList<String>
    ) {
        functions.add(smoothMinCubic)
        functions.add(sdMax)
        functions.add(sdBox)
        functions.add(sampleFunc)
        val trans = buildTransform(builder, posIndex0, nextVariableId, uniforms, functions, seeds)
        smartMinBegin(builder, dstIndex)
        val tex = defineUniform(uniforms, GLSLType.S2D) {
            val img = TextureCache[source, true]
            if (img != null && (img.filtering != Filtering.LINEAR || img.clamping != Clamping.CLAMP)) {
                img.bind(GFX.maxBoundTextures - 1, Filtering.LINEAR, Clamping.CLAMP)
            }
            if (lastImg != img) {
                lastImg = img
                invalidateBounds()
            }
            img ?: whiteTexture
        }
        val scale = defineUniform(uniforms, GLSLType.V3F) {
            val img = TextureCache[source, true]
            if (img != null) Vector3f(
                max(1f, img.height.toFloat() / img.width.toFloat()),
                max(1f, img.width.toFloat() / img.height.toFloat()),
                1f / maxHeight
            ) else Vector3f(1f, 1f, 1f / maxHeight)
        }
        builder.append("sampleHeightMapDistance(pos").append(trans.posIndex)
            .append(",dir").append(trans.posIndex)
            .append(',').append(tex)
            .append(',').append(scale)
            .append(",sca").append(posIndex0).append(')')
        smartMinEnd(builder, dstIndex, nextVariableId, uniforms, functions, seeds, trans)
    }

    override fun calculateBaseBounds(dst: AABBf) {
        val img = lastImg
        val dx: Float
        val dy: Float
        if (img == null) {
            dx = 1f
            dy = 1f
        } else if (img.width > img.height) {
            dx = 1f
            dy = img.height.toFloat() / img.width.toFloat()
        } else {
            dx = img.width.toFloat() / img.height.toFloat()
            dy = 1f
        }
        dst.setMin(-dx, 0f, -dy)
        dst.setMax(+dx, maxHeight, +dy)
    }

    override fun onDrawGUI(all: Boolean) {

        val img = ImageCache[source, true] ?: return
        val scale = Vector3f(
            max(1f, img.height.toFloat() / img.width.toFloat()),
            1f / maxHeight,
            max(1f, img.width.toFloat() / img.height.toFloat()),
        )

        // draw test ray on CPU side to make sure it's correct
        val pos = Vector3f(-1f, 0.19f, 0f)
        var pos0 = Vector3d(pos)
        debugPoints.add(DebugPoint(pos0, -1, Time.nanoTime))

        val dir = Vector3f(pos).normalize(-1f)

        pos.mul(scale)
        dir.mul(scale)
        dir.y *= 2f

        pos.set(pos.x * 0.5f + 0.5f, pos.y, pos.z * 0.5f + 0.5f)

        val step = 1f / (length(dir.x, dir.z) * max(img.width, img.height))

        fun next(pos: Vector3f, color: Int) {
            val pos1 = Vector3d(pos)
                .sub(0.5, 0.0, 0.5).div(0.5, 1.0, 0.5)
                .div(scale)
            debugPoints.add(DebugPoint(pos1, color, Time.nanoTime))
            debugLines.add(DebugLine(pos0, pos1, color, Time.nanoTime))
            pos0 = pos1
        }

        // shall be (0,2,0.5),(-1,-4,0)
        // println("local start ($scale): $pos,$dir")

        var h0 = pos.y - sq(img.getValueAt(pos.x * img.width, pos.z * img.height, 16))
        for (i in 0 until img.width) {
            pos.add(step * dir.x, step * dir.y, step * dir.z)
            val hi = pos.y - sq(img.getValueAt(pos.x * img.width, pos.z * img.height, 16) / 255f)
            if (h0 * hi < 0f) {
                val fract = step * h0 / (h0 - hi)
                pos.sub(fract * dir.x, fract * dir.y, fract * dir.z)
                next(pos, greenish or black)
                break
            }
            next(pos, if (hi > 0f) -1 else red or black)
            h0 = hi
        }

    }

    override val className: String
        get() = "SDFHeightMap"

}
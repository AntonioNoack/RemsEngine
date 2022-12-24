package me.anno.ecs.components.mesh.sdf.modifiers

import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.mesh.sdf.SDFComponent.Companion.appendVec
import me.anno.ecs.components.mesh.sdf.SDFComponent.Companion.defineUniform
import me.anno.ecs.components.mesh.sdf.SDFComponent.Companion.globalDynamic
import me.anno.ecs.components.mesh.sdf.VariableCounter
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.shader.GLSLType
import me.anno.utils.structures.arrays.IntArrayList
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.floor
import kotlin.math.sign

class SDFHexGrid : PositionMapper() {

    // todo the grid id would be interesting for sdf materials

    var dynamicSize = false
        set(value) {
            if (field != value) {
                field = value
                if(!globalDynamic) invalidateShader()
            }
        }

    var dynamicLimits = false
        set(value) {
            if (field != value) {
                field = value
                if(!globalDynamic) invalidateShader()
            }
        }

    var cellSize = 1f
        set(value) {
            if (field != value) {
                if (dynamicSize || globalDynamic) invalidateBounds()
                else invalidateShader()
                field = value
            }
        }

    var lim1 = Vector3f(2.2f)
        set(value) {
            if (dynamicLimits || globalDynamic) invalidateBounds()
            else invalidateShader()
            field.set(value)
        }

    var lim2 = Vector3f(2.2f)
        set(value) {
            if (dynamicLimits || globalDynamic) invalidateBounds()
            else invalidateShader()
            field.set(value)
        }

    override fun buildShader(
        builder: StringBuilder,
        posIndex: Int,
        nextVariableId: VariableCounter,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>,
        seeds: ArrayList<String>
    ): String? {
        functions.add(hexFunc)
        builder.append("pos").append(posIndex).append(".xz")
        builder.append("=hexGrid(")
        builder.append("pos").append(posIndex).append(".xz")
        val dynamicSize = dynamicSize || globalDynamic
        if (cellSize != 1f || dynamicSize) {
            if (dynamicSize) {
                val uniform = defineUniform(uniforms, GLSLType.V1F) { cellSize }
                builder.append("/")
                builder.append(uniform)
                buildLimParams(builder, uniforms)
                builder.append(").xy*")
                builder.append(uniform)
            } else {
                builder.append("*")
                builder.append(1f / cellSize)
                buildLimParams(builder, uniforms)
                builder.append(").xy*")
                builder.append(cellSize)
            }
            builder.append(";\n")
        } else {
            buildLimParams(builder, uniforms)
            builder.append(").xy;\n")
        }
        return null
    }

    private fun buildLimParams(builder: StringBuilder, uniforms: HashMap<String, TypeValue>) {
        val dynamicLimits = dynamicLimits || globalDynamic
        if (dynamicLimits || (lim1.x > 0 && lim1.y > 0 && lim1.z > 0 && lim2.x > 0 && lim2.y > 0 && lim2.z > 0)) {
            if (dynamicLimits) {
                val u1 = defineUniform(uniforms, lim1)
                val u2 = defineUniform(uniforms, lim2)
                builder.append(',')
                builder.append(u1)
                builder.append(',')
                builder.append(u2)
            } else {
                builder.append(',')
                builder.appendVec(lim1)
                builder.append(',')
                builder.appendVec(lim2)
            }
        } // else no limits
    }

    override fun calcTransform(pos: Vector4f, seeds: IntArrayList) {
        val scale = cellSize
        var px = pos.x / scale
        var py = pos.z / scale

        if (lim1.x > 0f) {
            // check bounds, and if outside, project onto them
            // needs to be equal to the glsl implementation
            // sine of 60°, cosine of 30°
            val c = 0.8660254f
            val l1 = lim1
            val l2 = lim2
            val px2 = px * 0.5f
            val py2 = py * c
            val s0 = -px + l1.x
            val s1 = -px2 + py2 + l1.y
            val s2 = +px2 + py2 + l1.z
            val s3 = +px + l2.x
            val s4 = +px2 - py2 + l2.y
            val s5 = -px2 - py2 + l2.z
            val sum = sign(s0) + sign(s1) + sign(s2) + sign(s3) + sign(s4) + sign(s5)
            if (sum < 6f) {
                var minV = s0
                var dx = -1f
                var dy = +0f
                if (s1 < minV) {
                    minV = s1
                    dx = -0.5f
                    dy = +c
                }
                if (s2 < minV) {
                    minV = s2
                    dx = +0.5f
                    dy = +c
                }
                if (s3 < minV) {
                    minV = s3
                    dx = 1f
                    dy = 0f
                }
                if (s4 < minV) {
                    minV = s4
                    dx = +0.5f
                    dy = -c
                }
                if (s5 < minV) {
                    minV = s5
                    dx = -0.5f
                    dy = -c
                }
                px -= minV * dx
                py -= minV * dy
            }
        }

        // cell dimensions
        val s = hexScale
        // hexagon centers
        val hc0 = floor(px / s.x) + 0.5f
        val hc1 = floor(py / s.y) + 0.5f
        val hc2 = floor((px - 1.0f) / s.x) + 0.5f
        val hc3 = floor((py - 0.5f) / s.y) + 0.5f
        // centering the coordinates with the hexagon centers above
        val h0 = px - hc0 * s.x
        val h1 = py - hc1 * s.y
        val h2 = px - (hc2 + 0.5f) * s.x
        val h3 = py - (hc3 + 0.5f) * s.y
        // local coordinates
        if (h0 * h0 + h1 * h1 < h2 * h2 + h3 * h3) {
            pos.x = h0 * scale
            pos.z = h1 * scale
        } else {
            pos.x = h2 * scale
            pos.z = h3 * scale
        }
    }

    override fun clone(): SDFHexGrid {
        val clone = SDFHexGrid()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SDFHexGrid
        clone.cellSize = cellSize
        clone.lim1.set(lim1)
        clone.lim2.set(lim2)
        clone.dynamicLimits = dynamicLimits
        clone.dynamicSize = dynamicSize
    }

    override val className get() = "SDFHexGrid"

    companion object {
        val hexScale = Vector2f(1.7320508f, 1f)
        const val hexFunc = "" +
                "vec4 hexGrid(vec2 p){\n" +
                // cell dimensions
                "   const vec2 s = vec2(1.7320508, 1.0);\n" +
                // hexagon centers
                "   vec4 hC = floor(vec4(p, p - vec2(1, .5))/s.xyxy) + .5;\n" +
                // centering the coordinates with the hexagon centers above
                "   vec4 h = vec4(p - hC.xy*s, p - (hC.zw + .5)*s);\n" +
                // local coordinates and ID or nearest hexagon center
                "   return dot(h.xy, h.xy)<dot(h.zw, h.zw) ? vec4(h.xy, hC.xy) : vec4(h.zw, hC.zw + .5);\n" +
                "}\n" +
                "vec2 hexGrid(vec2 p, vec3 l1, vec3 l2){\n" +
                "   if(l1.x > 0.0){\n" +
                // sine of 60°, cosine of 30°
                "       const float s60 = 0.8660254037844387;\n" +
                "       vec3 q = vec3(p,1.0);\n" +
                "       vec3 s1 = vec3(\n" +
                "           dot(q,vec3(-1.0,+0.0,l1.x)),\n" +
                "           dot(q,vec3(-0.5,+s60,l1.y)),\n" +
                "           dot(q,vec3(+0.5,+s60,l1.z))\n" +
                "       );\n" +
                "       vec3 s2 = vec3(\n" +
                "           dot(q,vec3(+1.0,+0.0,l2.x)),\n" +
                "           dot(q,vec3(+0.5,-s60,l2.y)),\n" +
                "           dot(q,vec3(-0.5,-s60,l2.z))\n" +
                "       );\n" +
                // check all 6 half spaces
                "       float sum = sign(s1.x)+sign(s1.y)+sign(s1.z)+sign(s2.x)+sign(s2.y)+sign(s2.z);\n" +
                // if is outside, project point actually onto hex shape
                "       if(sum < 6.0){\n" +
                "           float minV = s1.x;\n" +
                "           vec2  minD = vec2(-1.0,+0.0);\n" +
                "           if(s1.y < minV){ minV = s1.y; minD = vec2(-0.5,+s60); };\n" +
                "           if(s1.z < minV){ minV = s1.z; minD = vec2(+0.5,+s60); };\n" +
                "           if(s2.x < minV){ minV = s2.x; minD = vec2(+1.0,+0.0); };\n" +
                "           if(s2.y < minV){ minV = s2.y; minD = vec2(+0.5,-s60); };\n" +
                "           if(s2.z < minV){ minV = s2.z; minD = vec2(-0.5,-s60); };\n" +
                "           p -= minV * minD;\n" + // point from which we may check the next hex
                "       }\n" +
                "   }\n" +
                "   return hexGrid(p).xy;\n" +
                "}\n"
    }
}
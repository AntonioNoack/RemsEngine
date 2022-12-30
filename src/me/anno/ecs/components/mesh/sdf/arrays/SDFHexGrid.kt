package me.anno.ecs.components.mesh.sdf.arrays

import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.mesh.sdf.SDFComponent.Companion.defineUniform
import me.anno.ecs.components.mesh.sdf.SDFComponent.Companion.globalDynamic
import me.anno.ecs.components.mesh.sdf.VariableCounter
import me.anno.ecs.components.mesh.sdf.modifiers.PositionMapper
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.shader.GLSLType
import me.anno.utils.structures.arrays.IntArrayList
import org.joml.AABBf
import org.joml.Vector4f
import kotlin.math.floor

class SDFHexGrid : PositionMapper() {

    // todo allow any axis pair to be chosen

    var dynamicSize = false
        set(value) {
            if (field != value) {
                field = value
                if (!globalDynamic) invalidateShader()
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

    override fun buildShader(
        builder: StringBuilder,
        posIndex: Int,
        nextVariableId: VariableCounter,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>,
        seeds: ArrayList<String>
    ): String? {
        functions.add(hexFunc)
        val rnd = nextVariableId.next()
        builder.append("ivec2 tmp").append(rnd).append("=ivec2(0);\n")
        builder.append("pos").append(posIndex).append(".xz")
        builder.append("=hexGrid(pos").append(posIndex).append(".xz")
        val dynamicSize = dynamicSize || globalDynamic
        if (cellSize != 1f || dynamicSize) {
            if (dynamicSize) {
                val uniform = defineUniform(uniforms, GLSLType.V1F) { cellSize }
                builder.append("/")
                builder.append(uniform)
                builder.append(",tmp").append(rnd).append(").xy*")
                builder.append(uniform)
            } else {
                builder.append("*")
                builder.append(1f / cellSize)
                builder.append(",tmp").append(rnd).append(").xy*")
                builder.append(cellSize)
            }
            builder.append(";\n")
        } else {
            builder.append(",tmp").append(rnd).append(").xy;\n")
        }
        val seed = "seed" + nextVariableId.next()
        builder.append("int ").append(seed).append("=twoInputRandom(tmp")
            .append(rnd).append(".x,tmp")
            .append(rnd).append(".y);\n")
        seeds.add(seed)
        return null
    }

    override fun calcTransform(pos: Vector4f, seeds: IntArrayList) {
        val scale = cellSize
        val px = pos.x / scale
        val py = pos.z / scale
        // cell dimensions
        val s = hexScaleX
        // hexagon centers
        val hc0 = floor(px / s) + 0.5f
        val hc1 = floor(py) + 0.5f
        val hc2 = floor((px - 1.0f) / s) + 0.5f
        val hc3 = floor((py - 0.5f)) + 0.5f
        // centering the coordinates with the hexagon centers above
        val h0 = px - hc0 * s
        val h1 = py - hc1
        val h2 = px - (hc2 + 0.5f) * s
        val h3 = py - (hc3 + 0.5f)
        // local coordinates
        // todo calculate seed for random
        if (h0 * h0 + h1 * h1 < h2 * h2 + h3 * h3) {
            pos.x = h0 * scale
            pos.z = h1 * scale
        } else {
            pos.x = h2 * scale
            pos.z = h3 * scale
        }
    }

    override fun applyTransform(bounds: AABBf) {
        // todo better solution, which respects limits
        bounds.minX = Float.NEGATIVE_INFINITY
        bounds.maxX = Float.POSITIVE_INFINITY
        bounds.minZ = Float.NEGATIVE_INFINITY
        bounds.maxZ = Float.POSITIVE_INFINITY
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
        clone.dynamicSize = dynamicSize
    }

    override val className get() = "SDFHexGrid"

    // todo how can we implement limits properly? distance modifier, which uses hex grid size :)
    companion object {
        const val hexScaleX = 1.7320508f
        const val hexFunc = "" +
                // returns local coordinates and nearest hexagon center
                "vec4 hexGrid0(vec2 p){\n" +
                "   const vec2 s = vec2(1.7320508, 1.0);\n" + // cell dimensions
                "   vec4 c = floor(vec4(p, p - vec2(1, .5))/s.xyxy) + 0.5;\n" + // hexagon centers; int + 0.5 for both cases
                // centering the coordinates with the hexagon centers above
                "   vec4 h = vec4(p - c.xy*s, p - (c.zw + .5)*s);\n" + // distance to center
                "   return dot(h.xy, h.xy) < dot(h.zw, h.zw) ? vec4(h.xy, c.xy) : vec4(h.zw, c.zw + 0.5);\n" +
                "}\n" +
                "vec2 hexGrid(vec2 p, out ivec2 id){\n" +
                "   vec4 r = hexGrid0(p);\n" +
                "   int idx = int(round(p.x*1.15470054343));\n" +
                "   int idy = int(round(p.y-0.5*float(idx&1)));\n" +
                "   id = ivec2(r.zw*2.0);\n" +
                "   return r.xy;\n" +
                "}\n"
    }
}
package me.anno.ecs.components.shaders.sdf.modifiers

import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.shaders.sdf.SDFComponent.Companion.defineUniform
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.Ptr
import me.anno.gpu.shader.GLSLType
import org.joml.Vector2f
import org.joml.Vector4f
import kotlin.math.floor

class SDFHexGrid : PositionMapper() {

    // todo the grid id would be interesting for sdf materials
    // todo can we limit the grid?

    var cellSize = 1f
    var dynamicScale = false

    override fun buildShader(
        builder: StringBuilder,
        posIndex: Int,
        nextVariableId: Ptr<Int>,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>
    ): String? {
        functions.add(hexFunc)
        builder.append("pos").append(posIndex).append(".xz")
        builder.append("=hexGrid(")
        builder.append("pos").append(posIndex).append(".xz")
        if (cellSize != 1f || dynamicScale) {
            if (dynamicScale) {
                val uniform = defineUniform(uniforms, GLSLType.V1F, { cellSize })
                builder.append("/")
                builder.append(uniform)
                builder.append(").xy*")
                builder.append(uniform)
            } else {
                builder.append("*")
                builder.append(1f / cellSize)
                builder.append(").xy*")
                builder.append(cellSize)
            }
            builder.append(";\n")
        } else builder.append(").xy;\n")
        return null
    }

    override fun calcTransform(pos: Vector4f) {
        val scale = cellSize
        val px = pos.x / scale
        val py = pos.z / scale
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
    }

    override val className = "SDFHexGrid"

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
                "}"
    }
}
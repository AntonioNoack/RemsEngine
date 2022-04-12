package me.anno.ecs.components.mesh.sdf.modifiers

import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.mesh.sdf.SDFComponent.Companion.appendVec
import me.anno.ecs.components.mesh.sdf.SDFComponent.Companion.defineUniform
import me.anno.ecs.components.mesh.sdf.SDFComponent.Companion.globalDynamic
import me.anno.ecs.components.mesh.sdf.TwoDims
import me.anno.ecs.components.mesh.sdf.VariableCounter
import me.anno.ecs.prefab.PrefabSaveable
import org.joml.AABBf
import org.joml.Vector2f
import org.joml.Vector4f
import kotlin.math.floor

class SDFTriangleArray : PositionMapper() {

    // probably should be mappable to all axes...
    // : write me a message, when you need it, and I'll add it :

    /**
     * how large a cell needs to be;
     * should never be zero
     * */
    var cellSize = Vector2f(1f)
        set(value) {
            if (!globalDynamic && !dynamicSize) invalidateShader()
            else invalidateBounds()
            field.set(value)
        }

    var dynamicSize = false
        set(value) {
            if (field != value && !globalDynamic) invalidateShader()
            field = value
        }

    var dims: TwoDims = TwoDims.XY
        set(value) {
            if (field != value) {
                invalidateShader()
                field = value
            }
        }

    override fun buildShader(
        builder: StringBuilder,
        posIndex: Int,
        nextVariableId: VariableCounter,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>
    ): String? {
        functions.add(sdTriangleArray)
        val cellSize = cellSize
        val dynamic = dynamicSize || globalDynamic
        builder.append("pos").append(posIndex).append('.').append(dims.glslName)
        builder.append("=sdTriArray(pos").append(posIndex).append('.').append(dims.glslName)
        if (dynamic) {
            val uniform = defineUniform(uniforms, cellSize)
            builder.append('/')
            builder.append(uniform)
            builder.append(")*")
            builder.append(uniform)
        } else {
            builder.append("*vec2(")
            builder.append(1f / cellSize.x).append(',')
            builder.append(1f / cellSize.y)
            builder.append("))*")
            builder.appendVec(cellSize)
        }
        builder.append(";\n")
        return null
    }

    override fun calcTransform(pos: Vector4f) {
        // correct?
        val size = cellSize
        var px = pos.x / size.x
        var py = pos.z / size.y
        val sum1 = (px + py) * 0.36602540378f
        val sx = floor(px + sum1)
        val sy = floor(py + sum1)
        val sum2 = (sx + sy) * 0.211324865f
        px -= sx - sum2
        py -= sy - sum2
        val ix: Float
        val iy: Float
        val c = 0.12200846787f
        if (px < py) {
            ix = c
            iy = c + 1f / 3f
        } else {
            ix = c + 1f / 3f
            iy = c
        }
        pos.x = (px - ix) * size.x
        pos.z = (py - iy) * size.y
    }

    override fun applyTransform(bounds: AABBf) {
        bounds.minX = Float.NEGATIVE_INFINITY
        bounds.maxX = Float.POSITIVE_INFINITY
        bounds.minZ = Float.NEGATIVE_INFINITY
        bounds.maxZ = Float.POSITIVE_INFINITY
    }

    override fun clone(): SDFTriangleArray {
        val clone = SDFTriangleArray()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SDFTriangleArray
        clone.dynamicSize = dynamicSize
        clone.cellSize = cellSize
    }

    override val className: String = "SDFTriangleArray"

    companion object {
        // extracted from https://www.shadertoy.com/view/WtfGDX
        const val sdTriangleArray = "" +
                "vec2 sdTriArray(vec2 p){\n" +
                // SIMPLEX GRID SETUP
                "    vec2 s = floor(p + (p.x + p.y)*0.36602540378);\n" + // Skew the current point
                "    p -= s - (s.x + s.y)*0.211324865;\n" + // Use it to attain the vector to the base vertex (from p)
                // Determine which triangle we're in. Much easier to visualize than the 3D version
                "    vec2 i = p.x < p.y? vec2(0.0, 1.0/3.0) : vec2(1.0/3.0, 0.0);\n" + // Apparently faster than: i = step(p.y, p.x)
                // Centralize everything, so that vec2(0) is in the center of the triangle
                "    return p - 0.12200846787 - i;\n" +
                "}\n"
    }

}
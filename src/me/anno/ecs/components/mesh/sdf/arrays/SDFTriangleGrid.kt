package me.anno.ecs.components.mesh.sdf.arrays

import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.mesh.sdf.SDFComponent.Companion.appendVec
import me.anno.ecs.components.mesh.sdf.SDFComponent.Companion.defineUniform
import me.anno.ecs.components.mesh.sdf.SDFComponent.Companion.globalDynamic
import me.anno.ecs.components.mesh.sdf.TwoDims
import me.anno.ecs.components.mesh.sdf.VariableCounter
import me.anno.ecs.components.mesh.sdf.modifiers.PositionMapper
import me.anno.ecs.components.mesh.sdf.random.SDFRandom.Companion.twoInputRandom
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.maths.Maths.hasFlag
import me.anno.maths.Maths.max
import me.anno.utils.structures.arrays.IntArrayList
import org.joml.AABBf
import org.joml.Vector2f
import org.joml.Vector4f
import kotlin.math.round
import kotlin.math.sign

class SDFTriangleGrid : PositionMapper() {

    // probably should be mappable to all axes...
    // : write me a message, when you need it, and I'll add it :

    /**
     * how large a cell needs to be;
     * should never be zero
     * */
    var cellSize = Vector2f(1f)
        set(value) {
            if (!globalDynamic && !dynamic) invalidateShader()
            else invalidateBounds()
            field.set(value)
        }

    var dynamic = false
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
        functions: HashSet<String>,
        seeds: ArrayList<String>
    ): String? {
        functions.add(sdTriangleArray)
        val rnd = nextVariableId.next()
        builder.append("ivec2 tmp").append(rnd).append("=ivec2(0);\n")
        val cellSize = cellSize
        val dynamic = dynamic || globalDynamic
        builder.append("pos").append(posIndex).append('.').append(dims.glslName)
        builder.append("=sdTriArray(pos").append(posIndex).append('.').append(dims.glslName)
        if (dynamic) {
            val uniform = defineUniform(uniforms, cellSize)
            builder.append("/")
                .append(uniform)
                .append(",tmp").append(rnd).append(")*")
                .append(uniform)
        } else {
            builder.append("*vec2(")
            builder.append(1f / cellSize.x).append(',')
            builder.append(1f / cellSize.y)
            builder.append("),tmp").append(rnd).append("))*")
            builder.appendVec(cellSize)
        }
        builder.append(";\n")
        val seed = "seed" + nextVariableId.next()
        builder.append("int ").append(seed).append("=twoInputRandom(tmp").append(rnd).append(".x,tmp").append(rnd)
            .append(".y);\n")
        seeds.add(seed)
        return null
    }

    override fun calcTransform(pos: Vector4f, seeds: IntArrayList) {
        // todo test if matrix transforms are correct :) (p->k and q->q2)
        val size = cellSize
        val px = pos.x * 0.5f / size.x
        val py = pos.z * 0.5f / size.y
        val kx = px + 0.6f * py
        val ky = px - 0.6f * py
        val ix = round(kx)
        val iy = round(ky)
        val qx = kx - ix
        val qy = ky - iy
        val s = sign(qy - qx)
        val cx = ix.toInt()
        val cy = (iy * 2 + max(s, 0f)).toInt()
        seeds.add(twoInputRandom(cx, cy))
        val qx2 = qx + qy
        val qy2 = 1.6666667f * (qx - qy) + 0.53f * s
        pos.x = qx2 * size.x
        pos.z = qy2 * size.y
    }

    override fun applyTransform(bounds: AABBf) {
        // to do apply with proper count limit
        if (dims.flags.hasFlag(1)) {
            bounds.minX = Float.NEGATIVE_INFINITY
            bounds.maxX = Float.POSITIVE_INFINITY
        }
        if (dims.flags.hasFlag(2)) {
            bounds.minY = Float.NEGATIVE_INFINITY
            bounds.maxY = Float.POSITIVE_INFINITY
        }
        if (dims.flags.hasFlag(4)) {
            bounds.minZ = Float.NEGATIVE_INFINITY
            bounds.maxZ = Float.POSITIVE_INFINITY
        }
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as SDFTriangleGrid
        dst.dynamic = dynamic
        dst.cellSize.set(cellSize)
    }

    override val className: String get() = "SDFTriangleGrid"

    companion object {
        // extracted from https://www.shadertoy.com/view/WtfGDX
        const val sdTriangleArray = "" +
                "vec2 sdTriArray(vec2 p, inout ivec2 c){\n" +
                "   p *= 0.5;\n" +
                "   mat2 m = mat2(1.0, 1.0, 0.6, -0.6);\n" +
                "   vec2 k = m * p;\n" +
                "   vec2 i = round(k);\n" +
                "   vec2 q = k-i;\n" +
                "   float s = sign(q.y-q.x);\n" +
                "   c = ivec2(i.x,i.y*2.0+max(s,0.0));\n" +
                "   q = mat2(1.0, 1.6666667, 1.0, -1.6666667) * q;" +
                "   q.y += 0.530 * s;\n" +
                "   return q;\n" +
                "}\n"
    }

}
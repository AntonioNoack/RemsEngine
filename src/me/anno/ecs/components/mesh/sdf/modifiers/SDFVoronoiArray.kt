package me.anno.ecs.components.mesh.sdf.modifiers

import me.anno.ecs.annotations.Range
import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.mesh.sdf.SDFComponent.Companion.appendUniform
import me.anno.ecs.components.mesh.sdf.SDFComponent.Companion.defineUniform
import me.anno.ecs.components.mesh.sdf.SDFComponent.Companion.globalDynamic
import me.anno.ecs.components.mesh.sdf.VariableCounter
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.shader.GLSLType
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.fract
import me.anno.maths.Maths.sq
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Booleans.toInt
import me.anno.utils.types.Vectors.dot2
import org.joml.AABBf
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.sin

class SDFVoronoiArray : PositionMapper() {

    // we could beautify the result when the shapes are overlapping by repeatedly calling the child...
    // would be pretty expensive...

    /**
     * how large a cell needs to be;
     * should never be zero
     * */
    var cellSize = Vector3f(1f)
        set(value) {
            if (!globalDynamic) invalidateShader()
            else invalidateBounds()
            field.set(value)
        }

    var seed = Vector3f(0f)
        set(value) {
            if (!globalDynamic) invalidateShader()
            else invalidateBounds()
            field.set(value)
        }

    var min = Vector3f(-2f)
        set(value) {
            invalidateBounds()
            field.set(value)
        }

    var max = Vector3f(+2f)
        set(value) {
            invalidateBounds()
            field.set(value)
        }

    @Range(-1.0, 1.0)
    var randomness = 1f

    var enableX = true
        set(value) {
            if (field != value) {
                invalidateShader()
                field = value
            }
        }

    var enableY = false
        set(value) {
            if (field != value) {
                invalidateShader()
                field = value
            }
        }

    var enableZ = true
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

        val x = enableX
        val y = enableY
        val z = enableZ

        val c = when (val cell = x.toInt(1) + y.toInt(2) + z.toInt(4)) {
            0 -> return null
            1, 2, 4 -> { // 1d
                if (x) ".x" else if (y) ".y" else ".z"
            }
            3, 5, 6 -> { // 2d
                if (cell == 3) ".xy" else if (cell == 5) ".xz" else ".yz"
            }
            // 7, 3d
            else -> ""
        }

        val cellSize = defineUniform(uniforms, cellSize)
        val seed = defineUniform(uniforms, seed)
        val min = defineUniform(uniforms, min)
        val max = defineUniform(uniforms, max)

        functions.add(sdVoronoi)

        builder.append("pos").append(posIndex).append(c)
        builder.append("=voronoi(pos").append(posIndex).append(c).append('/')
        builder.append(cellSize).append(c).append(',')
        builder.append(seed).append(c).append(',')
        builder.append(min).append(c).append(',')
        builder.append(max).append(c).append(',')
        builder.appendUniform(uniforms, GLSLType.V1F) { randomness }
        builder.append(")*")
        builder.append(cellSize).append(c)
        builder.append(";\n")

        return null
    }

    override fun calcTransform(pos: Vector4f) {

        // horribly complicated function, but whatever, we can't fix that...

        val x = enableX
        val y = enableY
        val z = enableZ
        if (x || y || z) {

            if (x) pos.x /= cellSize.x
            if (y) pos.y /= cellSize.y
            if (z) pos.z /= cellSize.z

            var bestDistance = Float.POSITIVE_INFINITY
            val randomness = randomness
            when (val case = x.toInt(1) + y.toInt(2) + z.toInt(4)) {
                1, 2, 4 -> {
                    val p: Float
                    val min: Float
                    val max: Float
                    val seed: Float
                    when (case) {
                        1 -> {
                            p = pos.x
                            min = this.min.x
                            max = this.max.x
                            seed = this.seed.x
                        }
                        2 -> {
                            p = pos.y
                            min = this.min.y
                            max = this.max.y
                            seed = this.seed.y
                        }
                        else -> {
                            p = pos.z
                            min = this.min.z
                            max = this.max.z
                            seed = this.seed.z
                        }
                    }
                    val cellIndex = round(clamp(p, min, max))
                    var bestDelta = p
                    val fractional = p - cellIndex
                    for (i in -1..1) {
                        val cellOffset = i.toFloat()
                        val cellPos = cellIndex + cellOffset
                        val center = cellOffset + (hash(cellPos + seed) - 0.5f) * randomness
                        val delta = fractional - center
                        val dist = abs(delta)
                        if (dist < bestDistance) {
                            bestDistance = dist
                            bestDelta = delta
                        }
                    }
                    when (case) {
                        1 -> pos.x = bestDelta
                        2 -> pos.y = bestDelta
                        else -> pos.z = bestDelta
                    }
                }
                3, 5, 6 -> {
                    val px: Float
                    val py: Float
                    val cix: Float
                    val ciy: Float
                    when (case) {
                        3 -> {
                            px = pos.x
                            py = pos.y
                            cix = round(clamp(px, min.x, max.x))
                            ciy = round(clamp(py, min.y, max.y))
                        }
                        5 -> {
                            px = pos.x
                            py = pos.z
                            cix = round(clamp(px, min.x, max.x))
                            ciy = round(clamp(py, min.z, max.z))
                        }
                        else -> {
                            px = pos.y
                            py = pos.z
                            cix = round(clamp(px, min.y, max.y))
                            ciy = round(clamp(py, min.z, max.z))
                        }
                    }
                    val fx = px - cix // fractional
                    val fy = py - ciy
                    val hash = JomlPools.vec2f.create()
                    var bx = px
                    var by = py
                    for (i in -1..1) {
                        val cox = i.toFloat() // cell offset
                        val cpx = cix + cox // cell pos
                        for (j in -1..1) {
                            val coy = j.toFloat()
                            val cpy = ciy + coy
                            hash(hash.set(cpx, cpy), hash)
                            val cx = cox + (hash.x - 0.5f) * randomness // center
                            val cy = coy + (hash.y - 0.5f) * randomness
                            val dx = fx - cx // delta
                            val dy = fy - cy
                            val dist = sq(dx, dy)
                            if (dist < bestDistance) {
                                bestDistance = dist
                                bx = dx
                                by = dy
                            }
                        }
                    }
                    JomlPools.vec2f.sub(1)
                    when (case) {
                        3 -> {
                            pos.x = bx
                            pos.y = by
                        }
                        5 -> {
                            pos.x = bx
                            pos.z = by
                        }
                        else -> {
                            pos.y = bx
                            pos.z = by
                        }
                    }
                }
                else -> {
                    val px = pos.x // p
                    val py = pos.y
                    val pz = pos.z
                    val min = min
                    val max = max
                    val cix = round(clamp(px, min.x, max.x)) // cell index
                    val ciy = round(clamp(py, min.y, max.y))
                    val ciz = round(clamp(pz, min.z, max.z))
                    val fx = px - cix // fractional
                    val fy = py - ciy
                    val fz = pz - ciz
                    val hash = JomlPools.vec3f.create()
                    for (i in -1..1) {
                        val cox = i.toFloat() // cell offset
                        val cpx = cix + cox // cell pos
                        for (j in -1..1) {
                            val coy = j.toFloat()
                            val cpy = ciy + coy
                            for (k in -1..1) {
                                val coz = k.toFloat()
                                val cpz = ciz + coz
                                hash(hash.set(cpx, cpy, cpz), hash)
                                val cx = cox + (hash.x - 0.5f) * randomness // center
                                val cy = coy + (hash.y - 0.5f) * randomness
                                val cz = coz + (hash.z - 0.5f) * randomness
                                val dx = fx - cx // delta
                                val dy = fy - cy
                                val dz = fz - cz
                                val dist = sq(dx, dy, dz)
                                if (dist < bestDistance) {
                                    bestDistance = dist
                                    pos.set(dx, dy, dz)
                                }
                            }
                        }
                    }
                    JomlPools.vec3f.sub(1)
                }
            }

            if (x) pos.x *= cellSize.x
            if (y) pos.y *= cellSize.y
            if (z) pos.z *= cellSize.z

        }
    }

    override fun applyTransform(bounds: AABBf) {
        // applying guessed limits
        // why 1.5?
        val cellSize = cellSize
        if (enableX) {
            bounds.minX += (round(min.x) - 1.5f) * cellSize.x
            bounds.maxX += (round(max.x) + 1.5f) * cellSize.x
        } // -1 for 1 extra
        if (enableY) {
            bounds.minY += (round(min.y) - 1.5f) * cellSize.y
            bounds.maxY += (round(max.y) + 1.5f) * cellSize.y
        }
        if (enableZ) {
            bounds.minZ += (round(min.z) - 1.5f) * cellSize.z
            bounds.maxZ += (round(max.z) + 1.5f) * cellSize.z
        }
    }

    private fun hash(p: Float): Float {
        return fract(sin(p * 127.1f) * 43758.5453f)
    }

    private fun hash(p: Vector2f, dst: Vector2f): Vector2f {
        return dst.set(
            fract(sin(p.dot2(127.1f, 311.7f)) * 43758.5453f),
            fract(sin(p.dot2(269.5f, 183.3f)) * 43758.5453f)
        )
    }

    private fun hash(p: Vector3f, dst: Vector3f): Vector3f {
        return dst.set(
            fract(sin(p.dot(127.1f, 311.7f, 157.2f)) * 43758.5453f),
            fract(sin(p.dot(269.5f, 183.3f, 97.5f)) * 43758.5453f),
            fract(sin(p.dot(175.3f, 217.9f, 278.4f)) * 43758.5453f)
        )
    }

    override fun clone(): SDFVoronoiArray {
        val clone = SDFVoronoiArray()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SDFVoronoiArray
        clone.enableX = enableX
        clone.enableY = enableY
        clone.enableZ = enableZ
        clone.seed = seed
        clone.cellSize = cellSize
        clone.min = min
        clone.max = max
    }

    override val className: String = "SDFVoronoiArray"

    companion object {

        // inspired by https://www.shadertoy.com/view/ldl3W8, Inigo Quilez
        const val sdVoronoi = "" +
                // can we mirror cells?
                // probably, but not that easily...
                "float hash1(float p) {\n" +
                "    return fract(sin(p*127.1)*43758.5453);\n" +
                "}\n" +
                "vec2 hash2(vec2 p) {\n" +
                "    return fract(sin(vec2(dot(p,vec2(127.1,311.7)),dot(p,vec2(269.5,183.3))))*43758.5453);\n" +
                "}\n" +
                "vec3 hash3(vec3 p) {\n" +
                "    return fract(sin(vec3(\n" +
                "       dot(p,vec3(127.1,311.7,157.2)),\n" +
                "       dot(p,vec3(269.5,183.3, 97.5)),\n" +
                "       dot(p,vec3(175.3,217.9,278.4))\n" +
                "   ))*43758.5453);\n" +
                "}\n" +
                "float voronoi(float p, float seed, float min, float max, float rnd){\n" +
                "   float cellIndex = round(clamp(p, min, max));\n" +
                "   float bestDistance = Infinity;\n" +
                "   float fractional = p - cellIndex;\n" +
                "   for(int i=-1;i<=1;i++){\n" +
                "       float cellOffset = float(i);\n" +
                "       float cellPos = cellIndex + cellOffset;\n" +
                "       float center = cellOffset + (hash1(cellPos + seed) - 0.5) * rnd;\n" +
                "       float delta = fractional - center;\n" +
                "       float dist = delta*delta;\n" + // abs or x², which is faster?
                "       if(dist < bestDistance){\n" +
                "           bestDistance = dist;\n" +
                "           p = delta;\n" +
                "       }\n" +
                "   }\n" +
                "   return p;\n" +
                "}\n" +
                "vec2 voronoi(vec2 p, vec2 seed, vec2 min, vec2 max, float rnd){\n" +
                "   vec2 cellIndex = round(clamp(p, min, max));\n" +
                "   float bestDistance = Infinity;\n" +
                "   vec2 fractional = p - cellIndex;\n" +
                "   for(int i=-1;i<=1;i++){\n" +
                "       for(int j=-1;j<=1;j++){\n" +
                "           vec2 cellOffset = vec2(float(i),float(j));\n" +
                "           vec2 cellPos = cellIndex + cellOffset;\n" +
                "           vec2 center = cellOffset + (hash2(cellPos + seed) - 0.5) * rnd;\n" +
                "           vec2 delta = fractional - center;\n" +
                "           float dist = dot(delta,delta);\n" +
                "           if(dist < bestDistance){\n" +
                "               bestDistance = dist;\n" +
                "               p = delta;\n" +
                "           }\n" +
                "       }\n" +
                "   }\n" +
                "   return p;\n" +
                "}\n" +
                "vec3 voronoi(vec3 p, vec3 seed, vec3 min, vec3 max, float rnd){\n" +
                "   vec3 cellIndex = round(clamp(p, min, max));\n" +
                "   float bestDistance = Infinity;\n" +
                "   vec3 fractional = p - cellIndex;\n" +
                "   for(int i=-1;i<=1;i++){\n" +
                "       for(int j=-1;j<=1;j++){\n" +
                "           for(int k=-1;k<=1;k++){\n" +
                "               vec3 cellOffset = vec3(float(i),float(j),float(k));\n" +
                "               vec3 cellPos = cellIndex + cellOffset;\n" +
                "               vec3 center = cellOffset + (hash3(cellPos + seed) - 0.5) * rnd;\n" +
                "               vec3 delta = fractional - center;\n" +
                "               float dist = dot(delta,delta);\n" +
                "               if(dist < bestDistance){\n" +
                "                   bestDistance = dist;\n" +
                "                   p = delta;\n" +
                "               }\n" +
                "           }\n" +
                "       }\n" +
                "   }\n" +
                "   return p;\n" +
                "}\n"
    }

}
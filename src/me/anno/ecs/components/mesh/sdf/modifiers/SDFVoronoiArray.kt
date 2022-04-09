package me.anno.ecs.components.mesh.sdf.modifiers

import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.mesh.sdf.SDFComponent.Companion.defineUniform
import me.anno.ecs.components.mesh.sdf.SDFComponent.Companion.globalDynamic
import me.anno.ecs.components.mesh.sdf.VariableCounter
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.utils.types.AABBs.allX
import me.anno.utils.types.AABBs.allY
import me.anno.utils.types.AABBs.allZ
import me.anno.utils.types.Booleans.toInt
import org.joml.AABBf
import org.joml.Vector3f
import org.joml.Vector4f

class SDFVoronoiArray : PositionMapper() {

    // todo we could limit the cell index :)
    // todo all arrays should have the option to evaluate the neighbors for correct sdfs without gaps

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

        functions.add(sdVoronoi)

        val cellSize = defineUniform(uniforms, cellSize)
        val seed = defineUniform(uniforms, seed)

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

        builder.append("pos").append(posIndex).append(c)
        builder.append("=voronoi(pos").append(posIndex).append(c).append('/')
        builder.append(cellSize).append(c).append(',')
        builder.append(seed).append(c).append(")*")
        builder.append(cellSize).append(c)
        builder.append(";\n")

        return null
    }

    override fun calcTransform(pos: Vector4f) {
        val x = enableX
        val y = enableY
        val z = enableZ
        if (x || y || z) {
            if (x) pos.x /= cellSize.x
            if (y) pos.y /= cellSize.y
            if (z) pos.z /= cellSize.z

            var bestDistance = 100f
            when (val case = x.toInt(1) + y.toInt(2) + z.toInt(4)) {
                1, 2, 4 -> {
                    // todo 1d
                    for (i in -1..1) {

                    }
                }
                3, 5, 6 -> {
                    // todo 2d
                    for (i in -1..1) {
                        for (j in -1..1) {

                        }
                    }
                }
                else -> {
                    for (i in -1..1) {
                        for (j in -1..1) {
                            for (k in -1..1) {

                            }
                        }
                    }
                    // todo 3d
                }
            }

            if (x) pos.x *= cellSize.x
            if (y) pos.y *= cellSize.y
            if (z) pos.z *= cellSize.z

        }
    }

    override fun applyTransform(bounds: AABBf) {
        if (enableX) bounds.allX()
        if (enableY) bounds.allY()
        if (enableZ) bounds.allZ()
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
    }

    override val className: String = "SDFVoronoiArray"

    companion object {

        // inspired by https://www.shadertoy.com/view/ldl3W8, Inigo Quilez
        const val sdVoronoi = "" +
                // todo can we mirror cells?
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
                "float voronoi(float p, float seed){\n" +
                "   float cellIndex = round(p);\n" +
                "   float bestDelta;\n" +
                "   float bestDistance = 128.0;\n" +
                "   float fractional = p - cellIndex;\n" +
                "   for(int i=-1;i<=1;i++){\n" +
                "       float cellOffset = float(i);\n" +
                "       float center = cellOffset + hash1(cellIndex + cellOffset + seed) - 0.5;\n" +
                "       float delta = fractional - center;\n" +
                "       float dist = delta*delta;\n" + // abs or x², which is faster?
                "       if(dist < bestDistance){\n" +
                "           bestDistance = dist;\n" +
                "           bestDelta = delta;\n" +
                "       }\n" +
                "   }\n" +
                "   p = bestDelta;\n" + // p - (cellIndex + cellOffset + hash)
                "   return p;\n" +
                "}\n" +
                "vec2 voronoi(vec2 p, vec2 seed){\n" +
                "   vec2 cellIndex = round(p);\n" +
                "   vec2 bestDelta;\n" +
                "   float bestDistance = 128.0;\n" +
                "   vec2 fractional = p - cellIndex;\n" +
                "   for(int i=-1;i<=1;i++){\n" +
                "       for(int j=-1;j<=1;j++){\n" +
                "           vec2 cellOffset = vec2(float(i),float(j));\n" +
                "           vec2 center = cellOffset + hash2(cellIndex + cellOffset + seed) - 0.5;\n" +
                "           vec2 delta = fractional - center;\n" +
                "           float dist = dot(delta,delta);\n" +
                "           if(dist < bestDistance){\n" +
                "               bestDistance = dist;\n" +
                "               bestDelta = delta;\n" +
                "           }\n" +
                "       }\n" +
                "   }\n" +
                "   p = bestDelta;\n" +
                "   return p;\n" +
                "}\n" +
                "vec3 voronoi(vec3 p, vec3 seed){\n" +
                "   vec3 cellIndex = round(p);\n" +
                "   vec3 bestDelta;\n" +
                "   float bestDistance = 128.0;\n" +
                "   vec3 fractional = p - cellIndex;\n" +
                "   for(int i=-1;i<=1;i++){\n" +
                "       for(int j=-1;j<=1;j++){\n" +
                "           for(int k=-1;k<=1;k++){\n" +
                "               vec3 cellOffset = vec3(float(i),float(j),float(k));\n" +
                "               vec3 center = cellOffset + hash3(cellIndex + cellOffset + seed) - 0.5;\n" +
                "               vec3 delta = fractional - center;\n" +
                "               float dist = dot(delta,delta);\n" +
                "               if(dist < bestDistance){\n" +
                "                   bestDistance = dist;\n" +
                "                   bestDelta = delta;\n" +
                "               }\n" +
                "           }\n" +
                "       }\n" +
                "   }\n" +
                "   p = bestDelta;\n" +
                "   return p;\n" +
                "}\n"
    }

}
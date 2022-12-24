package me.anno.ecs.components.mesh.sdf.modifiers.random

import me.anno.ecs.annotations.Range
import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.mesh.sdf.SDFComponent.Companion.appendUniform
import me.anno.ecs.components.mesh.sdf.SDFComponent.Companion.globalDynamic
import me.anno.ecs.components.mesh.sdf.VariableCounter
import me.anno.ecs.components.mesh.sdf.modifiers.PositionMapper
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.shader.GLSLType
import me.anno.maths.Maths.clamp
import me.anno.utils.structures.arrays.IntArrayList
import org.joml.Vector4f

// todo implement sth using this
// todo test it
// todo use it for imperfections in bricks :)

// todo generally: use array index procedurally :)

abstract class SDFRandom : PositionMapper() {

    var dynamic = false
        set(value) {
            if (field != value) {
                field = value
                if (!globalDynamic) invalidateShader()
            }
        }

    // to how many instances the shift is applied
    @Range(0.0, 1.0)
    var appliedPortion = 1f
        set(value) {
            if (field != value) {
                if (!globalDynamic && !dynamic) invalidateShader()
                else invalidateBounds()
                field = value
            }
        }

    var seedXOR = 0
        set(value) {
            if (field != value) {
                if (!globalDynamic && !dynamic) invalidateShader()
                field = value
            }
        }

    var randomIndex = 0
        set(value) {
            field = value
            invalidateShader()
        }

    override fun buildShader(
        builder: StringBuilder,
        posIndex: Int,
        nextVariableId: VariableCounter,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>,
        seeds: ArrayList<String>
    ): String? {

        val dynamic = dynamic || globalDynamic

        functions.add(randLib)

        val seedName = "tmp" + nextVariableId.next()
        builder.append("int ").append(seedName).append("=")
        if (dynamic) builder.appendUniform(uniforms, GLSLType.V1I) { seedXOR }
        else builder.append(seedXOR)
        builder.append(";\n")

        if (seeds.isNotEmpty()) {
            val seedName1 = seeds[clamp(seeds.lastIndex - randomIndex, 0, seeds.lastIndex)]
            builder.append(seedName).append("^=").append(seedName1).append(";\n")
        }

        return if (dynamic || appliedPortion > 0f) {
            builder.append(seedName).append("=nextRandI(").append(seedName).append(");\n")
            builder.append("if((").append(seedName).append(" & 16777215) < ${appliedPortion * 16777215}){\n")
            val offsetName = buildShader(builder, posIndex, nextVariableId, uniforms, functions, seedName)
            builder.append("}\n")
            offsetName
        } else null
    }

    abstract fun buildShader(
        builder: StringBuilder,
        posIndex: Int,
        nextVariableId: VariableCounter,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>,
        seed: String
    ): String?

    override fun calcTransform(pos: Vector4f, seeds: IntArrayList) {
        var seed = seedXOR
        if (seeds.isNotEmpty()) {
            seed = seed xor seeds[clamp(seeds.lastIndex - randomIndex, 0, seeds.lastIndex)]
        }
        calcTransform(pos, seed)
    }

    fun nextRandI(i: Int): Int {
        return 11 - i * 554899859
    }

    fun nextRandF(i: Int): Float {
        return (i and 16777215) / 16777215f
    }

    abstract fun calcTransform(pos: Vector4f, seed: Int)

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SDFRandom
        clone.dynamic = dynamic
        clone.appliedPortion = appliedPortion
        clone.seedXOR = seedXOR
        clone.randomIndex = randomIndex
    }

    companion object {
        val randLib = "" +
                // https://stackoverflow.com/questions/71420930/random-number-generator-with-3-inputs
                "int rotl32(int n, int k) {\n" +
                "  int a = n << k;\n" +
                "  int b = int(uint(n) >> (32 - k));\n" +
                "  return a | b;\n" +
                "}\n" +
                "int threeInputRandom(int x, int y, int z) {\n" +
                "  int a = x;\n" +
                "  int b = y;\n" +
                "  int c = z;\n" +
                "  b ^= rotl32(a + c, 7);\n" +
                "  c ^= rotl32(b + a, 9);\n" +
                "  a ^= rotl32(c + b, 18);\n" +
                "  b ^= rotl32(a + c, 7);\n" +
                "  c ^= rotl32(b + a, 9);\n" +
                "  a ^= rotl32(c + b, 18);\n" +
                "  b ^= rotl32(a + c, 7);\n" +
                "  c ^= rotl32(b + a, 9);\n" +
                "  a ^= rotl32(c + b, 18);\n" +
                "  return a + b + c + x + y + z;\n" +
                "}\n" +
                "int   nextRandI(       int i){return 11-i*554899859;}\n" +
                "float nextRandF( inout int i){float v = float(i&16777215)/16777215.0; i = nextRandI(i); return v; }\n" +
                "vec2  nextRandF2(inout int i){return vec2(nextRandF(i), nextRandF(i)); }\n"  +
                "vec3  nextRandF3(inout int i){return vec3(nextRandF(i), nextRandF(i), nextRandF(i)); }\n"  +
                "vec4  nextRandF4(inout int i){return vec4(nextRandF(i), nextRandF(i), nextRandF(i), nextRandF(i)); }\n"
    }

}
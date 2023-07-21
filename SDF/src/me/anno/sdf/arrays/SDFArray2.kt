package me.anno.sdf.arrays

import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.gpu.shader.GLSLType
import me.anno.sdf.VariableCounter
import me.anno.sdf.arrays.SDFArrayMapper.Companion.sdArray
import me.anno.sdf.random.SDFRandom.Companion.randLib
import org.joml.AABBf
import org.joml.Vector3f
import org.joml.Vector3i

// todo calculate things on cpu side
class SDFArray2 : SDFGroupArray() {

    override fun applyArrayTransform(bounds: AABBf) {
        val rep = cellSize
        val lim = count
        if (rep.x > 0f) {
            bounds.minX = SDFArrayMapper.minMod2(bounds.minX, rep.x, lim.x)
            bounds.maxX = SDFArrayMapper.maxMod2(bounds.maxX, rep.x, lim.x)
        }
        if (rep.y > 0f) {
            bounds.minY = SDFArrayMapper.minMod2(bounds.minY, rep.y, lim.y)
            bounds.maxY = SDFArrayMapper.maxMod2(bounds.maxY, rep.y, lim.y)
        }
        if (rep.z > 0f) {
            bounds.minZ = SDFArrayMapper.minMod2(bounds.minZ, rep.z, lim.z)
            bounds.maxZ = SDFArrayMapper.maxMod2(bounds.maxZ, rep.z, lim.z)
        }
    }

    /**
     * repetition count
     * */
    var count = Vector3i(3)
        set(value) {
            if (!globalDynamic && (!dynamic && (value.x > 0 || value.y > 0 || value.z > 0))) {
                invalidateShader()
            } else invalidateBounds()
            field.set(value)
        }

    /**
     * how large a cell needs to be;
     * should never be zero
     * */
    var cellSize = Vector3f(1f)
        set(value) {
            if (!globalDynamic && (!dynamic && (value.x > 0 || value.y > 0 || value.z > 0))) {
                invalidateShader()
            } else invalidateBounds()
            field.set(value)
        }

    var dynamic = false
        set(value) {
            if (field != value && !globalDynamic) invalidateShader()
            field = value
        }

    var mirrorX = false
        set(value) {
            if (field != value) {
                invalidateShader()
                field = value
            }
        }

    var mirrorY = false
        set(value) {
            if (field != value) {
                invalidateShader()
                field = value
            }
        }

    var mirrorZ = false
        set(value) {
            if (field != value) {
                invalidateShader()
                field = value
            }
        }

    override fun calculateHalfCellSize(
        builder: StringBuilder,
        nextVariableId: VariableCounter,
        dstIndex: Int,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>,
        seeds: ArrayList<String>
    ) {
        val cellSize = defineUniform(uniforms, GLSLType.V3F, cellSize)
        // todo use rotated dir
        builder.append("mcs").append(dstIndex).append("=dot(abs(rd)*").append(cellSize).append(",vec3(1.0));\n")
    }

    override fun defineLoopHead(
        builder: StringBuilder,
        posIndex0: Int,
        innerDstIndex: Int,
        outerDstIndex: Int,
        nextVariableId: VariableCounter,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>,
        seeds: ArrayList<String>
    ): Int {
        functions.add(sdArray)
        functions.add(randLib)
        // for all dimensions:
        val axes = "xyz"
        val relativeOverlap = defineUniform(uniforms, GLSLType.V3F, relativeOverlap)
        val count = defineUniform(uniforms, GLSLType.V3I, count)
        val cellSize = defineUniform(uniforms, GLSLType.V3F, cellSize)
        val pp1 = nextVariableId.next()
        builder.append("vec3 l$pp1=vec3($count-1)*0.5+vec3(lessThanEqual($count,ivec3(0)))*1e38;\n")
        builder.append("vec3 h$pp1=vec3(").append(count).append("&1)*0.5;\n")
        builder.append("vec3 min$pp1=mod2C(pos$posIndex0-$relativeOverlap*$cellSize,$cellSize,l$pp1,h$pp1);\n")
        builder.append("vec3 max$pp1=mod2C(pos$posIndex0+$relativeOverlap*$cellSize,$cellSize,l$pp1,h$pp1);\n")
        builder.append("vec3 pos").append(pp1).append("=pos").append(posIndex0).append(";\n")
        builder.append("vec3 dir").append(pp1).append("=dir").append(posIndex0).append(";\n")
        val tmp1 = nextVariableId.next()
        builder.append("vec3 tmp").append(tmp1).append(";\n")
        builder.append("bool first").append(innerDstIndex).append("=true;\n")
        for (axis in 0 until 3) {
            val mirror = when (axis) {
                0 -> mirrorX
                1 -> mirrorY
                else -> mirrorZ
            }
            val a = axes[axis]
            builder.append("for(tmp$tmp1.$a=min$pp1.$a;tmp$tmp1.$a<=max$pp1.$a;tmp$tmp1.$a++){\n")
            if (mirror) {
                builder.append("pos${pp1}.$a=(pos$posIndex0.$a-$cellSize.$a*tmp$tmp1.$a)*mirror(tmp$tmp1.$a);\n")
                builder.append("dir${pp1}.$a=dir$posIndex0.$a*mirror(tmp$tmp1.$a);\n")
            } else {
                builder.append("pos${pp1}.$a=pos$posIndex0.$a-$cellSize.$a*tmp$tmp1.$a;\n")
                builder.append("dir${pp1}.$a=dir$posIndex0.$a;\n")
            }
            // scale isn't changing
            appendIdentitySca(builder, pp1, posIndex0)
        }
        // calculate seed
        val seed = "seed" + nextVariableId.next()
        builder.append("int ").append(seed).append("=threeInputRandom(int(floor(tmp")
            .append(tmp1).append(".x)),int(floor(tmp")
            .append(tmp1).append(".y)),int(floor(tmp")
            .append(tmp1).append(".z)));\n")
        // todo unify this concept
        builder.append("vec3 cellPos=tmp$tmp1*$cellSize;\n")
        seeds.add(seed)
        return pp1
    }

    override fun defineLoopFoot(
        builder: StringBuilder,
        posIndex0: Int,
        innerDstIndex: Int,
        outerDstIndex: Int,
        nextVariableId: VariableCounter,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>,
        seeds: ArrayList<String>
    ) {
        val (func, smoothness, groove, stairs) = appendGroupHeader(functions, uniforms, CombinationMode.UNION, style)
        builder.append("if(first").append(innerDstIndex).append("){first").append(innerDstIndex)
            .append("=false;res$outerDstIndex=res$innerDstIndex;}else{\n")
        appendMerge(builder, outerDstIndex, innerDstIndex, func, smoothness, groove, stairs)
        builder.append("}}}}\n")
        seeds.removeLast()
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as SDFArray2
        dst.dynamic = dynamic
        dst.mirrorX = mirrorX
        dst.mirrorY = mirrorY
        dst.mirrorZ = mirrorZ
        dst.count = count
        dst.cellSize = cellSize
    }

    override val className: String get() = "SDFArray2"

}
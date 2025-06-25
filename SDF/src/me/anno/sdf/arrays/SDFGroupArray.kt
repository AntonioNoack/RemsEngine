package me.anno.sdf.arrays

import me.anno.ecs.components.mesh.material.utils.TypeValue
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.maths.Maths.clamp
import me.anno.maths.Maths.max
import me.anno.sdf.SDFGroup
import me.anno.sdf.VariableCounter
import org.joml.AABBf
import org.joml.Vector3f

// todo implement tri and hex arrays
abstract class SDFGroupArray : SDFGroup() {

    var modulatorIndex = 0
        set(value) {
            val v = max(value, 0)
            if (field != v) {
                field = v
                invalidateShader()
                invalidateShaderBounds()
            }
        }

    var useModulatorMaterials = false
        set(value) {
            if (field != value) {
                field = value
                invalidateShader()
            }
        }
    var useModulatorUVs = false
        set(value) {
            if (field != value) {
                field = value
                invalidateShader()
            }
        }

    // must be > 0 to be useful
    var relativeOverlap = Vector3f(1f)
        set(value) {
            field.set(value)
                .max(0f, 0f, 0f)
        }

    override fun calculateBaseBounds(dst: AABBf) {
        val modulatorIndex = clamp(modulatorIndex, 0, children.lastIndex)
        if (modulatorIndex > 0) {
            // modulators <intersect> other children
            super.calculateBaseBounds(dst, children.subList(modulatorIndex, children.size))
        } else super.calculateBaseBounds(dst)
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is SDFGroupArray) return
        dst.relativeOverlap.set(relativeOverlap)
    }

    abstract fun defineLoopHead(
        builder: StringBuilder,
        posIndex0: Int,
        innerDstIndex: Int,
        outerDstIndex: Int,
        nextVariableId: VariableCounter,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>,
        seeds: ArrayList<String>
    ): Int

    abstract fun defineLoopFoot(
        builder: StringBuilder,
        posIndex0: Int,
        innerDstIndex: Int,
        outerDstIndex: Int,
        nextVariableId: VariableCounter,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>,
        seeds: ArrayList<String>
    )

    abstract fun applyArrayTransform(bounds: AABBf)

    abstract fun calculateHalfCellSize(
        builder: StringBuilder,
        nextVariableId: VariableCounter,
        dstIndex: Int,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>,
        seeds: ArrayList<String>
    )

    override fun buildShader(
        builder: StringBuilder,
        posIndex0: Int,
        nextVariableId: VariableCounter,
        dstIndex: Int,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>,
        seeds: ArrayList<String>
    ) {
        val transform = buildTransform(builder, posIndex0, nextVariableId, uniforms, functions, seeds)
        val posIndex1 = transform.posIndex
        val modulatorIndex = clamp(modulatorIndex, 0, max(children.size - 1, 0))
        val res0 = nextVariableId.next()
        val res1 = if (modulatorIndex > 0) nextVariableId.next() else -1
        builder.append("res").append(dstIndex).append("=vec4(Infinity,-1.0,0.0,0.0);\n")
        builder.append("vec4 res").append(res0).append("; int ctr$res0=0;\n")
        if (modulatorIndex > 0) {
            builder.append("vec4 res").append(res1).append(";\n")
            builder.append("float mcs").append(res1).append(";\n")
            calculateHalfCellSize(builder, nextVariableId, res1, uniforms, functions, seeds)
            buildShader1(
                builder, posIndex1, nextVariableId, res1, uniforms, functions, seeds,
                children.subList(0, modulatorIndex), false
            )
            // use this shortcut, if the query is outside the shape, or cells of the array are too small to be noticeable
            builder.append("if(res").append(res1).append(".x>mcs").append(res1)
                .append(" || mcs$res1*sca$posIndex1<0.003){\n")
            // fast path, where the mesh is skipped
            builder.append("res").append(dstIndex).append("=0.5*res").append(res1).append(";\n")
            builder.append("res").append(dstIndex).append(".x+=0.5*mcs").append(res1).append(";\n")
            builder.append("res").append(dstIndex).append(".x=max(0.01,res").append(dstIndex).append(".x);\n")
            builder.append("} else {\n")
        }
        // start loops
        val posIndexI = defineLoopHead(builder, posIndex1, res0, dstIndex, nextVariableId, uniforms, functions, seeds)
        if (modulatorIndex > 0) {
            // todo remember the winning cell pos -> query it for modulator UVs and modulator materials
            val posIndex2 = nextVariableId.next()
            builder.append("vec3 pos").append(posIndex2).append("=cellPos; ctr$res0++;\n")
            appendIdentityDir(builder, posIndex2, posIndexI)
            appendIdentitySca(builder, posIndex2, posIndexI)
            buildShader1(
                builder, posIndex2, nextVariableId, res1, uniforms, functions, seeds,
                children.subList(0, modulatorIndex), false
            )
            // first check if there actually is an element
            builder.append("if(res").append(res1).append(".x<0.0) {\n")
        }
        // build group contents
        buildShader1(
            builder, posIndexI, nextVariableId, res0, uniforms, functions, seeds,
            children.subList(modulatorIndex, children.size), false
        )
        if (modulatorIndex > 0) {
            // todo why is this soo sensitive, and needs such a small pre-factor?
            // searched: distance until next brick
            builder.append("} else res").append(res0).append("=vec4(0.2*mcs$res1,-1.0,0.0,0.0);\n")
        }
        // finish loops
        defineLoopFoot(builder, posIndex1, res0, dstIndex, nextVariableId, uniforms, functions, seeds)
        if (modulatorIndex > 0) {
            builder.append("}\n")
            if (useModulatorUVs && useModulatorMaterials) {
                builder.append("if(res$res1.y>=0.0) res").append(dstIndex).append(".yzw=res").append(res1)
                    .append(".yzw;\n")
            } else {
                if (useModulatorMaterials)
                    builder.append("if(res$res1.y>=0.0) res").append(dstIndex).append(".y=res").append(res1)
                        .append(".y;\n")
                else if (useModulatorUVs)
                    builder.append("if(res$res1.y>=0.0) res").append(dstIndex).append(".zw=res").append(res1)
                        .append(".zw;\n")
            }
        }
        // builder.append("res$dstIndex.zw = vec2(0.1*float(ctr$res0));\n")
        buildTransformCorrection(builder, transform, dstIndex, uniforms)
        buildDistanceMapperShader(builder, posIndex0, dstIndex, nextVariableId, uniforms, functions, seeds)
    }

}
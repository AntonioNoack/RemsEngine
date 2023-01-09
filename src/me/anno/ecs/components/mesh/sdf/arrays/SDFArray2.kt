package me.anno.ecs.components.mesh.sdf.arrays

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Material
import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.mesh.sdf.VariableCounter
import me.anno.ecs.components.mesh.sdf.arrays.SDFArray.Companion.sdArray
import me.anno.ecs.components.mesh.sdf.random.SDFRandom.Companion.randLib
import me.anno.ecs.components.mesh.sdf.random.SDFRandomRotation
import me.anno.ecs.components.mesh.sdf.random.SDFRandomUV
import me.anno.ecs.components.mesh.sdf.shapes.SDFBox
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.shader.GLSLType
import me.anno.utils.OS.pictures
import org.joml.AABBf
import org.joml.Vector3f
import org.joml.Vector3i

// todo calculate things on cpu side
class SDFArray2 : SDFGroupArray() {

    override fun applyArrayTransform(bounds: AABBf) {
        val rep = cellSize
        val lim = count
        if (rep.x > 0f) {
            bounds.minX = SDFArray.minMod2(bounds.minX, rep.x, lim.x)
            bounds.maxX = SDFArray.maxMod2(bounds.maxX, rep.x, lim.x)
        }
        if (rep.y > 0f) {
            bounds.minY = SDFArray.minMod2(bounds.minY, rep.y, lim.y)
            bounds.maxY = SDFArray.maxMod2(bounds.maxY, rep.y, lim.y)
        }
        if (rep.z > 0f) {
            bounds.minZ = SDFArray.minMod2(bounds.minZ, rep.z, lim.z)
            bounds.maxZ = SDFArray.maxMod2(bounds.maxZ, rep.z, lim.z)
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
        val overlap = defineUniform(uniforms, GLSLType.V3F, overlap)
        val count = defineUniform(uniforms, GLSLType.V3I, count)
        val cellSize = defineUniform(uniforms, GLSLType.V3F, cellSize)
        builder.append("vec3 l=vec3($count-1)*0.5+vec3(lessThanEqual($count,ivec3(0)))*1e38;\n")
        builder.append("vec3 h=vec3(").append(count).append("&1)*0.5;\n")
        builder.append("vec3 min$posIndex0=mod2C(pos$posIndex0-$overlap,$cellSize,l,h);\n")
        builder.append("vec3 max$posIndex0=mod2C(pos$posIndex0+$overlap,$cellSize,l,h);\n")
        builder.append("vec3 pos").append(posIndex0 + 1).append("=pos").append(posIndex0).append(";\n")
        val rnd = nextVariableId.next()
        builder.append("vec3 tmp").append(rnd).append(";\n")
        builder.append("bool first$posIndex0=true;\n")
        for (axis in 0 until 3) {
            val mirror = when (axis) {
                0 -> mirrorX
                1 -> mirrorY
                else -> mirrorZ
            }
            val a = axes[axis]
            builder.append("for(tmp$rnd.$a=min$posIndex0.$a;tmp$rnd.$a<=max$posIndex0.$a;tmp$rnd.$a++){\n")
            builder.append("pos${posIndex0 + 1}.$a=")
            if (mirror) {
                builder.append("(pos$posIndex0.$a-$cellSize.$a*tmp$rnd.$a)*mirror(tmp$rnd.$a);\n")
            } else {
                builder.append("pos$posIndex0.$a-$cellSize.$a*tmp$rnd.$a;\n")
            }
        }
        // calculate seed
        val seed = "seed" + nextVariableId.next()
        builder.append("int ").append(seed).append("=threeInputRandom(int(floor(tmp")
            .append(rnd).append(".x)),int(floor(tmp")
            .append(rnd).append(".y)),int(floor(tmp")
            .append(rnd).append(".z)));\n")
        seeds.add(seed)
        return posIndex0 + 1
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
        val (funcName, smoothness, groove, stairs) =
            appendGroupHeader(functions, uniforms, CombinationMode.UNION, style)
        builder.append("if(first$posIndex0){ first$posIndex0=false; res$outerDstIndex=res$innerDstIndex;\n } else {\n")
        appendMerge(builder, outerDstIndex, innerDstIndex, funcName, smoothness, groove, stairs)
        builder.append("}\n")
        builder.append("}}}\n")
        seeds.removeLast()
    }

    override fun clone(): SDFArray2 {
        val clone = SDFArray2()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SDFArray2
        clone.dynamic = dynamic
        clone.mirrorX = mirrorX
        clone.mirrorY = mirrorY
        clone.mirrorZ = mirrorZ
        clone.count = count
        clone.cellSize = cellSize
    }

    override val className get() = "SDFArray2"

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            // brick wall with randomly crooked bricks :)
            ECSRegistry.init()
            testSceneWithUI(Entity().apply {
                add(SDFArray2().apply {
                    sdfMaterials = listOf(
                        Material().apply {
                            diffuseMap = pictures.getChild("speckle.jpg")
                        }.ref
                    )
                    maxSteps = 500
                    cellSize.set(2f, 1f, 1f)
                    count.set(100, 1, 25)
                    overlap.set(0.1f)
                    addChild(SDFBox().apply {
                        smoothness = 0.03f
                        halfExtends.set(1f, .2f, .5f)
                        addChild(SDFRandomRotation().apply {
                            minAngleDegrees.set(-5f, 0f, -5f)
                            maxAngleDegrees.set(+5f, 0f, +5f)
                            appliedPortion = 0.2f
                            seedXOR = 1234
                        })
                        addChild(SDFRandomUV())
                    })
                })
            })
        }
    }
}
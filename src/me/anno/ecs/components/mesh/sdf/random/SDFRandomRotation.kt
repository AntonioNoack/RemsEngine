package me.anno.ecs.components.mesh.sdf.random

import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.mesh.sdf.SDFComponent.Companion.appendUniform
import me.anno.ecs.components.mesh.sdf.SDFComponent.Companion.appendVec
import me.anno.ecs.components.mesh.sdf.SDFComponent.Companion.globalDynamic
import me.anno.ecs.components.mesh.sdf.VariableCounter
import me.anno.ecs.components.mesh.sdf.arrays.SDFArray
import me.anno.ecs.components.mesh.sdf.shapes.SDFBox
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.ECSRegistry
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.GFXBase
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Floats.piF180d
import me.anno.utils.types.Floats.toRadians
import org.joml.AABBf
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.abs
import kotlin.math.min

class SDFRandomRotation : SDFRandom() {

    var minAngleDegrees = Vector3f(0f, -15f, 0f)
        set(value) {
            if (!dynamic && !globalDynamic) invalidateShader()
            else invalidateBounds()
            field.set(value)
        }
    var maxAngleDegrees = Vector3f(0f, +15f, 0f)
        set(value) {
            if (!dynamic && !globalDynamic) invalidateShader()
            else invalidateBounds()
            field.set(value)
        }

    override fun buildShader(
        builder: StringBuilder,
        posIndex: Int,
        nextVariableId: VariableCounter,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>,
        seed: String
    ): String? {
        val tmp = nextVariableId.next()
        builder.append("vec3 tmp").append(tmp).append("=")
            .append(piF180d).append("*mix(")
        if (dynamic || globalDynamic) {
            builder.appendUniform(uniforms, minAngleDegrees).append(',')
                .appendUniform(uniforms, maxAngleDegrees)
        } else {
            builder.appendVec(minAngleDegrees).append(',')
                .appendVec(maxAngleDegrees)
        }
        builder.append(",nextRandF3(").append(seed).append("));\n")
        builder.append("pos").append(posIndex).append(".xz*=rot(tmp").append(tmp).append(".y);\n")
        builder.append("pos").append(posIndex).append(".yz*=rot(tmp").append(tmp).append(".x);\n")
        builder.append("pos").append(posIndex).append(".xy*=rot(tmp").append(tmp).append(".z);\n")
        return null
    }

    override fun calcTransform(pos: Vector4f, seed: Int) {
        // todo apply transform here (?)
    }

    override fun applyTransform(bounds: AABBf) {

        // rotate bounds :)
        // find extreme angles, and only resize by what is truly needed :)

        // first rotate bounds by avg, then min and max clamped :)
        val m = JomlPools.mat4x3f.borrow()
        m.identity()
            .rotateY((minAngleDegrees.y + maxAngleDegrees.y).toRadians() * 0.5f)
            .rotateX((minAngleDegrees.x + maxAngleDegrees.x).toRadians() * 0.5f)
            .rotateZ((minAngleDegrees.z + maxAngleDegrees.z).toRadians() * 0.5f)

        bounds.transformUnion(m, bounds)

        // now clamped the delta :) ; 90° is the worst case, as it mixes most
        val dx = min(abs(maxAngleDegrees.x - minAngleDegrees.x) * 0.5f, 90f).toRadians()
        val dy = min(abs(maxAngleDegrees.y - minAngleDegrees.y) * 0.5f, 90f).toRadians()
        val dz = min(abs(maxAngleDegrees.z - minAngleDegrees.z) * 0.5f, 90f).toRadians()

        if (dy > 0f) {
            m.identity().rotateY(dy)
            bounds.transformUnion(m, bounds)
        }

        if (dx > 0f) {
            m.identity().rotateX(dx)
            bounds.transformUnion(m, bounds)
        }

        if (dz > 0f) {
            m.identity().rotateZ(dz)
            bounds.transformUnion(m, bounds)
        }

    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        dst as SDFRandomRotation
        dst.minAngleDegrees = minAngleDegrees
        dst.maxAngleDegrees = maxAngleDegrees
    }

    override val className get() = "SDFRandomRotation"

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            ECSRegistry.init()

            val entity = Entity()

            val array = SDFArray()
            array.cellSize.set(2f)
            array.count.set(10, 1, 10)

            val rot = SDFRandomRotation()
            rot.minAngleDegrees.set(-2f, 0f, -2f)
            rot.maxAngleDegrees.set(+2f, 0f, +2f)

            val shape = SDFBox()
            shape.addChild(array)
            shape.addChild(rot)
            entity.addChild(shape)

            GFXBase.disableRenderDoc()
            testSceneWithUI(entity)
        }
    }

}
package me.anno.sdf.random

import me.anno.ecs.components.mesh.material.utils.TypeValue
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.maths.Maths.mix
import me.anno.sdf.SDFComponent.Companion.appendUniform
import me.anno.sdf.SDFComponent.Companion.appendVec
import me.anno.sdf.SDFComponent.Companion.globalDynamic
import me.anno.sdf.VariableCounter
import me.anno.utils.pooling.JomlPools
import me.anno.utils.types.Floats.piF180d
import me.anno.utils.types.Floats.toRadians
import org.joml.AABBf
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.math.abs
import kotlin.math.min

class SDFRandomRotation : SDFRandom() {

    var minAngleDegrees: Vector3f = Vector3f(0f, -15f, 0f)
        set(value) {
            if (!dynamic && !globalDynamic) invalidateShader()
            else invalidateBounds()
            field.set(value)
        }
    var maxAngleDegrees: Vector3f = Vector3f(0f, +15f, 0f)
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
        // are the signs correct???
        builder.append("pos").append(posIndex).append(".xz*=rot(tmp").append(tmp).append(".y);\n")
        builder.append("pos").append(posIndex).append(".zy*=rot(tmp").append(tmp).append(".x);\n")
        builder.append("pos").append(posIndex).append(".yx*=rot(tmp").append(tmp).append(".z);\n")
        return null
    }

    override fun calcTransform(pos: Vector4f, seed: Int) {
        val s1 = nextRandI(seed)
        val s2 = nextRandI(s1)
        pos.rotateY(mix(minAngleDegrees.y, maxAngleDegrees.y, nextRandF(s1)))
        pos.rotateX(mix(minAngleDegrees.x, maxAngleDegrees.x, nextRandF(seed)))
        pos.rotateZ(mix(minAngleDegrees.z, maxAngleDegrees.z, nextRandF(s2)))
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
            m.rotationY(dy)
            bounds.transformUnion(m, bounds)
        }

        if (dx > 0f) {
            m.rotationX(dx)
            bounds.transformUnion(m, bounds)
        }

        if (dz > 0f) {
            m.rotationZ(dz)
            bounds.transformUnion(m, bounds)
        }
    }

    override fun copyInto(dst: PrefabSaveable) {
        super.copyInto(dst)
        if (dst !is SDFRandomRotation) return
        dst.minAngleDegrees = minAngleDegrees
        dst.maxAngleDegrees = maxAngleDegrees
    }
}
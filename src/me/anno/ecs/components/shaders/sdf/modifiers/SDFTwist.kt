package me.anno.ecs.components.shaders.sdf.modifiers

import me.anno.ecs.components.mesh.TypeValue
import me.anno.ecs.components.shaders.sdf.SDFComponent.Companion.defineUniform
import me.anno.ecs.components.shaders.sdf.SDFComponent.Companion.writeVec
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.Ptr
import org.apache.logging.log4j.LogManager
import org.joml.Vector3f
import org.joml.Vector4f

class SDFTwist : PositionMapper() {

    private val sourceParams = Vector4f(0f, 1f, 0f, 0f)

    private fun calcSourceParams() {
        val w = sourceParams.w
        sourceParams.set(source, 0f)
        sourceParams.mul(strength / sourceParams.length())
        sourceParams.w = w
    }

    /**
     * source for rotation
     * 4th component: phase offset
     * */
    var source = Vector3f(0f, 1f, 0f)
        set(value) {
            field.set(value)
            field.normalize()
            calcSourceParams()
        }

    var destination = Vector3f(0f, 1f, 0f)
        set(value) {
            field.set(value)
            field.normalize()
        }

    var strength = 1f
        set(value) {
            if (field != value) {
                field = value
                calcSourceParams()
            }
        }

    var phaseOffset
        get() = sourceParams.w
        set(value) {
            sourceParams.w = value
        }

    /**
     * center of rotation
     * */
    var center = Vector3f()
        set(value) {
            field.set(value)
        }

    var dynamicCenter = false
    var dynamicSource = false
    var dynamicDestination = false

    override fun buildShader(
        builder: StringBuilder,
        posIndex: Int,
        nextVariableId: Ptr<Int>,
        uniforms: HashMap<String, TypeValue>,
        functions: HashSet<String>
    ): String? {
        functions.add(twistFunc)
        val dst = destination
        val dynDst = dynamicDestination
        // optimize for special cases dst=x/y/z
        val axis = when {
            absEqualsOne(dst.x) -> 'X'
            absEqualsOne(dst.y) -> 'Y'
            absEqualsOne(dst.z) -> 'Z'
            else -> '?'
        }
        if (!dynDst && axis != '?') {
            val sign = if (dst.x + dst.y + dst.z > 0f) '+' else '-'
            LOGGER.info("Using optimization $sign$axis")
            builder.append("pos").append(posIndex)
            builder.append("=twist").append(axis)
            builder.append("(pos").append(posIndex)
            builder.append(',')
            writeCenter(builder, uniforms)
            builder.append(',').append(sign)
            writeSource(builder, posIndex, uniforms)
            builder.append(");\n")
            return null
        } else {
            builder.append("pos").append(posIndex)
            builder.append("=twist(pos").append(posIndex)
            builder.append(',')
            writeCenter(builder, uniforms)
            builder.append(',')
            writeSource(builder, posIndex, uniforms)
            builder.append(',')
            if (dynDst) builder.append(defineUniform(uniforms, dst))
            else writeVec(builder, dst)
            builder.append(");\n")
            return null
        }
    }

    private fun writeCenter(builder: StringBuilder, uniforms: HashMap<String, TypeValue>) {
        if (dynamicCenter) builder.append(defineUniform(uniforms, center))
        else writeVec(builder, center)
    }

    private fun writeSource(builder: StringBuilder, posIndex: Int, uniforms: HashMap<String, TypeValue>) {
        // optimize angle for special cases like source=x/y/z? not really needed
        val src = sourceParams
        builder.append("dot(vec4(pos").append(posIndex)
        builder.append(",1.0),")
        if (dynamicSource) builder.append(defineUniform(uniforms, src))
        else writeVec(builder, src)
        builder.append(")")
    }

    override fun calcTransform(pos: Vector4f) {
        val destination = destination
        val angle = sourceParams.dot(pos.x, pos.y, pos.z, 1f)
        pos.rotateAbout(angle, destination.x, destination.y, destination.z)
    }

    override fun clone(): SDFTwist {
        val clone = SDFTwist()
        copy(clone)
        return clone
    }

    override fun copy(clone: PrefabSaveable) {
        super.copy(clone)
        clone as SDFTwist
    }

    override val className: String = "SDFTwist"

    companion object {

        private val LOGGER = LogManager.getLogger(SDFTwist::class)

        private fun absEqualsOne(r: Float): Boolean {
            return r.toRawBits().and(0x7fffffff) == 0x3f800000
        }

        const val twistFunc = "" +
                // to do check if the order matches: looks like it does :)
                "vec3 twistX(vec3 p, vec3 o, float a){\n" +
                "   float c = cos(a);\n" +
                "   float s = sin(a);\n" +
                "   p.yz = mat2(c,s,-s,c) * (p.yz - o.yz) + o.yz;\n" +
                "   return p;\n" +
                "}\n" +
                "vec3 twistY(vec3 p, vec3 o, float a){\n" +
                "   float c = cos(a);\n" +
                "   float s = sin(a);\n" +
                "   p.xz = mat2(c,-s,s,c) * (p.xz - o.xz) + o.xz;\n" +
                "   return p;\n" +
                "}\n" +
                "vec3 twistZ(vec3 p, vec3 o, float a){\n" +
                "   float c = cos(a);\n" +
                "   float s = sin(a);\n" +
                "   p.xy = mat2(c,s,-s,c) * (p.xy - o.xy) + o.xy;\n" +
                "   return p;\n" +
                "}\n" +
                // twist p around the axis dst by dot(p,src.xyz)+src.w
                "vec3 twist(vec3 p, vec3 o, float angle, vec3 axis){\n" +
                "   float ha = angle * 0.5;\n" + // half angle
                "   float sinAngle = sin(ha);\n" +
                "   vec4 q = vec4(axis * sinAngle, cos(ha));\n" +
                "   vec4 q2 = q * q;\n" +
                "   float zw = q.z * q.w;\n" +
                "   float xy = q.x * q.y;\n" +
                "   float xz = q.x * q.z;\n" +
                "   float yw = q.y * q.w;\n" +
                "   float yz = q.y * q.z;\n" +
                "   float xw = q.x * q.w;\n" +
                "   p -= o;\n" +
                "   return o + vec3(\n" +
                "     (q2.w + q2.x - q2.z - q2.y) * p.x + (xy - zw - zw + xy)         * p.y + (yw + xz + xz + yw)         * p.z,\n" +
                "     (xy + zw + zw + xy)         * p.x + (q2.y - q2.z + q2.w - q2.x) * p.y + (yz + yz - xw - xw)         * p.z,\n" +
                "     (xz - yw + xz - yw)         * p.x + (yz + yz + xw + xw)         * p.y + (q2.z - q2.y - q2.x + q2.w) * p.z\n" +
                "   );\n" +
                "}\n"
    }

}
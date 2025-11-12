package cz.advel.stack

import com.bulletphysics.collision.narrowphase.CastResult
import com.bulletphysics.collision.narrowphase.GjkConvexCast
import com.bulletphysics.collision.narrowphase.PointCollector
import com.bulletphysics.collision.narrowphase.VoronoiSimplexSolver
import com.bulletphysics.collision.shapes.ConvexShape
import com.bulletphysics.linearmath.Transform
import org.joml.AABBd
import org.joml.Matrix3f
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f
import java.nio.BufferUnderflowException

/**
 * this class is now fully thread safe :)
 * it is used to quickly borrow instances from specific classes used in bullet (javax.vecmath)
 */
class Stack {
    private var vectorPosition = 0
    private var vectorPositionF = 0
    private var matrixPosition = 0
    private var quatPosition = 0
    private var transPosition = 0

    private var vectors = Array(32) { Vector3d() }
    private var vectorsF = Array(32) { Vector3f() }
    private var matrices = Array(32) { Matrix3f() }
    private var quads = Array(32) { Quaternionf() }
    private var transforms = Array(32) { Transform() }

    // I either didn't find the library, or it was too large for my liking:
    // I rewrote the main functionalities
    private fun resetInstance(printSlack: Boolean) {
        if (printSlack) {
            println(
                "[BulletStack]: Slack: " +
                        vectorPosition + " vectors, " +
                        vectorPositionF + " vectors, " +
                        matrixPosition + " matrices, " +
                        quatPosition + " quaternions, " +
                        transPosition + " transforms"
            )
        }
        vectorPosition = 0
        vectorPositionF = 0
        matrixPosition = 0
        quatPosition = 0
        transPosition = 0
    }

    fun reset2(vec: Int, vec2: Int, mat: Int, quat: Int, trans: Int) {
        vectorPosition = vec
        vectorPositionF = vec2
        matrixPosition = mat
        quatPosition = quat
        transPosition = trans
    }

    var depth: Int = 0

    fun printSizes2() {
        println(
            "[BulletStack]: " +
                    vectors.size + " vectors, " +
                    vectorsF.size + " vectors, " +
                    matrices.size + " matrices, " +
                    quads.size + " quads, " +
                    transforms.size + " transforms"
        )
    }

    private fun checkLeaking(newSize: Int) {
        if (newSize > limit) throw OutOfMemoryError("Reached stack limit $limit, probably leaking")
    }

    fun newVec3d(): Vector3d {
        var values = vectors
        if (vectorPosition >= values.size) {
            val newSize = values.size * 2
            checkLeaking(newSize)
            values = Array(newSize) { values.getOrNull(it) ?: Vector3d() }
            vectors = values
        }
        return values[vectorPosition++]
    }

    fun newVec3f(): Vector3f {
        var values = vectorsF
        if (vectorPositionF >= values.size) {
            val newSize = values.size * 2
            checkLeaking(newSize)
            values = Array(newSize) { values.getOrNull(it) ?: Vector3f() }
            vectorsF = values
        }
        return values[vectorPositionF++]
    }

    fun newQuatF(): Quaternionf {
        var values = quads
        if (quatPosition >= values.size) {
            val newSize = values.size * 2
            checkLeaking(newSize)
            values = Array(newSize) { values.getOrNull(it) ?: Quaternionf() }
            quads = values
        }
        return values[quatPosition++]
    }

    fun newMat2(): Matrix3f {
        var values = matrices
        if (matrixPosition >= values.size) {
            val newSize = values.size * 2
            checkLeaking(newSize)
            values = Array(newSize) { values.getOrNull(it) ?: Matrix3f() }
            matrices = values
        }
        return values[matrixPosition++]
    }

    fun newTrans2(): Transform {
        var values = transforms
        if (transPosition >= values.size) {
            val newSize = values.size * 2
            checkLeaking(newSize)
            values = Array(newSize) { values.getOrNull(it) ?: Transform() }
            transforms = values
        }
        return values[transPosition++]
    }

    companion object {

        private val FLOAT_PTRS = GenericStack({ FloatArray(1) }, "float*")
        private val ARRAY_LISTS = GenericStack({ ArrayList<Any?>(16) }, "ObjectArrayList")
        private val AABBs = GenericStack({ AABBd() }, "AABBd")
        private val VSSs = GenericStack({ VoronoiSimplexSolver() }, "VoronoiSimplexSolver")
        private val CAST_RESULTS = GenericStack({ CastResult() }, "CastResult")
        private val POINT_COLLECTORS = GenericStack({ PointCollector() }, "PointCollector")
        private val GJK_CONVEX_CAST = GenericStack({ GjkConvexCast() }, "GjkConvexCast")

        var limit: Int = 65536

        private val instances = ThreadLocal.withInitial { Stack() }

        @JvmStatic
        fun reset(printSlack: Boolean) {
            instances.get().resetInstance(printSlack)
            for (stack in GenericStack.STACKS) {
                stack.reset(0)
            }
        }

        fun reset(vec: Int, vec2: Int, mat: Int, quat: Int, trans: Int) {
            instances.get().reset2(vec, vec2, mat, quat, trans)
        }

        fun getPosition(dst: IntArray?): IntArray {
            if (dst == null) return getPosition(IntArray(4))
            val instance = instances.get()
            dst[0] = instance.vectorPosition
            dst[1] = instance.matrixPosition
            dst[2] = instance.quatPosition
            dst[3] = instance.transPosition
            // System.out.println("Getting state [" + instance.depth + "] at " + Arrays.toString(dst));
            instance.depth++
            return dst
        }

        @Suppress("unused")
        fun checkSlack(pos: IntArray, name: String = "") {
            val instance = instances.get()
            val dv = instance.vectorPosition - pos[0]
            val dm = instance.matrixPosition - pos[1]
            val dq = instance.quatPosition - pos[2]
            val dt = instance.transPosition - pos[3]
            if (dv != 0 || dm != 0 || dq != 0 || dt != 0) {
                throw IllegalStateException("Slack: $dv vec + $dm mat + $dq quat + $dt trans, '$name'")
            }
        }

        fun reset(positions: IntArray) {
            val instance = instances.get()
            instance.vectorPosition = positions[0]
            instance.matrixPosition = positions[1]
            instance.quatPosition = positions[2]
            instance.transPosition = positions[3]
            instance.depth--
        }

        private fun checkUnderflow(position: Int) {
            if (position < 0) throw BufferUnderflowException()
        }

        @JvmStatic
        fun subVec3d(delta: Int) {
            val stack = instances.get()
            stack.vectorPosition -= delta
            printCaller("subVec(?)", 2, stack.vectorPosition)
            checkUnderflow(stack.vectorPosition)
        }

        @JvmStatic
        fun subVec3f(delta: Int) {
            val stack = instances.get()
            stack.vectorPositionF -= delta
            printCaller("subVecF(?)", 2, stack.vectorPositionF)
            checkUnderflow(stack.vectorPositionF)
        }

        fun subMat(delta: Int) {
            val stack = instances.get()
            stack.matrixPosition -= delta
            checkUnderflow(stack.matrixPosition)
        }

        fun subQuat(delta: Int) {
            val stack = instances.get()
            stack.quatPosition -= delta
            checkUnderflow(stack.quatPosition)
        }

        fun subTrans(delta: Int) {
            val stack = instances.get()
            stack.transPosition -= delta
            checkUnderflow(stack.transPosition)
        }

        fun printSizes() {
            instances.get().printSizes2()
        }

        var shallPrintCallers: Boolean = false

        private fun printCaller(type: String?, depth: Int, pos: Int) {
            if (!shallPrintCallers) return

            val elements = Throwable().stackTrace
            if (elements == null || depth >= elements.size) return

            val builder = StringBuilder()
            repeat(elements.size) {
                builder.append(" ")
            }
            builder.append(type).append(" on ").append(elements[depth])
            builder.append(" (").append(pos).append(")")
            println(builder)
        }

        fun newVec3d(): Vector3d {
            val stack = instances.get()
            printCaller("newVec()", 2, stack.vectorPosition)
            return stack.newVec3d().set(0.0, 0.0, 0.0)
        }

        fun newVec3f(): Vector3f {
            val stack = instances.get()
            printCaller("newVec()", 2, stack.vectorPosition)
            return stack.newVec3f().set(0f, 0f, 0f)
        }

        @JvmStatic
        fun newVec3f(src: Vector3f): Vector3f {
            val stack = instances.get()
            printCaller("newVec()", 2, stack.vectorPosition)
            return stack.newVec3f().set(src)
        }

        @JvmStatic
        fun newVec3d(xyz: Double): Vector3d {
            val stack = instances.get()
            printCaller("newVec(d)", 2, stack.vectorPosition)
            return stack.newVec3d().set(xyz, xyz, xyz)
        }

        @JvmStatic
        fun newVec3d(x: Double, y: Double, z: Double): Vector3d {
            val stack = instances.get()
            printCaller("newVec(xyz)", 2, stack.vectorPosition)
            return stack.newVec3d().set(x, y, z)
        }

        fun newVec3d(src: Vector3d): Vector3d {
            val stack = instances.get()
            printCaller("newVec(src)", 2, stack.vectorPosition)
            return stack.newVec3d().set(src)
        }

        fun newQuat(): Quaternionf {
            val v = instances.get().newQuatF()
            v.identity()
            return v
        }

        fun newMat(base: Matrix3f): Matrix3f {
            val v = instances.get().newMat2()
            v.set(base)
            return v
        }

        fun newMat(): Matrix3f {
            return instances.get().newMat2()
        }

        fun newTrans(): Transform {
            return instances.get().newTrans2()
        }

        fun newTrans(base: Transform): Transform {
            val v: Transform = instances.get().newTrans2()
            v.set(base)
            return v
        }

        fun newFloatPtr(): FloatArray {
            return FLOAT_PTRS.create()
        }

        fun subFloatPtr(delta: Int) {
            FLOAT_PTRS.release(delta)
        }

        @JvmStatic
        fun borrowVec3d(): Vector3d {
            val stack = instances.get()
            printCaller("borrowVec3d()", 2, stack.vectorPosition)
            val v = stack.newVec3d()
            stack.vectorPosition--
            return v
        }

        @JvmStatic
        fun borrowVec3f(): Vector3f {
            val stack = instances.get()
            printCaller("borrowVec3f()", 2, stack.vectorPositionF)
            val v = stack.newVec3f()
            stack.vectorPositionF--
            return v
        }

        @JvmStatic
        fun borrowVec3f(src: Vector3f): Vector3f {
            val stack = instances.get()
            printCaller("borrowVec3f()", 2, stack.vectorPositionF)
            val v = stack.newVec3f()
            stack.vectorPositionF--
            return v.set(src)
        }

        fun borrowVec3d(src: Vector3d): Vector3d {
            val stack = instances.get()
            printCaller("borrowVec3d(src)", 2, stack.vectorPosition)
            val v = stack.newVec3d()
            stack.vectorPosition--
            v.set(src)
            return v
        }

        /**
         * used in Rem's Engine for converting types
         */
        @Suppress("unused")
        fun borrowQuat(): Quaternionf {
            val stack = instances.get()
            val v = stack.newQuatF()
            stack.quatPosition--
            return v
        }

        /**
         * used in Rem's Engine for converting types
         */
        @Suppress("unused")
        fun borrowTrans(): Transform {
            val stack = instances.get()
            val t = stack.newTrans2()
            stack.transPosition--
            return t
        }

        fun libraryCleanCurrentThread() {
        }

        fun <V> newList(): ArrayList<V> {
            @Suppress("UNCHECKED_CAST")
            val instance = ARRAY_LISTS.create() as ArrayList<V>
            instance.clear()
            return instance
        }

        fun subList(delta: Int) {
            ARRAY_LISTS.release(delta)
        }

        fun newAabb(): AABBd {
            return AABBs.create()
        }

        fun subAabb(delta: Int) {
            AABBs.release(delta)
        }

        fun newVSS(): VoronoiSimplexSolver {
            return VSSs.create()
        }

        fun subVSS(delta: Int) {
            VSSs.release(delta)
        }

        fun newCastResult(): CastResult {
            val cr: CastResult = CAST_RESULTS.create()
            cr.init()
            return cr
        }

        fun subCastResult(delta: Int) {
            CAST_RESULTS.release(delta)
        }

        fun newPointCollector(): PointCollector {
            val pointCollector: PointCollector = POINT_COLLECTORS.create()
            pointCollector.init()
            return pointCollector
        }

        fun subPointCollector(delta: Int) {
            POINT_COLLECTORS.release(delta)
        }

        fun newGjkCC(convexA: ConvexShape, convexB: ConvexShape): GjkConvexCast {
            val convexCast = GJK_CONVEX_CAST.create()
            convexCast.init(convexA, convexB)
            return convexCast
        }

        fun subGjkCC(delta: Int) {
            GJK_CONVEX_CAST.release(delta)
        }
    }
}

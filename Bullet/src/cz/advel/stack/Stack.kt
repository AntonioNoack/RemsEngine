package cz.advel.stack

import com.bulletphysics.collision.broadphase.DbvtAabbMm
import com.bulletphysics.collision.narrowphase.CastResult
import com.bulletphysics.collision.narrowphase.GjkConvexCast
import com.bulletphysics.collision.narrowphase.PointCollector
import com.bulletphysics.collision.narrowphase.VoronoiSimplexSolver
import com.bulletphysics.collision.shapes.ConvexShape
import com.bulletphysics.linearmath.Transform
import org.joml.Quaterniond
import org.joml.Matrix3d
import org.joml.Vector3d
import java.nio.BufferUnderflowException

/**
 * this class is now fully thread safe :)
 * it is used to quickly borrow instances from specific classes used in bullet (javax.vecmath)
 */
class Stack {
    private var vectorPosition = 0
    private var matrixPosition = 0
    private var quatPosition = 0
    private var transPosition = 0

    private var vectors = Array(32) { Vector3d() }
    private var matrices = Array(32) { Matrix3d() }
    private var quads = Array(32) { Quaterniond() }
    private var transforms = Array(32) { Transform() }

    // I either didn't find the library, or it was too large for my liking:
    // I rewrote the main functionalities
    private fun resetInstance(printSlack: Boolean) {
        if (printSlack) {
            println(
                "[BulletStack]: Slack: " +
                        vectorPosition + " vectors, " +
                        matrixPosition + " matrices, " +
                        quatPosition + " quaternions, " +
                        transPosition + " transforms"
            )
        }
        vectorPosition = 0
        matrixPosition = 0
        quatPosition = 0
        transPosition = 0
    }

    fun reset2(vec: Int, mat: Int, quat: Int, trans: Int) {
        vectorPosition = vec
        matrixPosition = mat
        quatPosition = quat
        transPosition = trans
    }

    var depth: Int = 0

    fun printSizes2() {
        println(
            "[BulletStack]: " +
                    vectors.size + " vectors, " +
                    matrices.size + " matrices, " +
                    quads.size + " quads, " +
                    transforms.size + " transforms"
        )
    }

    private fun checkLeaking(newSize: Int) {
        if (newSize > limit) throw OutOfMemoryError("Reached stack limit $limit, probably leaking")
    }

    fun newVec2(): Vector3d {
        var values = vectors
        if (vectorPosition >= values.size) {
            val newSize = values.size * 2
            checkLeaking(newSize)
            values = Array(newSize) { values.getOrNull(it) ?: Vector3d() }
            vectors = values
        }
        return values[vectorPosition++]
    }

    fun newQuat2(): Quaterniond {
        var values = quads
        if (quatPosition >= values.size) {
            val newSize = values.size * 2
            checkLeaking(newSize)
            values = Array(newSize) { values.getOrNull(it) ?: Quaterniond() }
            quads = values
        }
        return values[quatPosition++]
    }

    fun newMat2(): Matrix3d {
        var values = matrices
        if (matrixPosition >= values.size) {
            val newSize = values.size * 2
            checkLeaking(newSize)
            values = Array(newSize) { values.getOrNull(it) ?: Matrix3d() }
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

        private val DOUBLE_PTRS = GenericStack<DoubleArray>({ DoubleArray(1) }, "double*")
        private val ARRAY_LISTS = GenericStack<ArrayList<*>>({ ArrayList<Any?>(16) }, "ObjectArrayList")
        private val AABB_MMs = GenericStack<DbvtAabbMm>({ DbvtAabbMm() }, "DbvtAabbMm")
        private val VSSs = GenericStack<VoronoiSimplexSolver>({ VoronoiSimplexSolver() }, "VoronoiSimplexSolver")
        private val CAST_RESULTS = GenericStack<CastResult>({ CastResult() }, "CastResult")
        private val POINT_COLLECTORS = GenericStack<PointCollector>({ PointCollector() }, "PointCollector")
        private val GJK_CONVEX_CAST = GenericStack<GjkConvexCast>({ GjkConvexCast() }, "GjkConvexCast")

        var limit: Int = 65536

        private val instances = ThreadLocal.withInitial { Stack() }

        @JvmStatic
        fun reset(printSlack: Boolean) {
            instances.get().resetInstance(printSlack)
            for (stack in GenericStack.STACKS) {
                stack.reset(0)
            }
        }

        fun reset(vec: Int, mat: Int, quat: Int, trans: Int) {
            instances.get().reset2(vec, mat, quat, trans)
        }

        fun getPosition(dst: IntArray?): IntArray {
            if (dst == null) return getPosition(IntArray(4))
            val instance: Stack = instances.get()
            dst[0] = instance.vectorPosition
            dst[1] = instance.matrixPosition
            dst[2] = instance.quatPosition
            dst[3] = instance.transPosition
            // System.out.println("Getting state [" + instance.depth + "] at " + Arrays.toString(dst));
            instance.depth++
            return dst
        }

        fun reset(positions: IntArray) {
            val instance: Stack = instances.get()
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
        fun subVec(delta: Int) {
            val stack: Stack = instances.get()
            stack.vectorPosition -= delta
            printCaller("subVec(?)", 2, stack.vectorPosition)
            checkUnderflow(stack.vectorPosition)
        }

        fun subMat(delta: Int) {
            val stack: Stack = instances.get()
            stack.matrixPosition -= delta
            checkUnderflow(stack.matrixPosition)
        }

        fun subQuat(delta: Int) {
            val stack: Stack = instances.get()
            stack.quatPosition -= delta
            checkUnderflow(stack.quatPosition)
        }

        fun subTrans(delta: Int) {
            val stack: Stack = instances.get()
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
            for (i in elements.indices) {
                builder.append(" ")
            }
            builder.append(type).append(" on ").append(elements[depth])
            builder.append(" (").append(pos).append(")")
            println(builder)
        }

        @JvmStatic
        fun newVec(): Vector3d {
            val stack: Stack = instances.get()
            printCaller("newVec()", 2, stack.vectorPosition)
            val v = stack.newVec2()
            v.set(0.0, 0.0, 0.0)
            return v
        }

        @JvmStatic
        fun newVec(xyz: Double): Vector3d {
            val stack: Stack = instances.get()
            printCaller("newVec(d)", 2, stack.vectorPosition)
            val v = stack.newVec2()
            v.set(xyz, xyz, xyz)
            return v
        }

        @JvmStatic
        fun newVec(x: Double, y: Double, z: Double): Vector3d {
            val stack: Stack = instances.get()
            printCaller("newVec(xyz)", 2, stack.vectorPosition)
            val v = stack.newVec2()
            v.set(x, y, z)
            return v
        }

        fun newVec(src: Vector3d): Vector3d {
            val stack: Stack = instances.get()
            printCaller("newVec(src)", 2, stack.vectorPosition)
            val value = stack.newVec2()
            value.set(src)
            return value
        }

        fun newQuat(): Quaterniond {
            val v: Quaterniond = instances.get().newQuat2()
            v.set(0.0, 0.0, 0.0, 1.0)
            return v
        }

        fun newQuat(base: Quaterniond): Quaterniond {
            val v: Quaterniond = instances.get().newQuat2()
            v.set(base)
            return v
        }

        fun newMat(base: Matrix3d): Matrix3d {
            val v: Matrix3d = instances.get().newMat2()
            v.set(base)
            return v
        }

        fun newMat(): Matrix3d {
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

        fun newDoublePtr(): DoubleArray {
            return DOUBLE_PTRS.create()
        }

        fun subDoublePtr(delta: Int) {
            DOUBLE_PTRS.release(delta)
        }

        @JvmStatic
        fun borrowVec(): Vector3d {
            val stack: Stack = instances.get()
            printCaller("borrowVec()", 2, stack.vectorPosition)
            val v = stack.newVec2()
            stack.vectorPosition--
            return v
        }

        fun borrowVec(src: Vector3d): Vector3d {
            val stack: Stack = instances.get()
            printCaller("borrowVec(src)", 2, stack.vectorPosition)
            val v = stack.newVec2()
            stack.vectorPosition--
            v.set(src)
            return v
        }

        /**
         * used in Rem's Engine for converting types
         */
        @Suppress("unused")
        fun borrowQuat(): Quaterniond {
            val stack: Stack = instances.get()
            val v = stack.newQuat2()
            stack.quatPosition--
            return v
        }

        /**
         * used in Rem's Engine for converting types
         */
        @Suppress("unused")
        fun borrowTrans(): Transform {
            val stack: Stack = instances.get()
            val t = stack.newTrans2()
            stack.transPosition--
            return t
        }

        fun borrowMat(set: Matrix3d): Matrix3d {
            val stack: Stack = instances.get()
            val m = stack.newMat2()
            m.set(set)
            stack.matrixPosition--
            return m
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

        fun newDbvtAabbMm(): DbvtAabbMm {
            return AABB_MMs.create()
        }

        fun subDbvtAabbMm(delta: Int) {
            AABB_MMs.release(delta)
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
            val cr: PointCollector = POINT_COLLECTORS.create()
            cr.init()
            return cr
        }

        fun subPointCollector(delta: Int) {
            POINT_COLLECTORS.release(delta)
        }

        fun newGjkCC(convexA: ConvexShape?, convexB: ConvexShape?): GjkConvexCast {
            val cc: GjkConvexCast = GJK_CONVEX_CAST.create()
            cc.init(convexA, convexB)
            return cc
        }

        fun subGjkCC(delta: Int) {
            GJK_CONVEX_CAST.release(delta)
        }
    }
}

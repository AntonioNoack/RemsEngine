package com.bulletphysics.softbody

import org.joml.Matrix3f
import org.joml.Matrix4x3f
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.math.exp
import kotlin.math.ln

object CellPose {

    private fun estimateTransform(
        currPositions: List<Vector3f>,
        restPositions: List<Vector3f>,

        prevPose: Matrix4x3f,

        volumeConst: Float,
        iterations: Int,// = 10,
        step: Float,// = 0.5f
        dst: Matrix4x3f
    ): Matrix4x3f {
        require(currPositions.size == restPositions.size)
        require(volumeConst > 0f)

        val t = prevPose.getTranslation(Vector3f())
        val q = prevPose.getUnnormalizedRotation(Quaternionf()).normalize()
        val s = prevPose.getScale(Vector3f())

        // log-scales
        var lnScaleX = ln(s.x)
        var lnScaleY = ln(s.y)
        var lnScaleZ = ln(s.z)

        // enforce volume constraint in log-space
        val lnVolume = ln(volumeConst)
        fun enforceConstraint() {
            lnScaleZ = lnVolume - lnScaleX - lnScaleY
        }

        val Rp = Vector3f()
        val R = Matrix3f()
        val gradT = Vector3f()
        val gradRot = Vector3f()
        val predicted = Vector3f()

        val invN = 1f / currPositions.size
        repeat(iterations) {

            R.set(q)
            gradT.set(0f)
            gradRot.set(0f)

            var gradSx = 0f
            var gradSy = 0f

            val scaleX = exp(lnScaleX)
            val scaleY = exp(lnScaleY)
            val scaleZ = exp(lnScaleZ)

            var totalErr = 0f
            for (i in currPositions.indices) {
                val p = currPositions[i]
                val qTarget = restPositions[i]

                // current transformed point
                val scaled = Vector3f(p).mul(scaleX, scaleY, scaleZ)
                R.transform(scaled, predicted)
                predicted.add(t)

                val r = Vector3f(qTarget).sub(predicted)
                totalErr += r.lengthSquared()

                // accumulate simple approximate gradients
                gradT.add(r)
                // rotation gradient ~ cross(R*p, r)
                R.transform(scaled, Rp)
                gradRot.add(Rp.cross(r, Vector3f()))
                // scale gradients (in log space)
                gradSx += r.dot(R.getColumn(0, Vector3f()).mul(p.x * exp(lnScaleX)))
                gradSy += r.dot(R.getColumn(1, Vector3f()).mul(p.y * exp(lnScaleY)))
            }

            gradT.mul(step * invN)
            gradRot.mul(step * invN)
            gradSx *= step * invN
            gradSy *= step * invN

            // update
            t.add(gradT)

            val dQ = Quaternionf().fromAxisAngleRad(Vector3f(gradRot).normalize(), gradRot.length())
            q.mul(dQ).normalize()

            lnScaleX += gradSx
            lnScaleY += gradSy
            enforceConstraint() // maintain constant volume

            if (totalErr * invN < 1e-6f) return@repeat // converged early
        }

        return dst.translationRotateScale(
            t.x, t.y, t.z,
            q.x, q.y, q.z, q.w,
            exp(lnScaleX), exp(lnScaleY), exp(lnScaleZ)
        )
    }


    fun updateCellTransforms(softMesh: SoftBody) {
        val s = softMesh.cellStructure
        val nc = s.numCells

        val currPositions = List(s.numVerticesPerCell) { Vector3f() }
        val restPositions = List(s.numVerticesPerCell) { Vector3f() }

        val cellTransform = Matrix4x3f()
        for (cz in 0 until nc.z) {
            for (cy in 0 until nc.y) {
                for (cx in 0 until nc.x) {

                    // load previous transform
                    val cellIndex = softMesh.cellIndex(cx, cy, cz)
                    val transformOffset = cellIndex * 12
                    cellTransform.set(softMesh.cellTransforms, transformOffset)

                    // update currPositions
                    for (i in 0 until s.numVerticesPerCell) {
                        val idx = s.getVertex(cx, cy, cz, i)
                        currPositions[i].set(softMesh.positions, idx * 3)
                    }

                    // update rest positions
                    for (i in 0 until s.numVerticesPerCell) {
                        s.getRestPose(cx, cy, cz, i, restPositions[i])
                    }

                    estimateTransform(
                        currPositions,
                        restPositions,
                        cellTransform,
                        s.getRestVolume(cx, cy, cz),
                        5, 0.5f,
                        cellTransform
                    )

                    // store cell transform
                    cellTransform.get(softMesh.cellTransforms, transformOffset)
                }
            }
        }
    }


}
package me.anno.ecs.components.mesh.terrain

import me.anno.utils.types.Vectors.print
import org.joml.Matrix2f
import org.joml.Vector2f
import org.joml.Vector3f
import kotlin.math.sqrt

abstract class TerrainInit {

    abstract fun ensure(position: Vector3f, radius: Float, terrain: TriTerrain)

    companion object {

        val k0 = sqrt(3f) * 0.5f
        val k1 = sqrt(3f) / 3f
        const val k2 = 1.1547005f

        @JvmStatic
        fun main(args: Array<String>) {

            val bx = 1f
            val by = sqrt(3f) * 0.5f
            val bxy = 0.5f
            val bxy2 = -0.57735026f
            val by22 = 1.1547005

            fun forwardX(x: Float, y: Float) = bx * x + bxy * y
            fun forwardY(x: Float, y: Float) = by * y

            // correct?
            fun backwardX(x: Float, y: Float) = bx * x + bxy2 * y
            fun backwardY(x: Float, y: Float) = by22 * y

            val x = 1f
            val y = 1f
            val fx = forwardX(x, y)
            val fy = forwardY(x, y)
            val bx2 = backwardX(fx, fy)
            val by2 = backwardY(fx, fy)

            val m = Matrix2f()
            m.m10(bxy)
            m.m11(by)

            println(m)

            println(m.transform(Vector2f(x, y)).print())

            m.invert()

            println("$x $y -> $fx $fy -> $bx2 $by2")

            println(m)
            println(m.m10)
            println(m.m11)

        }
    }

}
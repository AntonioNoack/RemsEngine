package me.anno.mesh

import me.anno.ecs.components.collider.Collider
import me.anno.ecs.components.mesh.Mesh
import me.anno.io.ISaveable
import me.anno.mesh.assimp.AnimGameItem
import me.anno.utils.types.AABBs.avgX
import me.anno.utils.types.AABBs.avgY
import me.anno.utils.types.AABBs.deltaX
import me.anno.utils.types.AABBs.deltaY
import me.anno.utils.types.AABBs.isEmpty
import me.anno.utils.types.AABBs.set
import me.anno.utils.types.AABBs.transformProject
import org.joml.*
import kotlin.math.abs
import kotlin.math.max

object MeshUtils {

    // todo make target frame usage dependent on size? probably better: ensure we have a ~ 3px padding

    fun centerMesh(stack: Matrix4f, localStack: Matrix4x3f, mesh: Mesh, targetFrameUsage: Float = 0.95f) {
        mesh.ensureBuffer()
        centerMesh(stack, localStack, AABBd().set(mesh.aabb), { mesh.getBounds(it, false) }, targetFrameUsage)
    }

    fun centerMesh(stack: Matrix4f, localStack: Matrix4x3f, collider: Collider, targetFrameUsage: Float = 0.95f) {
        val aabb = AABBd()
        collider.fillSpace(Matrix4x3d(), aabb)
        centerMesh(stack, localStack, aabb, { transform ->
            val aabb2 = AABBd()
            collider.fillSpace(Matrix4x3d(), aabb2)
            AABBf().set(aabb2).transformProject(transform)
        }, targetFrameUsage)
    }

    fun centerMesh(stack: Matrix4f, localStack: Matrix4x3f, model0: AnimGameItem, targetFrameUsage: Float = 0.95f) {
        centerMesh(stack, localStack, model0.staticAABB.value, { model0.getBounds(it) }, targetFrameUsage)
    }

    fun centerMesh(
        transform: ISaveable?,
        stack: Matrix4f,
        localStack: Matrix4x3f,
        model0: AnimGameItem,
        targetFrameUsage: Float = 0.95f
    ) {
        val staticAABB = model0.staticAABB.value
        if (!staticAABB.isEmpty()) {
            if (transform == null) {
                centerMesh(stack, localStack, staticAABB, { model0.getBounds(it) }, targetFrameUsage)
            } else {
                AnimGameItem.centerStackFromAABB(localStack, staticAABB)
            }
        }
    }

    fun centerMesh(
        stack: Matrix4f,
        localStack: Matrix4x3f,
        aabb0: AABBd,
        getBounds: (Matrix4f) -> AABBf,
        targetFrameUsage: Float = 0.95f
    ) {

        if (aabb0.isEmpty()) return

        // rough approximation using bounding box
        AnimGameItem.centerStackFromAABB(localStack, aabb0)

        // todo whenever possible, this optimization should be on another thread
        // (because for high-poly meshes, it is expensive)

        // Newton iterations to improve the result
        val matrix = Matrix4f()
        fun test(dx: Float, dy: Float): AABBf {
            matrix
                .set(stack)
                .translateLocal(dx, dy, 0f)
                .mul(localStack)
            return getBounds(matrix)
        }

        for (i in 1..5) {

            val m0 = test(0f, 0f)

            val scale = 2f * targetFrameUsage / max(m0.deltaX(), m0.deltaY())
            if (scale !in 0.1f..10f) break // scale is too wrong... mmh...
            val scaleIsGoodEnough = scale in 0.9999f..1.0001f

            val x0 = m0.avgX()
            val y0 = m0.avgY()

            // good enough for pixels
            // exit early to save two evaluations
            if (scaleIsGoodEnough && abs(x0) + abs(y0) < 1e-4f) break

            val epsilon = 0.1f
            val mx = test(epsilon, 0f)
            val my = test(0f, epsilon)

            /*LOGGER.info("--- Iteration $i ---")
            LOGGER.info("m0: ${m0.print()}")
            LOGGER.info("mx: ${mx.print()}")
            LOGGER.info("my: ${my.print()}")*/

            val dx = (mx.avgX() - x0) / epsilon
            val dy = (my.avgY() - y0) / epsilon

            // todo dx and dy seem to always be close to 1.00000,
            // todo what is the actual meaning of them, can we set them to 1.0 without errors?

            // stack.translateLocal(-alpha * x0 / dx, -alpha * y0 / dy, 0f)
            val newtonX = -x0 / dx
            val newtonY = -y0 / dy

            if (abs(newtonX) + abs(newtonY) < 5f) {

                // good enough for pixels
                if (scaleIsGoodEnough && abs(newtonX) + abs(newtonY) < 1e-4f) break

                stack.translateLocal(newtonX, newtonY, 0f)
                stack.scaleLocal(scale, scale, scale)

                // LOGGER.info("Tested[$i]: $epsilon, Used: Newton = (-($x0/$dx=$newtonX), -($y0/$dy=$newtonY)), Scale: $scale")

            } else break // translation is too wrong... mmh...

        }

    }

}
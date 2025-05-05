package me.anno.tests.utils.boids

import me.anno.Time
import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.IMesh
import me.anno.ecs.components.mesh.MeshSpawner
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.systems.OnDrawGUI
import me.anno.ecs.systems.OnUpdate
import me.anno.engine.WindowRenderFlags
import me.anno.engine.ui.render.DrawAABB
import me.anno.engine.ui.render.RenderMode
import me.anno.engine.ui.render.RenderView
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.pipeline.Pipeline
import me.anno.graph.octtree.KdTree
import me.anno.graph.octtree.OctTreeF
import me.anno.maths.Maths.dtTo01
import me.anno.utils.Color.withAlpha
import me.anno.utils.pooling.Stack
import me.anno.utils.structures.arrays.FloatArrayList
import me.anno.utils.structures.arrays.FloatArrayListUtils.add
import me.anno.utils.structures.lists.Lists.createList
import me.anno.utils.types.Vectors.normalToQuaternionY
import org.joml.AABBd
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.random.Random

class Accelerator(val boids: BoidV3) : OctTreeF<Int>(32) {
    override fun getPoint(data: Int) = boids.positions[data]
    override fun createChild(): Accelerator {
        val instance = boids.accPool.create()
        instance.left = null
        instance.right = null
        instance.min.set(Float.POSITIVE_INFINITY)
        instance.max.set(Float.NEGATIVE_INFINITY)
        instance.children?.clear()
        return instance
    }
}

/**
 * only a little faster,
 * which means we did good, I think, with Entity optimizations and such :)
 * */
class BoidV3(val n: Int) : MeshSpawner(), OnUpdate, OnDrawGUI {

    val newDir = Vector3f()
    val center = Vector3f()

    val min = Vector3f()
    val max = Vector3f()

    val positions = createList(n) { Vector3f() }
    val directions = createList(n) { Vector3f() }
    val rotations = createList(n) { Quaternionf() }
    val velocities = createList(n) { Vector3f() }

    val accPool = Stack { Accelerator(this) }
    val accelerator = Accelerator(this)

    override fun onUpdate() {

        accelerator.clear()
        for (i in 0 until n) {
            accelerator.add(i)
        }

        for (i in 0 until n) {

            val posA = positions[i]
            val dirA = directions[i]

            center.set(0f)
            newDir.set(dirA)

            var weight = 0
            posA.sub(dr, dr, dr, min)
            posA.add(dr, dr, dr, max)

            // calculate update
            accelerator.query(min, max) { j ->
                if (i != j) {
                    val posB = positions[j]
                    val distSq = posA.distanceSquared(posB)
                    if (distSq < mr2) {
                        val dirB = directions[j]
                        // separation
                        val sepForce = 1000f / (distSq * distSq)
                        newDir.add(
                            (posA.x - posB.x) * sepForce,
                            (posA.y - posB.y) * sepForce,
                            (posA.z - posB.z) * sepForce
                        )
                        // alignment
                        dirB.mulAdd(1f, newDir, newDir)
                    }
                    if (distSq < mr3) {
                        // cohesion
                        center.add(posB)
                        weight++
                    }
                }
                false
            }

            val time = Time.gameTime.toFloat() * 10f
            dirA.add(
                noiseFlowField[posA.x, posA.y, posA.z, time],
                noiseFlowField[posA.x, posA.y, posA.z, time + 1e4f],
                noiseFlowField[posA.x, posA.y, posA.z, time + 2e4f]
            )

            if (weight > 0f) {
                dirA.mulAdd(weight.toFloat(), newDir, newDir)

                // apply cohesion
                center.mul(1f / weight)
                newDir.add(center.sub(posA))
            } else posA.mulAdd(-1f, newDir, newDir)

            // apply update
            // and calculate new position and direction
            newDir.normalize(dirA)
            val velocity = velocities[i]
            val dt = Time.deltaTime.toFloat()
            dirA.mulAdd(dt * speed, velocity, velocity)
            velocity.mulAdd(dt, posA, posA)
            velocity.mul(1f - dtTo01(0.1f * dt))
            velocity.normalize(center).normalToQuaternionY(rotations[i])
        }
        accPool.sub(accPool.index)

        invalidateAABB()
    }

    override fun forEachMesh(callback: (IMesh, Material?, Transform) -> Boolean) {
        val mesh = birdMesh
        for (i in 0 until n) {
            val transform = getTransform(i)
            transform.setLocalPosition(positions[i])
            transform.setLocalRotation(rotations[i])
            if (callback(mesh, null, transform)) break
        }
    }

    override fun forEachMeshGroupTRS(callback: (IMesh, Material?) -> FloatArrayList): Boolean {
        val list = callback(birdMesh, null)
        list.ensureExtra(8 * n)
        for (i in 0 until n) {
            val pos = positions[i]
            val rot = rotations[i]
            list.add(pos)
            list.add(1f)
            list.add(rot)
        }
        return true
    }

    private val tmp = AABBd()
    override fun onDrawGUI(pipeline: Pipeline, all: Boolean) {
        if (RenderView.currentInstance?.renderMode == RenderMode.SHOW_AABB) {
            draw(accelerator, 127)
        }
    }

    fun <V> draw(acc: KdTree<Vector3f, V>, alpha: Int) {
        if (alpha < 1) return
        val min = acc.min
        val max = acc.max
        tmp.setMin(min.x.toDouble(), min.y.toDouble(), min.z.toDouble())
        tmp.setMax(max.x.toDouble(), max.y.toDouble(), max.z.toDouble())
        DrawAABB.drawAABB(tmp, (-1).withAlpha(alpha))
        val childAlpha = alpha * 4 / 5
        draw(acc.left ?: return, childAlpha)
        draw(acc.right ?: return, childAlpha)
    }
}

fun main() {

    val n = 1000
    val boid = BoidV3(n)

    val s = 1000f
    val rnd = Random(1234)
    for (i in 0 until n) {
        boid.positions[i]
            .set(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat())
            .sub(0.5f).mul(s)
        boid.directions[i]
            .set(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat())
            .sub(0.5f).safeNormalize()
    }

    testSceneWithUI("Boids V3", boid) {
        WindowRenderFlags.showFPS = true
    }
}
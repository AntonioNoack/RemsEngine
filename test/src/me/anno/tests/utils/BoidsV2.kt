package me.anno.tests.utils

import me.anno.Time
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.graph.octtree.OctTreeF
import me.anno.maths.Maths.dtTo01
import me.anno.studio.StudioBase
import me.anno.utils.types.Vectors.normalToQuaternionY
import org.joml.Quaternionf
import org.joml.Vector3f
import kotlin.random.Random

// improved, 70 fps -> 90-120 fps
class BoidV2(
    val index: Int, val n: Int,
    val accelerator: OctTreeF<BoidV2>,
) : Component() {

    val posA = Vector3f()
    val dirA = Vector3f()

    val newDir = Vector3f()
    val center = Vector3f()
    val tmpQ = Quaternionf()

    val velocity = Vector3f()
    val min = Vector3f()
    val max = Vector3f()

    override fun onUpdate(): Int {

        center.set(0f)
        newDir.set(dirA)

        var weight = 0
        posA.sub(dr, dr, dr, min)
        posA.add(dr, dr, dr, max)

        // calculate update
        accelerator.query(min, max) {
            if (it !== this) {
                val posB = it.posA
                val distSq = posA.distanceSquared(posB)
                if (distSq < mr2) {
                    val dirB = it.dirA
                    // separation
                    val sepForce = 1000f / (distSq * distSq)
                    newDir.add((posA.x - posB.x) * sepForce, (posA.y - posB.y) * sepForce, (posA.z - posB.z) * sepForce)
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
        val dt = Time.deltaTime.toFloat()
        dirA.mulAdd(dt * speed, velocity, velocity)
        velocity.mulAdd(dt, posA, posA)
        velocity.mul(1f - dtTo01(0.1f * dt))
        val entity = entity!!
        val transform = entity.transform
        transform.localPosition = transform.localPosition.set(posA)
        transform.localRotation = transform.localRotation.set(dirA.normalToQuaternionY(tmpQ))
        entity.invalidateAABBsCompletely()
        return 1
    }
}

fun main() {

    val n = 1000
    val scene = Entity("Scene")
    val rnd = Random(1234)

    class Accelerator : OctTreeF<BoidV2>(64) {
        override fun createChild() = Accelerator()
        override fun getPoint(data: BoidV2) = data.posA
        override fun getMin(data: BoidV2) = data.posA
        override fun getMax(data: BoidV2) = data.posA
    }

    val accelerator = Accelerator()
    val s = 1000f
    val boids = ArrayList<BoidV2>()
    scene.add(object : Component() {
        override fun onUpdate(): Int {
            accelerator.clear()
            for (bird in boids) {
                accelerator.add(bird)
            }
            return 1
        }
    })

    for (i in 0 until n) {
        val boid = Entity()
        val mesh = MeshComponent(birdMesh)
        mesh.isInstanced = true
        boid.add(mesh)
        val boid1 = BoidV2(i, n, accelerator)
        boids.add(boid1)
        boid.add(boid1)
        boid1.posA.set(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat()).sub(0.5f, 0.5f, 0.5f).mul(s)
        boid1.dirA.set(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat()).sub(0.5f, 0.5f, 0.5f).normalize()
        scene.add(boid)
    }
    testSceneWithUI("Boids V2", scene) {
        StudioBase.showFPS = true
    }
}
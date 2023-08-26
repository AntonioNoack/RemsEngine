package me.anno.tests.utils

import me.anno.Engine
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.maths.Maths.dtTo01
import me.anno.mesh.Shapes.flatCube
import me.anno.studio.StudioBase
import me.anno.utils.types.Vectors.normalToQuaternion
import org.joml.Quaternionf
import org.joml.Vector3f
import java.util.*

// https://en.wikipedia.org/wiki/Boids
// boid = bird-like object
// O(n²) complexity, so don't use it xD, but still runs at 70 fps for 1000 boids :)
class Bird(
    val index: Int, val n: Int,
    val positions: Array<Vector3f>,
    val directions: Array<Vector3f>
) : Component() {

    val newDir = Vector3f()
    val center = Vector3f()
    val maxRadius = 50f
    val maxRadius2 = 200f
    val speed = 30f
    val tmpQ = Quaternionf()

    val velocity = Vector3f()

    override fun onUpdate(): Int {

        // easy, lazy, O(n²) algorithm
        // should be fast enough for 1k birds
        val mr2 = maxRadius * maxRadius
        val mr3 = maxRadius2 * maxRadius2
        val posA = positions[index]
        val dirA = directions[index]
        center.set(0f)
        newDir.set(dirA)

        var weight = 0
        // calculate update
        for (i in 0 until n) {
            if (i == index) continue
            val posB = positions[i]
            val distSq = posA.distanceSquared(posB)
            if (distSq < mr2) {
                val dirB = directions[i]
                // separation
                val sepForce = 1000f / (distSq * distSq)
                newDir.add((posA.x - posB.x) * sepForce, (posA.y - posB.y) * sepForce, (posA.z - posB.z) * sepForce)
                // alignment
                dirB.mulAdd(0.01f, newDir, newDir)
            }
            if (distSq < mr3) {
                // cohesion
                center.add(posB)
                weight++
            }
        }

        if (weight > 0f) {
            dirA.mulAdd(weight.toFloat(), newDir, newDir)

            // apply cohesion
            center.mul(1f / weight)
            newDir.add(center.sub(posA))
        }

        // general force towards 0,0,0 -> looks too planetary
        // posA.mulAdd(0.01f, newDir, newDir)

        // apply update
        // and calculate new position and direction
        newDir.normalize(dirA)
        dirA.mulAdd(Engine.deltaTime * speed, velocity, velocity)
        velocity.mulAdd(Engine.deltaTime, posA, posA)
        velocity.mul(1f - dtTo01(0.1f * Engine.deltaTime))
        val entity = entity!!
        val transform = entity.transform
        transform.localPosition = transform.localPosition.set(posA)
        transform.localRotation = transform.localRotation.set(dirA.normalToQuaternion(tmpQ))
        entity.invalidateAABBsCompletely()
        return 1
    }
}

fun main() {
    // test boids, because Useless Game Dev gets unacceptably bad performance in https://www.youtube.com/watch?v=6dJlhv3hfQ0
    // O(n²) should be fine with n=1000
    val n = 1000
    val scene = Entity("Scene")
    val positions = Array(n) { Vector3f() }
    val directions = Array(n) { Vector3f() }
    val rnd = Random()
    val s = 1000f
    for (i in 0 until n) {
        val bird = Entity()
        val mesh = MeshComponent(flatCube.front)
        mesh.isInstanced = true
        bird.add(mesh)
        bird.add(Bird(i, n, positions, directions))
        positions[i].set(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat()).sub(0.5f, 0.5f, 0.5f).mul(s)
        directions[i].set(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat()).sub(0.5f, 0.5f, 0.5f).normalize()
        scene.add(bird)
    }
    testSceneWithUI("Boids", scene) {
        StudioBase.instance?.showFPS = true
    }
}
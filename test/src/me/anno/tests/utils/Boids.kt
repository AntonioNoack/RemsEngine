package me.anno.tests.utils

import me.anno.Time
import me.anno.ecs.Component
import me.anno.ecs.Entity
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.maths.Maths
import me.anno.maths.Maths.dtTo01
import me.anno.maths.noise.PerlinNoise
import me.anno.mesh.Shapes.flatCube
import me.anno.studio.StudioBase
import me.anno.utils.types.Vectors.normalToQuaternionY
import org.joml.Quaternionf
import org.joml.Vector3f
import org.joml.Vector4f
import kotlin.random.Random

val maxRadius1 = 30f
val maxRadius2 = 150f
val speed = 30f

val dr = Maths.max(maxRadius1, maxRadius2)
val mr2 = maxRadius1 * maxRadius1
val mr3 = maxRadius2 * maxRadius2

val noiseFlowField = PerlinNoise(1234L, 5, 0.5f, -1f, +1f, Vector4f(0.01f))

val birdMesh = run {
    val birdMesh = Mesh()
    val newPos = flatCube.front.positions!!.copyOf()
    for (i in newPos.indices step 3) {
        val scale = if (newPos[i + 1] > 0f) 0f else 0.5f
        newPos[i + 0] *= scale
        newPos[i + 2] *= scale
    }
    birdMesh.positions = newPos
    birdMesh.indices = flatCube.front.indices
    birdMesh
}

// https://en.wikipedia.org/wiki/Boids
// boid = bird-like object
// O(n²) complexity, so don't use it xD, but still runs at 70 fps for 1000 boids :)
class Boid(
    val index: Int, val n: Int,
    val positions: Array<Vector3f>,
    val directions: Array<Vector3f>
) : Component() {

    val newDir = Vector3f()
    val center = Vector3f()
    val tmpQ = Quaternionf()

    val velocity = Vector3f()

    override fun onUpdate(): Int {

        // easy, lazy, O(n²) algorithm
        // should be fast enough for 1k birds
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
                dirB.mulAdd(1f, newDir, newDir)
            }
            if (distSq < mr3) {
                // cohesion
                center.add(posB)
                weight++
            }
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
    // test boids, because Useless Game Dev gets unacceptably bad performance in https://www.youtube.com/watch?v=6dJlhv3hfQ0
    // O(n²) should be fine with n=1000
    val n = 1000
    val scene = Entity("Scene")
    val positions = Array(n) { Vector3f() }
    val directions = Array(n) { Vector3f() }
    val rnd = Random(1234)
    val s = 1000f
    for (i in 0 until n) {
        val boid = Entity()
        val mesh = MeshComponent(birdMesh)
        mesh.isInstanced = true
        boid.add(mesh)
        boid.add(Boid(i, n, positions, directions))
        positions[i].set(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat()).sub(0.5f, 0.5f, 0.5f).mul(s)
        directions[i].set(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat()).sub(0.5f, 0.5f, 0.5f).normalize()
        scene.add(boid)
    }
    testSceneWithUI("Boids", scene) {
        StudioBase.showFPS = true
    }
}
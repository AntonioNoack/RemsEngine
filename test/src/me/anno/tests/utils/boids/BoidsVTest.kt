package me.anno.tests.utils.boids

import me.anno.Time
import me.anno.ecs.Transform
import me.anno.ecs.components.mesh.IMesh
import me.anno.ecs.components.mesh.MeshSpawner
import me.anno.ecs.components.mesh.material.Material
import me.anno.ecs.systems.OnUpdate
import me.anno.engine.WindowRenderFlags
import me.anno.engine.debug.DebugLine
import me.anno.engine.debug.DebugShapes
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.gpu.pipeline.Pipeline
import me.anno.utils.Color.white
import me.anno.utils.Color.withAlpha
import me.anno.utils.hpc.ProcessingGroup
import me.anno.utils.structures.arrays.FloatArrayList
import me.anno.utils.structures.arrays.FloatArrayListUtils.add
import me.anno.utils.structures.lists.Lists.createArrayList
import me.anno.utils.types.Vectors.normalToQuaternionY
import org.joml.AABBd
import org.joml.Matrix4x3
import org.joml.Quaternionf
import org.joml.Vector3d
import org.joml.Vector3f
import kotlin.random.Random

// only a little faster,
// which means we did good, I think, with Entity optimizations and such :)
class BoidVTest(val m: Int, val n: Int) : MeshSpawner(), OnUpdate {

    val speed = m * 90f

    val newDir = Vector3f()
    val tmp = Vector3f()

    val positions = createArrayList(n) { Vector3f() }
    val pos2 = createArrayList(n) { Vector3d() }
    val directions = createArrayList(n) { Vector3f() }
    val rotations = createArrayList(n) { Quaternionf() }
    val velocities = createArrayList(n) { Vector3f() }
    val following = IntArray(m * n)

    val group = ProcessingGroup("boids", 1f)

    override fun fillSpace(globalTransform: Matrix4x3, dstUnion: AABBd) {
        dstUnion.all() // ^^
    }

    override fun onUpdate() {
        val dt = Time.deltaTime.toFloat()
        for (i in 0 until n) {

            val posA = positions[i]
            val dirA = directions[i]

            newDir.set(dirA)
            newDir.mul(m.toFloat())

            // calculate update
            for (ji in 0 until m) {
                val j = following[i * m + ji]
                addNeighbor(j, posA)
            }

            // apply update
            // and calculate new position and direction
            newDir.normalize(dirA)
            val velocity = velocities[i]
            dirA.mulAdd(dt, velocity, velocity) // update velocity using dirA
            velocity.normalize()
            velocity.mulAdd(dt * speed, posA, posA) // update position by velocity
            velocity.normalToQuaternionY(rotations[i])
        }
    }

    fun addNeighbor(j: Int, posA: Vector3f) {
        val posB = positions[j]
        val dirB = directions[j]

        // separation
        val distSq = posA.distanceSquared(posB)
        val sepForce = 1000f / (distSq * distSq)
        newDir.add(
            (posA.x - posB.x) * sepForce,
            (posA.y - posB.y) * sepForce,
            (posA.z - posB.z) * sepForce
        )

        // alignment
        newDir.add(dirB)

        // apply cohesion
        newDir.add(posB).sub(posA)
    }

    override fun forEachMesh(pipeline: Pipeline?, callback: (IMesh, Material?, Transform) -> Boolean) {
        val mesh = birdMesh
        for (i in 0 until n) {
            val transform = getTransform(i)
            transform.setLocalPosition(positions[i])
            transform.setLocalRotation(rotations[i])
            if (callback(mesh, null, transform)) break
        }
    }

    override fun forEachMeshGroupTRS(pipeline: Pipeline, callback: (IMesh, Material?) -> FloatArrayList): Boolean {
        val list = callback(birdMesh, null)
        list.ensureExtra(8 * n)
        for (i in 0 until n) {
            pos2[i].set(positions[i])
        }
        for (i in 0 until n) {
            val pos = positions[i]
            val rot = rotations[i]
            list.add(pos)
            list.add(1f)
            list.add(rot)
        }
        return true
    }
}

// experiment: what happens if every boid has exactly one follower?
fun main() {

    val n = 10000
    val m = 2
    val rnd = Random(1234)
    val boid = BoidVTest(m, n)

    val s = 1000f
    for (i in 0 until n) {
        boid.positions[i].set(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat()).sub(0.5f, 0.5f, 0.5f).mul(s)
        boid.directions[i].set(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat()).sub(0.5f, 0.5f, 0.5f).normalize()
        for (ji in 0 until m) { // prevent duplicates? no, not really needed
            val j = rnd.nextInt(n - 1)
            boid.following[i * m + ji] = if (j < i) j else j + 1
        }
    }

    val color = white.withAlpha(0.05f)
    for (i in 0 until n) {
        for (j in 0 until m) {
            DebugShapes.debugLines.add(
                DebugLine(
                    boid.pos2[i], boid.pos2[boid.following[m * i + j]],
                    color, 1e9f
                )
            )
        }
    }

    testSceneWithUI("Boids V4", boid) {
        WindowRenderFlags.showFPS = true
    }
}
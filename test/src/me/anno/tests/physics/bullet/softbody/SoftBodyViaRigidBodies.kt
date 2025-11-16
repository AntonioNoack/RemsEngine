package me.anno.tests.physics.bullet.softbody

import me.anno.bullet.BulletPhysics
import me.anno.bullet.bodies.DynamicBody
import me.anno.bullet.bodies.StaticBody
import me.anno.bullet.constraints.PointConstraint
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.components.anim.Bone
import me.anno.ecs.components.anim.Skeleton
import me.anno.ecs.components.collider.BoxCollider
import me.anno.ecs.components.collider.InfinitePlaneCollider
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.components.mesh.shapes.PlaneModel.createPlane
import me.anno.ecs.components.mesh.utils.MeshJoiner
import me.anno.ecs.systems.Systems.registerSystem
import me.anno.engine.DefaultAssets
import me.anno.engine.ui.render.SceneView.Companion.testSceneWithUI
import me.anno.maths.Maths.clamp
import me.anno.mesh.Shapes.flatCube
import me.anno.mesh.vox.meshing.BlockSide
import me.anno.utils.algorithms.ForLoop.forLoopSafely
import me.anno.utils.assertions.assertTrue
import me.anno.utils.structures.maps.LazyMap
import me.anno.utils.types.Booleans.hasFlag
import org.joml.Matrix4x3f
import org.joml.Vector2i
import org.joml.Vector3d
import org.joml.Vector3f
import org.joml.Vector3f.Companion.length
import org.joml.Vector3i
import kotlin.math.ceil
import kotlin.math.floor

// todo why is the friction with the floor much stronger than the bonds themselves???

fun main() {

    // todo create a cube/chain/cloth of dynamic objects
    //  and inside, spawn lots and lots of point constraints:
    //  - on edges or vertices for keeping it together,
    //  - diagonally to avoid shearing
    //  - neighboring centers to avoid bending

    // todo visually overlay a mesh
    //  ... we could define a skeletal animation/texture from it, too :)
    //  only outside vertices are needed, probably...

    val physics = BulletPhysics().apply {
        // updateInEditMode = true
    }
    registerSystem(physics)

    val scene = Entity()

    // todo create sub-divided cube mesh, somehow...

    createString(scene, Vector3f(1f, 0.1f, 0.1f), 10, StringPattern.FOUR)
        .setPosition(-3.0, 0.2, 0.0)

    createCloth(scene, Vector3f(1f, 1f, 0.1f), Vector2i(5, 4))
        .setPosition(0.0, 1.1, 0.0)

    createBox(scene, Vector3f(1f), Vector3i(3, 4, 5))
        .setPosition(+3.0, 1.1, 0.0)

    Entity("Floor", scene)
        .add(StaticBody())
        .add(InfinitePlaneCollider())
        .add(MeshComponent(DefaultAssets.plane))
        .setScale(6f)

    testSceneWithUI("SoftBody via DynamicBodies", scene)
}

enum class StringPattern {
    ONE,
    TWO,
    FOUR,
}

fun createFineCube(halfSize: Vector3f, dim: Vector3i): Mesh {
    val dx = Vector3f(halfSize.x, 0f, 0f)
    val dy = Vector3f(0f, halfSize.y, 0f)
    val dz = Vector3f(0f, 0f, halfSize.z)
    return object : MeshJoiner<BlockSide>(false, false, true) {
        override fun getMesh(element: BlockSide): Mesh {
            return when (element) {
                BlockSide.PX -> createPlane(dim.z, dim.y, +dx, dz, dy)
                BlockSide.NX -> createPlane(dim.y, dim.z, -dx, dy, dz)
                BlockSide.PY -> createPlane(dim.x, dim.z, +dy, dx, dz)
                BlockSide.NY -> createPlane(dim.z, dim.x, -dy, dz, dx)
                BlockSide.PZ -> createPlane(dim.y, dim.x, +dz, dy, dx)
                BlockSide.NZ -> createPlane(dim.x, dim.y, -dz, dx, dy)
            }
        }
    }.join(BlockSide.entries)
}

fun getSoftIndex(xi: Int, yi: Int, zi: Int, dim: Vector3i): Int {
    assertTrue(xi in 0 until dim.x)
    assertTrue(yi in 0 until dim.y)
    assertTrue(zi in 0 until dim.z)
    return xi + (yi + zi * dim.z) * dim.x
}

fun createCloth(softBody: Entity, halfSize: Vector3f, dim: Vector3i, mesh: Mesh): Mesh {

    var numBones = 0
    val boneToSoftBodyIndex = IntArray(256)
    val boneMap = LazyMap<Int, Byte> { softBodyIndex ->
        val index = numBones++
        boneToSoftBodyIndex[index] = softBodyIndex
        index.toByte()
    }

    // todo for each vertex, resolve the closest bones,
    //  and assign weights to them

    val x0 = (dim.x - 1) * 0.5f
    val y0 = (dim.y - 1) * 0.5f
    val z0 = (dim.z - 1) * 0.5f

    data class IndexAndWeight(val softBodyIndex: Int, val weight: Float)

    val positions = mesh.positions ?: return mesh
    val numVertices = positions.size / 3
    val indices = ByteArray(numVertices * 4)
    val weights = FloatArray(numVertices * 4)
    var k = 0
    forLoopSafely(positions.size, 3) { i ->
        // todo is this correct???
        val xif = clamp(positions[i] / halfSize.x * dim.x + x0, 0f, dim.x - 1f)
        val yif = clamp(positions[i + 1] / halfSize.y * dim.y + y0, 0f, dim.y - 1f)
        val zif = clamp(positions[i + 2] / halfSize.z * dim.z + z0, 0f, dim.z - 1f)
        val softIndexAndWeights = List(8) { idx ->
            val xi = clamp((if (idx.hasFlag(1)) floor(xif) else ceil(xif)).toInt(), 0, dim.x - 1)
            val yi = clamp((if (idx.hasFlag(2)) floor(yif) else ceil(yif)).toInt(), 0, dim.y - 1)
            val zi = clamp((if (idx.hasFlag(4)) floor(zif) else ceil(zif)).toInt(), 0, dim.z - 1)
            val index = getSoftIndex(xi, yi, zi, dim)
            val weight = length(xi - xif, yi - yif, zi - zif)
            IndexAndWeight(index, weight)
        }.sortedByDescending { it.weight }
        val invSumWeight = 1f / softIndexAndWeights.subList(0, 4)
            .sumOf { it.weight.toDouble() }.toFloat()
        for (i in 0 until 4) {
            val (softBodyIndex, w) = softIndexAndWeights[i]
            indices[k] = boneMap[softBodyIndex]
            weights[k] = w * invSumWeight
            k++
        }
    }

    // create skeleton for the given bones
    val bones = List(numBones) { idx ->
        val softBodyIndex = boneToSoftBodyIndex[idx]
        // is the parenting necessary?
        Bone(idx, if (idx == 0) -1 else 0, "B$idx").apply {
            val ix = softBodyIndex % dim.x
            val iyz = softBodyIndex / dim.x
            val iy = iyz % dim.y
            val iz = iyz / dim.y
            val px = (ix - x0) * halfSize.x / dim.x
            val py = (iy - y0) * halfSize.y / dim.y
            val pz = (iz - z0) * halfSize.z / dim.z
            val transform = Matrix4x3f().translation(px, py, pz)
            setBindPose(transform)
            originalTransform.set(transform)
        }
    }
    for (i in bones.indices) {
        // todo define relative transform

    }

    // todo define realtime animation based on child transforms
    // todo when links get broken, we need to split and heal the mesh...

    val skeleton = Skeleton()
    skeleton.bones = bones

    mesh.boneIndices = indices
    mesh.boneWeights = weights
    mesh.skeleton = skeleton.ref

    return mesh
}

fun createString(
    scene: Entity, halfSize: Vector3f, dim: Int,
    stringPattern: StringPattern
): Entity {
    val child = Entity("String", scene)

    // todo padding for compressibility? issues with insides...
    val childSize = Vector3f(halfSize.x / dim, halfSize.y, halfSize.z)
    val childRadius = childSize.min()

    // todo create rounded-box mesh
    val mesh = flatCube.scaled(childSize).front

    val xi0 = (dim - 1) * 0.5

    val bodies = List(dim) { xi ->
        Entity(child)
            .add(DynamicBody().apply { friction = 0.1f })
            .add(MeshComponent(mesh))
            .add(BoxCollider().apply { halfExtents.set(childSize); roundness = childRadius })
            .setPosition(
                (xi - xi0) * 2.0 / dim * halfSize.x,
                0.0, 0.0
            )
    }

    val halfStep = halfSize.x.toDouble() / dim

    val dy = halfSize.y.toDouble()
    val dz = halfSize.z.toDouble()

    val l0 = Vector3d(+halfStep, 0.0, 0.0)
    val l1 = Vector3d(-halfStep, 0.0, 0.0)

    val l0a = Vector3d(+halfStep, -dy, -dz)
    val l1a = Vector3d(-halfStep, -dy, -dz)

    val l0b = Vector3d(+halfStep, -dy, +dz)
    val l1b = Vector3d(-halfStep, -dy, +dz)

    val l0c = Vector3d(+halfStep, +dy, -dz)
    val l1c = Vector3d(-halfStep, +dy, -dz)

    val l0d = Vector3d(+halfStep, +dy, +dz)
    val l1d = Vector3d(-halfStep, +dy, +dz)

    val l0e = Vector3d(+halfStep, +dy, 0.0)
    val l1e = Vector3d(-halfStep, +dy, 0.0)

    val l0f = Vector3d(+halfStep, -dy, 0.0)
    val l1f = Vector3d(-halfStep, -dy, 0.0)

    for (i in 1 until dim) {
        val bodyA = bodies[i - 1]
        val bodyB = bodies[i]
        when (stringPattern) {
            StringPattern.ONE -> connect(bodyA, bodyB, l0, l1)
            StringPattern.TWO -> {
                connect(bodyA, bodyB, l0e, l1e)
                connect(bodyA, bodyB, l0f, l1f)

                // constraints against shear
                connect(bodyA, bodyB, l0e, l1f)
                connect(bodyA, bodyB, l0f, l1e)
            }
            StringPattern.FOUR -> {
                connect(bodyA, bodyB, l0a, l1a)
                connect(bodyA, bodyB, l0b, l1b)
                connect(bodyA, bodyB, l0c, l1c)
                connect(bodyA, bodyB, l0d, l1d)

                // constraints against shear
                connect(bodyA, bodyB, l0a, l1b)
                connect(bodyA, bodyB, l0a, l1c)
                connect(bodyA, bodyB, l0d, l1b)
                connect(bodyA, bodyB, l0d, l1c)
            }
        }

        // todo constraints against bending

    }

    val fineMesh = createFineCube(halfSize, Vector3i(dim * 3, 1, 1))
    createCloth(child, halfSize, Vector3i(dim, 1, 1), fineMesh)
    child.add(MeshComponent(fineMesh))

    return child
}

fun connect(
    bodyA: Entity, bodyB: Entity,
    pos0: Vector3d, pos1: Vector3d
) {
    bodyA.add(PointConstraint().apply {
        other = bodyB.getComponent(DynamicBody::class)!!
        selfPosition.set(pos0)
        otherPosition.set(pos1)
        tau = 0.1f
        restLength = restLength(bodyA, bodyB, pos0, pos1)
        elasticRange.set(-0.1f, 0.1f)
        plasticDeformationRate = 0.5f
        plasticRange.set(restLength - 0.2f, restLength + 0.2f)
    })
}

fun restLength(
    bodyA: Entity, bodyB: Entity,
    pos0: Vector3d, pos1: Vector3d
): Float {
    return (bodyA.position + pos0).distance(bodyB.position + pos1).toFloat()
}

fun createCloth(scene: Entity, halfSize: Vector3f, dim: Vector2i): Entity {
    val child = Entity("Cloth", scene)

    val childSize = Vector3f(halfSize.x / dim.x, halfSize.y / dim.y, halfSize.z)
    val childRadius = childSize.min()

    val mesh = flatCube.scaled(childSize).front

    val xi0 = (dim.x - 1) * 0.5
    val yi0 = (dim.y - 1) * 0.5

    val bodies = List(dim.y) { yi ->
        List(dim.x) { xi ->
            Entity(child)
                .add(DynamicBody().apply { friction = 0.1f })
                .add(MeshComponent(mesh))
                .add(BoxCollider().apply { halfExtents.set(childSize); roundness = childRadius })
                .setPosition(
                    (xi - xi0) * 2.0 / dim.x * halfSize.x,
                    (yi - yi0) * 2.0 / dim.y * halfSize.y, 0.0
                )
        }
    }

    val halfStepX = halfSize.x.toDouble() / dim.x
    val halfStepY = halfSize.y.toDouble() / dim.y

    val dz = halfSize.z.toDouble()

    val l0x = Vector3d(+halfStepX, 0.0, -dz)
    val l1x = Vector3d(-halfStepX, 0.0, -dz)

    val l2x = Vector3d(+halfStepX, 0.0, +dz)
    val l3x = Vector3d(-halfStepX, 0.0, +dz)

    val l0y = Vector3d(0.0, +halfStepY, -dz)
    val l1y = Vector3d(0.0, -halfStepY, -dz)

    val l2y = Vector3d(0.0, +halfStepY, +dz)
    val l3y = Vector3d(0.0, -halfStepY, -dz)

    // todo create constraints between neighbors
    for (yi in 0 until dim.y) {
        for (xi in 1 until dim.x) {
            val bodyA = bodies[yi][xi - 1]
            val bodyB = bodies[yi][xi]
            // normal
            connect(bodyA, bodyB, l0x, l1x)
            connect(bodyA, bodyB, l2x, l3x)
            // against shear
            connect(bodyA, bodyB, l0x, l3x)
            connect(bodyA, bodyB, l2x, l1x)
        }
    }

    for (xi in 0 until dim.x) {
        for (yi in 1 until dim.y) {
            val bodyA = bodies[yi - 1][xi]
            val bodyB = bodies[yi][xi]
            // normal
            connect(bodyA, bodyB, l0y, l1y)
            connect(bodyA, bodyB, l2y, l3y)
            // against shear
            connect(bodyA, bodyB, l0y, l3y)
            connect(bodyA, bodyB, l2y, l1y)
        }
    }

    // todo counter bending??

    // todo diagonals??
    for (yi in 1 until dim.y) {
        for (xi in 1 until dim.x) {

        }
    }

    return child
}

fun createBox(scene: Entity, halfSize: Vector3f, dim: Vector3i): Entity {
    val child = Entity("Box", scene)

    val childSize = Vector3f(halfSize.x / dim.x, halfSize.y / dim.y, halfSize.z / dim.z)
    val childRadius = childSize.min()

    val mesh = flatCube.scaled(childSize).front

    val xi0 = (dim.x - 1) * 0.5
    val yi0 = (dim.y - 1) * 0.5
    val zi0 = (dim.z - 1) * 0.5

    val bodies = List(dim.z) { zi ->
        List(dim.y) { yi ->
            List(dim.x) { xi ->
                Entity(child)
                    .add(DynamicBody().apply { friction = 0.1f })
                    .add(MeshComponent(mesh))
                    .add(BoxCollider().apply { halfExtents.set(childSize); roundness = childRadius })
                    .setPosition(
                        (xi - xi0) * 2.0 / dim.x * halfSize.x,
                        (yi - yi0) * 2.0 / dim.y * halfSize.y,
                        (zi - zi0) * 2.0 / dim.z * halfSize.z
                    )
            }
        }
    }

    val halfStepX = halfSize.x.toDouble() / dim.x
    val halfStepY = halfSize.y.toDouble() / dim.y
    val halfStepZ = halfSize.z.toDouble() / dim.z

    val l0x = Vector3d(+halfStepX, 0.0, 0.0)
    val l1x = Vector3d(-halfStepX, 0.0, 0.0)

    val l0y = Vector3d(0.0, +halfStepY, 0.0)
    val l1y = Vector3d(0.0, -halfStepY, 0.0)

    val l0z = Vector3d(0.0, 0.0, +halfStepZ)
    val l1z = Vector3d(0.0, 0.0, -halfStepZ)

    // todo create constraints between neighbors
    for (zi in 0 until dim.z) {
        for (yi in 0 until dim.y) {
            for (xi in 1 until dim.x) {
                val bodyA = bodies[zi][yi][xi - 1]
                val bodyB = bodies[zi][yi][xi]
                connect(bodyA, bodyB, l0x, l1x)
            }
        }

        for (xi in 0 until dim.x) {
            for (yi in 1 until dim.y) {
                val bodyA = bodies[zi][yi - 1][xi]
                val bodyB = bodies[zi][yi][xi]
                connect(bodyA, bodyB, l0y, l1y)
            }
        }
    }

    for (yi in 0 until dim.y) {
        for (xi in 0 until dim.x) {
            for (zi in 1 until dim.z) {
                val bodyA = bodies[zi - 1][yi][xi]
                val bodyB = bodies[zi][yi][xi]
                connect(bodyA, bodyB, l0z, l1z)
            }
        }
    }

    return child
}
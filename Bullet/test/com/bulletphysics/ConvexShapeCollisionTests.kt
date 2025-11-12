package com.bulletphysics

import com.bulletphysics.collision.dispatch.CollisionDispatcher
import com.bulletphysics.collision.shapes.BoxShape
import com.bulletphysics.collision.shapes.BvhTriangleMeshShape
import com.bulletphysics.collision.shapes.CapsuleShape
import com.bulletphysics.collision.shapes.CollisionShape
import com.bulletphysics.collision.shapes.ConeShape
import com.bulletphysics.collision.shapes.CylinderShape
import com.bulletphysics.collision.shapes.SphereShape
import com.bulletphysics.collision.shapes.TriangleIndexVertexArray
import com.bulletphysics.dynamics.DiscreteDynamicsWorld
import com.bulletphysics.dynamics.RigidBody
import com.bulletphysics.extras.gimpact.GImpactCollisionAlgorithm
import com.bulletphysics.extras.gimpact.GImpactMeshShape
import me.anno.ecs.components.collider.Axis
import me.anno.utils.assertions.assertTrue
import org.joml.Vector3d
import org.joml.Vector3f
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer

class ConvexShapeCollisionTests {
    var shapes: Array<CollisionShape> = arrayOf(
        BoxShape(Vector3f(0.5, 0.5, 0.5)),
        SphereShape(0.5f),
        CapsuleShape(0.3f, 1.0f, Axis.X),
        CapsuleShape(0.3f, 1.0f, Axis.Y),
        CapsuleShape(0.3f, 1.0f, Axis.Z),
        CylinderShape(Vector3f(0.5, 0.5, 0.5), Axis.X),
        CylinderShape(Vector3f(0.5, 0.5, 0.5), Axis.Y),
        CylinderShape(Vector3f(0.5, 0.5, 0.5), Axis.Z),
        ConeShape(0.5f, 1.0f, Axis.X),
        ConeShape(0.5f, 1.0f, Axis.Y),
        ConeShape(0.5f, 1.0f, Axis.Z),
        // todo add a soft-body, when we have it
    )

    @Test
    fun testAllConvexShapeCombinationsCollide() {
        for (shape in shapes) {
            for (collisionShape in shapes) {
                val collided = simulateAndCheckCollision(false, shape, collisionShape)
                val pair = shape.javaClass.getSimpleName() + " vs " + collisionShape.javaClass.getSimpleName()
                Assertions.assertTrue(collided, "Expected collision: $pair")
            }
        }
    }

    @Test
    fun testAllConvexShapeCombinationsDoNotCollideWhenSeparated() {
        for (shape in shapes) {
            for (collisionShape in shapes) {
                val collided = simulateAndCheckCollision(true, shape, collisionShape)
                val pair = shape.javaClass.getSimpleName() + " vs " + collisionShape.javaClass.getSimpleName()
                Assertions.assertFalse(collided, "Unexpected collision detected: $pair")
            }
        }
    }

    @Test
    fun testConvexShapesCollideWithCubeMesh() {
        for (convex in shapes) {
            val collided = simulateConvexVsMeshCollision(convex)
            val name = convex.javaClass.getSimpleName()
            assertTrue(collided, "Expected collision: $name vs CubeMesh")
        }
    }

    @Test
    fun testConvexShapesCollideWithGImpactMesh() {
        for (convex in shapes) {
            val collided = simulateConvexVsGImpactMesh(convex)
            val name = convex.javaClass.getSimpleName()
            assertTrue(collided, "Expected collision: $name vs GImpactMesh")
        }
    }

    private fun simulateConvexVsMeshCollision(convexShape: CollisionShape): Boolean {
        // Create mesh shape
        val meshShape = createCubeMeshShape(0.5f)

        // Setup world
        val world = StackOfBoxesTest.createWorld()
        StackOfBoxesTest.createGround(world)

        // Add static mesh body
        val bodyA = StackOfBoxesTest.createRigidBody(0f, Vector3d(0f, 0f, 0f), meshShape)
        world.addRigidBody(bodyA)

        // Add dynamic convex shape body slightly above the mesh
        var y = 0.8f
        if (convexShape is ConeShape && convexShape.upAxis == Axis.Z) y = 0f
        val bodyB = StackOfBoxesTest.createRigidBody(1f, Vector3d(0f, y, 0f), convexShape)
        world.addRigidBody(bodyB)

        // Simulate
        world.stepSimulation(1.0f / 60.0f, 10)

        return isColliding(world, bodyA, bodyB)
    }

    private fun simulateConvexVsGImpactMesh(convexShape: CollisionShape): Boolean {
        val meshShape = createGImpactCubeMeshShape(0.5f)
        val world = StackOfBoxesTest.createWorld()

        // IMPORTANT: Register GImpactCollisionAlgorithm
        world.dispatcher as CollisionDispatcher
        GImpactCollisionAlgorithm.registerAlgorithm(world.dispatcher)
        // SoftBodyCollisionAlgorithm.registerAlgorithm(world.dispatcher)

        val meshBody = StackOfBoxesTest.createRigidBody(0f, Vector3d(0f, 0f, 0f), meshShape)
        world.addRigidBody(meshBody)

        val convexBody = StackOfBoxesTest.createRigidBody(1f, Vector3d(0f, 0.5f, 0f), convexShape)
        world.addRigidBody(convexBody)

        repeat(10) {
            world.stepSimulation((1f / 240f), 10)
        }

        return isColliding(world, convexBody, meshBody)
    }

    private fun simulateAndCheckCollision(
        separated: Boolean,
        shapeA: CollisionShape,
        shapeB: CollisionShape
    ): Boolean {
        // Setup physics world
        val world: DiscreteDynamicsWorld = StackOfBoxesTest.createWorld()

        // Create two overlapping dynamic bodies
        val bodyA: RigidBody = StackOfBoxesTest.createRigidBody(1f, Vector3d(0f, 0f, 0f), shapeA)
        val bodyB: RigidBody = StackOfBoxesTest.createRigidBody(
            1f,
            Vector3d(0f, if (separated) 3f else 0.2f, 0f),
            shapeB
        ) // Slight vertical offset

        world.addRigidBody(bodyA)
        world.addRigidBody(bodyB)

        // Step simulation a few times
        repeat(5) {
            world.stepSimulation((1f / 60f), 10)
        }

        return isColliding(world, bodyA, bodyB)
    }

    private fun isColliding(world: DiscreteDynamicsWorld, bodyA: RigidBody?, bodyB: RigidBody?): Boolean {
        // Check for collision between A and B
        val ha = bodyA?.broadphaseHandle ?: return false
        val hb = bodyB?.broadphaseHandle ?: return false
        return world.broadphase.overlappingPairCache.findPair(ha, hb) != null
    }

    @Suppress("SameParameterValue")
    private fun createCubeMeshShape(halfExtents: Float): BvhTriangleMeshShape {
        // Define cube vertices

        val vertices = arrayOf<Vector3d?>(
            Vector3d(-halfExtents, -halfExtents, -halfExtents),
            Vector3d(halfExtents, -halfExtents, -halfExtents),
            Vector3d(halfExtents, halfExtents, -halfExtents),
            Vector3d(-halfExtents, halfExtents, -halfExtents),
            Vector3d(-halfExtents, -halfExtents, halfExtents),
            Vector3d(halfExtents, -halfExtents, halfExtents),
            Vector3d(halfExtents, halfExtents, halfExtents),
            Vector3d(-halfExtents, halfExtents, halfExtents)
        )

        // Define triangles (12 for a cube)
        val indices = intArrayOf(
            0, 1, 2, 2, 3, 0,  // back
            4, 5, 6, 6, 7, 4,  // front
            0, 4, 7, 7, 3, 0,  // left
            1, 5, 6, 6, 2, 1,  // right
            3, 2, 6, 6, 7, 3,  // top
            0, 1, 5, 5, 4, 0 // bottom
        )

        // Flatten vertices into float array
        val vertexArray = FloatArray(vertices.size * 3)
        for (i in vertices.indices) {
            vertexArray[i * 3] = vertices[i]!!.x.toFloat()
            vertexArray[i * 3 + 1] = vertices[i]!!.y.toFloat()
            vertexArray[i * 3 + 2] = vertices[i]!!.z.toFloat()
        }

        // Create mesh
        val mesh = TriangleIndexVertexArray(
            indices.size / 3,
            wrap(indices),
            3 * 4,
            vertexArray.size / 3,
            wrap(vertexArray),
            3 * 4
        )

        return BvhTriangleMeshShape(mesh, true)
    }

    @Suppress("SameParameterValue")
    private fun createGImpactCubeMeshShape(halfExtents: Float): GImpactMeshShape {
        // Define cube vertices
        val vertices = arrayOf(
            Vector3d(-halfExtents, -halfExtents, -halfExtents),
            Vector3d(halfExtents, -halfExtents, -halfExtents),
            Vector3d(halfExtents, halfExtents, -halfExtents),
            Vector3d(-halfExtents, halfExtents, -halfExtents),
            Vector3d(-halfExtents, -halfExtents, halfExtents),
            Vector3d(halfExtents, -halfExtents, halfExtents),
            Vector3d(halfExtents, halfExtents, halfExtents),
            Vector3d(-halfExtents, halfExtents, halfExtents)
        )

        val indices = intArrayOf(
            0, 1, 2, 2, 3, 0,  // back
            4, 5, 6, 6, 7, 4,  // front
            0, 4, 7, 7, 3, 0,  // left
            1, 5, 6, 6, 2, 1,  // right
            3, 2, 6, 6, 7, 3,  // top
            0, 1, 5, 5, 4, 0 // bottom
        )

        val vertexArray = FloatArray(vertices.size * 3)
        for (i in vertices.indices) {
            vertexArray[i * 3] = vertices[i].x.toFloat()
            vertexArray[i * 3 + 1] = vertices[i].y.toFloat()
            vertexArray[i * 3 + 2] = vertices[i].z.toFloat()
        }

        val mesh = TriangleIndexVertexArray(
            indices.size / 3,
            wrap(indices),
            3 * 4,
            vertexArray.size / 3,
            wrap(vertexArray),
            3 * 4
        )

        val shape = GImpactMeshShape(mesh)
        shape.updateBound() // IMPORTANT
        return shape
    }

    private fun wrap(indices: IntArray): ByteBuffer {
        val data = ByteBuffer.allocate(indices.size * 4)
        data.asIntBuffer().put(indices)
        return data
    }

    private fun wrap(vertices: FloatArray): ByteBuffer {
        val data = ByteBuffer.allocate(vertices.size * 4)
        data.asFloatBuffer().put(vertices)
        return data
    }
}

package me.anno.tests.physics.bullet

import com.bulletphysics.collision.shapes.CollisionShape
import com.bulletphysics.collision.shapes.ConvexShape
import com.bulletphysics.linearmath.Transform
import me.anno.bullet.BulletPhysics
import me.anno.bullet.bodies.DynamicBody
import me.anno.bullet.bodies.StaticBody
import me.anno.bullet.createBulletBoxShape
import me.anno.ecs.Entity
import me.anno.ecs.components.collider.BoxCollider
import me.anno.ecs.components.collider.SphereCollider
import me.anno.ecs.systems.Systems
import me.anno.engine.ui.render.SceneView.Companion.createSceneUI
import me.anno.sdf.SDFCollider
import me.anno.sdf.shapes.SDFBox
import me.anno.sdf.shapes.SDFSphere
import me.anno.ui.debug.TestEngine.Companion.testUI
import org.joml.Vector3d
import org.joml.Vector3f
import java.util.Random

fun main() {

    // todo create reliable sdf physics

    // done -> create scene with default mesh
    // todo -> compare the functions of their colliders with our custom implementation

    fun test0() {
        val scene = Entity()
        val physics = BulletPhysics()
        Systems.registerSystem(physics)
        // physics.updateInEditMode = true

        Entity("SphereCollider", scene)
            .add(StaticBody())
            .add(SphereCollider())
            .setPosition(0.0, -20.0, 0.0)
            .setScale(20f)
            .add(SDFSphere())

        Entity("BoxCollider", scene)
            .add(DynamicBody())
            .add(BoxCollider())
            .setPosition(-3.3, 15.0, 0.0)
            .setScale(3f)
            .add(SDFBox())

        Entity("SDFCollider", scene)
            .add(DynamicBody())
            .add(SDFCollider().apply { isConvex = true })
            .setPosition(+3.3, 15.0, 0.0)
            .setScale(3f)
            .add(SDFBox())

        testUI("SDF Physics") {
            createSceneUI(scene)
        }
    }

    fun test1() {

        val random = Random(1234L)
        val samples = Array(64) {
            Vector3f(random.nextGaussian(), random.nextGaussian(), random.nextGaussian())
        }

        println(samples)

        fun print(shape: CollisionShape) {

            // convex sdf collisions shapes look quite good now :)
            // todo scaled convex shapes don't work correctly, looks as if it wasn't scaled

            val min = Vector3d()
            val max = Vector3d()
            val tr = Transform()
            tr.setIdentity()
            shape.getBounds(tr, min, max)
            println("$min - $max")
            println(shape.margin)
            println(shape.shapeType)
            println(shape.getBoundingSphere(null))
            shape as ConvexShape
            println("${shape.numPreferredPenetrationDirections}") // only used for hull building
            println(samples.map { shape.localGetSupportingVertex(it, Vector3f()) })
            println(samples.map { shape.localGetSupportingVertexWithoutMargin(it, Vector3f()) })
            println(samples.map { dir ->
                val out = arrayOf(Vector3f())
                shape.batchedUnitVectorGetSupportingVertexWithoutMargin(
                    arrayOf(dir), out, 1
                )
                out[0]
            })
            println()
        }

        val shape0 = BoxCollider().createBulletBoxShape(Vector3f(1f))
        val shape1c = SDFCollider()
        val entity = Entity()
        entity.add(SDFBox())
        entity.add(shape1c)
        val shape1 = shape1c.createBulletCollider(Vector3f(1f))
        print(shape0)
        print(shape1)
    }

    test1()
    test0()
}
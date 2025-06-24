package me.anno.bench

import com.bulletphysics.BulletStats
import com.bulletphysics.linearmath.CProfileManager
import com.bulletphysics.linearmath.CProfileNode
import me.anno.Engine
import me.anno.Time
import me.anno.bullet.BulletPhysics
import me.anno.bullet.DynamicBody
import me.anno.bullet.StaticBody
import me.anno.ecs.Entity
import me.anno.ecs.components.collider.BoxCollider
import me.anno.ecs.components.collider.InfinitePlaneCollider
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.ecs.systems.Systems
import me.anno.engine.OfficialExtensions
import me.anno.io.saveable.Saveable.Companion.registerCustomClass
import me.anno.maths.Maths.SECONDS_TO_NANOS
import me.anno.mesh.Shapes.flatCube
import me.anno.utils.Clock
import me.anno.utils.types.Floats.f3
import me.anno.utils.types.Floats.toRadians
import org.apache.logging.log4j.LogManager
import org.joml.Vector3f

private val LOGGER = LogManager.getLogger("DominoPhysics")

fun main() {
    init()
    val clock = Clock(LOGGER)
    val numSteps = 1000
    LOGGER.info("NumSteps: $numSteps")
    runDominoTest(2, 2) // warmup

    // todo test performance debugging
    BulletStats.isProfileEnabled = true

    for (n in listOf(16, 32, 64, 128, 256, 512, 1024)) {
        clock.start()
        runDominoTest(n, numSteps)
        clock.stop("$n Dominos", n * numSteps)
    }

    // just prints the last simulation step
    printProfiling()

    Engine.requestShutdown()
}

fun printProfiling() {
    printProfiling(CProfileManager.root, 0)
}

fun printProfiling(node: CProfileNode, depth: Int) {
    println("  ".repeat(depth) + "\"${node.name}\" ${node.totalCalls}x: ${(node.totalTimeSeconds * 1e3).f3()} ms")
    var child = node.child
    while (child != null) {
        printProfiling(child, depth + 1)
        child = child.sibling
    }
}

fun init() {
    OfficialExtensions.initForTests()
    registerCustomClass(BoxCollider())
}

fun runDominoTest(numDominos: Int, numSteps: Int) {

    val inch = 0.1f//2.54e-2f
    val width = 17f / 16f * inch
    val height = 35f / 16f * inch
    val thickness = 7f / 16f * inch

    val scene = Entity("Scene")
    Systems.world = scene

    val physics = BulletPhysics().apply {
        // updateInEditMode = true
        // fixedStep = 1.0 / 240.0
        synchronousPhysics = true
    }

    Systems.registerSystem(physics)

    val density = 1.0
    val mass1 = width * height * thickness * density

    val margin1 = 0.1f * inch
    val halfExtents1 = Vector3f(width * 0.5f, height * 0.5f, thickness * 0.5f)
    val mesh = flatCube.scaled(halfExtents1).front.ref

    val dominos = Entity("Dominos", scene)

    fun add(x: Float, z: Float): Entity {
        return Entity(dominos)
            .setPosition(x.toDouble(), (halfExtents1.y + margin1).toDouble(), z.toDouble())
            .add(MeshComponent(mesh))
            .add(DynamicBody().apply {
                mass = mass1
                friction = 0.3
                restitution = 0.0
            })
            .add(BoxCollider().apply {
                halfExtents = halfExtents1
                roundness = margin1
            })
    }

    // slightly faster
    Entity("Floor", scene)
        .add(InfinitePlaneCollider())
        .add(StaticBody().apply {
            friction = 0.9
            restitution = 0.0
        })

    val spacingZ = height * 0.7f
    for (i in 0 until numDominos - 1) {
        add(0f, spacingZ * i)
    }

    add(0f, spacingZ * (numDominos - 1)).apply {
        rotation = rotation.rotateX((-20f).toRadians())
    }

    var thisTime = 0L
    val dt = 1.0 / 60.0
    val dtNanos = (dt * SECONDS_TO_NANOS).toLong()
    repeat(numSteps) {
        Time.updateTime(1.0 / 60.0, thisTime)
        Systems.onUpdate()
        thisTime += dtNanos
    }
}
package me.anno.tests.collider

import me.anno.Engine
import me.anno.ecs.Entity
import me.anno.ecs.components.collider.BoxCollider
import me.anno.ecs.components.collider.CapsuleCollider
import me.anno.ecs.components.collider.Collider
import me.anno.ecs.components.collider.ConeCollider
import me.anno.ecs.components.collider.CylinderCollider
import me.anno.ecs.components.collider.MeshCollider
import me.anno.ecs.components.collider.SphereCollider
import me.anno.ecs.components.mesh.Mesh
import me.anno.ecs.components.mesh.MeshComponent
import me.anno.engine.OfficialExtensions
import me.anno.engine.raycast.RayQueryLocal
import me.anno.image.ImageWriter
import me.anno.io.Saveable.Companion.registerCustomClass
import me.anno.maths.Maths.fract
import me.anno.tests.LOGGER
import me.anno.utils.Color.rgba
import me.anno.utils.OS.documents
import org.apache.logging.log4j.LogManager
import org.joml.Vector3f

val testShapes = listOf(
    BoxCollider(),
    BoxCollider().apply {
        name = "BoxWithBorder"
        roundness = 0.5
    },
    SphereCollider(),
    CapsuleCollider().apply {
        radius = 0.8
        halfHeight = 0.8
    },
    CylinderCollider(),
    CylinderCollider().apply {
        name = "CylinderWithBorder"
        roundness = 0.5
    },
    ConeCollider().apply {
        name = "Cone"
    },
    ConeCollider().apply {
        name = "ConeWithBorder"
        roundness = 0.5
    },
    ConeCollider().apply {
        name = "ConeSmall"
        radius *= 0.5
        height *= 0.5
    },
    ConeCollider().apply {
        name = "ConeWide"
        radius *= 1.5
        height /= 1.5
    },
    MeshCollider().apply {
        registerCustomClass(Entity())
        registerCustomClass(MeshComponent())
        registerCustomClass(Mesh())
        name = "IcoSphere"
        meshFile = documents.getChild("icosphere.obj")
    },
    MeshCollider().apply {
        registerCustomClass(Entity())
        registerCustomClass(MeshComponent())
        registerCustomClass(Mesh())
        name = "Triangle"
        meshFile = documents.getChild("triangle.obj")
    },
    MeshCollider().apply {
        registerCustomClass(Entity())
        registerCustomClass(MeshComponent())
        registerCustomClass(Mesh())
        name = "Monkey"
        meshFile = documents.getChild("monkey.obj")
    }
)

fun renderSDF(collider: Collider, name: String) {

    val logger = LogManager.getLogger("SDFTest")

    val heavy = name == "Monkey"

    val size = 8.0f
    val res = if (heavy) 128 else 512

    val scaleX = size / (res - 1)
    val scale = Vector3f(scaleX, -scaleX, 1f)
    val offset = Vector3f(size * 0.5f, -size * 0.5f, 0f)

    // logger.info()
    logger.info(collider.name.ifEmpty { collider.className })
    /*logger.info(
        "raycast from 1,0,0 to 0,0,0: " + collider.raycast(
            Vector3f(1f, 0f, 0f),
            Vector3f(-1f, 0f, 0f),
            0f, 0f,
            Vector3f(),
            1f
        )
    )
    logger.info("distance at 0,0,0: " + collider.getSignedDistance(Vector3f()))
    logger.info("distance at 1,0,0: " + collider.getSignedDistance(Vector3f(1f, 0f, 0f)))*/

    writeImage(res, res, heavy, name, false) { x, y ->
        val pos = Vector3f(x, y, 0f)
        pos.mul(scale)
        pos.sub(offset)
        val distance = collider.getSignedDistance(pos)
        var color = fract(distance)
        if (distance < 0f) color--
        color
    }

    val name1 = name.substring(0, name.length - 4) + "-d.png"
    writeImage(res, res, heavy, name1, false) { x, y ->
        val pos = Vector3f(x, y, 0f).mul(scale).sub(offset)
        val dir = Vector3f(pos).mul(-1f).normalize()
        val query = RayQueryLocal(pos, dir, size * 2f, 0f, 0f)
        val distance = collider.raycastClosestHit(query, null)
        if (distance.isInfinite()) LOGGER.debug("{} {} -> {}, {} -> {}", x, y, pos, dir, distance)
        if (distance < 0f) -255f else distance * (if (heavy) 0.25f else 2f) * 255f / size
    }

    val name2 = name.substring(0, name.length - 4) + "-n.png"
    ImageWriter.writeRGBAImageInt(res, res, name2, 512) { x, y, _ ->
        val pos = Vector3f(x.toFloat(), y.toFloat(), 0f)
        val normal = Vector3f(0f, 0f, 1f)
        pos.mul(scale)
        pos.sub(offset)
        collider.getSignedDistance(pos, normal)
        normal.normalize(0.5f).add(0.5f, 0.5f, 0.5f)
        rgba(normal.x, normal.y, normal.z, 1f)
    }
}

fun main() {

    OfficialExtensions.initForTests()

    for (shape in testShapes) {
        var name = shape.name
        if (name.isEmpty()) name = shape.className
        renderSDF(shape, "img/$name.png")
    }
    Engine.requestShutdown()
}
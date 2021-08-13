package me.anno.ecs.components.collider

import me.anno.utils.Color.rgba
import me.anno.utils.Maths.fract
import me.anno.utils.image.ImageWriter
import org.joml.Vector3f

fun main() {

    val shapes = listOf(
        BoxCollider(),
        BoxCollider().apply {
            name = "BoxWithBorder"
            roundness = 0.5
        },
        SphereCollider(),
        CapsuleCollider().apply {
            radius = 0.8
            height = 0.8
        },
        CylinderCollider(),
        CylinderCollider().apply {
            name = "CylinderWithBorder"
            roundness = 0.5
        }, // */
        /*ConeCollider(),
        ConeCollider().apply {
            name = "Cone2x"
            radius *= 2
            height *= 2
        },
        ConeCollider().apply {
            name = "ConeWithBorder"
            roundness = 0.5
        },
        ConeCollider().apply {
            name = "ConeWide"
            radius *= 2
        }*/
    )

    for (shape in shapes) {
        var name = shape.name
        if (name.isEmpty()) name = shape.className
        renderSDF(shape, "img/$name.png")
    }

}

fun renderSDF(collider: Collider, name: String) {

    val size = 8.0f
    val res = 512

    val scale = Vector3f(size / (res - 1))
    val offset = Vector3f(size * 0.5f, size * 0.5f, 0f)

    println()
    println(collider.name.ifEmpty { collider.className })
    println(
        "raycast from 1,0,0 to 0,0,0: " + collider.raycast(
            Vector3f(1f, 0f, 0f),
            Vector3f(-1f, 0f, 0f),
            Vector3f(),
            1f
        )
    )
    println("distance at 0,0,0: " + collider.getSignedDistance(Vector3f()))
    println("distance at 1,0,0: " + collider.getSignedDistance(Vector3f(1f, 0f, 0f)))

    ImageWriter.writeImageFloatMSAA(res, res, name, 512, false) { x, y ->
        val pos = Vector3f(x, y, 0f)
        pos.mul(scale)
        pos.sub(offset)
        val distance = collider.getSignedDistance(pos)
        var color = fract(distance)
        if (distance < 0f) color--
        color * 255
    }

    val name1 = name.substring(0, name.length - 4) + "-d.png"
    ImageWriter.writeImageFloatMSAA(res, res, name1, 512, false) { x, y ->
        val pos = Vector3f(x, y, 0f)
        val dir = Vector3f()
        val normal = Vector3f()
        pos.mul(scale)
        pos.sub(offset)
        dir.set(pos).mul(-1f).normalize()
        val distance = collider.raycast(pos, dir, normal, size)
        if (distance < 0f) -255f else distance * 2f * 255f / size
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
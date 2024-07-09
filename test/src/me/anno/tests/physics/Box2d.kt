package me.anno.tests.physics

import me.anno.Time
import me.anno.box2d.Box2dPhysics
import me.anno.box2d.CircleCollider
import me.anno.box2d.Collider2d
import me.anno.box2d.RectCollider
import me.anno.box2d.Rigidbody2d
import me.anno.config.DefaultConfig.style
import me.anno.ecs.Entity
import me.anno.ecs.EntityQuery.forAllComponents
import me.anno.ecs.EntityQuery.getComponent
import me.anno.ecs.components.collider.Collider
import me.anno.gpu.drawing.DrawCurves
import me.anno.gpu.drawing.DrawCurves.drawCubicBezier
import me.anno.gpu.drawing.DrawCurves.drawLine
import me.anno.input.Input
import me.anno.input.Key
import me.anno.maths.Maths
import me.anno.maths.Maths.PIf
import me.anno.maths.Maths.angleDifference
import me.anno.ui.base.groups.MapPanel
import me.anno.ui.debug.TestEngine.Companion.testUI3
import me.anno.utils.Color.withAlpha
import me.anno.utils.structures.lists.Lists.createArrayList
import me.anno.utils.types.Floats.toDegrees
import me.anno.utils.types.Triangles.subCross
import org.jbox2d.collision.shapes.PolygonShape
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.BodyDef
import org.jbox2d.dynamics.BodyType
import org.jbox2d.dynamics.FixtureDef
import org.jbox2d.dynamics.World
import org.joml.Vector2f
import org.joml.Vector3d
import java.util.Random
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

fun main() {
    test1()
    test2()
    test3()
}

private fun test1() {
    println("Library Test")
    // works, just why is it not accelerating?
    // create test world
    val world = World(Vec2(0f, -9.81f))
    // test gravity and maybe collisions
    val groundDef = BodyDef()
    groundDef.position.set(0f, -10f)
    val groundBody = world.createBody(groundDef)
    val groundShape = PolygonShape()
    groundShape.setAsBox(50f, 5f)
    groundBody.createFixture(groundShape, 0f)
    val boxDef = BodyDef()
    boxDef.type = BodyType.DYNAMIC
    boxDef.position.set(0f, 10f)
    val boxBody = world.createBody(boxDef)
    val boxShape = PolygonShape()
    boxShape.setAsBox(1f, 1f)
    val fixtureDef = FixtureDef()
    fixtureDef.shape = boxShape
    fixtureDef.density = 1f
    fixtureDef.friction = 0.3f
    boxBody.createFixture(fixtureDef)
    for (i in 0 until 10) {
        println(boxBody.position.toString() + ", " + boxBody.angle.toDegrees() + "°")
        world.step(1f, 1, 1)
    }
}

private fun test2() {
    // why is the result slightly different?
    println("Own World Test")
    // create the same world as in test 1, now just with our own classes
    // create test world
    val world = Entity()
    val physics = Box2dPhysics()
    physics.velocityIterations = 1
    physics.positionIterations = 1
    world.add(physics)
    val ground = Entity()
    val groundRB = Rigidbody2d()
    ground.add(groundRB)
    val groundShape = RectCollider()
    groundShape.halfExtends.set(50f, 5f)
    ground.setPosition(0.0, -10.0, 0.0)
    ground.add(groundShape)
    world.add(ground)
    // test gravity and maybe collisions
    val box = Entity()
    val boxRB = Rigidbody2d()
    box.add(boxRB)
    box.setPosition(0.0, 10.0, 0.0)
    val boxShape = RectCollider()
    boxShape.density = 1f
    boxShape.halfExtends.set(1f, 1f)
    box.add(boxShape)
    world.add(box)
    world.validateTransform()
    groundRB.invalidatePhysics()
    boxRB.invalidatePhysics()
    for (i in 0 until 10) {
        box.validateTransform()
        ground.validateTransform()
        println(box.position.toString() + ", " + (box.rotation.getEulerAnglesYXZ(Vector3d()).z.toDegrees()) + "°")
        physics.step(Maths.MILLIS_TO_NANOS * 1000, false)
    }
}

fun test3() {
    val world = Entity("World")
    val physics = Box2dPhysics()
    physics.velocityIterations = 1
    physics.positionIterations = 1
    physics.gravity.y = 90.0
    physics.allowedSpace.all()
    physics.updateGravity()
    world.add(physics)

    val width = 1000
    val height = 1000

    val strength = 20.0

    for (dir in 0 until 4) {
        val angle = dir * PI / 2
        val ground = Entity("Ground")
        val groundRB = Rigidbody2d()
        ground.add(groundRB)
        val groundShape = RectCollider()
        groundShape.halfExtends.set(500f, strength.toFloat())
        ground.setPosition(
            sin(angle) * width * 0.5,
            cos(angle) * height * 0.5,
            0.0
        )
        ground.setRotation(0.0, 0.0, angle)
        ground.add(groundShape)
        world.add(ground)
    }

    val random = Random(1234L)
    for (i in 0 until 100) {
        val box = Entity("Box")
        box.add(Rigidbody2d())
        box.setPosition(
            (random.nextDouble() - 0.5) * 600.0,
            (random.nextDouble() - 0.5) * 600.0,
            0.0
        )
        box.setRotation(0.0, 0.0, random.nextDouble() * PI)
        val shape = RectCollider()
        shape.density = 1f
        shape.halfExtends.set(20f, 7f)
        box.add(shape)
        world.add(box)
    }

    for (i in 0 until 100) {
        val circle = Entity("Circle")
        circle.add(Rigidbody2d())
        circle.setPosition(
            (random.nextDouble() - 0.5) * 600.0,
            (random.nextDouble() - 0.5) * 600.0,
            0.0
        )
        val shape = CircleCollider()
        shape.density = 1f
        shape.radius = 12f
        circle.add(shape)
        world.add(circle)
    }

    val rect = listOf(
        Vector2f(-1f, -1f),
        Vector2f(+1f, -1f),
        Vector2f(+1f, +1f),
        Vector2f(-1f, +1f),
    )

    // more resolution = fewer pixels need to be evaluated, but also more vertices
    val circleResolution = 12
    val offset = 4f / 3f * tan(PIf / (2f * circleResolution))
    val circle = createArrayList(circleResolution) {
        val a = PIf * 2f * it / circleResolution
        val c = cos(a)
        val s = sin(a)
        Pair(Vector2f(c, s), Vector2f(c + s * offset, s - c * offset))
    }

    // todo add chains between stuff?
    // todo spawn new points?

    testUI3("Box2d") {
        object : MapPanel(style) {

            init {
                minScale.set(0.25)
                maxScale.set(250.0)
            }

            var targetAngle = 0.0

            val dragButton = Key.BUTTON_LEFT
            val moveButton = Key.BUTTON_RIGHT

            override fun shallMoveMap() =
                (Input.isKeyDown(dragButton) && hovered == null) || Input.isKeyDown(moveButton)

            override fun onUpdate() {
                super.onUpdate()
                physics.step((Time.deltaTime * 1e9f).toLong(), false)
                if (Input.isKeyDown(dragButton)) {
                    val hovered = hovered
                    val entity = hovered?.entity
                    val rb = entity?.getComponent(Rigidbody2d::class)
                    if (rb != null) {
                        val body = rb.box2dInstance!!
                        val pos = entity.transform.globalPosition
                        val ang = entity.transform.globalRotation.getEulerAnglesYXZ(tmp).z
                        // calculate anchorWS from mouse position
                        val mouseX = (mouse.x - (x + this.width / 2)) / scale.x.toFloat() + center.x
                        val mouseY = (mouse.y - (y + this.height / 2)) / scale.y.toFloat() + center.y
                        // calculate global position of anchor target from anchorLS
                        val targetX = pos.x + cos(ang) * anchorLS.x
                        val targetY = pos.y + sin(ang) * anchorLS.y
                        val speed = 25f
                        force.set(speed * (mouseX - targetX).toFloat(), speed * (mouseY - targetY).toFloat())
                        anchorWS.set(targetX.toFloat(), targetY.toFloat())
                        body.linearVelocity.set(force.x, force.y)
                        body.angularVelocity = speed * angleDifference((targetAngle - ang).toFloat())
                    }
                }
                invalidateDrawing()
            }

            override fun onMouseWheel(x: Float, y: Float, dx: Float, dy: Float, byMouse: Boolean) {
                if (Input.isKeyDown(dragButton)) {
                    val hovered = hovered
                    val entity = hovered?.entity
                    val rb = entity?.getComponent(Rigidbody2d::class)
                    if (rb != null) {
                        targetAngle += dy * 0.1f
                        return
                    }
                }
                super.onMouseWheel(x, y, dx, dy, byMouse)
            }

            var hovered: Collider? = null

            val t0 = Vector2f()
            val t1 = Vector2f()
            val mouse = Vector2f()
            val force = Vec2()

            val anchorLS = Vec2()
            val anchorWS = Vec2()

            val tmp = Vector3d()

            override fun onDraw(x0: Int, y0: Int, x1: Int, y1: Int) {
                super.onDraw(x0, y0, x1, y1)

                val x0i = x + this.width / 2 - (center.x * scale.x).toFloat()
                val y0i = y + this.height / 2 - (center.y * scale.y).toFloat()
                val scale = scale.y.toFloat()

                val bg = backgroundColor.withAlpha(0)

                val th = 1f
                val cl = -1
                val fe = false

                var bestDist = if (Input.wasKeyPressed(dragButton)) Float.POSITIVE_INFINITY else -1f
                val window = window
                if (window != null) mouse.set(window.mouseX, window.mouseY)

                val v = DrawCurves.lineBatch.start()
                world.validateTransform()
                world.forAll { entity ->
                    if (entity is Entity) entity.forAllComponents(Collider2d::class) { collider ->
                        val pos = entity.transform.globalPosition
                        val x1i = x0i + scale * pos.x.toFloat()
                        val y1i = y0i + scale * pos.y.toFloat()
                        val angle = entity.transform.globalRotation.getEulerAnglesYXZ(tmp).z
                        val c = cos(angle).toFloat()// could be directly read from matrix
                        val s = sin(angle).toFloat()
                        val dist = mouse.distanceSquared(x1i, y1i)
                        var inside = dist < bestDist
                        when (collider) {
                            is RectCollider -> {
                                val he = collider.halfExtends
                                val hx = he.x * scale
                                val hy = he.y * scale
                                fun px(p0: Vector2f) = x1i + (hx * p0.x * c - hy * p0.y * s)
                                fun py(p0: Vector2f) = y1i + (hx * p0.x * s + hy * p0.y * c)
                                val p1 = rect.last()
                                var lx = px(p1)
                                var ly = py(p1)
                                t1.set(lx, ly)
                                for (i in rect.indices) {
                                    val p0 = rect[i]
                                    val cx = px(p0)
                                    val cy = py(p0)
                                    t0.set(cx, cy)
                                    drawLine(cx, cy, lx, ly, th, cl, bg, fe)
                                    inside = inside && subCross(t0, t1, mouse) < 0f
                                    lx = cx
                                    ly = cy
                                    t1.set(t0)
                                }
                            }
                            is CircleCollider -> {
                                val radius = collider.radius * scale
                                inside = dist < radius * radius
                                fun px(p0: Vector2f) = x1i + (p0.x * c - p0.y * s) * radius
                                fun py(p0: Vector2f) = y1i + (p0.x * s + p0.y * c) * radius
                                var p1 = circle.last()
                                for (i in circle.indices) {
                                    val p0 = circle[i]
                                    val lx = px(p1.first)
                                    val ly = py(p1.first)
                                    drawCubicBezier(
                                        px(p0.first), py(p0.first),
                                        px(p0.second), py(p0.second),
                                        2 * lx - px(p1.second), 2 * ly - py(p1.second),
                                        lx, ly,
                                        th, cl, bg, fe
                                    )
                                    p1 = p0
                                }
                            }
                        }
                        if (inside && Input.wasKeyPressed(dragButton)) {
                            bestDist = dist
                            hovered = collider
                            // calculate mouse position in local space of collider
                            val rx = (mouse.x - x1i) / scale
                            val ry = (mouse.y - y1i) / scale
                            targetAngle = angle
                            anchorLS.set(c * rx + s * ry, c * ry - s * rx)
                        } else if (!Input.isKeyDown(dragButton)) hovered = null
                    }
                }

                DrawCurves.lineBatch.finish(v)
            }
        }
    }
}
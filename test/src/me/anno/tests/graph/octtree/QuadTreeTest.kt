package me.anno.tests.graph.octtree

import me.anno.Time
import me.anno.engine.WindowRenderFlags
import me.anno.gpu.RenderDoc.disableRenderDoc
import me.anno.gpu.drawing.DrawCurves
import me.anno.gpu.drawing.DrawCurves.drawLine
import me.anno.gpu.drawing.DrawRectangles
import me.anno.gpu.drawing.DrawRectangles.drawRect
import me.anno.gpu.drawing.DrawTexts.drawSimpleTextCharByChar
import me.anno.gpu.drawing.DrawTexts.monospaceFont
import me.anno.graph.octtree.KdTree
import me.anno.graph.octtree.QuadTreeF
import me.anno.input.Input
import me.anno.input.Key
import me.anno.maths.Maths.max
import me.anno.maths.Maths.sq
import me.anno.ui.UIColors
import me.anno.ui.debug.FrameTimings
import me.anno.ui.debug.TestDrawPanel.Companion.testDrawing
import me.anno.utils.Color.withAlpha
import me.anno.utils.hpc.ProcessingGroup
import me.anno.utils.structures.lists.Lists.createArrayList
import org.joml.Vector2f
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * 10000 planets/stars,
 *  force depending on surrounding planets/stars
 *  toggle for using quadtree
 * */
fun main() {

    class Agent {
        val position = Vector2f()
        val lastPos = Vector2f()
        val velocity = Vector2f()
        val acceleration = Vector2f()
    }

    class AgentNode : QuadTreeF<Agent>(16) {
        override fun getPoint(data: Agent): Vector2f = data.position
        override fun getMin(data: Agent): Vector2f = data.position
        override fun getMax(data: Agent): Vector2f = data.position
        override fun createChild(): KdTree<Vector2f, Agent> {
            return AgentNode()
        }
    }

    fun reflectOnEdges(pos: Vector2f, vel: Vector2f) {
        if (pos.x < 0f && vel.x < 0f) {
            pos.x = 0f
            vel.x = -vel.x
        } else if (pos.x > 1f && vel.x > 0f) {
            pos.x = 1f
            vel.x = -vel.x
        }
        if (pos.y < 0f && vel.y < 0f) {
            pos.y = 0f
            vel.y = -vel.y
        } else if (pos.y > 1f && vel.y > 0f) {
            pos.y = 1f
            vel.y = -vel.y
        }
    }

    val random = Random(1234)
    val agents = createArrayList(10000) {
        Agent().apply {
            position.set(random.nextFloat(), random.nextFloat())
            lastPos.set(position)
        }
    }

    val quadTree = AgentNode()
    for (i in agents.indices) {
        quadTree.add(agents[i])
    }

    var useQuadtree = true
    var showQuadtree = true
    var showLines = true
    var useParallelization = false

    val workers = ProcessingGroup("QuadTree", 1f)

    val min = Vector2f()
    val max = Vector2f()
    val speed = 0.1f / agents.size
    val radius = 2f * sqrt(1f / agents.size)
    val radSq = sq(radius)
    disableRenderDoc()
    WindowRenderFlags.showFPS = true
    val controlsText = listOf(
        " --- Controls ---------------- ",
        "|   Q   | Hide/Show QuadTree  |",
        "|   L   | Hide/Show Lines     |",
        "|   P   | Use Parallelization |",
        "| Shift | Toggle use Quadtree |",
        "|Control| Rebuild Quadtree    |",
        "|  Alt  | Dynamic Rebuilding  |",
        " ----------------------------- ",
    )
    testDrawing("QuadTree") {
        it.clear()

        val dt = Time.deltaTime.toFloat()

        if (Input.wasKeyPressed(Key.KEY_LEFT_SHIFT) || Input.wasKeyPressed(Key.KEY_RIGHT_SHIFT)) {
            useQuadtree = !useQuadtree
        }

        if (Input.wasKeyPressed(Key.KEY_Q)) {
            showQuadtree = !showQuadtree
        }

        if (Input.wasKeyPressed(Key.KEY_L)) {
            showLines = !showLines
        }

        if (Input.wasKeyPressed(Key.KEY_P)) {
            useParallelization = !useParallelization
        }

        val ds = min(it.width, it.height)
        val x0 = it.x + (it.width - ds) / 2
        val y0 = it.y + (it.height - ds) / 2

        // update all agents
        // (1)
        //  - set force to zero
        //  - accumulate force
        // (2)
        //  - apply force to motion
        //  - update tree

        var ctr = 0
        val bg = it.backgroundColor.withAlpha(0)
        val lineColor = (-1).withAlpha(16)

        fun accelerateAgent(i: Int) {
            val agent = agents[i]
            agent.acceleration.set(0f)
            val pos = agent.position
            pos.sub(radius, radius, min)
            pos.add(radius, radius, max)

            val x = x0 + pos.x * ds
            val y = y0 + pos.y * ds

            fun update(other: Agent) {
                ctr++
                val pos2 = other.position
                val dx = pos2.x - pos.x
                val dy = pos2.y - pos.y
                val lenSq = dx * dx + dy * dy
                if (lenSq < radSq) {
                    val len = speed / (1e-9f + lenSq)
                    agent.acceleration.add(dx * len, dy * len)
                    if (showLines && !useParallelization) {
                        val x1 = x0 + pos2.x * ds
                        val y1 = y0 + pos2.y * ds
                        drawLine(x, y, x1, y1, 0.5f, lineColor, bg, true)
                    }
                }
            }

            if (useQuadtree) {
                quadTree.queryLists(min, max) { others ->
                    for (j in others.indices) {
                        val other = others[j]
                        if (other !== agent) {
                            update(other)
                        }
                    }
                    false
                }
            } else {
                for (j in agents.indices) {
                    if (i != j) {
                        update(agents[j])
                    }
                }
            }
        }

        val batch0 = DrawCurves.lineBatch.start()
        val t0 = Time.nanoTime
        if (useParallelization) {
            workers.processBalanced(0, agents.size, 16) { i0, i1 ->
                for (i in i0 until i1) {
                    accelerateAgent(i)
                }
            }
        } else {
            for (i in agents.indices) {
                accelerateAgent(i)
            }
        }
        val t1 = Time.nanoTime
        FrameTimings.add(t1 - t0, UIColors.midOrange)

        // visualize quadTree
        val lineColor1 = UIColors.midOrange.withAlpha(12)
        fun drawQuadTree(node: AgentNode) {

            val x2 = x0 + node.min.x * ds
            val y2 = y0 + node.min.y * ds
            val x3 = x0 + node.max.x * ds
            val y3 = y0 + node.max.y * ds

            drawLine(x2, y2, x3, y2, 0.5f, lineColor1, bg, true)
            drawLine(x3, y2, x3, y3, 0.5f, lineColor1, bg, true)
            drawLine(x3, y3, x2, y3, 0.5f, lineColor1, bg, true)
            drawLine(x2, y3, x2, y2, 0.5f, lineColor1, bg, true)

            val left = node.left as? AgentNode ?: return
            drawQuadTree(left)
            drawQuadTree(node.right as AgentNode)
        }
        if (showQuadtree) drawQuadTree(quadTree)
        DrawCurves.lineBatch.finish(batch0)

        fun isOnEdge(oldPos: Vector2f, newPos: Vector2f, min: Vector2f, max: Vector2f): Boolean {
            return min(oldPos.x, newPos.x) <= min.x || max(oldPos.x, newPos.x) >= max.x ||
                    min(oldPos.y, newPos.y) <= min.y || max(oldPos.y, newPos.y) >= max.y
        }

        val t2 = Time.nanoTime
        for (i in agents.indices) {
            // update agent:
            val agent = agents[i]
            agent.lastPos.set(agent.position)
            agent.acceleration.mulAdd(dt, agent.velocity, agent.velocity)
            reflectOnEdges(agent.position, agent.velocity)
            agent.velocity.mulAdd(dt, agent.position, agent.position)

            // update quadtree:
            // only reinsert point, if it is on the edge
            val node = quadTree.find(agent, agent.lastPos)
            if (node == null || isOnEdge(agent.lastPos, agent.position, node.min, node.max)) {
                // using remove+add, or clearing and re-inserting everything has roughly the same performance
                if (Input.isAltDown) {
                    quadTree.remove(agent, agent.lastPos)
                    quadTree.add(agent)
                } else {
                    quadTree.update(agent, agent.lastPos)
                }
            }
        }
        val t3 = Time.nanoTime
        FrameTimings.add(t3 - t2, UIColors.dodgerBlue)
        if (Input.isControlDown) {
            quadTree.clear()
            for (i in agents.indices) {
                quadTree.add(agents[i])
            }
        }
        val t4 = Time.nanoTime
        FrameTimings.add(t4 - t3, UIColors.paleGoldenRod)

        // draw all agents
        val batch = DrawRectangles.startBatch()
        for (i in agents.indices) {
            val agent = agents[i]
            val x = x0 + agent.position.x * ds
            val y = y0 + agent.position.y * ds
            drawRect(x, y, 1f, 1f, -1)
        }
        DrawRectangles.finishBatch(batch)

        // draw statistics
        drawSimpleTextCharByChar(it.x, it.y, 2, if (useQuadtree) "QuadTree" else "Naive")
        drawSimpleTextCharByChar(it.x, it.y + monospaceFont.sizeInt, 2, "$ctr Interactions")

        // draw help
        for (i in controlsText.indices) {
            val y = it.y + it.height + (i - controlsText.size) * monospaceFont.sizeInt
            drawSimpleTextCharByChar(it.x, y, 2, controlsText[i])
        }
    }
}
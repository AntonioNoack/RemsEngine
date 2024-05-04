package me.anno.tests.game.pacman.logic

import me.anno.maths.Maths.ceilDiv
import me.anno.maths.Maths.sq
import me.anno.utils.structures.Collections.crossMap
import me.anno.utils.structures.lists.Lists.any2
import org.joml.Vector2f
import org.joml.Vector2i
import kotlin.random.Random

open class PacmanLogic {

    // todo define level: walls
    // todo define ghosts

    val size = Vector2i(10, 10)
    val random = Random(System.nanoTime())

    val nodePositions = (0 until size.x).toList()
        .crossMap((0 until size.y).toList(), ArrayList()) { x, y ->
            Vector2i(x, y)
        }

    val voidPositions = listOf(
        Vector2i(0, 3),
        Vector2i(1, 3),
        Vector2i(8, 3),
        Vector2i(9, 3),
        Vector2i(0, 5),
        Vector2i(1, 5),
        Vector2i(8, 5),
        Vector2i(9, 5),
        Vector2i(4, 4),
        Vector2i(5, 4),
    ).toSet()

    val nodes = nodePositions
        .filter { it !in voidPositions }
        .map { Node(it) }

    val collectables = nodes.shuffled().subList(0, 10)
        .map { Vector2f(it.position) }.toMutableList()

    // should be created before nodes are connected
    val player = Player(nodes.first())

    fun wx(x0: Int, x1: Int, y: Int) = Wall(Vector2i(x0, y), Vector2i(x1, y))
    fun wy(y0: Int, y1: Int, x: Int) = Wall(Vector2i(x, y0), Vector2i(x, y1))
    val walls = listOf(
        wx(0, 10, 0),
        wx(0, 10, 10),
        wy(0, 3, 0),
        wy(6, 10, 0),
        wy(0, 3, 10),
        wy(6, 10, 10),
        wx(1, 2, 1),
        wx(3, 4, 1),
        wx(6, 7, 1),
        wx(8, 9, 1),
        wy(0, 1, 5),
        wy(2, 3, 5),
        wx(1, 4, 9),
        wx(6, 9, 9),
        wy(8, 9, 3),
        wy(8, 9, 7),
        wx(0, 1, 8),
        wx(9, 10, 8),
        wx(1, 2, 7),
        wx(8, 9, 7),
        wx(3, 4, 7),
        wx(6, 7, 7),
        wx(0, 2, 6),
        wx(8, 10, 6),
        wx(4, 6, 6),
        wx(0, 2, 4),
        wx(0, 2, 5),
        wx(8, 10, 4),
        wx(8, 10, 5),
        wx(4, 6, 8),
        wy(8, 9, 5),
        wy(6, 7, 5),
        wx(0, 2, 3),
        wx(8, 10, 3),
        wx(1, 2, 2),
        wx(8, 9, 2),
        wx(4, 6, 2),
        wy(2, 4, 3),
        wy(2, 4, 7),
        wx(3, 4, 3),
        wx(6, 7, 3),
        wy(3, 4, 2),
        wy(5, 6, 2),
        wy(3, 4, 8),
        wy(5, 6, 8),
        wy(5, 6, 3),
        wy(5, 6, 7),
        wy(7, 8, 2),
        wy(7, 8, 8),
        wx(4, 6, 4),
        wx(4, 6, 5),
        wy(4, 5, 4),
        wy(4, 5, 6),
    )

    // create movement graph
    init {
        fun connect(n1: Node, n2: Node?) {
            n2 ?: return
            n1.neighbors.add(n2)
            n2.neighbors.add(n1)
        }

        val posToNode = nodes.associateBy { it.position }
        for (node in nodes) {
            if (node.position.x > 0 &&
                walls.none { it.start.x == it.end.x && node.position.x == it.start.x && node.position.y in it.start.y until it.end.y }
            ) { // add a path to the left
                connect(node, posToNode[Vector2i(node.position).sub(1, 0)])
            }
            if (node.position.y > 0 &&
                walls.none { it.start.y == it.end.y && node.position.y == it.start.y && node.position.x in it.start.x until it.end.x }
            ) { // add a path to the top
                connect(node, posToNode[Vector2i(node.position).sub(0, 1)])
            }
        }
    }

    val minEnemyDistanceSq = ceilDiv(size.lengthSquared(), 4)
    val enemies = nodes.filter { it.position.lengthSquared() > minEnemyDistanceSq }
        .shuffled().subList(0, 5).map(::Enemy)

    private fun tickEnemies(dt: Float) {
        for (enemy in enemies) {
            enemy.tick(dt)
        }
    }

    private fun tickPlayer(dt: Float) {
        checkCollectibles()
        checkDeath()
        movePlayer(dt)
    }

    private fun movePlayer(dt: Float) {
        val c = player.current.position
        val n = player.next.position
        val d = player.requestedMovement
        if (d.dot(n) < d.dot(c)) {
            // inverse direction
            val tmp = player.current
            player.current = player.next
            player.next = tmp
            player.progress = 1f - player.progress
        }
        player.tick(dt)
    }

    val collectDistanceSq = sq(0.3f)
    private fun checkCollectibles() {
        collectables.removeIf { collectible ->
            if (collectible.distanceSquared(player.position) < collectDistanceSq) {
                player.points++
                onCollect(collectible)
                true
            } else false
        }
    }

   open fun onCollect(collectible: Vector2f) {}

    val killDistanceSq = sq(0.5f)
    private fun checkDeath() {
        val isKilled = enemies.any2 { it.position.distanceSquared(player.position) < killDistanceSq }
        if (isKilled && !player.wasKilled) {
            player.lives--
        }
        player.wasKilled = isKilled
    }

    fun tick(dt: Float) {
        if (player.requestedMovement.lengthSquared() > 0) {
            tickPlayer(dt)
            tickEnemies(dt)
        }
    }
}
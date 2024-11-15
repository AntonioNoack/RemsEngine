package me.anno.games.pacman.logic

class Enemy(node: Node) : Moveable(node) {
    override fun findNext(): Node {
        val n = next.position
        val c = current.position
        return next.neighbors
            .filter {
                val nn = it.position
                (nn.x - n.x) * (n.x - c.x) + (nn.y - n.y) * (n.y - c.y) >= 0 // don't turn around randomly
            }
            .randomOrNull() ?: current
    }
}
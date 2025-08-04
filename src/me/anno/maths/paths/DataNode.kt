package me.anno.maths.paths

class DataNode<Node>(var distance: Double, var score: Double, var previous: Node?) {
    constructor() : this(0.0, 0.0, null)

    fun set(dist: Double, score: Double, previous: Node?): DataNode<Node> {
        this.distance = dist
        this.score = score
        this.previous = previous
        return this
    }

    fun set(dist: Double, previous: Node?): DataNode<Node> {
        this.distance = dist
        this.previous = previous
        return this
    }

    override fun toString() = "($distance,$score,$previous)"
}
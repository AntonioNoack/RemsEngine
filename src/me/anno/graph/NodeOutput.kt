package me.anno.graph

class NodeOutput : NodeConnector {

    constructor(): super()
    constructor(type: String): super(type)
    constructor(type: String, node: Node) : super(type, node)

    override val className: String = "NodeOutput"

}
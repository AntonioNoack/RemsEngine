package me.anno.graph

class NodeOutput : NodeConnector {

    constructor(): super()
    constructor(type: String): super(type)
    constructor(type: String, node: Node) : super(type, node)
    constructor(type: String, name: String, node: Node) : super(type, name, node)

    override val className: String = "NodeOutput"

}
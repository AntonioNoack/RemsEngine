package me.anno.graph.visual.node

class NodeOutput : NodeConnector {
    constructor() : super(false)
    constructor(isCustom: Boolean) : super(isCustom)
    constructor(type: String, isCustom: Boolean) : super(type, isCustom)
    constructor(type: String, node: Node, isCustom: Boolean) : super(type, node, isCustom)
    constructor(type: String, name: String, node: Node, isCustom: Boolean) : super(type, name, node, isCustom)
}
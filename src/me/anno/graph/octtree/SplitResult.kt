package me.anno.graph.octtree

class SplitResult<Point>(
    val splitPoint: Point,
    val children: Array<OctTree<Point>?>
)
package org.recast4j.detour

object TotalCostComparator : Comparator<Node> {
    override fun compare(n1: Node, n2: Node): Int {
        return n1.totalCost.compareTo(n2.totalCost)
    }
}
package kmammou.vhacd

import java.util.PriorityQueue

class HullPair(val hull1MeshId: Int, val hull2MeshId: Int, val concavity: Double) : Comparable<HullPair> {

    override fun compareTo(other: HullPair): Int {
        return concavity.compareTo(other.concavity)
    }

    companion object {
        val hullPairQueue = PriorityQueue<HullPair>()
    }
}


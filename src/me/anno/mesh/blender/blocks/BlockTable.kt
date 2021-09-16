package me.anno.mesh.blender.blocks

class BlockTable(val blocks: Array<Block>?, offHeapStructs: IntArray?) {

    companion object {
        val HEAPBASE = 4096L
    }

    constructor() : this(null, null)

    val sorted = blocks?.sortedBy { it.header.address }?.toMutableList() ?: ArrayList()
    val blockList = blocks?.toMutableList() ?: ArrayList()

    var offHeapAreas: HashMap<Int, BlockTable>? = null

    init {
        if (offHeapStructs != null) {
            offHeapAreas = HashMap()
            for (index in offHeapStructs.indices) {
                offHeapAreas!![offHeapStructs[index]] = BlockTable()
            }
            val sorted = sorted
            var i = 0
            while (i < sorted.size) {
                val b = sorted[i]
                for (index in offHeapStructs.indices) {
                    val sdnaIndex = offHeapStructs[index]
                    if (b.header.sdnaIndex == sdnaIndex) {
                        offHeapAreas!![sdnaIndex]!!.add(b)
                        sorted.removeAt(i)
                        i--
                    }
                }
                i++
            }
            checkBlockOverlaps()
        }
        if (sorted.isNotEmpty()) {
            val first = sorted.first()
            if (first.header.address <= HEAPBASE) throw IllegalStateException()
        }
    }

    fun checkBlockOverlaps() {
        /*for (var i=0;i<this.sorted.length;i++) {
			var cur = this.sorted[i];
			for (var j=i+1;j<this.sorted.length;j++) {
				var b = this.sorted[j];
				if(cur.contains(b.header.address)) {
					overlapping.add(cur, b);
					throw "blocks are overlapping!";
				}
			}
		}*/
    }

    fun binarySearch(address: Long): Int {
        return sorted.binarySearch { it.header.address.compareTo(address) }
    }

    fun add(block: Block) {
        val index = binarySearch(block.header.address)
        if (index >= 0) throw RuntimeException()
        sorted.add(-index - 1, block)
    }

    fun findBlock(startAddress: Long): Block {
        val index = binarySearch(startAddress)
        if (index < 0) throw IllegalStateException()
        return sorted[index]
    }

    fun getBlockAt(positionInFile: Int): Block {
        var index = blockList.binarySearch { it.positionInFile.compareTo(positionInFile) }
        if (index < 0) index = -index - 2 // -2, because we want the block before
        return blockList[index]
    }

    fun getBlock(address: Long): Block? {
        if (address == 0L) return null
        val sorted = sorted
        var i = binarySearch(address)
        if (i >= 0) {
            return sorted[i]
        } else {
            // if the address lies between two block start addresses, then
            // -i-1 is the pos of the block with start address larger
            // than address. But we need the block with a start address
            // lower than address. Thus, -i-2
            i = -i - 2;
            if (i >= 0) {
                val b = sorted[i]
                if (address < (b.header.address + b.header.size)) {
                    // block found
                    return b
                }
            }
            return null
        }
    }

}
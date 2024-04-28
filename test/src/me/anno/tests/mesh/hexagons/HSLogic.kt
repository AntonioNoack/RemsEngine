package me.anno.tests.mesh.hexagons

import me.anno.gpu.buffer.Attribute
import me.anno.gpu.buffer.AttributeType
import me.anno.gpu.buffer.ComputeBuffer
import me.anno.gpu.shader.ComputeShader
import me.anno.gpu.shader.GLSLType
import me.anno.gpu.shader.builder.Variable
import me.anno.maths.chunks.spherical.Hexagon
import org.joml.Vector3i
import java.nio.ByteBuffer

class HSLogic(val world: HexagonSphereMCWorld) {

    fun block(hex: Hexagon, yi: Int) = hex.index * world.sy + yi
    fun blockYi(block: Long) = (block % world.sy).toInt()
    fun blockHex(block: Long) = block / world.sy

    val blockToIndex = HashMap<Long, Int>()
    val sorted = ArrayList<Long>()

    fun ofInterest(byte: Byte): Boolean {
        return byte in logic0..light1
    }

    fun onSetBlock(hex: Hexagon, yi: Int, oldBlock: Byte, newBlock: Byte) {
        if (oldBlock == newBlock) return
        val o = ofInterest(oldBlock)
        val n = ofInterest(newBlock)
        if (!o && !n) return
        val idx = block(hex, yi)
        when {
            o && n -> replace(hex, yi, idx, newBlock)
            o && !n -> remove(hex, yi, idx)
            else -> insert(hex, yi, idx, newBlock)
        }
    }

    fun replace(hex: Hexagon, yi: Int, idx: Long, newBlock: Byte) {
        updateStructure(hex, yi, idx)
        updateValue(idx, newBlock)
    }

    fun remove(hex: Hexagon, yi: Int, idx: Long) {
        val tmp = sorted.removeLast()
        if (idx == tmp) return
        sorted[blockToIndex[idx]!!] = tmp
        updateStructure(hex, yi, idx)
    }

    val structureData = ComputeBuffer("structure", dataLayout, 1024)
    var stateData0 = ComputeBuffer("state0", stateLayout, 1024)
    var stateData1 = ComputeBuffer("state1", stateLayout, 1024)

    init {
        uploadState()
        uploadStructure()
    }

    fun uploadState() {
        upload(stateData0)
        validState = true
    }

    fun uploadStructure() {
        upload(structureData)
        validStructure = true
    }

    var validState = false
    var validStructure = false
    fun validate() {
        if (!validState) uploadState()
        if (!validStructure) uploadStructure()
    }

    fun tick() {
        validate()
        // todo step
        val shader = tickShader
        shader.use()
        shader.v1i("size", blockToIndex.size)
        shader.runBySize(blockToIndex.size)
        val tmp = stateData0
        stateData0 = stateData1
        stateData1 = tmp
    }

    fun upload(data: ComputeBuffer) {
        val nio = data.nioBuffer!!
        nio.position(nio.capacity())
        data.ensureBuffer()
    }

    fun insert(hex: Hexagon, yi: Int, idx: Long, newBlock: Byte) {
        val index = blockToIndex.size
        sorted.add(idx)
        blockToIndex[idx] = index
        if (index >= structureData.elementCount) {
            structureData.nioBuffer = resize(structureData.nioBuffer!!)
            stateData0.nioBuffer = resize(stateData0.nioBuffer!!)
            stateData1.nioBuffer = resize(stateData1.nioBuffer!!)
            structureData.elementCount *= 2
            stateData0.elementCount *= 2
            stateData1.elementCount *= 2
        }
        updateStructure(hex, yi, idx)
        updateValue(idx, newBlock)
    }

    fun resize(buffer: ByteBuffer): ByteBuffer {
        val newBuffer = ByteBuffer.allocateDirect(buffer.capacity() * 2)
        newBuffer.put(buffer)
        newBuffer.position(0)
        return newBuffer
    }

    fun updateStructure(hex: Hexagon, yi: Int, idx: Long, updateNeighbors: Boolean = true) {
        // update neighbors as well, if they are active
        val index = blockToIndex[idx]!!
        val structure = structureData.nioBuffer!!
        structure.position(index * structureData.stride)
        var i = 0
        if (yi > 0) {
            val idx1 = block(hex, yi - 1)
            val idx1i = blockToIndex[idx1]
            if (idx1i != null) {
                if (updateNeighbors) updateStructure(hex, yi - 1, idx1, false)
                structure.putInt(idx1i)
            }
        }
        if (yi + 1 < world.sy) {
            val idx1 = block(hex, yi + 1)
            val idx1i = blockToIndex[idx1]
            if (idx1i != null) {
                if (updateNeighbors) updateStructure(hex, yi + 1, idx1, false)
                structure.putInt(idx1i)
            }
        }
        for (hex1 in hex.neighbors) {
            val idx1 = block(hex1!!, yi)
            val idx1i = blockToIndex[idx1]
            if (idx1i != null) {
                if (updateNeighbors) updateStructure(hex, yi, idx1, false)
                structure.putInt(idx1i)
            }
        }
        while (i < 8) {
            structure.putInt(-1)
            i++
        }
    }

    fun updateValue(idx: Long, newBlock: Byte) {
        val index = blockToIndex[idx]!!
        val state = stateData0.nioBuffer!!
        state.position(index * stateData0.stride)
        state.put((newBlock - logic0).and(1).toByte())
    }

    // todo logic blocks: separate material/shader
    // todo blocks: negator
    // todo each block is assigned an index
    // todo each block is assigned its neighbors indices
    // todo a compute shader computes all intensities :3

}

val tickShader = ComputeShader(
    "logic", Vector3i(512, 1, 1), listOf(
        Variable(GLSLType.V1I, "size")
    ), "" +
            "struct Structure {\n" +
            "   uint other[8];\n" +
            "};\n" +
            "layout(std140, shared, binding = 0) readonly buffer structureBuffer { Structure structures[]; };\n" +
            "layout(std140, shared, binding = 1) readonly buffer srcBuffer { uint8 data; };\n" +
            "layout(std140, shared, binding = 2) writeonly buffer dstBuffer { uint8 data; };\n" +
            "void main(){\n" +
            "   int index = int(gl_GlobalInvocationID.x);\n" +
            "   if(index < size){\n" +
            // read, process and store
            "       bool any = false;\n" +
            "       uint other[8] = srcBuffer[index];\n" +
            "       for(int i=0;i<8;i++){" +
            "           int j = other[i];\n" +
            "           if(j < 0) break;\n" +
            "           if(srcBuffer[j] != 0){\n" +
            "               any = true; break;\n" +
            "           }\n" +
            "       }\n" +
            "       dstBuffer[index] = any ? 1 : 0;\n" +
            "   }\n" +
            "}\n"
)

val stateLayout = listOf(
    // 8 neighbors for all sides
    Attribute("n0", AttributeType.UINT32, 4),
    Attribute("n1", AttributeType.UINT32, 4),
)

val dataLayout = listOf(
    Attribute("v", AttributeType.UINT8, 1)
)
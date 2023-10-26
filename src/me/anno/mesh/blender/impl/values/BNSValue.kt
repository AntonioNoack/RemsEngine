package me.anno.mesh.blender.impl.values

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import me.anno.mesh.blender.impl.BlendData
import java.nio.ByteBuffer

/**
 * bNodeSocketValue
 * */
abstract class BNSValue(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BlendData(file, type, buffer, position)
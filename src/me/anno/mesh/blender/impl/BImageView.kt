package me.anno.mesh.blender.impl

import me.anno.mesh.blender.BlenderFile
import me.anno.mesh.blender.DNAStruct
import java.nio.ByteBuffer

@Suppress("unused")
class BImageView(file: BlenderFile, type: DNAStruct, buffer: ByteBuffer, position: Int) :
    BLink<BImageView>(file, type, buffer, position)
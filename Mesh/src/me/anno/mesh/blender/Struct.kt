package me.anno.mesh.blender

class Struct(file: BinaryFile) {
    val type = file.readShort()
    // type, name
    val fieldsAsTypeName = ShortArray(file.readShort().toInt().and(0xffff) * 2) {
        file.readShort()
    }
}
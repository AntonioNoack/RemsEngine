package me.anno.export.reflect.clazz

import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertFail
import me.anno.utils.structures.arrays.BooleanArrayList
import me.anno.utils.structures.lists.Lists.createArrayList
import java.io.DataInputStream
import java.io.DataOutputStream

class Clazz(input: DataInputStream) {

    companion object {
        private const val magic = 0xcafebabe.toInt()
    }

    init {
        assertEquals(magic, input.readInt())
    }

    val minorVersion = input.readUnsignedShort()
    val majorVersion = input.readUnsignedShort()
    val constantPool = readConstantPool(input)
    val accessFlags = input.readUnsignedShort()
    val clazz = constantPool[input.readUnsignedShort()] as ClassInfo
    val superClass = constantPool[input.readUnsignedShort()] as? ClassInfo
    val interfaces = createArrayList(input.readUnsignedShort()) { constantPool[input.readUnsignedShort()] as ClassInfo }
    val fields = createArrayList(input.readUnsignedShort()) { Member(this, input) }
    val methods = createArrayList(input.readUnsignedShort()) { Member(this, input) }
    val attributes = createArrayList(input.readUnsignedShort()) { Attribute(this, input) }

    val constantMap = constantPool.withIndex().associate { it.value to it.index }
    val usedConstants = BooleanArrayList(constantPool.size)

    fun getConstant(key: Any?): Int {
        val index = constantMap[key]!!
        usedConstants.set(index)
        return index
    }

    fun getConstantOrNull(key: Any?): Int {
        val index = constantMap[key] ?: return 0
        usedConstants.set(index)
        return index
    }

    fun write(dst: DataOutputStream) {
        dst.writeInt(magic)
        dst.writeShort(minorVersion)
        dst.writeShort(majorVersion)
        dst.writeShort(constantPool.size)
        for (entry in constantPool) {
            writeConstantPoolEntry(dst, entry)
        }
        dst.writeShort(accessFlags)
        dst.writeShort(getConstant(clazz))
        dst.writeShort(getConstantOrNull(superClass))
        dst.writeShort(interfaces.size)
        for (entry in interfaces) {
            dst.writeShort(getConstant(entry))
        }
        dst.writeShort(fields.size)
        for (field in fields) {
            field.write(this, dst)
        }
        dst.writeShort(methods.size)
        for (method in methods) {
            method.write(this, dst)
        }
        dst.writeShort(attributes.size)
        for (attrib in attributes) {
            attrib.write(this, dst)
        }
    }

    data class ClassInfo(val name: String)
    data class FieldRef(val clazz: ClassInfo, val nameType: NameType)
    data class MethodRef(val clazz: ClassInfo, val nameType: NameType)
    data class InterfaceMethodRef(val clazz: ClassInfo, val nameType: NameType)
    data class NameType(val name: String, val type: String)

    enum class ReferenceKind(val id: Int) {
        GET_FIELD(1),
        GET_STATIC(2),
        PUT_FIELD(3),
        PUT_STATIC(4),
        INVOKE_VIRTUAL(5),
        INVOKE_STATIC(6),
        INVOKE_SPECIAL(7),
        INVOKE_NEW_SPECIAL(8),
        INVOKE_INTERFACE(9),
    }

    data class MethodHandle(val kind: ReferenceKind, val item: Any)
    data class MethodType(val descriptor: String)
    data class InvokeDynamic(val bootstrapIndex: Int, val nameType: NameType)
    data class StringConst(val value: String)

    fun readConstantPool(input: DataInputStream): List<Any?> {
        val size = input.readUnsignedShort()
        val pool = ArrayList<Any>(size)
        pool.add(Unit)
        var i = 0
        fun ref() = pool[input.readUnsignedShort()]
        while (++i < size) {
            val tag = input.read()
            val value: Any = when (tag) {
                1 -> input.readUTF()
                3 -> input.readInt()
                4 -> input.readFloat()
                5 -> input.readLong()
                6 -> input.readDouble()
                7 -> ClassInfo(ref() as String)
                8 -> StringConst(ref() as String)
                9 -> FieldRef(ref() as ClassInfo, ref() as NameType)
                10 -> MethodRef(ref() as ClassInfo, ref() as NameType)
                11 -> InterfaceMethodRef(ref() as ClassInfo, ref() as NameType)
                12 -> NameType(ref() as String, ref() as String)
                15 -> MethodHandle(ReferenceKind.entries[input.read() - 1], ref())
                16 -> MethodType(ref() as String)
                18 -> InvokeDynamic(input.readUnsignedShort(), ref() as NameType)
                else -> assertFail("Unknown tag $tag")
            }
            pool.add(value)
            if (tag == 5 || tag == 6) {
                pool.add(Unit)
                i++
            }
        }
        return pool
    }

    fun writeConstantPoolEntry(dst: DataOutputStream, v: Any?) {
        when (v) {
            Unit -> {}
            is String -> {
                dst.writeByte(1)
                dst.writeUTF(v)
            }
            is Int -> {
                dst.writeByte(3)
                dst.writeInt(v)
            }
            is Float -> {
                dst.writeByte(4)
                dst.writeFloat(v)
            }
            is Long -> {
                dst.writeByte(5)
                dst.writeLong(v)
            }
            is Double -> {
                dst.writeByte(6)
                dst.writeDouble(v)
            }
            is ClassInfo -> {
                dst.writeByte(7)
                dst.writeShort(constantMap[v.name]!!)
            }
            is StringConst -> {
                dst.writeByte(8)
                dst.writeShort(constantMap[v.value]!!)
            }
            is FieldRef -> {
                dst.writeByte(9)
                dst.writeShort(constantMap[v.clazz]!!)
                dst.writeShort(constantMap[v.nameType]!!)
            }
            is MethodRef -> {
                dst.writeByte(10)
                dst.writeShort(constantMap[v.clazz]!!)
                dst.writeShort(constantMap[v.nameType]!!)
            }
            is InterfaceMethodRef -> {
                dst.writeByte(11)
                dst.writeShort(constantMap[v.clazz]!!)
                dst.writeShort(constantMap[v.nameType]!!)
            }
            is NameType -> {
                dst.writeByte(12)
                dst.writeShort(constantMap[v.name]!!)
                dst.writeShort(constantMap[v.type]!!)
            }
            else -> assertFail("Unknown type ${v!!::class}")
        }
    }

    override fun toString(): String {
        return "version: $majorVersion.$minorVersion, flags: 0x${accessFlags.toString(16)},\n" +
                "class: $clazz, super: $superClass,\n" +
                "interfaces: ${interfaces.toList()},\n" +
                "fields: [\n\t${fields.joinToString(",\n\t")}],\n" +
                "methods: [\n\t${methods.joinToString(",\n\t")}],\n" +
                "attributes: ${attributes.toList()}"
    }
}
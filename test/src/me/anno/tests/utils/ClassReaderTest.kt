package me.anno.tests.utils

import me.anno.engine.OfficialExtensions
import me.anno.export.reflect.clazz.Clazz
import me.anno.utils.OS.desktop
import me.anno.utils.assertions.assertContentEquals
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

class ClassReaderTest {
    @Test
    fun testClassReader() {
        // todo read Java .class-files produced by Kotlin to find out about strip-able parts
        //  -> all annotations except for a few can be removed, constant-pool needs to be stripped,
        // theoretically, we could also strip not-null tests of input parameters, because we follow them ->
        //  -> was there a setting for that?
        // kotlin.reflect. shall be renamed to kotlyn.reflect for a lighter implementation
        OfficialExtensions.initForTests()
        val file = desktop.getChild("TestGame.jar/me/anno/Engine.class")
        val bytes = file.readBytesSync()
        val clazz = Clazz(DataInputStream(bytes.inputStream()))
        println(clazz)
        val writeTest = ByteArrayOutputStream(bytes.size)
        clazz.write(DataOutputStream(writeTest))
        val writeTestBytes = writeTest.toByteArray()
        assertContentEquals(bytes, writeTestBytes)
    }
}
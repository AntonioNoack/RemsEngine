package me.anno.tests.io

import me.anno.ecs.Entity
import me.anno.ecs.prefab.Prefab
import me.anno.ecs.prefab.PrefabCache
import me.anno.ecs.prefab.PrefabSaveable
import me.anno.engine.OfficialExtensions
import me.anno.engine.projects.FileEncoding
import me.anno.io.binary.BinaryReader
import me.anno.io.binary.BinaryWriter
import me.anno.io.files.InvalidRef
import me.anno.io.files.SignatureCache
import me.anno.io.files.inner.temporary.InnerTmpByteFile
import me.anno.tests.FlakyTest
import me.anno.utils.assertions.assertEquals
import me.anno.utils.assertions.assertIs
import me.anno.utils.assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream

/**
 * test for all encodings, whether they work;
 * small, general test on actually used classes
 * */
class GeneralFileEncodingTest {

    @BeforeEach
    fun init() {
        OfficialExtensions.initForTests()
    }

    @Test
    @FlakyTest
    fun testPrettyJson() {
        testEncoding(FileEncoding.PRETTY_JSON, "json")
    }

    @Test
    @FlakyTest
    fun testCompactJson() {
        testEncoding(FileEncoding.COMPACT_JSON, "json")
    }

    @Test
    @FlakyTest
    fun testPrettyXML() {
        testEncoding(FileEncoding.PRETTY_XML, "xml-re")
    }

    @Test
    @FlakyTest
    fun testCompactXML() {
        testEncoding(FileEncoding.COMPACT_XML, "xml-re")
    }

    @Test
    @FlakyTest
    fun testYAMLEncodings() {
        testEncoding(FileEncoding.YAML, "yaml-re")
    }

    @Test
    @FlakyTest
    fun testBinaryEncodings() {
        testEncoding(FileEncoding.BINARY, "rem")
    }

    @Test
    fun testBinarySerialization() {
        val srcPrefab = Prefab("Entity")
        srcPrefab["name"] = "RemsEngine"

        val bytes = ByteArrayOutputStream().use { bos ->
            val writer = BinaryWriter(bos, InvalidRef)
            writer.add(srcPrefab)
            writer.writeAllInList()
            writer.close()
            bos.toByteArray()
        }

        val reader = BinaryReader(bytes.inputStream(), InvalidRef)
        reader.readAllInList()
        assertEquals(
            listOf(srcPrefab).toString(),
            reader.allInstances.toString()
        )
    }

    fun testEncoding(encoding: FileEncoding, expectedSignature: String) {

        val workspace = InvalidRef
        val srcPrefab = Prefab("Entity")
        srcPrefab["name"] = "RemsEngine"

        val bytes = encoding.encode(listOf(srcPrefab), workspace)
        val tmpFile = InnerTmpByteFile(bytes, encoding.extension)
        println("Wrote '${bytes.decodeToString()}'")
        assertTrue(tmpFile.exists)

        val signature = SignatureCache[tmpFile].waitFor()?.name
        assertEquals(expectedSignature, signature)

        val prefab = PrefabCache[tmpFile].waitFor()!!
        val sample = prefab.sample as PrefabSaveable
        assertIs(Entity::class, sample)
        assertEquals("RemsEngine", sample.name)
    }
}
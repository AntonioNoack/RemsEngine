package me.anno.tests.files

import me.anno.engine.serialization.SerializedProperty
import me.anno.gpu.pipeline.PipelineStage
import me.anno.io.Saveable
import me.anno.io.base.BaseWriter
import me.anno.io.files.InvalidRef
import me.anno.io.json.saveable.JsonStringReader
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class EnumSerializationTest : Saveable() {
    @SerializedProperty
    var stage = PipelineStage.OPAQUE

    override fun save(writer: BaseWriter) {
        super.save(writer)
        saveSerializableProperties(writer)
    }

    override fun setProperty(name: String, value: Any?) {
        if (!setSerializableProperty(name, value)) {
            super.setProperty(name, value)
        }
    }

    @Test
    fun testEnumSerialization() {
        registerCustomClass(EnumSerializationTest::class)
        val instance = EnumSerializationTest()
        instance.stage = PipelineStage.TRANSPARENT
        val asText = instance.toString()
        assertEquals("[{\"class\":\"EnumSerializationTest\",\"i:*ptr\":1,\"i:stage\":1}]", asText)
        val clone = JsonStringReader.readFirst<EnumSerializationTest>(asText, InvalidRef)
        assertEquals(instance.stage, clone.stage)
    }
}
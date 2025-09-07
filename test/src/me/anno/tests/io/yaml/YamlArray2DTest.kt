package me.anno.tests.io.yaml

import me.anno.engine.projects.FileEncoding
import me.anno.io.files.InvalidRef
import me.anno.io.json.generic.JsonLike
import me.anno.io.saveable.Saveable
import me.anno.io.saveable.UnknownSaveable
import me.anno.tests.io.CompleteFileEncodingTest
import me.anno.utils.structures.lists.Lists.firstInstance2
import org.joml.Vector2i
import org.joml.Vector3f
import org.joml.Vector3i
import org.junit.jupiter.api.Test

/**
 * tests serialization of 1d and 2d arrays via YAML
 * */
class YamlArray2DTest {

    val instances0 = listOf(
         "x",
        Vector2i(1, 2),
        Vector3f(-.5f, 10f, 5f),
        Vector3i(1, 2, 3),
        CompleteFileEncodingTest.Circular(1),
    )

    val instances = instances0 + instances0.map { listOf(it, it) } +
        instances0.map { listOf(listOf(it, it), listOf(it, it)) }

    @Test
    fun testYamlLists() {

        val encoding = FileEncoding.YAML

        // register classes
        Saveable.Companion.registerCustomClass(UnknownSaveable())
        Saveable.Companion.registerCustomClass(CompleteFileEncodingTest.Circular())

        // prepare mega-instance will all properties
        val instance = UnknownSaveable()
        for ((idx, v) in instances.withIndex()) {
            instance.setProperty("p$idx", v)
        }

        // serialization
        val bytes = encoding.encode(instance, InvalidRef)
        println(bytes.decodeToString())
        val jsonLike = JsonLike.yamlBytesToJsonLike(bytes)
        println(jsonLike)
        println(JsonLike.jsonLikeToJson(jsonLike))

        // deserialization
        val clone = encoding.decode(bytes, InvalidRef, false)
            .firstInstance2(UnknownSaveable::class)

        // equality check
        for ((idx, v) in instances.withIndex()) {
            CompleteFileEncodingTest.checkEquals(v, clone["p$idx"])
        }
    }

    @Test
    fun test2DArray() {
        // this is valid YAML-syntax, so it should be supported
        val bytes = "" +
                " - class: ?\n" +
                "   i:*ptr: 1\n" +
                "   S[][]:p0:\n" +
                "    - 1\n" +
                "    - - 1\n" +
                "      - x\n" +
                "      - y"
        val jsonLike = JsonLike.yamlBytesToJsonLike(bytes.encodeToByteArray())
        println(jsonLike)
        println(JsonLike.jsonLikeToJson(jsonLike))
    }
}
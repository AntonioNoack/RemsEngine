package me.anno.tests.io.xml

import me.anno.ecs.prefab.change.Path
import me.anno.engine.projects.FileEncoding
import me.anno.io.files.InvalidRef
import me.anno.io.json.generic.JsonLike
import me.anno.io.saveable.Saveable
import me.anno.io.saveable.UnknownSaveable
import me.anno.tests.io.CompleteFileEncodingTest
import me.anno.utils.structures.lists.Lists.firstInstance2
import org.junit.jupiter.api.Test

/**
 * tests serialization of failing types via XML
 * */
class XmlArray2DTest {

    val p = Path.Companion.fromString("a15,hi/b7,jo/c9,test")!!
    val instances = listOf(
        'x', '0',
        charArrayOf('a', 'b'),
        listOf(charArrayOf('a', 'b'), charArrayOf('c', 'd')),
           p,
         listOf(p),
        CompleteFileEncodingTest.Circular(1),
        CompleteFileEncodingTest.Circular(2, CompleteFileEncodingTest.Circular(3)),
    )

    @Test
    fun testXmlLists() {

        val encoding = FileEncoding.PRETTY_XML

        // register classes
        Saveable.Companion.registerCustomClass(UnknownSaveable())
        Saveable.Companion.registerCustomClass(CompleteFileEncodingTest.Circular())
        Saveable.Companion.registerCustomClass(Path())

        // prepare mega-instance will all properties
        val instance = UnknownSaveable()
        for ((idx, v) in instances.withIndex()) {
            instance.setProperty("p$idx", v)
        }

        // serialization
        val bytes = encoding.encode(instance, InvalidRef)
        println(bytes.decodeToString())
        val jsonLike = JsonLike.xmlBytesToJsonLike(bytes)
        println(jsonLike)
        println(JsonLike.jsonLikeToJson(jsonLike))

        // deserialization
        val clone = encoding.decode(bytes, InvalidRef, false)
            .firstInstance2(UnknownSaveable::class)

        // equality check
        for ((idx, v) in instances.withIndex()) {
            CompleteFileEncodingTest.Companion.checkEquals(v, clone["p$idx"])
        }
    }
}
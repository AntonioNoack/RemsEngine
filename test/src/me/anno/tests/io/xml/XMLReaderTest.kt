package me.anno.tests.io.xml

import me.anno.io.xml.generic.XMLNode
import me.anno.io.xml.generic.XMLReader
import me.anno.io.xml.generic.XMLScanner
import me.anno.io.xml.generic.XMLWriter
import me.anno.utils.assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.Reader

// todo test escaping characters
class XMLReaderTest {

    class ScannerToXMLNode : XMLScanner {

        private val nodes = ArrayList<XMLNode>()
        private var lastNode: XMLNode? = null

        fun read(input: Reader): XMLNode {
            scan(input)
            return lastNode!!
        }

        override fun onStart(depth: Int, type: CharSequence): Boolean {
            val newNode = XMLNode(type.toString())
            if (nodes.isNotEmpty()) addToLastNode(newNode)
            nodes.add(newNode)
            assertEquals(depth + 1, nodes.size)
            return true
        }

        private fun addToLastNode(value: Any) {
            nodes.last().children.add(value)
        }

        override fun onEnd(depth: Int, type: CharSequence) {
            assertEquals(depth, nodes.lastIndex)
            val node = nodes.removeAt(depth)
            lastNode = node
            node.children // just initialize it for easier comparability
        }

        override fun onAttribute(depth: Int, type: CharSequence, key: CharSequence, value: CharSequence) {
            nodes.last()[key.toString()] = value.toString()
            assertEquals(depth + 1, nodes.size)
        }

        override fun onContent(depth: Int, type: CharSequence, value: CharSequence) {
            addToLastNode(value.toString())
            assertEquals(depth + 1, nodes.size)
        }
    }

    private fun createBaseline(): XMLNode {
        val baseline = XMLNode("html")
        val child1 = XMLNode("a")
        child1.attributes["key1"] = "value1"
        val child2 = XMLNode("b").apply {
            children.add(XMLNode("n"))
        }
        baseline.children.add(child1)
        baseline.children.add("test")
        baseline.children.add(child2)
        return baseline
    }

    @Test
    fun testXMLNodeToString() {
        val expected = """
            <?xml version="1.0" encoding="utf-8"?>
            <html>
              <a key1="value1">
              </a>
              test
              <b>
                <n>
                </n>
              </b>
            </html>
        """.trimIndent()
        assertEquals(expected, formatXML(createBaseline()))
    }

    @Test
    fun testXMLScanner() {
        val baseline = createBaseline()
        val xml = formatXML(baseline)
        val copy = ScannerToXMLNode().read(xml.reader())
        assertEquals(baseline, copy)
    }

    @Test
    fun testXMLReader() {
        val baseline = createBaseline()
        val copy = XMLReader(formatXML(baseline).reader()).readXMLNode()
        assertEquals(baseline, copy)
    }

    @Test
    fun testReaderCData() {
        val data = "Hello World!<&>"
        val baseline = XMLNode("p")
        baseline.children.add(data)
        val formatted = "<p><![cdata[$data]]></p>"
        val copy = XMLReader(formatted.reader()).readXMLNode()
        assertEquals(baseline, copy)
    }

    @Test
    fun testScannerCData() {
        val data = "Hello World!<&>"
        val baseline = XMLNode("p")
        baseline.children.add(data)
        val formatted = "<p><![cdata[$data]]></p>"
        val copy = ScannerToXMLNode().read(formatted.reader())
        assertEquals(baseline, copy)
    }

    @Test
    fun testReaderCDataEarlyEnd() {
        val data = "Hello World!<&>"
        val baseline = XMLNode("p")
        baseline.children.add(data)
        val formatted = "<p><![cdata[$data"
        val copy = XMLReader(formatted.reader()).readXMLNode()
        assertEquals(baseline, copy)
    }

    @Test
    fun testScannerCDataEarlyEnd() {
        val data = "Hello World!<&>"
        val baseline = XMLNode("p")
        baseline.children.add(data)
        val formatted = "<p><![cdata[$data"
        val copy = ScannerToXMLNode().read(formatted.reader())
        assertEquals(baseline, copy)
    }

    @Test
    fun testReaderSkipComment() {
        val input = """
            <!-- this is a first comment -->
            <p>
            <!--
            this is a second comment
            -->
            content
            </p>
        """.trimIndent()
        val output = "<p>content</p>"
        val inputParsed = XMLReader(input.reader()).readXMLNode()
        val outputParsed = XMLReader(output.reader()).readXMLNode()
        assertEquals(outputParsed, inputParsed)
    }

    @Test
    fun testScannerSkipComment() {
        val input = """
            <!-- this is a first comment -->
            <p>
            <!--
            this is a second comment
            -->
            content
            </p>
        """.trimIndent()
        val output = "<p>content</p>"
        val inputParsed = XMLReader(input.reader()).readXMLNode()
        val outputParsed = XMLReader(output.reader()).readXMLNode()
        assertEquals(outputParsed, inputParsed)
    }

    @Test
    fun testReaderSkipDocType() {
        val input = """
            <!docType someType
            [
            <!entity idk "more content">
            <!entity idk "more content">
            ]>
            <p>content</p>
        """.trimIndent()
        val output = "<p>content</p>"
        val inputParsed = XMLReader(input.reader()).readXMLNode()
        val outputParsed = XMLReader(output.reader()).readXMLNode()
        assertEquals(outputParsed, inputParsed)
    }

    @Test
    fun testScannerSkipDocType() {
        val input = """
            <!docType someType
            [
            <!entity idk "more content">
            <!entity idk "more content">
            ]>
            <p>content</p>
        """.trimIndent()
        val output = "<p>content</p>"
        val inputParsed = XMLReader(input.reader()).readXMLNode()
        val outputParsed = XMLReader(output.reader()).readXMLNode()
        assertEquals(outputParsed, inputParsed)
    }

    @Test
    fun testReaderSkipVersion() {
        val input = """
            <?xml version="1.0"?>
            <p>content</p>
        """.trimIndent()
        val output = "<p>content</p>"
        val inputParsed = XMLReader(input.reader()).readXMLNode()
        val outputParsed = XMLReader(output.reader()).readXMLNode()
        assertEquals(outputParsed, inputParsed)
    }

    @Test
    fun testScannerSkipVersion() {
        val input = """
            <?xml version="1.0"?>
            <p>content</p>
        """.trimIndent()
        val output = "<p>content</p>"
        val inputParsed = XMLReader(input.reader()).readXMLNode()
        val outputParsed = XMLReader(output.reader()).readXMLNode()
        assertEquals(outputParsed, inputParsed)
    }

    fun formatXML(node: XMLNode): String {
        return XMLWriter.write(node)
    }
}
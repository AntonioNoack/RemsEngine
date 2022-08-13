package me.anno.tests

import me.anno.engine.ECSRegistry
import me.anno.io.files.InvalidRef
import me.anno.io.json.JsonFormatter
import me.anno.io.text.TextReader
import me.anno.io.text.TextWriter
import me.anno.utils.LOGGER

fun main() {

    // issue: there are multiple instances of the same path
    // solution: hashCode() was incorrect, because IntArray.hashCode() is reference specific, not content specific
    // solution: using IntArray.contentHashCode() / Arrays.contentHashCode(array)

    val raw = """
        [{
        		"class": "Prefab",
        		"i:*ptr": 1,
        		"R:prefab": "Scene.prefab",
        		"S:className": "Entity",
        		"CAdd[]:adds": [2, {
        				"Path:path": {
        					"S:v": "e0,Globally Shared"
        				},
        				"c:type": 'c',
        				"S:name": "Canvas Component",
        				"S:className": "CanvasComponent"
        			}, {
        				"Path:path": {
        					"i:*ptr": 5,
        					"S:v": "e0,Globally Shared/c0,Canvas Component"
        				},
        				"c:type": 'c',
        				"S:name": "Text Panel",
        				"S:className": "TextPanel"
        			}
        		],
        		"CSet[]:sets": [3, {
        				"Path:path": {
        					"S:v": "e0,Globally Shared/c0,Canvas Component/p0,Text Panel"
        				},
        				"i:backgroundColor": -5352967
        			}, {
        				"Path:path": {
        					"i:*ptr": 9,
        					"S:v": "e0,Globally Shared/c0,Canvas Component"
        				},
        				"i:width": 400
        			}, {
        				"Path:path": {
        					"S:v": "e0,Globally Shared/c0,Canvas Component"
        				},
        				"i:width": 401
        			}
        		]
        	}
        ]
    """.trimIndent()

    ECSRegistry.init()

    val asObject = TextReader.read(raw, InvalidRef, false).first()
    val asString = TextWriter.toText(asObject, InvalidRef)

    LOGGER.info(JsonFormatter.format(asString))

}
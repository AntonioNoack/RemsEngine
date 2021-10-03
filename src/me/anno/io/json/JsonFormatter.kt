package me.anno.io.json

object JsonFormatter {

    @Throws(IndexOutOfBoundsException::class)
    fun format(str: String, spaces: Int = 2, spaceChar: Char = ' ', maxCompressedEnds: Int = 1): String {

        val size = str.length
        val sizeGuess = size * 2
        val res = StringBuilder(sizeGuess)

        var depth = 0
        fun breakLine() {
            if (maxCompressedEnds > 1 && (res.endsWith(']') || res.endsWith('}'))) {
                // remove 2 tabs at the start of this last line
                val startOfLastLine = res.lastIndexOf('\n') + 1
                val endsOfTabs = res.lastIndexOf(spaceChar)
                if (res.length - endsOfTabs <= maxCompressedEnds) {
                    var done = 0
                    for (j in startOfLastLine until res.length) {
                        if (res[j] == spaceChar) {
                            res.replace(j, j + 1, "")
                            // correct?? mmh...
                            // I think we should subtract j by one...
                            done++
                            if (done > spaces) break
                        } else break
                    }
                    return
                }// else would compress to strictly
            }
            if (!res.endsWith("},")) {
                res.append('\n')
                for (j in 0 until spaces * depth) {
                    res.append(spaceChar)
                }
            } else res.append(spaceChar)
        }

        var i = 0
        while (i < size) {
            when (val char = str[i++]) {
                '[', '{' -> {
                    // todo if ends with ]/} and spaces, just append it there
                    res.append(char)
                    depth++
                    breakLine()
                }
                ']', '}' -> {
                    depth--
                    breakLine()
                    res.append(char)
                }
                ' ', '\t', '\r' -> {
                } // skip, done automatically
                ':' -> res.append(": ")
                '"' -> {
                    res.append('"')
                    while (i < size) {
                        when (val c2 = str[i++]) {
                            '"' -> break
                            // just skip the next '"', could throw an IndexOutOfBoundsException
                            '\\' -> res.append(str[i++])
                            else -> res.append(c2)
                        }
                    }
                    res.append('"')
                }
                ',' -> {
                    res.append(char)
                    breakLine()
                }
                else -> res.append(char)
            }
        }

        return res.toString()

    }

    @JvmStatic
    fun main(args: Array<String>) {

        val src =
            "[{\"class\":\"Prefab\",\"i:*ptr\":1,\"S:className\":\"Entity\",\"CAdd[]:adds\":[7,{\"c:type\":'c',\"S:name\":\"BoxCollider\"," +
                    "\"S:className\":\"BoxCollider\"},{\"c:type\":'c',\"S:name\":\"BulletPhysics\",\"S:className\":\"BulletPhysics\"},{\"c:type\":'c'," +
                    "\"S:name\":\"Rigidbody\",\"S:className\":\"Rigidbody\"},{\"c:type\":'c',\"S:name\":\"SliderConstraint\",\"S:className\":\"SliderConstraint\"}," +
                    "{\"c:type\":'e',\"S:name\":\"Entity\",\"S:className\":\"Entity\"},{\"Path:path\":{\"S:v\":\"e0,Entity\"},\"c:type\":'c',\"S:name\":\"BoxCollider\"," +
                    "\"S:className\":\"BoxCollider\"},{\"Path:path\":{\"S:v\":\"e0,Entity\"},\"c:type\":'c',\"S:name\":\"Rigidbody\",\"S:className\":\"Rigidbody\"}]," +
                    "\"CSet[]:sets\":[18,{\"i:*ptr\":11,\"Path:path\":{\"i:*ptr\":12,\"S:v\":\"c2,Rigidbody\"},\"b:overrideGravity\":true},{\"Path:path\":{\"S:v\":\"e0,Entity\"}," +
                    "\"v3d:position\":[-2.4744907802625904,0.018504960234546175,-1.7895318679968293]},{\"Path:path\":{\"S:v\":\"c3,SliderConstraint\"},\"Path:other\":{\"S:v\":" +
                    "\"e0,Entity/c1,Rigidbody\"}},{\"Path:path\":{\"S:v\":\"e0,Entity/c1,Rigidbody\"},\"b:overrideGravity\":true},{\"Path:path\":{\"S:v\":\"e0,Entity\"},\"b:isCollapsed\":false}," +
                    "{\"v3d:gravity\":[10,0,0]},{\"b:isEnabled\":true},{\"q4d:rotation\":[0,0,0,1]},{\"v3d:scale\":[1]},{\"S:name\":\"Colliders\"},{\"b:isCollapsed\":false}," +
                    "{\"v3d:position\":[0.9965685024139616,-4.998401793503934,3.8464554916186247]},{\"Path:path\":{\"S:v\":\"c1,BulletPhysics\"},\"d:automaticDeathHeight\":-5}," +
                    "{\"Path:path\":{\"S:v\":\"c2,Rigidbody\"},\"d:mass\":1},{\"Path:path\":32,\"v3d:gravity\":[0,1,0]},{\"Path:path\":{\"S:v\":\"e0,Entity\"},\"v3d:position\":" +
                    "[-1.8121681560740845,-0.022380086565996216,-2.405396522738568]},{\"Path:path\":35,\"q4d:rotation\":[0,0,0,1]},{\"Path:path\":35,\"v3d:scale\":[1]}]}]"
        println(format(src))

    }

}
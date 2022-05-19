package me.anno.ecs.components.anim

import me.anno.cache.CacheData
import me.anno.cache.CacheSection
import me.anno.language.embeddings.WordEmbedding

object BoneEmbeddings {

    val helperWE = WordEmbedding()
    private val helperWECache = CacheSection("RetargetingWordEmbeddings")

    init {

        // create/define all word embeddings
        // for semantic, automatic bone name mapping
        helperWE.apply {

            // used indices:
            //   0- 3 sides, root
            //   4- 7 arm - finger
            //   8-12 thumb, index, middle, ring, pinky
            //  13-21 head - toe
            //  22-31 numbers

            register("left", 0, +1f)
            register(".l", 0, +1f)
            register("/l", 0, +1f)
            register("right", 0, -1f)
            register("/r", 0, -1f)
            register(".r", 0, -1f)

            register("upper", 1, +1f)
            register("up", 1, +1f)
            register("lower", 1, -1f)
            register("low", 1, -1f)

            register("root", 2)

            register("front", 3, +1f)
            register("back", 3, -1f)

            // 13-24
            registerChain(
                listOf(
                    // 13-15
                    "head", "neck", "shoulder",
                    // 16-19
                    "chest", "spine", "hips", "pelvis",
                    // 20-24
                    "thigh", "shin", "foot", "ankle", "toe"
                ), 13
            )

            similar("foot", "toe")
            synonym("breast", "chest")
            synonym("ball", "ankle")
            synonym("femur", "thigh")
            synonym("leg", "shin") // in the mixamo example
            synonym("patella", "shin")
            synonym("tibia", "shin")
            synonym("skull", "head")
            synonym("sternum", "chest")
            synonym("spinal", "spine")
            synonym("vertebrae", "spine")

            registerChain(
                // 4-7
                listOf("arm", "forearm", "hand", "finger"),
                4
            )

            register("thumb", 8)
            register("index", 9)
            register("middle", 10)
            synonym("mid", "middle")
            register("ring", 11)
            register("pinky", 12)

            similar("finger", "thumb", 0.2f)
            similar("finger", "index", 0.2f)
            similar("finger", "middle", 0.2f)
            similar("finger", "ring", 0.2f)
            similar("finger", "pinky", 0.2f)

            synonym("humerus", "arm")
            synonym("ulna", "forearm")
            synonym("radius", "forearm")

            similar("shoulder", "arm")

            register2("elbow", mapOf("arm" to 1f, "forearm" to 0.7f))
            register2("knee", mapOf("thigh" to 0.7f, "shin" to 1f))

            // 24-33
            registerChain(listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9"), 25)

            val s = 0.15f
            similar("1", "thumb", s)
            similar("2", "index", s)
            similar("3", "middle", s)
            similar("4", "ring", s)
            similar("5", "pinky", s)

            // nearly the same as shoulder
            // define slightly different name for it
            clone("clavicle", "shoulder")
            similar("1", "clavicle")
            similar("2", "shoulder")

            normalize()

        }

    }

    fun getWEs(skeleton: Skeleton): List<FloatArray?> {
        val data = helperWECache.getEntry(skeleton, 10_000L, false) {
            CacheData(skeleton.bones.map { calcWE(it.name) })
        } as CacheData<*>
        val value = data.value
        @Suppress("unchecked_cast")
        return value as List<FloatArray?>
    }

    fun calcWE(name: String): FloatArray? {
        // extract base names like "leg", "pelvis", ...
        val lcName = "/${name.lowercase()}/" // slashes as end markers
        var embedding: FloatArray? = null
        var hits = 0
        for ((key, value) in helperWE.values) {
            if (lcName.contains(key)) {
                if (embedding == null) embedding = FloatArray(value.size)
                embedding = helperWE.add(embedding, value)
                hits++
            }
        }
        if (hits > 1) {
            helperWE.scale(embedding!!, 1f / hits)
        }
        return embedding
    }

}
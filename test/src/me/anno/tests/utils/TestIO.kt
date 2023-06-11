package me.anno.tests.utils

import me.anno.io.config.ConfigBasics
import me.anno.io.files.FileReference.Companion.getReference
import me.anno.io.files.InvalidRef
import me.anno.io.utils.StringMap
import me.anno.utils.OS
import org.apache.commons.compress.archivers.zip.ZipFile
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel
import org.apache.logging.log4j.LogManager

fun main() {

    val logger = LogManager.getLogger("TestIO")

    /*// in this test with a homogenous array,
    // binary is 1.5x as efficient as text
    val logger = LogManager.getLogger()
    val ap = AnimatedProperty.float()
    ap.isAnimated = true
    for(i in 0 until 512){
        ap.addKeyframe(i.toDouble(), i.toFloat())
    }
    val bos = ByteArrayOutputStream()
    use(DataOutputStream(DeflaterOutputStream(bos))){
        val writer = BinaryWriter(it)
        writer.add(ap)
        writer.writeAllInList()
    }
    bos.close()
    val bytes = bos.toByteArray()
    logger.info("when binary is compressed: ${bytes.size}")
    val reader = BinaryReader(DataInputStream(InflaterInputStream(ByteArrayInputStream(bytes))))
    reader.readAllInList()
    val content = reader.sortedContent
    logger.info("content: $content")
    val bos2 = ByteArrayOutputStream()
    use(DeflaterOutputStream(bos2)){
        it.write(TextWriter.toText(content,false).toByteArray())
    }
    logger.info("when text is compressed: ${bos2.toByteArray().size}")*/

    // a special file, which does not work as a stream using the default ZipInputStream (only DEFLATED entries can have EXT descriptor)
    @Suppress("SpellCheckingInspection")
    val ref = getReference(OS.downloads, "Stone_Wall_uljlcdwew_4K_surface_ms.zip")
    logger.info(ref.isSomeKindOfDirectory)

    val zis2 = ZipFile(SeekableInMemoryByteChannel(ref.readBytesSync()))
    val entries = zis2.entries
    while (entries.hasMoreElements()) {
        val next = entries.nextElement()
        logger.info(next.name)
        logger.info(zis2)
    }

    // crashes
    /*val zis = ZipInputStream(ref.inputStream())
    while(true){
        val entry = zis.nextEntry ?: break
        LOGGER.info(entry.name)
    }*/

}

@Suppress("unused")
fun testConfig() {
    val logger = LogManager.getLogger("TestConfig")
    val configDefaults = StringMap()
    configDefaults["key.a"] = 17
    configDefaults["key.float"] = 11f / 3f
    configDefaults["key.double"] = 11.0 / 3.0
    configDefaults["key.string"] = "yes!"
    configDefaults["key.config"] = StringMap()
    logger.info("default: $configDefaults")
    val config = ConfigBasics.loadConfig("test.config", InvalidRef, configDefaults, true)
    logger.info("loaded: $config")
}
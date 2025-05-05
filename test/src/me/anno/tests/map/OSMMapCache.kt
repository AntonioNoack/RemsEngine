package me.anno.tests.map

import me.anno.cache.AsyncCacheData
import me.anno.cache.CacheSection
import me.anno.graph.hdb.ByteSlice
import me.anno.graph.hdb.HierarchicalDatabase
import me.anno.io.Streams.readText
import me.anno.io.Streams.writeString
import me.anno.io.config.ConfigBasics
import me.anno.io.files.WebRef.Companion.encodeURIComponent
import me.anno.maths.Maths.min
import me.anno.tests.map.OSMReaderV2.readOSM2
import me.anno.utils.files.Files.formatFileSize
import me.anno.utils.types.Floats.toLongOr
import org.joml.AABBd
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Semaphore
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log2
import kotlin.math.pow
import kotlin.math.round

object OSMMapCache : CacheSection("OSMMapData") {

    data class OSMChunkKey(val xi: Long, val yi: Long, val level: Int)

    val MAIN_URL = "https://overpass-api.de/api/interpreter"

    val limit = Semaphore(1)
    val apiLimitMillis = 500L

    val hdb = HierarchicalDatabase(
        "OSMMapData", ConfigBasics.cacheFolder.getChild("OSMap"),
        20_000_000, 10_000, 7 * 24 * 3600 * 1000
    )

    fun getMapData(bounds: AABBd, key: OSMChunkKey, async: Boolean): OSMap? {
        val query = "" +
                "[bbox:${bounds.minY},${bounds.minX},${bounds.maxY},${bounds.maxX}]\n" +
                "[out:xml]\n" +
                "[timeout:90]\n" + // seconds
                ";\n" +
                "way(${bounds.minY},${bounds.minX},${bounds.maxY},${bounds.maxX});\n" +
                "node(${bounds.minY},${bounds.minX},${bounds.maxY},${bounds.maxX});\n" +
                "relation(${bounds.minY},${bounds.minX},${bounds.maxY},${bounds.maxX});\n" +
                "out geom;"
        val result = getEntry(key, 10_000, async) { key1 ->
            val result = AsyncCacheData<OSMap>()
            val path = listOf(key1.xi.toString(), key1.yi.toString(), key1.level.toString())
            val hash = query.hashCode().toLong().and(0xffffffff)
            hdb.get(path, hash) { bytes, _ ->
                result.value = if (bytes != null) {
                    readOSM2(bytes.stream())
                } else {
                    startDownloading(query, path, hash)
                }
            }
            result
        }
        if (!async && result != null) result.waitFor()
        return result?.value
    }

    private fun startDownloading(query: String, path: List<String>, hash: Long): OSMap? {
        limit.acquire()
        val con = buildRequest(query)
        return if (con.responseCode == 200) {
            val bytes = con.inputStream.use { it.readBytes() }
            val result = readOSM2(ByteArrayInputStream(bytes))
            hdb.put(path, hash, ByteSlice(bytes))
            println("made successful request, ${bytes.size.formatFileSize()}")
            Thread.sleep(apiLimitMillis)
            limit.release()
            result
        } else {
            println("Illegal response code: ${con.responseCode}")
            println(con.errorStream.readText())
            Thread.sleep(apiLimitMillis)
            limit.release()
            null
        }
    }

    private fun buildRequest(query: String): HttpURLConnection {
        val con = URL(MAIN_URL).openConnection() as HttpURLConnection
        con.doOutput = true
        con.outputStream.writeString("data=${encodeURIComponent(query)}")
        return con
    }

    fun getMapData(bounds: AABBd, async: Boolean): List<Pair<AABBd, OSMap>> {
        val dx = min(bounds.deltaX, bounds.deltaY)
        val level = round(log2(dx))
        val levelI = level.toInt()
        // calculate min/max positions
        val di = 2.0.pow(level)
        val x0 = floor(bounds.minX / di).toLongOr()
        val y0 = floor(bounds.minY / di).toLongOr()
        val x1 = ceil(bounds.maxX / di).toLongOr()
        val y1 = ceil(bounds.maxY / di).toLongOr()
        val result = ArrayList<Pair<AABBd, OSMap>>(((x1 - x0) * (y1 - y0)).toInt())
        for (yi in y0 until y1) {
            for (xi in x0 until x1) {
                val x = xi * di
                val y = yi * di
                val boundsI = AABBd(x, y, 0.0, x + di, y + di, 0.0)
                val piece = getMapData(boundsI, OSMChunkKey(xi, yi, levelI), async)
                if (piece != null) {
                    // println("got piece :)")
                    result.add(boundsI to piece)
                }// else println("no piece :/")
            }
        }
        return result
    }
}

package me.anno.mesh.jmonkey

import com.jme3.asset.AssetInfo
import com.jme3.asset.AssetKey
import com.jme3.asset.AssetLocator
import com.jme3.asset.AssetManager
import org.apache.logging.log4j.LogManager
import java.io.File
import java.io.InputStream

class CustomLocator : AssetLocator {

    companion object {
        private val LOGGER = LogManager.getLogger(CustomLocator::class.java)
    }

    var root: String? = ""
    override fun setRootPath(rootPath: String?) {
        LOGGER.info("Setting root: '$rootPath'")
        root = rootPath
    }

    override fun locate(manager: AssetManager, key: AssetKey<*>): AssetInfo {
        return object : AssetInfo(manager, key) {
            override fun openStream(): InputStream? {
                LOGGER.info("'$key', '${key.name}', '${key.extension}', '${key.folder}'")
                val file = File(key.toString())
                return if (file.exists()) {
                    file.inputStream().buffered()
                } else null
            }
        }
    }

}
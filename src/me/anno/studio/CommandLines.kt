package me.anno.studio

import org.apache.commons.cli.CommandLine
import org.apache.logging.log4j.LogManager

/**
 * utility functions for command line applications based on Apache CLI;
 * sample using it: https://github.com/AntonioNoack/RemsStudio/blob/master/src/me/anno/remsstudio/cli/RemsCLI.kt
 * */
@Suppress("unused")
object CommandLines {

    fun warn(name: String, value: String, defaultValue: Any) {
        LOGGER.warn("Could not parse $name '$value', using default value $defaultValue")
    }

    fun CommandLine.parseInt(name: String, defaultValue: Int): Int {
        return if (hasOption(name)) {
            val value = getOptionValue(name)
            value.toIntOrNull() ?: run {
                warn(name, value, defaultValue)
                defaultValue
            }
        } else defaultValue
    }

    fun CommandLine.parseLong(name: String, defaultValue: Long): Long {
        return if (hasOption(name)) {
            val value = getOptionValue(name)
            value.toLongOrNull() ?: run {
                warn(name, value, defaultValue)
                defaultValue
            }
        } else defaultValue
    }

    fun CommandLine.parseFloat(name: String, defaultValue: Float): Float {
        return if (hasOption(name)) {
            val value = getOptionValue(name)
            value.toFloatOrNull() ?: run {
                warn(name, value, defaultValue)
                defaultValue
            }
        } else defaultValue
    }

    fun CommandLine.parseDouble(name: String, defaultValue: Double): Double {
        return if (hasOption(name)) {
            val value = getOptionValue(name)
            value.toDoubleOrNull() ?: run {
                warn(name, value, defaultValue)
                defaultValue
            }
        } else defaultValue
    }

    private val LOGGER = LogManager.getLogger(CommandLines::class)

}
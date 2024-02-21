package me.anno.utils

import org.apache.commons.cli.CommandLine
import org.apache.logging.log4j.LogManager

/**
 * utility functions for command line applications based on Apache CLI;
 * sample using it: https://github.com/AntonioNoack/RemsStudio/blob/master/src/me/anno/remsstudio/cli/RemsCLI.kt
 * */
@Suppress("unused")
object CommandLineUtils {
    private val LOGGER = LogManager.getLogger(CommandLineUtils::class)

    fun <V : Any> CommandLine.parseAny(name: String, defaultValue: V, parseValue: (String) -> V?): V {
        return if (hasOption(name)) {
            val value = getOptionValue(name)
            parseValue(value) ?: run {
                LOGGER.warn("Could not parse $name '$value', using default value $defaultValue")
                defaultValue
            }
        } else defaultValue
    }

    fun CommandLine.parseInt(name: String, defaultValue: Int): Int {
        return parseAny(name, defaultValue) { it.toIntOrNull() }
    }

    fun CommandLine.parseLong(name: String, defaultValue: Long): Long {
        return parseAny(name, defaultValue) { it.toLongOrNull() }
    }

    fun CommandLine.parseFloat(name: String, defaultValue: Float): Float {
        return parseAny(name, defaultValue) { it.toFloatOrNull() }
    }

    fun CommandLine.parseDouble(name: String, defaultValue: Double): Double {
        return parseAny(name, defaultValue) { it.toDoubleOrNull() }
    }
}
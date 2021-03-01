package me.anno.studio.cli

import org.apache.commons.cli.CommandLine
import org.apache.logging.log4j.LogManager

object CommandLines {

    fun warn(name: String, value: String, defaultValue: Any) {
        LOGGER.warn("Could not parse $name '$value', using default value $defaultValue")
    }

    fun CommandLine.parseInt(name: String, defaultValue: Int): Int {
        return if (hasOption(name)) {
            val stringValue = getOptionValue(name)
            val value = stringValue.toIntOrNull()
            if (value == null) {
                warn(name, stringValue, defaultValue)
                defaultValue
            } else value
        } else defaultValue
    }

    fun CommandLine.parseLong(name: String, defaultValue: Long): Long {
        return if (hasOption(name)) {
            val stringValue = getOptionValue(name)
            val value = stringValue.toLongOrNull()
            if (value == null) {
                warn(name, stringValue, defaultValue)
                defaultValue
            } else value
        } else defaultValue
    }

    fun CommandLine.parseFloat(name: String, defaultValue: Float): Float {
        return if (hasOption(name)) {
            val stringValue = getOptionValue(name)
            val value = stringValue.toFloatOrNull()
            if (value == null) {
                warn(name, stringValue, defaultValue)
                defaultValue
            } else value
        } else defaultValue
    }

    fun CommandLine.parseDouble(name: String, defaultValue: Double): Double {
        return if (hasOption(name)) {
            val stringValue = getOptionValue(name)
            val value = stringValue.toDoubleOrNull()
            if (value == null) {
                warn(name, stringValue, defaultValue)
                defaultValue
            } else value
        } else defaultValue
    }

    private val LOGGER = LogManager.getLogger(CommandLines::class)

}
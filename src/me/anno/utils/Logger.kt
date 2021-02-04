package me.anno.utils

import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

/**
 * should only be used by tests, which are too lazy to create their own LOGGER
 * */
val LOGGER: Logger = LogManager.getLogger("Utils")
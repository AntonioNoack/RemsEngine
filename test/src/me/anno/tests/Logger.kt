package me.anno.tests

import org.apache.logging.log4j.LogManager

/**
 * should only be used by tests, which are too lazy to create their own LOGGER
 * */
@JvmField
val LOGGER = LogManager.getLogger("Tests")
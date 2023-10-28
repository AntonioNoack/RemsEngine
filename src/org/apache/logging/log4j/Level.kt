package org.apache.logging.log4j

enum class Level(val value: Int) {
    OFF(Int.MAX_VALUE),
    FATAL(2000),
    ERROR(1500),
    SEVERE(1000),
    WARN(900),
    INFO(800),
    DEBUG(500),
    TRACE(300),
    ALL(Int.MIN_VALUE);
}
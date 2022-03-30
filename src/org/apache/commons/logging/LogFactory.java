package org.apache.commons.logging;

import org.apache.logging.log4j.LogManager;

/**
 * simple wrapper to join all loggers from all packages
 */
public class LogFactory {

    public static Log getLog(String clazz) throws LogConfigurationException {
        return LogManager.getLogger(clazz);
    }

    public static Log getLog(Class<?> clazz) throws LogConfigurationException {
        return LogManager.getLogger(clazz);
    }

}

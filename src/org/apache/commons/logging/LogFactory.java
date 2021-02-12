package org.apache.commons.logging;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.LoggerImpl;

public class LogFactory {

    public static Log getLog(Class<?> clazz) throws LogConfigurationException {
        return (LoggerImpl) LogManager.getLogger(clazz);
    }

}

package org.apache.logging.log4j;

import kotlin.reflect.KClass;

import java.util.HashMap;

/**
 * the main logging manager, which should be used
 * */
public class LogManager {

    private static final Logger logger = new LoggerImpl(null);
    private static final HashMap<String, Logger> loggers = new HashMap<>();

    public static Logger getLogger(){
        return logger;
    }

    public static Logger getLogger(Class<?> clazz){
        return getLogger(clazz.getSimpleName());
    }

    public static Logger getLogger(KClass<?> clazz){
        return getLogger(clazz.getSimpleName());
    }

    public static Logger getLogger(String name){
        if(name == null) return logger;
        Logger logger = loggers.get(name);
        if(logger == null){
            logger = new LoggerImpl(name);
            loggers.put(name, logger);
        }
        return logger;
    }

}

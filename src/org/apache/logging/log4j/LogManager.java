package org.apache.logging.log4j;

import kotlin.reflect.KClass;

import java.util.HashMap;
import java.util.HashSet;

/**
 * the main logging manager, which should be used
 * */
public class LogManager {

    private static final HashSet<String> disabled = new HashSet<>();

    public static boolean isEnabled(LoggerImpl logger){
        return !disabled.contains(logger.getPrefix());
    }

    public static void disableLogger(String logger){
        disabled.add(logger);
    }

    public static void enableLogger(String logger){
        disabled.remove(logger);
    }

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

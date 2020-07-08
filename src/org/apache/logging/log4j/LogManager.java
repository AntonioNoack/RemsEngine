package org.apache.logging.log4j;

import java.util.HashMap;

public class LogManager {

    private static final Logger logger = new LoggerImpl(null);
    private static final HashMap<String, Logger> loggers = new HashMap<>();

    public static Logger getLogger(){
        return logger;
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

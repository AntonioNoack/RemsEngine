package org.apache.logging.log4j;

public class LogManager {
    private static Logger logger = new LoggerImpl();
    public static Logger getLogger(){
        return logger;
    }
}

package org.lwjgl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LWJGLUtil {
    private static Logger LOGGER = LogManager.getLogger(LWJGLUtil.class);
    public static void log(CharSequence msg){
        LOGGER.info(msg.toString());
    }
    public static void log(String msg){
        LOGGER.info(msg);
    }
}

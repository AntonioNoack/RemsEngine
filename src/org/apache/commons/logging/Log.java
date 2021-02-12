
package org.apache.commons.logging;

public interface Log {
    void debug(Object o);

    void debug(Object o, Throwable throwable);

    void error(Object o);

    void error(Object o, Throwable throwable);

    void fatal(Object o);

    void fatal(Object o, Throwable throwable);

    void info(Object o);

    void info(Object o, Throwable throwable);

    boolean isDebugEnabled();

    boolean isErrorEnabled();

    boolean isFatalEnabled();

    boolean isInfoEnabled();

    boolean isTraceEnabled();

    boolean isWarnEnabled();

    void trace(Object o);

    void trace(Object o, Throwable throwable);

    void warn(Object o);

    void warn(Object o, Throwable throwable);
}

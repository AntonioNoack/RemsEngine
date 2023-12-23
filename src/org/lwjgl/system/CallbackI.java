package org.lwjgl.system;

import org.lwjgl.system.libffi.FFICIF;
import org.lwjgl.system.libffi.LibFFI;

/**
 * This class is a chimera between LWJGL 3.2.3 and LWJGL 3.3.2 to make Assimp 4.1 work with the newest LWJGL.
 * If you update LWJGL, just compile this file separately, and copy it into the new jar at the right place.
 */
@SuppressWarnings("unused")
public interface CallbackI extends Pointer {

    /**
     * Returns the libffi Call Interface for this callback function. [INTERNAL API]
     *
     * @return the CIF structure for this callback function
     */
    default FFICIF getCallInterface() {
        return null;
    }

    @Override
    default long address() {
        FFICIF ci = getCallInterface();
        if (ci == null) return 0L;
        return Callback.create(ci, this);
    }

    /**
     * Returns the dyncall signature for this callback function. [INTERNAL API]
     *
     * @return the dyncall signature
     */
    default String getSignature() {
        return "";
    }

    /**
     * The Java method that will be called from native code when the native callback function is invoked.
     *
     * @param ret  a pointer to the memory used for the function's return value.
     *
     *             <p>If the function is declared as returning {@code void}, then this value is garbage and should not be used.</p>
     *
     *             <p>Otherwise, the callback must fill the object to which this points, following the same special promotion behavior as
     *             {@link LibFFI#ffi_call}. That is, in most cases, {@code ret} points to an object of exactly the size of the type specified when {@code CIF}
     *             was constructed.  However, integral types narrower than the system register size are widened. In these cases your program may assume that
     *             {@code ret} points to an {@code ffi_arg} object.</p>
     * @param args a vector of pointers to memory holding the arguments to the function
     */
    default void callback(long ret, long args) {
    }


    /**
     * A {@code Callback} with no return value.
     */
    interface V extends CallbackI {
        /**
         * Will be called by native code.
         *
         * @param args pointer to a {@code DCArgs} iterator
         */
        void callback(long args);
    }

    /**
     * A {@code Callback} that returns a boolean value.
     */
    interface Z extends CallbackI {
        /**
         * Will be called by native code.
         *
         * @param args pointer to a {@code DCArgs} iterator
         * @return the value to store to the result {@code DCValue}
         */
        boolean callback(long args);
    }

    /**
     * A {@code Callback} that returns a byte value.
     */
    interface B extends CallbackI {
        /**
         * Will be called by native code.
         *
         * @param args pointer to a {@code DCArgs} iterator
         * @return the value to store to the result {@code DCValue}
         */
        byte callback(long args);
    }

    /**
     * A {@code Callback} that returns a short value.
     */
    interface S extends CallbackI {
        /**
         * Will be called by native code.
         *
         * @param args pointer to a {@code DCArgs} iterator
         * @return the value to store to the result {@code DCValue}
         */
        short callback(long args);
    }

    /**
     * A {@code Callback} that returns an int value.
     */
    interface I extends CallbackI {
        /**
         * Will be called by native code.
         *
         * @param args pointer to a {@code DCArgs} iterator
         * @return the value to store to the result {@code DCValue}
         */
        int callback(long args);
    }

    /**
     * A {@code Callback} that returns a long value.
     */
    interface J extends CallbackI {
        /**
         * Will be called by native code.
         *
         * @param args pointer to a {@code DCArgs} iterator
         * @return the value to store to the result {@code DCValue}
         */
        long callback(long args);
    }

    /**
     * A {@code Callback} that returns a C long value.
     */
    interface N extends CallbackI {
        /**
         * Will be called by native code.
         *
         * @param args pointer to a {@code DCArgs} iterator
         * @return the value to store to the result {@code DCValue}
         */
        long callback(long args);
    }

    /**
     * A {@code Callback} that returns a float value.
     */
    interface F extends CallbackI {
        /**
         * Will be called by native code.
         *
         * @param args pointer to a {@code DCArgs} iterator
         * @return the value to store to the result {@code DCValue}
         */
        float callback(long args);
    }

    /**
     * A {@code Callback} that returns a double value.
     */
    interface D extends CallbackI {
        /**
         * Will be called by native code.
         *
         * @param args pointer to a {@code DCArgs} iterator
         * @return the value to store to the result {@code DCValue}
         */
        double callback(long args);
    }

    /**
     * A {@code Callback} that returns a pointer value.
     */
    interface P extends CallbackI {
        /**
         * Will be called by native code.
         *
         * @param args pointer to a {@code DCArgs} iterator
         * @return the value to store to the result {@code DCValue}
         */
        long callback(long args);
    }

}

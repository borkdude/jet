package org.babashka;

import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.function.CFunction;
import org.graalvm.nativeimage.c.function.CFunction.Transition;
import org.graalvm.nativeimage.c.CContext;
import java.util.List;
import java.util.Collections;

import org.graalvm.word.Pointer;

@CContext(CLibrary.Directives.class)
public final class CLibrary {
    public static final class Directives implements CContext.Directives {
        @Override
        public List<String> getHeaderFiles() {
            if ((System.getProperty("os.name").startsWith("Win"))) {
                return Collections.singletonList("<io.h>");
            }
            else {
                return Collections.singletonList("<unistd.h>");
            }
        }
    }
    @CFunction
    public static native int isatty(int fd);
}

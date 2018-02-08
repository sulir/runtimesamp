package com.github.sulir.runtimesamp.support;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ThreadFactory;

public class ToStringThread extends Thread {
    public ToStringThread(@NotNull Runnable target) {
        super(target);
    }

    public static class Factory implements ThreadFactory {
        @Override
        public Thread newThread(@NotNull Runnable runnable) {
            return new ToStringThread(runnable);
        }
    }
}

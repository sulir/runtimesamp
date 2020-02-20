package com.github.sulir.runtimesamp.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.regex.Pattern;

public class SampAgent {
    private static final HierarchyReader hierarchy = new HierarchyReader();
    private static Pattern include = Pattern.compile(".*");
    private static Pattern exclude = Pattern.compile("javax?/.*|sun/.*|" +
            "com/intellij/rt/.*|(.*/)?org/objectweb/asm/.*|com/github/sulir/runtimesamp/.*");

    public static void premain(String agentArgs, Instrumentation inst) {
        if (agentArgs != null) {
            include = Pattern.compile(agentArgs);
            exclude = Pattern.compile("");
        }

        inst.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                    ProtectionDomain protectionDomain, byte[] classfileBuffer) {
                try {
                    if (className != null && include.matcher(className).matches()
                            && !exclude.matcher(className).matches()) {
                        hierarchy.setClassLoader(loader);
                        ClassTransformer transformer = new ClassTransformer(className, classfileBuffer, hierarchy);
                        return transformer.transform();
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }

                return null;
            }
        });
    }
}

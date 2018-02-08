package com.github.sulir.runtimesamp.agent;

import java.lang.instrument.Instrumentation;
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

        inst.addTransformer((loader, className, classBeingRedefined, protectionDomain, classfileBuffer) -> {
            try {
                if (className != null && include.matcher(className).matches()
                        && !exclude.matcher(className).matches()) {
                    hierarchy.setClassLoader(loader);
                    ClassTransformer transformer = new ClassTransformer(classfileBuffer, hierarchy);
                    return transformer.transform();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        });
    }
}

package com.github.sulir.runtimesamp.agent;

import java.lang.instrument.Instrumentation;
import java.util.regex.Pattern;

public class SampAgent {

    public static void premain(String agentArgs, Instrumentation inst) {
        Pattern include = Pattern.compile((agentArgs == null) ? ".*" : agentArgs);
        Pattern exclude = Pattern.compile("java/.*|sun/.*|com/intellij/rt/.*" +
                "|(.*/)?org/objectweb/asm/.*|com/github/sulir/runtimesamp/.*");

        inst.addTransformer((loader, className, classBeingRedefined, protectionDomain,
                             classfileBuffer) -> {
            try {
                if (className != null && include.matcher(className).matches()
                        && !exclude.matcher(className).matches()) {
                    ClassTransformer transformer = new ClassTransformer(classfileBuffer);
                    return transformer.transform();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        });
    }
}

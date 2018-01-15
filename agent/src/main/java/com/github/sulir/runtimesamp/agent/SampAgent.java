package com.github.sulir.runtimesamp.agent;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.instrument.Instrumentation;
import java.util.regex.Pattern;

public class SampAgent {
    public static void premain(String agentArgs, Instrumentation inst) {
        Pattern shouldInstrument = Pattern.compile((agentArgs == null) ? ".*" : agentArgs);

        inst.addTransformer((loader, className, classBeingRedefined, protectionDomain,
                             classfileBuffer) -> {
            try {
                if (className != null && shouldInstrument.matcher(className).matches()) {
                    new ClassVisitor(Opcodes.ASM5) {

                    };
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        });
    }
}

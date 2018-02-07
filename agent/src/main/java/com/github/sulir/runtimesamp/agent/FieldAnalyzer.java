package com.github.sulir.runtimesamp.agent;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.*;

import java.util.HashSet;
import java.util.Set;

public class FieldAnalyzer {
    private static final BasicValue THIS = new BasicValue(null);

    private final MethodNode method;
    private final Set<AbstractInsnNode> fieldInstructionsOnThis = new HashSet<>();

    private class IsThisInterpreter extends BasicInterpreter {
        @Override
        public BasicValue copyOperation(AbstractInsnNode instruction, BasicValue value) throws AnalyzerException {
            if (instruction.getOpcode() == ALOAD && ((VarInsnNode) instruction).var == 0)
                return THIS;
            else
                return super.copyOperation(instruction, value);
        }

        @Override
        public BasicValue merge(BasicValue v, BasicValue w) {
            if (v == THIS && w == THIS)
                return THIS;
            else
                return super.merge(v, w);
        }
    }

    public FieldAnalyzer(MethodNode method) {
        this.method = method;
    }

    public void analyze() {
        if ((method.access & Opcodes.ACC_STATIC) != 0)
            return;

        Analyzer<BasicValue> analyzer = new Analyzer<>(new IsThisInterpreter());

        try {
            analyzer.analyze("java/lang/Object", method);
            AbstractInsnNode[] instructions = method.instructions.toArray();
            Frame<BasicValue>[] frames = analyzer.getFrames();

            for (int i = 0; i < instructions.length; i ++) {
                if (getObjectOperand(instructions[i], frames[i]) == THIS)
                    fieldInstructionsOnThis.add(instructions[i]);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean operatesOnThis(FieldInsnNode instruction) {
        return fieldInstructionsOnThis.contains(instruction);
    }

    private BasicValue getObjectOperand(AbstractInsnNode instruction, Frame<BasicValue> frame) {
        switch (instruction.getOpcode()) {
            case Opcodes.GETFIELD:
                return frame.getStack(frame.getStackSize() - 1);
            case Opcodes.PUTFIELD:
                return frame.getStack(frame.getStackSize() - 2);
            default:
                return null;
        }
    }
}

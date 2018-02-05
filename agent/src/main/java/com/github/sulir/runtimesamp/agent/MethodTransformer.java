package com.github.sulir.runtimesamp.agent;

import com.github.sulir.runtimesamp.support.Data;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.util.Printer;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

import java.util.Iterator;

public class MethodTransformer {
    private final InsnList instructions;
    private final LineMap lineMap;
    private final FieldAnalyzer fieldAnalyzer;
    private final VariableMap variableMap;
    private final int passVariable;

    public MethodTransformer(MethodNode method) {
        instructions = method.instructions;
        lineMap = new LineMap(method);
        fieldAnalyzer = new FieldAnalyzer(method);
        variableMap = new VariableMap(method);
        passVariable = method.maxLocals;
    }

    public void transform() {
        lineMap.construct();
        fieldAnalyzer.analyze();

        Iterator<AbstractInsnNode> iterator = instructions.iterator();
        instructions.insert(getResetPass());

        while (iterator.hasNext()) {
            AbstractInsnNode node = iterator.next();
            variableMap.update(node, this);

            if (lineMap.getLine(node) == LineMap.NO_LINE)
                continue;

            SourceInstruction source = new SourceInstruction(node, lineMap);

            if (source.canGoToOtherLine()) {
                if (source.isSequential())
                    instructions.insert(node, getInstrumentation(source));
                else
                    instructions.insertBefore(node, getInstrumentation(source));
            }
        }
    }

    public FieldAnalyzer getFieldAnalyzer() {
        return fieldAnalyzer;
    }

    public VariableMap getVariableMap() {
        return variableMap;
    }

    private InsnList getResetPass() {
        InsnList resetPass = new InsnList();
        resetPass.add(new InsnNode(Opcodes.LCONST_0));
        resetPass.add(new VarInsnNode(Opcodes.LSTORE, passVariable));
        return resetPass;
    }

    private InsnList getInstrumentation(SourceInstruction source) {
        InsnList list = new InsnList();
        LabelNode skipAll = new LabelNode();

        for (Branch branch : source.getBranches()) {
            if (branch.isSameLine()) {
                list.add(branch.getConditionOperandDuplication());
                list.add(new JumpInsnNode(branch.getCondition(), skipAll));
            }
        }

        for (Variable variable : variableMap.getVariablesAtLine()) {
            list.add(new LdcInsnNode(variable.getName()));
            list.add(variable.getBoxedLoad());
            list.add(Data.getInvokeStoreVariable());
        }

        list.add(skipAll);
        return list;
    }

    private void printInstructions() {
        Printer printer = new Textifier();
        TraceMethodVisitor tracer = new TraceMethodVisitor(printer);

        for (AbstractInsnNode instruction : instructions.toArray()) {
            instruction.accept(tracer);
        }

        for (Object line : printer.getText()) {
            System.out.print(line);
        }
    }
}

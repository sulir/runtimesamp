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
    private final String className;
    private final LineMap lineMap;
    private final FieldAnalyzer fieldAnalyzer;
    private final VariableMap variableMap;
    private final int passVariable;
    private InsnList inserted;

    public MethodTransformer(MethodNode method, String className) {
        instructions = method.instructions;
        this.className = className;
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
            AbstractInsnNode instruction = iterator.next();
            variableMap.update(instruction, this);

            if (lineMap.getLine(instruction) == LineMap.NO_LINE)
                continue;

            ControlNode node = new ControlNode(instruction, lineMap);

            if (node.canGoToOtherLine()) {
                inserted = new InsnList();
                generateInstrumentation(node);

                if (node.isSequential())
                    instructions.insert(instruction, inserted);
                else
                    instructions.insertBefore(instruction, inserted);
            }
        }
    }

    public FieldAnalyzer getFieldAnalyzer() {
        return fieldAnalyzer;
    }

    public VariableMap getVariableMap() {
        return variableMap;
    }

    private void generateInstrumentation(ControlNode node) {
        int lineId = lineMap.getLineId(node.getInstruction());
        int line = lineMap.getLine(node.getInstruction());

        LabelNode skipRecording = new LabelNode();
        LabelNode skipToEnd = new LabelNode();

        skipIfLineWillStay(node, skipToEnd);
        skipIfNoHitsLeft(lineId, skipRecording);

        Variable[] variables = variableMap.getVariablesAtLine();

        if (variables.length >= 1 && variables.length <= Data.VARIABLE_METHODS) {
            storeNVariables(lineId, line, variables);
        } else {
            updateIds(lineId, line);
            storeVariables(line, variables);
        }

        inserted.add(skipRecording);
        resetPassIfBackward(node, skipToEnd);
        inserted.add(skipToEnd);
    }

    private void skipIfLineWillStay(ControlNode node, LabelNode skip) {
        for (Branch branch : node.getBranches()) {
            if (branch.getDirection() == Branch.Direction.SAME_LINE)
                branch.getConditionalJump(skip);
        }
    }

    private void skipIfNoHitsLeft(int lineId, LabelNode skip) {
        inserted.add(Data.getReadLineHitsLeft());
        inserted.add(getPushInstruction(lineId));
        inserted.add(new InsnNode(Opcodes.BALOAD));
        inserted.add(new JumpInsnNode(Opcodes.IFEQ, skip));
    }

    private void storeNVariables(int lineId, int line, Variable[] variables) {
        inserted.add(getLineArguments(lineId, line));

        for (Variable variable : variables) {
            inserted.add(new LdcInsnNode(variable.getName()));
            inserted.add(variable.getBoxedLoad());
        }

        inserted.add(Data.getInvokeStoreNVariables(variables.length));
        inserted.add(new VarInsnNode(Opcodes.ISTORE, passVariable));
    }

    private void updateIds(int lineId, int line) {
        inserted.add(getLineArguments(lineId, line));
        inserted.add(Data.getInvokeUpdateIds());
        inserted.add(new VarInsnNode(Opcodes.ISTORE, passVariable));
    }

    private void storeVariables(int line, Variable[] variables) {
        for (Variable variable : variables) {
            inserted.add(new VarInsnNode(Opcodes.ILOAD, passVariable));
            inserted.add(getPushInstruction(line));
            inserted.add(new LdcInsnNode(variable.getName()));
            inserted.add(variable.getBoxedLoad());
            inserted.add(Data.getInvokeStoreVariable());
        }
    }

    private void resetPassIfBackward(ControlNode node, LabelNode skip) {
        if (node.hasNoBackwardBranch())
            return;

        for (Branch branch : node.getBranches()) {
            if (branch.getDirection() == Branch.Direction.FORWARD)
                inserted.add(branch.getConditionalJump(skip));
        }

        inserted.add(getResetPass());
    }

    private InsnList getResetPass() {
        InsnList resetPass = new InsnList();
        resetPass.add(new InsnNode(Opcodes.ICONST_0));
        resetPass.add(new VarInsnNode(Opcodes.ISTORE, passVariable));
        return resetPass;
    }

    private InsnList getLineArguments(int lineId, int line) {
        InsnList arguments = new InsnList();
        arguments.add(getPushInstruction(lineId));
        arguments.add(new VarInsnNode(Opcodes.ILOAD, passVariable));
        arguments.add(new LdcInsnNode(className));
        arguments.add(getPushInstruction(line));
        return arguments;
    }

    private AbstractInsnNode getPushInstruction(int constant) {
        if (constant <= Byte.MAX_VALUE) {
            return new IntInsnNode(Opcodes.BIPUSH, constant);
        } else if (constant <= Short.MAX_VALUE) {
            return new IntInsnNode(Opcodes.SIPUSH, constant);
        } else {
            return new LdcInsnNode(constant);
        }
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

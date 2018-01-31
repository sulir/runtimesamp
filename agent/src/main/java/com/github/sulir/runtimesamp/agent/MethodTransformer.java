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
    private final FieldAnalyzer fieldAnalyzer;
    private final VariableMap variableMap;
    private final int passVariable;

    public MethodTransformer(MethodNode method) {
        instructions = method.instructions;
        fieldAnalyzer = new FieldAnalyzer(method);
        variableMap = new VariableMap(method);
        passVariable = method.maxLocals;
    }

    public void transform() {
        fieldAnalyzer.analyze();
        instructions.insert(getResetPass());

        Iterator<AbstractInsnNode> iterator = instructions.iterator();
        while (iterator.hasNext()) {
            AbstractInsnNode node = iterator.next();
            Instruction instruction = new Instruction(node, this);

            if (node instanceof LabelNode)
                variableMap.labelEncountered(((LabelNode) node));

            if (instruction.hasVariable())
                variableMap.variableInstructionEncountered(instruction.getVariable());

            if (instruction.isLastAtLine())
                instrumentAfter(node, iterator);

            if (node instanceof LineNumberNode)
                variableMap.lineEncountered();
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

    private void instrumentAfter(AbstractInsnNode node, Iterator<AbstractInsnNode> iterator) {
        InsnList list = new InsnList();

        for (Variable variable : variableMap.getVariablesAtLine()) {
            list.add(new LdcInsnNode(variable.getName()));
            list.add(variable.getBoxedLoad());
            list.add(Data.getInvokeStoreVariable());
        }

        instructions.insert(node, list);
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

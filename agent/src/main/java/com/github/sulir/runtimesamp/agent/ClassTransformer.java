package com.github.sulir.runtimesamp.agent;

import org.mutabilitydetector.asm.NonClassloadingClassWriter;
import org.mutabilitydetector.asm.typehierarchy.TypeHierarchyReader;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.MethodNode;

public class ClassTransformer {
    private ClassReader reader;
    private TypeHierarchyReader hierarchy;

    public ClassTransformer(byte[] classBytes, TypeHierarchyReader hierarchy) {
        reader = new ClassReader(classBytes);
        this.hierarchy = hierarchy;
    }

    public byte[] transform() {
        ClassWriter writer = new NonClassloadingClassWriter(reader, ClassWriter.COMPUTE_FRAMES, hierarchy);

        ClassVisitor classVisitor = new ClassVisitor(Opcodes.ASM5, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc,
                                             String signature, String[] exceptions) {
                MethodVisitor methodVisitor = super.visitMethod(access, name, desc, signature, exceptions);
                return new MethodNode(Opcodes.ASM5, access, name, desc, signature, exceptions) {
                    @Override
                    public void visitEnd() {
                        if (instructions.size() != 0 && (access & Opcodes.ACC_SYNTHETIC) == 0)
                            new MethodTransformer(this).transform();

                        accept(methodVisitor);
                    }
                };
            }
        };

        reader.accept(classVisitor, ClassReader.SKIP_FRAMES);
        return writer.toByteArray();
    }
}

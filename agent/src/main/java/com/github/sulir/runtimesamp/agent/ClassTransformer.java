package com.github.sulir.runtimesamp.agent;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.MethodNode;

public class ClassTransformer {
    private ClassReader reader;

    public ClassTransformer(byte[] classBytes) {
        reader = new ClassReader(classBytes);
    }

    public byte[] transform() {
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES);

        ClassVisitor classVisitor = new ClassVisitor(Opcodes.ASM6, writer) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String desc,
                                             String signature, String[] exceptions) {
                MethodVisitor methodVisitor = super.visitMethod(access, name, desc, signature, exceptions);
                return new MethodNode(Opcodes.ASM6, access, name, desc, signature, exceptions) {
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

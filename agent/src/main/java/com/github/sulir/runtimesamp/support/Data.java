package com.github.sulir.runtimesamp.support;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.NoSuchElementException;

public class Data {
    private static String THIS_CLASS = Data.class.getName();
    private static final AbstractInsnNode invokeStoreVariable = getInvokeInstruction("storeVariable");

    public static void storeVariable(String name, Object value) {
        StackTraceElement[] stack = new Throwable().getStackTrace();
        for (int i = 1; i < stack.length; i++) {
            if (stack[i].getClassName().equals(THIS_CLASS))
                return;
        }

        String stringValue = objectToString(value);

    }

    public static AbstractInsnNode getInvokeStoreVariable() {
        return invokeStoreVariable.clone(null);
    }

    private static String objectToString(Object object) {
        try {
            if (object == null)
                return "null";
            else
                return object.toString();
        } catch (Exception e) {
            return  object.getClass().getName() + "@" + Integer.toHexString(object.hashCode());
        }
    }

    private static AbstractInsnNode getInvokeInstruction(String methodName) {
        String owner = Type.getType(Data.class).getInternalName();
        Method method = Arrays.stream(Data.class.getMethods())
                .filter(m -> m.getName().equals(methodName))
                .findFirst().orElseThrow(NoSuchElementException::new);
        String descriptor = Type.getMethodDescriptor(method);
        return new MethodInsnNode(Opcodes.INVOKESTATIC, owner, methodName, descriptor, false);
    }
}

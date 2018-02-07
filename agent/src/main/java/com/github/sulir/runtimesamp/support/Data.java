package com.github.sulir.runtimesamp.support;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

public class Data {
    public static byte[] lineHitsLeft = new byte[100_000];
    private static int nextPassId = 1;
    private static final byte HITS_PER_LINE = 1;

    private static final FieldInsnNode readLineHitsLeft = getReadHitsInstruction();
    private static final MethodInsnNode invokeUpdateIds = getInvokeInstruction("updateIds");
    private static final MethodInsnNode invokeStoreVariable = getInvokeInstruction("storeVariable");

    static {
        Arrays.fill(lineHitsLeft, HITS_PER_LINE);
    }

    public static int updateIds(int lineId, int passId) {
        StackTraceElement[] stack = new Throwable().getStackTrace();
        if (containsRecursiveInstrumentation(stack))
            return passId;

        synchronized (Data.class) {
            if (lineHitsLeft[lineId] > 0)
                lineHitsLeft[lineId]--;

            if (passId == 0)
                passId = nextPassId++;
        }

        String className = stack[1].getClassName();
        String packageName = className.substring(0, className.lastIndexOf('.'));
        String file = packageName.replace('.', '/') + '/' + stack[1].getFileName();
        int line = stack[1].getLineNumber();

        return passId;
    }

    public static void storeVariable(String name, Object value, int passId) {
        StackTraceElement[] stack = new Throwable().getStackTrace();
        if (containsRecursiveInstrumentation(stack))
            return;

        String stringValue = objectToString(value);
    }

    public static synchronized void increaseHitsCapacity() {
        byte[] result = Arrays.copyOf(lineHitsLeft, 2 * lineHitsLeft.length);
        Arrays.fill(result, lineHitsLeft.length, result.length, HITS_PER_LINE);
        lineHitsLeft = result;
    }

    public static AbstractInsnNode getReadLineHitsLeft() {
        return readLineHitsLeft.clone(null);
    }

    public static AbstractInsnNode getInvokeUpdateIds() {
        return invokeUpdateIds.clone(null);
    }

    public static AbstractInsnNode getInvokeStoreVariable() {
        return invokeStoreVariable.clone(null);
    }

    private static boolean containsRecursiveInstrumentation(StackTraceElement[] stack) {
        for (int i = 1; i < stack.length; i++) {
            if (stack[i].getClassName().equals(Data.class.getName()))
                return true;
        }
        return false;
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

    private static FieldInsnNode getReadHitsInstruction() {
        String owner = Type.getType(Data.class).getInternalName();
        String fieldName = "lineHitsLeft";

        try {
            Field field = Data.class.getField(fieldName);
            String descriptor = Type.getDescriptor(field.getType());
            return new FieldInsnNode(Opcodes.GETSTATIC, owner, fieldName, descriptor);
        } catch (NoSuchFieldException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static MethodInsnNode getInvokeInstruction(String methodName) {
        String owner = Type.getType(Data.class).getInternalName();
        Method method = Arrays.stream(Data.class.getMethods())
                .filter(m -> m.getName().equals(methodName))
                .findFirst().orElseThrow(ExceptionInInitializerError::new);
        String descriptor = Type.getMethodDescriptor(method);
        return new MethodInsnNode(Opcodes.INVOKESTATIC, owner, methodName, descriptor, false);
    }
}

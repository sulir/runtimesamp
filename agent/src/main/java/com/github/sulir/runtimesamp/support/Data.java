package com.github.sulir.runtimesamp.support;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class Data {
    public static final int MAX_VARIABLE_ARGS = 3;
    private static final byte HITS_PER_LINE = 1;
    private static final ExecutorService executor = Executors.newCachedThreadPool(new ToStringThread.Factory());
    private static final FieldInsnNode readLineHitsLeft = getReadHitsInstruction();
    private static final MethodInsnNode[] invokeStoreVariables = new MethodInsnNode[MAX_VARIABLE_ARGS + 1];
    private static final MethodInsnNode invokeStoreNVariables = getInvokeInstruction("storeNVariables");

    public static byte[] lineHitsLeft = new byte[100_000];
    private static int nextPassId = 1;

    static {
        for (int i = 0; i <= MAX_VARIABLE_ARGS; i++)
            invokeStoreVariables[i] = getInvokeInstruction("store" + i + "Variables");

        Arrays.fill(lineHitsLeft, HITS_PER_LINE);
    }

    public static int store0Variables(int lineId, int passId, String className, int line) {
        if (Thread.currentThread() instanceof ToStringThread)
            return passId;

        return updateIds(lineId, passId, className, line);
    }

    public static int store1Variables(int lineId, int passId, String className, int line,
                                      String name1, Object value1) {
        if (Thread.currentThread() instanceof ToStringThread)
            return passId;

        passId = updateIds(lineId, passId, className, line);
        storeVariable(passId, line, name1, value1);

        return passId;
    }

    public static int store2Variables(int lineId, int passId, String className, int line,
                                      String name1, Object value1,
                                      String name2, Object value2) {
        if (Thread.currentThread() instanceof ToStringThread)
            return passId;

        passId = updateIds(lineId, passId, className, line);
        storeVariable(passId, line, name1, value1);
        storeVariable(passId, line, name2, value2);

        return passId;
    }

    public static int store3Variables(int lineId, int passId, String className, int line,
                                      String name1, Object value1,
                                      String name2, Object value2,
                                      String name3, Object value3) {
        if (Thread.currentThread() instanceof ToStringThread)
            return passId;

        passId = updateIds(lineId, passId, className, line);
        storeVariable(passId, line, name1, value1);
        storeVariable(passId, line, name2, value2);
        storeVariable(passId, line, name3, value3);

        return passId;
    }

    public static int storeNVariables(int lineId, int passId, String className, int line,
                                      String[] names, Object[] values) {
        if (Thread.currentThread() instanceof ToStringThread)
            return passId;

        passId = updateIds(lineId, passId, className, line);

        for (int i = 0; i < names.length; i++)
            storeVariable(passId, line, names[i], values[i]);

        return passId;
    }

    private static int updateIds(int lineId, int passId, String className, int line) {
        synchronized (Data.class) {
            if (lineHitsLeft[lineId] > 0)
                lineHitsLeft[lineId]--;

            if (passId == 0)
                passId = nextPassId++;
        }

        return passId;
    }

    private static void storeVariable(int passId, int line, String name, Object value) {
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

    public static AbstractInsnNode getInvokeStoreVariables(int n) {
        if (n <= MAX_VARIABLE_ARGS)
            return invokeStoreVariables[n].clone(null);
        else
            return invokeStoreNVariables.clone(null);
    }

    private static String objectToString(Object object) {
        if (object == null)
            return "null";

        try {
            Future<String> futureValue = executor.submit(object::toString);
            return futureValue.get(100, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            return object.getClass().getName() + "@" + Integer.toHexString(object.hashCode());
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

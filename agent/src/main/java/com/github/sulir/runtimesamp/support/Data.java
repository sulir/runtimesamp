package com.github.sulir.runtimesamp.support;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.StringJoiner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class Data {
    private static final byte HITS_PER_LINE = 1;
    public static byte[] lineHitsLeft = new byte[100_000];
    private static int nextPass = 1;

    private static final ExecutorService stringService = Executors.newCachedThreadPool(new ToStringThread.Factory());
    private static final ExecutorService dbService = Executors.newSingleThreadExecutor();
    private static final Jedis redis = new Jedis("localhost");
    private static final String EXECUTION_KEY = "next-execution-id";
    private static final String executionId = redis.incr(EXECUTION_KEY).toString();

    private static final FieldInsnNode readLineHitsLeft = getReadHitsInstruction();
    public static final int MAX_VARIABLE_ARGS = 3;
    private static final MethodInsnNode[] invokeStoreVariables = new MethodInsnNode[MAX_VARIABLE_ARGS + 1];
    private static final MethodInsnNode invokeStoreNVariables = getInvokeInstruction("storeNVariables");

    private static final String[] NO_NAMES = {};
    private static final Object[] NO_VALUES = {};
    private static final int VALUE_LENGTH = 120;

    static {
        for (int i = 0; i <= MAX_VARIABLE_ARGS; i++)
            invokeStoreVariables[i] = getInvokeInstruction("store" + i + "Variables");

        Arrays.fill(lineHitsLeft, HITS_PER_LINE);
    }

    public static int store0Variables(int lineId, int pass, String className, int line) {
        return storeNVariables(lineId, pass, className, line, NO_NAMES, NO_VALUES);
    }

    public static int store1Variables(int lineId, int pass, String className, int line,
                                      String name1, Object value1) {
        return storeNVariables(lineId, pass, className, line, new String[] {name1}, new Object[] {value1});
    }

    public static int store2Variables(int lineId, int pass, String className, int line,
                                      String name1, Object value1,
                                      String name2, Object value2) {
        return storeNVariables(lineId, pass, className, line, new String[] {name1, name2},
                new Object[] {value1, value2});
    }

    public static int store3Variables(int lineId, int pass, String className, int line,
                                      String name1, Object value1,
                                      String name2, Object value2,
                                      String name3, Object value3) {
        return storeNVariables(lineId, pass, className, line, new String[] {name1, name2, name3},
                new Object[] {value1, value2, value3});
    }

    @SuppressWarnings("WeakerAccess")
    public static int storeNVariables(int lineId, int pass, String className, int line,
                                      String[] names, Object[] values) {
        if (Thread.currentThread() instanceof ToStringThread)
            return pass;

        final int newPass;
        synchronized (Data.class) {
            if (lineHitsLeft[lineId] > 0)
                lineHitsLeft[lineId]--;

            newPass = (pass == 0) ? nextPass++ : pass;
        }

        String[] stringValues = new String[values.length];
        for (int i = 0; i < values.length; i++)
            stringValues[i] = objectToString(values[i]);

        dbService.submit(() -> {
            Pipeline pipeline = redis.pipelined();
            String passName = "pass:" + executionId + ":" + newPass;
            pipeline.rpush("line:" + className + ":" + line, passName);

            String[] dbValues;
            if (names.length == 0) {
                dbValues = new String[] {String.valueOf(line)};
            } else {
                dbValues = new String[names.length];
                for (int i = 0; i < names.length; i++) {
                    dbValues[i] = String.join(":", String.valueOf(line), names[i], stringValues[i]);
                }

            }

            pipeline.rpush(passName, dbValues);
            pipeline.sync();
        });

        return newPass;
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
            Future<String> futureValue = stringService.submit(() -> {
                Class elementType = object.getClass().getComponentType();

                if (elementType != null && (elementType.isPrimitive() || elementType == String.class)) {
                    int length = Array.getLength(object);
                    StringJoiner joiner = new StringJoiner(", ", "{", "}");

                    for (int i = 0; i < length; i++) {
                        if (joiner.length() >= VALUE_LENGTH) {
                            joiner.add("...");
                            break;
                        }
                        joiner.add(elementToString(Array.get(object, i)));
                    }

                    return joiner.toString();
                } else {
                    return elementToString(object);
                }
            });
            return futureValue.get(100, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            return defaultToString(object);
        }
    }

    private static String elementToString(Object object) throws Exception {
        if (object.getClass().getMethod("toString").getDeclaringClass() == Object.class)
            return defaultToString(object);

        String string = String.valueOf(object);

        if (string.length() > VALUE_LENGTH + 3)
            string = string.substring(0, VALUE_LENGTH) + "...";

        if (object instanceof String)
            string = '"' + string + '"';

        return string;
    }

    private static String defaultToString(Object object) {
        return object.getClass().getSimpleName() + "@" + Integer.toHexString(object.hashCode());
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

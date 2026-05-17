package com.sky.context;

public class BaseContext {

    public static ThreadLocal<Long> threadLocal = new ThreadLocal<>();
    private static ThreadLocal<String> intentThreadLocal = new ThreadLocal<>();
    private static ThreadLocal<Object> functionDataThreadLocal = new ThreadLocal<>();

    public static void setCurrentId(Long id) {
        threadLocal.set(id);
    }

    public static Long getCurrentId() {
        return threadLocal.get();
    }

    public static void removeCurrentId() {
        threadLocal.remove();
    }

    public static void setCurrentIntent(String intent) {
        intentThreadLocal.set(intent);
    }

    public static String getCurrentIntent() {
        return intentThreadLocal.get();
    }

    public static void removeIntent() {
        intentThreadLocal.remove();
    }

    public static void setFunctionData(Object data) {
        functionDataThreadLocal.set(data);
    }

    public static Object getFunctionData() {
        return functionDataThreadLocal.get();
    }

    public static void removeFunctionData() {
        functionDataThreadLocal.remove();
    }

}

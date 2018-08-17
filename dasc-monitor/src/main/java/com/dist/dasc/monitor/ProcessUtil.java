package com.dist.dasc.monitor;

import java.lang.management.ManagementFactory;

public class ProcessUtil {

    private static String name;

    public static long getCurrentProcessPid(){
        String pid = getCurrentMxBeanName().split("@")[0];
        return Long.valueOf(pid);
    }
    public static String getCurrentProcessUser(){
        return getCurrentMxBeanName().split("@")[1];
    }
    static String getCurrentMxBeanName(){
        if (name == null) {
            name = ManagementFactory.getRuntimeMXBean().getName();
        }
        return name;
    }
}

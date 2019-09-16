package com.web.common;

import java.lang.management.ManagementFactory;

/**
 * <p>简要说明...</p>
 *
 * @author JingHe
 * @version 1.0
 * @since 2019/8/13
 */
public final class SystemInfoUtil {

    /** 服务器进程的PID */
    public static final int PID;
    /** 系统主机名称 */
    public static final String HOSTNAME;
    /** 启动类完整名称加参数 */
    public static final String COMMAND_FULL;
    /** 启动类完整名称，不包括参数 */
    public static final String COMMAND;
    /** 服务器启动类类名的简称（不包含包名和Launch） */
    public static final String COMMAND_SHORT;
    /** 服务器启动类和机器hostname，可用于标记一个服务 */
    public static final String RUN_APP_NAME;

    static { // 启动时获取启动类等系统信息
        String command_full = "";
        String command_short = "";
        String command = "";
        String hostName = "UNKNOWN";
        int pid = -1;
        try {
            String sunjavacommand = System.getProperty("sun.java.command");
            if (StringTools.isNotEmpty(sunjavacommand)) {
                command_full = sunjavacommand;
                command = command_full.split(" ")[0];
                command_short = command.substring(command.lastIndexOf('.') + 1);
            }
            // 尽量去掉Launch让标题能更短一点
            command_short = !"Launch".equals(command_short) && command_short.endsWith("Launch") ? command_short.substring(0, command_short.length() - "Launch".length()) : command_short;

            String pidAtHostName = ManagementFactory.getRuntimeMXBean().getName();
            int idx = pidAtHostName.indexOf('@');
            if (idx > 0) {
                pid = StringTools.getInt(pidAtHostName.substring(0, idx), pid);
                hostName = pidAtHostName.substring(idx + 1);
            }
        } catch (Throwable ex) {
            throw new IllegalStateException(ex);
        } finally {
            COMMAND_FULL = command_full;
            COMMAND = command;
            COMMAND_SHORT = command_short;
            PID = pid;
            HOSTNAME = hostName;
            RUN_APP_NAME = "[" + COMMAND_SHORT + "@" + HOSTNAME + "]";
        }
    }

}

package com.kanven.leech.sink;

import lombok.extern.slf4j.Slf4j;
import org.hyperic.sigar.NetInterfaceConfig;
import org.hyperic.sigar.Sigar;

@Slf4j
public class SystemConfig {

    private static final Sigar sigar = new Sigar();

    static {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                sigar.close();
            }
        });
    }

    public static String getHost() {
        try {
            return sigar.getNetInfo().getHostName();
        } catch (Exception e) {
            log.error("get host name has an error", e);
        }
        return "";
    }

    public static String getIP() {
        try {
            String[] ifNames = sigar.getNetInterfaceList();
            for (String ifName : ifNames) {
                NetInterfaceConfig config = sigar.getNetInterfaceConfig(ifName);
                if (Integer.parseInt(config.getType()) == 6) {
                    return config.getAddress();
                }
            }
        } catch (Exception e) {
            log.error("get ip has an error", e);
        }
        return "";
    }

    public static void main(String[] args) {
        System.out.println(getHost() + "/" + getIP());
    }

}

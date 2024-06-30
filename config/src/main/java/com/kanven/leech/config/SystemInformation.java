package com.kanven.leech.config;

import oshi.SystemInfo;
import oshi.hardware.HardwareAbstractionLayer;

public final class SystemInformation {

    private static final SystemInfo si = new SystemInfo();

    private static final HardwareAbstractionLayer hal = si.getHardware();

    public static long memoryPageSize() {
        return hal.getMemory().getPageSize();
    }

}

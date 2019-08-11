package com.kirchnersolutions.bench;

import com.sun.management.OperatingSystemMXBean;
import oshi.hardware.ComputerSystem;
import oshi.hardware.platform.windows.*;
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;
import oshi.hardware.platform.windows.WindowsDisks;
import oshi.hardware.HWDiskStore;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

public class SystemStatsService {

    private ThreadPoolExecutor threadPoolTaskExecutor;

    /**
     * Returns string of systems stats formatted for maintenance console table.
     * @return
     */
    String getSystemStats(String OS){
        return SystemStats(OS);
    }

    int getCPUThreadCount(){
        return GetCpuThreadCount();
    }

    private String SystemStats(String OS){
        String stats = "";
        if(OS.equals("WIN")){
            SystemInfo si = new SystemInfo();
            OperatingSystem os = si.getOperatingSystem();
            HardwareAbstractionLayer hal = si.getHardware();
            CentralProcessor cpu = hal.getProcessor();
            ComputerSystem cs = hal.getComputerSystem();
            WindowsGlobalMemory wgm = new WindowsGlobalMemory();
            WindowsCentralProcessor wcp = new WindowsCentralProcessor();
            OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(
                    OperatingSystemMXBean.class);
            ThreadMXBean bean = ManagementFactory.getThreadMXBean();
            stats = getSystem(si, os, cs, cpu) + ",\r\n  " + getCPU(cpu, wcp)
                    + getCPULoads(osBean) +
                    "%,\r\n  " + getThreads(bean) + ", \r\n " + getMemory(wgm)
                    + getAllWinDiskStats();
        }else{
            //Still needs oshi variations.
            OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(
                    OperatingSystemMXBean.class);
            ThreadMXBean bean = ManagementFactory.getThreadMXBean();
            stats = getCPULoads(osBean) +
                    "%,\r\n : " + getThreads(bean);
        }
        return stats;
    }

    private String getSystem(SystemInfo si, OperatingSystem os, ComputerSystem cs, CentralProcessor cpu){
        return "OS: " + os + getUptimes(cpu) + ",System Manufacturer:"
                + cs.getManufacturer().split(",")[0] + ",System Model:" + cs.getModel();
    }

    private String getMemory(WindowsGlobalMemory wgm){
        int MB = 1024 * 1024;
        long vmMem = Runtime.getRuntime().totalMemory();
        vmMem/= (1024 * 1024);
        long dbMem = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        dbMem = dbMem / (1024 * 1024);
        return ",Total System Memory:"
                + wgm.getTotal() / MB +  ",Used System Memory:" + ((wgm.getTotal() / MB) - (wgm.getAvailable() / MB)) +
                " MB,Availible System Memory:" + wgm.getAvailable() / MB + " MB,System Memory Used by Application:" + dbMem + " MB,System Memory Reserved for Application:" +
                    vmMem + " MB,Total Swap:" + wgm.getSwapTotal() / MB + " MB,Swap Used:" + wgm.getSwapUsed() / MB + " MB";
    }

    private String getCPU(CentralProcessor cpu, WindowsCentralProcessor wcp){
        return ",CPU Model:" + cpu.getName() + ",CPU Frequency:" + cpu.getVendorFreq() / (1000 * 1000) + " Mhz,CPU ID:" + cpu.getProcessorID() +
                ",CPU Vender:" + cpu.getVendor() + ",CPU Sockets:" + wcp.getPhysicalPackageCount() +
                ",Total Physical CPU Cores:" + wcp.getPhysicalProcessorCount() + ",Total Logical CPU Cores:" + wcp.getLogicalProcessorCount();
    }

    private String getThreads(ThreadMXBean bean){
        return ",Active Application Core Threads:" + bean.getThreadCount() + ",Active Request Threads:" + threadPoolTaskExecutor.getActiveCount();
    }

    private String getUptimes(CentralProcessor cpu){
        //long dbUp = System.currentTimeMillis() - tableManagerService.getStartTime();
        //long dbUph = (dbUp / 1000) / (60 * 60);
        //long dbUpmin = ((dbUp / 1000) / 60) - (dbUph * 60);
        //long dbUpsec = (dbUp / 1000) - (dbUpmin * 60) - (dbUph * (60 * 60));
        int upTime = (int)cpu.getSystemUptime();
        int upTimeh = upTime/ (60 * 60);
        int upTimem = (upTime/ 60) - (upTimeh * 60);
        int upTimes = upTime - (upTimem * 60) - (upTimeh * (60 * 60));
        return ",System Uptime:" + upTimeh + " hours " + upTimem + " minutes " + upTimes;
        //return ",System Uptime:" + upTimeh + " hours " + upTimem + " minutes " + upTimes + ",Application Uptime:" +dbUph + " hours " + dbUpmin + " minutes " + dbUpsec + " seconds";
    }

    private String getCPULoads(OperatingSystemMXBean osBean){
        double totLoad = osBean.getSystemCpuLoad();
        //<bug>
        double sysLoad = osBean.getProcessCpuLoad();
        double progLoad = sysLoad * 100;
        sysLoad = totLoad - sysLoad;
        //Not simultaneous, can be wrong.
        //</bug>
        sysLoad*= 100;
        sysLoad = Double.parseDouble(String.format("%.2f", (sysLoad)));
        totLoad*= 100;
        totLoad = Double.parseDouble(String.format("%.2f", (totLoad)));
        return ",Avg. CPU Load:" + String.format("%.2f", totLoad) + "%,System CPU Load:" + String.format("%.2f", sysLoad) + "%,Program CPU Load:" + String.format("%.2f", progLoad);
    }

    private String getAllWinDiskStats(){

        HWDiskStore[] hwDiskStores = getWinDiskStore();
        String stats = ",\n : ,System Disk Count:" + hwDiskStores.length;
        int count = 0;
        for (HWDiskStore disk : hwDiskStores){
            stats+= ",\n : ,Disk Number:" + count;
            stats+= getDiskStats(disk);
            count++;
        }
        return stats;
    }

    private HWDiskStore[] getWinDiskStore(){
        WindowsDisks windowsDisks = new WindowsDisks();
        return windowsDisks.getDisks();
    }

    private String getDiskStats(HWDiskStore hwDiskStore){
        hwDiskStore.updateDiskStats();
        String stats  = ",Disk Model:" + hwDiskStore.getModel();
        long size = hwDiskStore.getSize() / (1024 * 1024 * 1024);
        stats+= ",Disk Size:" + size + " GB";
        stats+= ",Serial Number:" + hwDiskStore.getSerial();
        return stats;
    }

    private int GetCpuThreadCount(){
        WindowsCentralProcessor wcp = new WindowsCentralProcessor();
        return wcp.getLogicalProcessorCount();
    }



}

package com.dist;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.hyperic.sigar.*;
import org.junit.Test;
import org.springframework.boot.autoconfigure.web.ResourceProperties;

import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

@Slf4j
public class SigarTest {
    public CpuKPI printCpuUsage() throws SigarException {
        Sigar sigar = new Sigar();
        CpuPerc[] cpuPers = sigar.getCpuPercList();
        CpuKPI kpi = Arrays.stream(cpuPers).filter(Objects::nonNull)
                .map(cpuPerc -> new CpuKPI(cpuPerc.getIdle(), cpuPerc.getCombined()))
                .reduce(CpuKPI::add).get();
        System.out.println(String.format("Thread:%s,idle:%s,combine:%s", Thread.currentThread().getName(), kpi.getIdle() / kpi.getIndex(), kpi.getCombine() / kpi.getIndex()));
        return kpi;
    }

    @Test
    public void test() {
        Runnable callable = () -> {
            CpuKPI kpi = null;
            int count = 0;
            while (count < 100) {
                count++;
                try {
                    printCpuUsage();
                } catch (SigarException e) {
                    e.printStackTrace();
                }
            }
        };
        Executor executor = Executors.newFixedThreadPool(8);
        for (int i = 0; i < 20; i++) {
            executor.execute(callable);
        }
        ((ExecutorService) executor).shutdown();
        while (true) {
            if (((ExecutorService) executor).isTerminated()) {
                return;
            }
        }
    }

    @Data
    public class CpuKPI {
        double idle;
        double combine;
        int index = 1;

        CpuKPI add(CpuKPI other) {
            index++;
            idle += other.getIdle();
            combine += other.getCombine();
            return this;
        }


        public CpuKPI(double idle, double combine) {
            this.idle = idle;
            this.combine = combine;
        }
    }

    /**
     * 1024*1024*1024
     */
    private static final int GB_UNIT = 0x40000000;

    @Test
    public void testMem() {
        try {
            Mem memInfo = getSigar().getMem();
            DecimalFormat decimalFormat = new DecimalFormat("#.0000");
            log.debug("内存剩余\t{}GB",decimalFormat.format(memInfo.getActualFree() / (double) GB_UNIT));
            log.debug("内存总量\t{}GB",decimalFormat.format(memInfo.getTotal() / (double) GB_UNIT));
            log.debug("内存已使用\t{}GB",decimalFormat.format(memInfo.getActualUsed() / (double) GB_UNIT));
            log.debug("内存使用率\t{}%",decimalFormat.format(memInfo.getUsedPercent()));
        } catch (SigarException e) {
            e.printStackTrace();
        }
    }

    String[] invalidAddress = new String[]{"0.0.0.0","127.0.0.1","::1"};

    @Test
    public void testNetwork() throws SigarException {
        String[] netInterfaceList = getSigar().getNetInterfaceList();

        // 获取网络流量信息
        for (int i = 0; i < netInterfaceList.length; i++) {
            String netInterface = netInterfaceList[i];// 网络接口
            NetInterfaceConfig netInterfaceConfig = getSigar().getNetInterfaceConfig(netInterface);
            if ((netInterfaceConfig.getFlags() & 1L) <= 0L) {
                // 网络装置是否正常启用，如果没有启用就忽略掉该网卡
                continue;
            }
            if(NetFlags.NULL_HWADDR.equals(netInterfaceConfig.getHwaddr())){
                //跳过无硬件地址的网卡
                continue;
            }
            String addres = netInterfaceConfig.getAddress();
            if(ArrayUtils.contains(invalidAddress,addres)){
                // 忽略回环网卡和无意义网卡
                continue;
            }

            System.out.println("Address = " + netInterfaceConfig.getAddress());// IP地址
            NetInterfaceStat netInterfaceStat = getSigar().getNetInterfaceStat(netInterface);
//            System.out.println("netInterfaceStat rxPackets：" + netInterfaceStat.getRxPackets());// 接收的总包裹数
//            System.out.println("netInterfaceStat txPackets：" + netInterfaceStat.getTxPackets());// 发送的总包裹数
            System.out.println("netInterfaceStat rxBytes：" + netInterfaceStat.getRxBytes());// 接收到的总字节数
            System.out.println("netInterfaceStat txBytes：" + netInterfaceStat.getTxBytes());// 发送的总字节数
//            System.out.println("netInterfaceStat rxErrors：" + netInterfaceStat.getRxErrors());// 接收到的错误包数
//            System.out.println("netInterfaceStat txErrors：" + netInterfaceStat.getTxErrors());// 发送数据包时的错误数
//            System.out.println("netInterfaceStat rxDropped：" + netInterfaceStat.getRxDropped());// 接收时丢弃的包数
//            System.out.println("netInterfaceStat txDropped：" + netInterfaceStat.getTxDropped());// 发送时丢弃的包数
//            System.out.println("netInterfaceStat rxOverruns：" + netInterfaceStat.getRxOverruns());
//            System.out.println("netInterfaceStat txOverruns：" + netInterfaceStat.getTxOverruns());
//            System.out.println("netInterfaceStat rxFrame：" + netInterfaceStat.getRxFrame());
//            System.out.println("netInterfaceStat txCollisions：" + netInterfaceStat.getTxCollisions());
//            System.out.println("netInterfaceStat txCarrier：" + netInterfaceStat.getTxCarrier());
//            System.out.println("netInterfaceStat thruput：" + netInterfaceStat.getThruput());
        }

        // 一些其它的信息
//        for (int i = 0; i < netInterfaceList.length; i++) {
//            String netInterface = netInterfaceList[i];// 网络接口
//            NetInterfaceConfig netInterfaceConfig = getSigar().getNetInterfaceConfig(netInterface);
//            if (NetFlags.LOOPBACK_ADDRESS.equals(netInterfaceConfig.getAddress())
//                    || (netInterfaceConfig.getFlags() & NetFlags.IFF_LOOPBACK) != 0
//                    || NetFlags.NULL_HWADDR.equals(netInterfaceConfig.getHwaddr())) {
//                continue;
//            }
//
//            System.out.println("netInterfaceConfig name：" + netInterfaceConfig.getName());
//            System.out.println("netInterfaceConfig hwaddr：" + netInterfaceConfig.getHwaddr());// 网卡MAC地址
//            System.out.println("netInterfaceConfig type:" + netInterfaceConfig.getType());
//            System.out.println("netInterfaceConfig description：" + netInterfaceConfig.getDescription());// 网卡描述信息
//            System.out.println("netInterfaceConfig address：" + netInterfaceConfig.getAddress());// IP地址
//            System.out.println("netInterfaceConfig destination：" + netInterfaceConfig.getDestination());
//            System.out.println("netInterfaceConfig broadcast：" + netInterfaceConfig.getBroadcast());// 网关广播地址
//            System.out.println("netInterfaceConfig netmask：" + netInterfaceConfig.getNetmask());// 子网掩码
//            System.out.println("netInterfaceConfig flags：" + netInterfaceConfig.getFlags());
//            System.out.println("netInterfaceConfig mtu：" + netInterfaceConfig.getMtu());
//            System.out.println("netInterfaceConfig metric：" + netInterfaceConfig.getMetric());
//        }
    }

    @Test
    public void name() throws SigarException {
        String[] netifs = getSigar().getNetInterfaceList();
        Stream.of(netifs).parallel().filter(StringUtils::isNotEmpty)
                .map(netif -> {
                    try {
                        return getSigar().getNetInterfaceConfig(netif);
                    } catch (SigarException e) {
                        return null;
                    }
                })
                .filter(netInterfaceConfig -> {
                    boolean working = false;
                    if (Objects.nonNull(netInterfaceConfig)) {
                        //网卡必须要处于启动状态
                        working = (netInterfaceConfig.getFlags() & 1L) > 0;
                        working = working && StringUtils.isNotBlank(netInterfaceConfig.getAddress());
                    }
                    return working;
                });
    }

    @Test
    public void cpu() throws InterruptedException {
        Runnable runable = () -> {
            int i = 0;
            while (true) {
                i++;
            }
        };
        ExecutorService exe = Executors.newFixedThreadPool(20);
        for (int i = 0; i < 20; i++) {
            exe.submit(runable);
        }
        while (!exe.isTerminated()) {
            Thread.sleep(1000);
        }
    }

    private SigarProxy getSigar() {
        return new Sigar();
    }
}

package com.dist.dasc.monitor.impl;


import com.dist.bdf.util.base.StringUtil;
import com.dist.dasc.health.HealthKPI;
import com.dist.dasc.health.SumReducibleHealthKPI;
import com.dist.dasc.monitor.api.KpiMonitorSvc;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.hyperic.sigar.*;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * 获取指标的工具类
 *
 * @author luojing
 * @date 2018年 8月16日 星期四 15时00分14秒 CST
 */
@Slf4j
@Component
public class KpiExecutorSvcImpl implements KpiMonitorSvc {

    /**
     * 网络状况监控，刷新频率1HZ，单位byte/s,里面只有吞吐量信息
     * Value KPI
     */
    final Map netStat = new HashMap();

    /**
     * 硬盘监控信息，刷新频率1HZ，记录了硬盘最大、剩余、已使用、百分比、吞吐量（单位：byte/s）存在
     **/
    final Map hardDrive = new HashMap();
    final int ONE_SECOND_IN_MILLISECOND = 1000;
    final ScheduledExecutorService EXCUTOR = Executors.newScheduledThreadPool(16);
    final String FORMAT_STRING = "%.2fMbps/s";
    final String VIRTUAL = "Virtual";
    final double KB_UNIT = 1024;
    final double MB_UNIT = KB_UNIT * KB_UNIT;
    final double MBPS_UNIT = MB_UNIT / 8;
    final double GB_FROM_KB_UNIT = MB_UNIT;
    final int PERCENT_UNIT = 100;
    /**
     * 1024*1024*1024
     */
    private final double GB_UNIT = MB_UNIT * KB_UNIT;

    final String READ_WRITE_ABLE_DRIVE_OPTION = "rw";
    /**
     * 筛选吞吐量最小值
     */
    final double THRUPUT_MIN = 0.01d;


    /**
     * 通过地址（both ipv4 and ipv6）过滤不需要监控的网卡
     */
    final String[] INVALID_ADDRESS_ARRAY = new String[]{
            NetFlags.ANY_ADDR,
            NetFlags.ANY_ADDR_V6,
            NetFlags.LOOPBACK_ADDRESS,
            NetFlags.LOOPBACK_ADDRESS_V6,
            NetFlags.LOOPBACK_HOSTNAME
    };


    /**
     * 返回网络指标，有几张网卡list就返回多少内容
     * 关注KPI的吞吐量指标，单位Mbps
     *
     * @return
     */
    @Override
    public List<HealthKPI> getNetKpi() {
        return new ArrayList<>(netStat.values());
    }

    /**
     * 返回网络指标，有几张网卡list就返回多少内容
     * 关注KPI的吞吐量指标，单位Mbps
     *
     * @param name
     * @return
     */
    @Override
    public HealthKPI getNetKpi(String name) {
        return filterKpi(name,getNetKpi());
    }


    @Override
    public List<HealthKPI> getDriveKpi() {
        return new ArrayList<>(hardDrive.values());
    }

    @Override
    public HealthKPI getDriveKpi(String dir) {
        return filterKpi(dir,getDriveKpi());
    }

    private  HealthKPI filterKpi(String name,List<HealthKPI> kpis){
        HealthKPI kpi = null;
        if (CollectionUtils.isNotEmpty(kpis)) {
            Optional<HealthKPI> op = kpis.stream().filter(k -> StringUtil.equals(k.getName(), name)).findAny();
            kpi = op.isPresent() ? op.get() : null;
        }
        return kpi;
    }

    @Override
    public HealthKPI getCpuKpi() {
        HealthKPI cpuKpi = new HealthKPI();
        try {
            Sigar sigar = new Sigar();
            CpuPerc[] cpuPers = sigar.getCpuPercList();

            Optional<SumReducibleHealthKPI> optional = Arrays.stream(cpuPers).filter(Objects::nonNull)
                    .map(cpuPerc -> {
                        SumReducibleHealthKPI kpi = new SumReducibleHealthKPI();
                        kpi.setUsage(cpuPerc.getCombined() * PERCENT_UNIT);
                        return kpi;
                    })
                    .reduce(SumReducibleHealthKPI::reduce);
            if (optional.isPresent()) {
                cpuKpi.setUsage(optional.get().getAverageUsage());
                cpuKpi.setType(KPI_TYPE_CPU);
            }
        } catch (SigarException e) {
            log.debug("收集Sigar的数据出了一些问题。", e);
        }
        return cpuKpi;
    }

    /**
     * 获取内存信息监控API
     *
     * @return
     */
    @Override
    public HealthKPI getMemKpi() {
        HealthKPI healthKPI = new HealthKPI();
        healthKPI.setType(KPI_TYPE_MEMORY);
        try {
            Sigar sigar = new Sigar();
            Mem memInfo = sigar.getMem();
            healthKPI.setFree(memInfo.getActualFree() / GB_UNIT);
            healthKPI.setUsed(memInfo.getActualUsed() / GB_UNIT);
            healthKPI.setMax(memInfo.getTotal() / GB_UNIT);
            healthKPI.setUsage(memInfo.getUsedPercent());
        } catch (SigarException e) {
            log.debug("收集Sigar的数据出了一些问题。", e);
        }

        return healthKPI;
    }

    @Override
    public Collection prepareHardDrive() {
        return hardDrive.keySet();
    }

    @Override
    public Collection prepareNetwork() {
        return netStat.keySet();
    }

    ThisRunable getNetRunable() {
        return () ->
                getValidInterfaces()
                        .parallelStream()
                        .filter(inet -> !StringUtil.containsIgnoreCase(inet.getDescription(), VIRTUAL))
                        .map(inet -> {
                            HealthKPI kpi = new HealthKPI();
                            kpi.setName(inet.getName());
                            kpi.setType(KPI_TYPE_NETWORK);
                            kpi.setThruput(getNetSpeedInByte(inet.getName(), ONE_SECOND_IN_MILLISECOND, TimeUnit.MILLISECONDS) / MBPS_UNIT);
                            return kpi;
                        })
                        .filter(kpi -> kpi.getThruput() > THRUPUT_MIN)
                        .forEach(kpi -> netStat.put(kpi.getName(), kpi));
    }


    ThisRunable getDriveRunable() {
        return () -> {
            Stream.of(getValidDrives()).parallel()
                    .filter(Objects::nonNull)
                    .filter(fileSystem -> StringUtil.contains(fileSystem.getOptions(), READ_WRITE_ABLE_DRIVE_OPTION))
                    .map(fs -> {
                        HealthKPI kpi = new HealthKPI();
                        kpi.setType(KPI_TYPE_HARDDRIVE);
                        kpi.setName(fs.getDirName());
                        collectDriveKpi(kpi);
                        return kpi;
                    })
                    .forEach(kpi -> hardDrive.put(kpi.getName(), kpi));
            ;
        };
    }

    private void collectDriveKpi(HealthKPI healthKPI) {
        try {
            Sigar sigar = new Sigar();
            FileSystemUsage fsUsage = sigar.getFileSystemUsage(healthKPI.getName());
            healthKPI.setMax(fsUsage.getTotal() / GB_FROM_KB_UNIT);
            healthKPI.setUsed(fsUsage.getUsed() / GB_FROM_KB_UNIT);
            healthKPI.setFree(fsUsage.getFree() / GB_FROM_KB_UNIT);
            healthKPI.setUsage(fsUsage.getUsePercent() * PERCENT_UNIT);
            healthKPI.setThruput(getDriveThrupthInByte(healthKPI.getName(), ONE_SECOND_IN_MILLISECOND, TimeUnit.MILLISECONDS) / MBPS_UNIT);
        } catch (SigarException e) {
            log.debug("收集Sigar的数据出了一些问题。", e);
        }
    }

    double getDriveThrupthInByte(String dirName, long duration, TimeUnit unit) {
        double result = 0d;
        try {
            Sigar sigar = new Sigar();
            FileSystemUsage begin = sigar.getFileSystemUsage(dirName);
            TimeUnit.MILLISECONDS.sleep(unit.toMillis(duration));
            FileSystemUsage end = sigar.getFileSystemUsage(dirName);
            result = (end.getDiskReadBytes() + end.getDiskWriteBytes() - begin.getDiskReadBytes() - begin.getDiskWriteBytes()) / unit.toSeconds(duration);
        } catch (SigarException e) {
            log.debug("收集Sigar的数据出了一些问题。", e);
        } catch (InterruptedException e) {
            log.debug("获取网络状况时线程被终止。");
        } finally {
            return result;
        }
    }


    private FileSystem[] getValidDrives() throws SigarException {
        Sigar sigar = new Sigar();
        FileSystem[] fileSystemArray = sigar.getFileSystemList();
//        TODO:对文件系统进行筛选
        return fileSystemArray;
    }


    @FunctionalInterface
    interface ThisRunable {
        void run() throws SigarException;
    }


    /**
     * 返回带有异常处理的真正Runables
     *
     * @param runable 这个runable不继承Runable，它支持抛出Sigar异常，然后统一处理
     * @return
     */
    Runnable wrapRunable(ThisRunable runable) {
        return () -> {
            try {
                runable.run();
            } catch (SigarException e) {
                if (log.isDebugEnabled()) {
                    log.debug("收集Sigar的数据出了一些问题。", e);
                }
            }
        };
    }

    @PostConstruct
    public void init() throws InterruptedException {
        EXCUTOR.scheduleAtFixedRate(wrapRunable(getNetRunable()), 0, 1, TimeUnit.SECONDS);
        EXCUTOR.scheduleAtFixedRate(wrapRunable(getDriveRunable()), 0, 1, TimeUnit.SECONDS);
    }

    List<NetInterfaceConfig> getValidInterfaces() throws SigarException {
        Sigar sigar = new Sigar();
        String[] netInterfaceList = sigar.getNetInterfaceList();
        List<NetInterfaceConfig> interfaceConfigList = new ArrayList();
        for (String netInterfaceName : netInterfaceList) {
            if (Objects.nonNull(netInterfaceName)) {
                NetInterfaceConfig netInterface = sigar.getNetInterfaceConfig(netInterfaceName);
                if ((netInterface.getFlags() & 1L) <= 0L) {
                    // 网络装置是否正常启用，如果没有启用就忽略掉该网卡
                    continue;
                }
                if (NetFlags.NULL_HWADDR.equals(netInterface.getHwaddr())) {
                    //跳过无硬件地址的网卡
                    continue;
                }
                String addres = netInterface.getAddress();
                if (ArrayUtils.contains(INVALID_ADDRESS_ARRAY, addres)) {
                    // 忽略回环网卡和ANY网卡
                    continue;
                }
                interfaceConfigList.add(netInterface);
            }
        }
        return interfaceConfigList;
    }

    /**
     * 获取网络每秒的数据量
     *
     * @param netInterfaceName 网卡名称
     * @param duration         时间段长度
     * @param unit             时间单位
     * @return 单位Byte
     */
    double getNetSpeedInByte(String netInterfaceName, long duration, TimeUnit unit) {
        long start = getXByte(netInterfaceName);
        try {
            TimeUnit.MILLISECONDS.sleep(unit.toMillis(duration));
        } catch (InterruptedException e) {
            log.debug("获取网络状况时线程被终止。");
        }
        long end = getXByte(netInterfaceName);
        return (end - start) / unit.toSeconds(duration);
    }

    /**
     * 获取指定网卡的数据包量，发送+接受的数据包 单位Byte
     *
     * @param netInterfaceName
     * @return
     */
    long getXByte(String netInterfaceName) {
        try {
            Sigar sigar = new Sigar();
            NetInterfaceStat netstat = sigar.getNetInterfaceStat(netInterfaceName);
            return netstat.getRxBytes() + netstat.getTxBytes();
        } catch (SigarException e) {
            log.error("无法成功调用sigar的jni接口。", e);
            return 0;
        }
    }

}

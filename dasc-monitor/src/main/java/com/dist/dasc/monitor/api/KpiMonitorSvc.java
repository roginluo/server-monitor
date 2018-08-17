package com.dist.dasc.monitor.api;

import com.dist.dasc.health.HealthKPI;

import java.util.Collection;
import java.util.List;

/**
 * <p>Description: 获取服务器状态指标的接口</p>
 * <p>Copyright: Copyright (c) 2018</p>
 * <p>Company: www.dist.com.cn</p>
 *
 * @author luojing
 * @version 1.0
 * @date 2018/8/16 下午3:12
 */
public interface KpiMonitorSvc {

    final String
            KPI_TYPE_NETWORK = "NETWORK",
            KPI_TYPE_HARDDRIVE = "HARDDRIVE",
            KPI_TYPE_CPU = "CPU",
            KPI_TYPE_MEMORY = "MEMORY";

    /**
     * 返回网络指标，有几张网卡list就返回多少内容
     * 关注KPI的吞吐量指标，单位Mbps
     * @return
     */
    List<HealthKPI> getNetKpi();

    /**
     * 返回网络指标，有几张网卡list就返回多少内容
     * 关注KPI的吞吐量指标，单位Mbps
     *
     * @return
     * @param name
     */
    HealthKPI getNetKpi(String name);

    /**
     * 返回可写的（HardDrive）分区指标，有多少分区List的size就有多大<br>
     * 关注指标：
     * <pre>
     *     max 当前分区容量（磁盘容量）单位：GB
     *     used 已使用（磁盘占用量）单位：GB
     *     free 剩余（可用磁盘空间）单位：GB
     *     usage 使用率，数字类型，最大100
     *     thruput 吞吐量（读写速率）单位：Mbps
     * </pre>
     * name 分区的名字
     * type HARDDRIVE
     *
     * @return
     */
    List<HealthKPI> getDriveKpi();
    /**
     * 返回可写的（HardDrive）分区指标，有多少分区List的size就有多大<br>
     * 关注指标：
     * <pre>
     *     max 当前分区容量（磁盘容量）单位：GB
     *     used 已使用（磁盘占用量）单位：GB
     *     free 剩余（可用磁盘空间）单位：GB
     *     usage 使用率，数字类型，最大100
     *     thruput 吞吐量（读写速率）单位：Mbps
     * </pre>
     * name 分区的名字
     * type HARDDRIVE
     *
     * @return
     */
    HealthKPI getDriveKpi(String dir);

    /**
     * 获取CPU的监控内容
     *
     * @return
     */
    HealthKPI getCpuKpi();
    /**
     * 获取内存信息监控
     *
     * @return
     */
    HealthKPI getMemKpi();


    Collection prepareHardDrive();

    Collection prepareNetwork();
}

package com.dist.dasc.health;


import lombok.Data;

/**
 * 健康监控关键指标对象
 * 描述指标的数据数值小数不区精确度，小数点后的所有数据保留
 * @author 罗京
 * @date 2018/08/03 周五 16:48:27.50
 */
@Data
public class HealthKPI {

    /**
     * 总量，该指标主要用于对内存，硬盘空间等监控，约定单位为GB
     */
    double max;

    /**
     * 已经使用，该指标主要用于内存，硬盘等监控，约定单位为GB
     */
    double used;
    /**
     * 剩余量，该指标主要用于硬盘，内存的监控，约定单位为GB
     */
    double free;

    /**
     * 使用百分比，常用语监控CPU，内存、硬盘等使用率 数字值，最大为100
     */
    double usage;
    /**
     * 吞吐量，用于对网络、硬盘的监控 默认单位是Mbps
     */
    double thruput;
    /**
     * 名字，可取值：硬盘+分区号、内存、cpu、网卡+网卡名字
     */
    String name;
    String type;
    String identity;
}

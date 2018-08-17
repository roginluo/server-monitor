package com.dist.dasc.health;

import lombok.Data;

/**
 * 可以被增加模式的Reduce操作指标对象
 * @author luojing
 * @date 2018/08/03 周五 16:48:27.50
 */
@Data
public class SumReducibleHealthKPI extends HealthKPI {

    static final int NEVER_REDUCE_TIME = 1;

    int reduceTime = NEVER_REDUCE_TIME;

    public double getAverageMax() {
        return reduceTime == NEVER_REDUCE_TIME ? max : getMax() / reduceTime;
    }

    public double getAverageUsed() {
        return reduceTime == NEVER_REDUCE_TIME ? used : getUsed() / reduceTime;
    }

    public double getAverageUsage() {
        return reduceTime == NEVER_REDUCE_TIME ? usage : getUsage() / reduceTime;
    }

    public double getAverageThruput() {
        return reduceTime == NEVER_REDUCE_TIME ? thruput : getThruput() / reduceTime;
    }

    public double getAverageFree() {
        return reduceTime == NEVER_REDUCE_TIME ? free : getFree() / reduceTime;
    }

    /**
     * 对指标对象进行添加式的reduce操作
     *
     * @param other
     * @return
     */
    public SumReducibleHealthKPI reduce(HealthKPI other) {
        this.reduceTime++;
        this.max += other.getMax();
        this.used += other.getUsed();
        this.free += other.getFree();
        this.usage += other.getUsage();
        this.thruput += other.getThruput();
        return this;
    }


    public HealthKPI average() {
        this.max = getAverageMax();
        this.free = getAverageFree();
        this.used = getAverageUsed();
        this.usage = getAverageUsage();
        this.thruput = getAverageThruput();
        this.reduceTime = NEVER_REDUCE_TIME;
        return this;
    }
}

package com.dist.dasc.monitor.restapi;

import com.dist.bdf.framework.core.controller.BaseController;
import com.dist.bdf.util.base.StringUtil;
import com.dist.dasc.monitor.api.KpiMonitorSvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author luojing
 * @date 2018年 8月16日 星期四 21时31分17秒 CST
 */
@RestController
public class ServerMonitorController extends BaseController {

    @Autowired
    KpiMonitorSvc serverMonitorSvc;

    @GetMapping("/cpu/combine")
    public String cpuCombine() {
        return success(serverMonitorSvc.getCpuKpi().getUsage());
    }

    @GetMapping("/cpu/kpi")
    public String cpuKpi() {
        return success(serverMonitorSvc.getCpuKpi());
    }

    @GetMapping("/mem/combine")
    public String memCombine() {
        return success(serverMonitorSvc.getMemKpi().getUsage());
    }

    @GetMapping("/mem/kpi")
    public String memKpi() {
        return success(serverMonitorSvc.getMemKpi());
    }

    @GetMapping("/harddrive/prepare")
    public String hardPrepare(){
        return success(serverMonitorSvc.prepareHardDrive());
    }

    @GetMapping("/harddrive/kpi")
    public String hardDriveKpi(@RequestParam(required = false) String dir) {
        Object data;
        if (StringUtil.isNotEmpty(dir)) {
            data = serverMonitorSvc.getDriveKpi(dir);
        } else {
            data = serverMonitorSvc.getDriveKpi();
        }
        return success(data);
    }

    @GetMapping("/network/prepare")
    public String networkPrepare(){
        return success(serverMonitorSvc.prepareNetwork());
    }

    @GetMapping("/network/kpi")
    public String networkDriveKpi(@RequestParam(required = false) String name) {
        Object data;
        if (StringUtil.isNotEmpty(name)) {
            data = serverMonitorSvc.getNetKpi(name);
        } else {
            data = serverMonitorSvc.getNetKpi();
        }
        return success(data);
    }


}

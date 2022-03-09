package org.example.controller;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.example.service.BannerService;
import org.example.util.JsonData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api("轮播图模块")
@RestController
@RequestMapping("/api/banner/v1")
public class BannerController {

    @Autowired
    private BannerService bannerService;

    @ApiOperation("轮播图列表接口")
    @GetMapping("list")
    public JsonData list() {
        return JsonData.buildSuccess(bannerService.list());
    }


}


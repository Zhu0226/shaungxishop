package com.seckill.controller;

import com.seckill.annotation.RequiresPermission;
import com.seckill.common.ApiResponse;
import com.seckill.entity.SeckillGoods;
import com.seckill.service.impl.SeckillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/admin/seckill/goods")
public class AdminSeckillController {

    @Autowired private SeckillService seckillService;

    // 客服和管理员都能看列表
    @RequiresPermission("admin:goods:list")
    @GetMapping
    public ApiResponse<List<SeckillGoods>> list() {
        return ApiResponse.success(seckillService.listAllGoodsForAdmin());
    }

    @RequiresPermission("admin:goods:list")
    @GetMapping("/{id}")
    public ApiResponse<SeckillGoods> get(@PathVariable Long id) {
        return ApiResponse.success(seckillService.getGoodsDetail(id));
    }

    // 只有管理员能修改和新增
    @RequiresPermission("admin:goods:edit")
    @PostMapping
    public ApiResponse<SeckillGoods> create(@RequestBody SeckillGoods goods) {
        if (goods.getOnShelf() == null) goods.setOnShelf(1);
        seckillService.saveGoods(goods);
        return ApiResponse.success(goods);
    }

    @RequiresPermission("admin:goods:edit")
    @PutMapping("/{id}")
    public ApiResponse<SeckillGoods> update(@PathVariable Long id, @RequestBody SeckillGoods goods) {
        goods.setId(id);
        seckillService.updateGoods(goods);
        return ApiResponse.success(seckillService.getGoodsDetail(id));
    }

    @RequiresPermission("admin:goods:edit")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        seckillService.removeGoods(id);
        return ApiResponse.success(null);
    }
}
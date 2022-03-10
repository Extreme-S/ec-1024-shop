package org.example.controller;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.example.request.CartItemRequest;
import org.example.service.CartService;
import org.example.util.JsonData;
import org.example.vo.CartVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;


@Api("购物车")
@RestController
@RequestMapping("/api/cart/v1")
public class CartController {


    @Autowired
    private CartService cartService;

    @ApiOperation("添加到购物车")
    @PostMapping("add")
    public JsonData addToCart(@ApiParam("购物项") @RequestBody CartItemRequest cartItemRequest) {
        cartService.addToCart(cartItemRequest);
        return JsonData.buildSuccess();
    }


    @ApiOperation("修改购物车数量")
    @PostMapping("change")
    public JsonData changeItemNum(@ApiParam("购物项") @RequestBody CartItemRequest cartItemRequest) {
        cartService.changeItemNum(cartItemRequest);
        return JsonData.buildSuccess();
    }


    @ApiOperation("清空购物车")
    @DeleteMapping("/clear")
    public JsonData cleanMyCart() {
        cartService.clear();
        return JsonData.buildSuccess();
    }


    @ApiOperation("查看我的购物车")
    @GetMapping("/mycart")
    public JsonData findMyCart() {
        CartVO cartVO = cartService.getMyCart();
        return JsonData.buildSuccess(cartVO);
    }


    @ApiOperation("删除购物项")
    @DeleteMapping("/delete/{product_id}")
    public JsonData deleteItem(@ApiParam(value = "商品id", required = true) @PathVariable("product_id") long productId) {
        cartService.deleteItem(productId);
        return JsonData.buildSuccess();
    }


}

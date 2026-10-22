package com.rag.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.rag.dto.Result;
import com.rag.entity.Shop;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface IShopService extends IService<Shop> {
    Result queryById(Long id) throws InterruptedException;

    Result update(Shop shop);

    Result queryShopByType(Integer typeId, Integer current, Double x, Double y);

    void saveShop2Redis(Long id, Long expireSeconds);
}

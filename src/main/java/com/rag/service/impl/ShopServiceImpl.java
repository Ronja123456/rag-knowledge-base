package com.rag.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.rag.dto.Result;
import com.rag.entity.Shop;
import com.rag.mapper.ShopMapper;
import com.rag.service.IShopService;


import com.rag.util.CacheClient;
import com.rag.utils.SystemConstants;
import jakarta.annotation.Resource;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.rag.utils.RedisData;
import org.springframework.stereotype.Service;


import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import jakarta.servlet.*;

import static com.rag.utils.RedisConstants.*;


/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Resource
    private CacheClient cacheClient;

    @Resource
    private StringRedisTemplate stringRedisTemplate;
@Override
    public Result queryById(Long id){
    //用逻辑缓存来解决缓存击穿
    Shop shop = cacheClient.
            queryWithLogicalExpire(CACHE_SHOP_KEY, id, Shop.class, this::getById, CACHE_SHOP_TTL, TimeUnit.MINUTES);
    if (shop == null) {
        return Result.fail("店铺不存在！");
    }


    return Result.ok(shop);


}
//缓存预热   把商铺数据提前加载进 Redis  逻辑过期要求：Redis 里必须提前有数据
    public void saveShop2Redis(Long id, Long expireSeconds) {
        // 1.查询店铺数据
        Shop shop = getById(id);
        // 2.封装成逻辑过期
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        // 3.写入Redis
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY + id, JSONUtil.toJsonStr(redisData));
    }

    @Override
    public Result update(Shop shop){

    Long id = shop.getId();
    if(id==null){
        return Result.fail("店铺id不能为空");
    }

    //更新数据库
    updateById(shop);
    //删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY +shop.getId());

        return Result.ok();
    }
    @Override
    public Result queryShopByType(Integer typeId, Integer current, Double x, Double y) {
        // 1.是否根据坐标查询
        if (x == null || y == null) {
            // 不需要坐标查询，走数据库查询
            Page<Shop> page = query()
                    .eq("type_id", typeId)
                    .page(new Page<>(current, SystemConstants.DEFAULT_PAGE_SIZE));
            return Result.ok(page.getRecords());
        }
        // 2.计算分页参数
        int from = (current - 1) * SystemConstants.DEFAULT_PAGE_SIZE; //算分页起始数据
        int end = current * SystemConstants.DEFAULT_PAGE_SIZE; //算分页结束数据

        // 3.查询redis，按照距离排序，分页。结果：shopId,distance
        // 3. 查询 Redis，按距离排序
        String key = SHOP_GEO_KEY + typeId;

        GeoResults<RedisGeoCommands.GeoLocation<String>> results = stringRedisTemplate.opsForGeo()
                .radius(
                        key,
                        new Circle(new Point(x, y), new Distance(5000)),
                        RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                                .includeDistance()
                                .sortAscending()
                                .limit(end)
                );
        if (results == null) {
            return Result.ok(Collections.emptyList());
            // Collections.emptyList()	说"没结果返回空列表"即可
        }
        // 4.解析出id
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> list = results.getContent();
        if (list.size() <= from) {
            return Result.ok(Collections.emptyList());
        }
        List<Long> ids = new ArrayList<>(list.size());
        Map<String, Distance> distanceMap = new HashMap<>(list.size());
        //手动分页
        list.stream().skip(from).forEach(result -> {
            String shopIdStr = result.getContent().getName();
            ids.add(Long.valueOf(shopIdStr));
            distanceMap.put(shopIdStr, result.getDistance());
            //distanceMap 缓存 id → 距离，方便后面填回
        });
        // 5.根据id查询shop
        String idStr = StrUtil.join(",", ids);
        List<Shop> shops = query()
                .in("id", ids).last("order by field(id," + idStr + ")").list();
        for (Shop shop : shops) {
            shop.setDistance(distanceMap.get(shop.getId().toString()).getValue());
        }
        // 6.返回
        return Result.ok(shops);
    }
}






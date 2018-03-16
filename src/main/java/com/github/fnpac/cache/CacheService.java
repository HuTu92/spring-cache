package com.github.fnpac.cache;

import com.github.fnpac.domain.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Created by 刘春龙 on 2018/3/16.
 */
@Service
public class CacheService {

    private static final Logger logger = LoggerFactory.getLogger(CacheService.class);

//    @Cacheable // At least one cache should be provided per cache operation.
    @Cacheable(value = "myCacheName")
    public Product findById(long id) {
        Product product = new Product();
        product.setId(id);
        product.setName("Spring Boot 实战");
        logger.info("未访问缓存...");
        return product;
    }
}
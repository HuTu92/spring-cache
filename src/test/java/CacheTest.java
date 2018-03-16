import com.alibaba.fastjson.JSON;
import com.github.fnpac.cache.CacheService;
import com.github.fnpac.config.WebApplicationConfig;
import com.github.fnpac.domain.Product;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

/**
 * Created by 刘春龙 on 2018/3/16.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@WebAppConfiguration
@ContextConfiguration(
        classes = {
                WebApplicationConfig.class
        })
public class CacheTest {

    private static final Logger logger = LoggerFactory.getLogger(CacheTest.class);

    @Autowired
    private CacheService cacheService;

    /**
     * 第一次运行（redisCacheManager.setUsePrefix(false)）：
     * 15:18:41.043 [main] INFO  com.github.fnpac.cache.CacheService - 未访问缓存...
     * 15:18:42.975 [main] INFO  CacheTest - {"id":123456,"name":"Spring Boot 实战"}
     * <p>
     * Redis中多了如下key：
     * myCacheName~keys
     * \xac\xed\x00\x05sr\x00\x0ejava.lang.Long;\x8b\xe4\x90\xcc\x8f#\xdf\x02\x00\x01J\x00\x05valuexr\x00\x10java.lang.Number\x86\xac\x95\x1d\x0b\x94\xe0\x8b\x02\x00\x00xp\x00\x00\x00\x00\x00\x01\xe2@
     * <p>
     * 第二次运行：
     * 15:30:06.234 [main] INFO  CacheTest - {"id":123456,"name":"Spring Boot 实战"}
     */
    @Test
    public void findById() {
        Product product = cacheService.findById(123456);
        logger.info(JSON.toJSONString(product));
    }

    /**
     * 第一次运行（redisCacheManager.setUsePrefix(true)）：
     * 15:32:39.487 [main] INFO  com.github.fnpac.cache.CacheService - 未访问缓存...
     * 15:32:41.525 [main] INFO  CacheTest - {"id":7890,"name":"Spring Boot 实战"}
     * <p>
     * Redis中多了如下key：
     * myCacheName:\xac\xed\x00\x05sr\x00\x0ejava.lang.Long;\x8b\xe4\x90\xcc\x8f#\xdf\x02\x00\x01J\x00\x05valuexr\x00\x10java.lang.Number\x86\xac\x95\x1d\x0b\x94\xe0\x8b\x02\x00\x00xp\x00\x00\x00\x00\x00\x00\x1e\xd2
     * <p>
     * 由此可见，添加了"[cacheName]:"前缀
     */
    @Test
    public void findByIdWithPrefix() {
        Product product = cacheService.findById(7890);
        logger.info(JSON.toJSONString(product));
    }
}

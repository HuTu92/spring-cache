# spring-cache

**_涉及特性：_**

## CacheManager

```java
public interface CacheManager {

	/**
	 * Return the cache associated with the given name.
	 * @param name the cache identifier (must not be {@code null})
	 * @return the associated cache, or {@code null} if none found
	 */
	Cache getCache(String name);

	/**
	 * Return a collection of the cache names known by this manager.
	 * @return the names of all caches known by the cache manager
	 */
	Collection<String> getCacheNames();

}
```

## AbstractCacheManager

AbstractCacheManager声明如下：

```java
public abstract class AbstractCacheManager implements CacheManager, InitializingBean {
```

从其实现`InitializingBean`接口，我们得知，

```java
@Override
public void afterPropertiesSet() {
    initializeCaches();
}
```

```java
/**
 * 初始化缓存的静态配置。通过afterPropertiesSet()启动时触发; 也可以调用在运行时重新初始化。
 * 
 * @since 4.2.2
 * @see #loadCaches()
 */
public void initializeCaches() {
    // 返回用户指定的caches（如果指定的话），以及redis中指定的caches
    Collection<? extends Cache> caches = loadCaches();

    synchronized (this.cacheMap) {
        this.cacheNames = Collections.emptySet();
        this.cacheMap.clear();
        Set<String> cacheNames = new LinkedHashSet<String>(caches.size());
        for (Cache cache : caches) {
            String name = cache.getName();
            this.cacheMap.put(name, decorateCache(cache));
            cacheNames.add(name);
        }
        this.cacheNames = Collections.unmodifiableSet(cacheNames);
    }
}

protected abstract Collection<? extends Cache> loadCaches();
```

`loadCaches`方法被`RedisCacheManager`所实现。

```java
/**
* 1. 已有指定name的Cache，直接返回
* 2. 指定name的Cache不存在
*       1. 如果当前RedisCacheManager为“动态”模式，则会创建新的cache，保存到cacheMap，并将cache name保存到cacheNames
*       2. 如果当前RedisCacheManager为“静态”模式，则不会创建新的cache，直接返回null
* @param name
*/
@Override
public Cache getCache(String name) {
    Cache cache = this.cacheMap.get(name);
    // 已有指定name的Cache，直接返回
    if (cache != null) {
        return cache;
    }
    else {
        // 指定name的Cache不存在，
        // 1. 如果当前RedisCacheManager为“动态”模式，
        //      则会创建新的cache，保存到cacheMap，并将cache name保存到cacheNames
        // 如果当前RedisCacheManager为“静态”模式，
        //      则会直接返回null
        
        // Fully synchronize now for missing cache creation...
        synchronized (this.cacheMap) {
            cache = this.cacheMap.get(name);
            if (cache == null) {
                // RedisCacheManager#getMissingCache(String name)
                cache = getMissingCache(name);
                if (cache != null) {
                    // 将setTransactionAware(boolean)设置为true会强制将Cache装饰为TransactionAwareCacheDecorator，
                    // 因此只有在成功提交周围事务后才会将值写入缓存
                    cache = decorateCache(cache);
                    this.cacheMap.put(name, cache);
                    updateCacheNames(name);
                }
            }
            return cache;
        }
    }
}
```

```java
/**
 * 将cache name加入cacheNames
 * 
 * @param name the name of the cache to be added
 */
private void updateCacheNames(String name) {
    Set<String> cacheNames = new LinkedHashSet<String>(this.cacheNames.size() + 1);
    cacheNames.addAll(this.cacheNames);
    cacheNames.add(name);
    this.cacheNames = Collections.unmodifiableSet(cacheNames);
}
```

## RedisCacheManager

CacheManager的Redis实现。

默认情况下直接保存key，而不添加前缀（作为命名空间）。
为避免冲突，建议更改此设置（将'usePrefix'设置为'true'）。

默认情况下，除非提供了一组预定义的缓存名称，否则RedisCaches将对每个getCache(String)请求进行延迟初始化。

将setTransactionAware(boolean)设置为true会强制将Cache装饰为TransactionAwareCacheDecorator，
因此只有在成功提交周围事务后才会将值写入缓存。

---

```java
/**
* 返回用户指定的caches以及redis中指定的caches
* 
* 此外，如果用户没指定configuredCacheNames（即为“动态”模式），则将redis中cache name key（以 ~keys 作为后缀，标识 cache name）构造的cache保存到了cacheMap，cache name保存到了cacheNames
*/
@Override
protected Collection<? extends Cache> loadCaches() {

    Assert.notNull(this.redisOperations, "A redis template is required in order to interact with data store");

    // 初始状态下，
    // “动态”模式：
    //          caches 为空集合;
    //          redis中cache name key（以 ~keys 作为后缀，标识 cache name）构造的cache保存到了cacheMap，cache name保存到了cacheNames
    // “静态”模式：
    //          caches 中保存了redis中cache name key（以 ~keys 作为后缀，标识 cache name）构造的cache集合 
    Set<Cache> caches = new LinkedHashSet<Cache>(
            loadRemoteCachesOnStartup ? loadAndInitRemoteCaches() : new ArrayList<Cache>());

    // “动态”模式：
    //          this.configuredCacheNames 为 Null；
    //          this.getCacheNames() 为 redis中cache name key（以 ~keys 作为后缀，标识 cache name）构造的cache集合的cacheNames；
    // “静态”模式：
    //          this.configuredCacheNames为用户所指定的cache name集合；
    //          this.getCacheNames() 为 Null；

    // 所以最终，
    // cachesToLoad要么只包含redis中指定的cache names（“动态”模式），要么只包含用户指定的cache names（“静态”模式）
    Set<String> cachesToLoad = new LinkedHashSet<String>(this.configuredCacheNames);
    cachesToLoad.addAll(this.getCacheNames());

    if (!CollectionUtils.isEmpty(cachesToLoad)) {
        // “动态”模式：caches = Empty（caches初始值，empty） + redis中指定的caches；
        // “静态”模式：caches = redis中指定的caches（caches初始值） + 用户指定的caches； 
        for (String cacheName : cachesToLoad) {
            caches.add(createCache(cacheName));
        }
    }

    return caches;
}
```

```java
/**
* 当RedisCacheManager为“动态”模式，会创建新的cache，保存到cacheMap，并将cache name保存到cacheNames，返回一个空集合
* 
* 当RedisCacheManager为“静态”模式，会创建新的cache，以集合的形式作为返回值返回
*/
protected List<Cache> loadAndInitRemoteCaches() {

    // “静态”模式下，redis中cache name key（以 ~keys 作为后缀，标识 cache name）构造的cache集合 
    List<Cache> caches = new ArrayList<Cache>();

    try {
        Set<String> cacheNames = loadRemoteCacheKeys();
        if (!CollectionUtils.isEmpty(cacheNames)) {
            for (String cacheName : cacheNames) {
                // AbstractCacheManager#getCache(String name)
                // 1. 已有指定name的Cache，直接返回
                // 2. 指定name的Cache不存在
                //       1. 如果当前RedisCacheManager为“动态”模式，则会创建新的cache，保存到cacheMap，并将cache name保存到cacheNames
                //       2. 如果当前RedisCacheManager为“静态”模式，则会直接返回null
                if (null == super.getCache(cacheName)) {// 返回null，为“静态”模式
                    caches.add(createCache(cacheName));
                }
            }
        }
    } catch (Exception e) {
        if (logger.isWarnEnabled()) {
            logger.warn("Failed to initialize cache with remote cache keys.", e);
        }
    }

    return caches;
}
```

```java
/**
* 获取redis中的cache name Set
*/
protected Set<String> loadRemoteCacheKeys() {
    // redisOperations 即 RedisTemplate
    return (Set<String>) redisOperations.execute(new RedisCallback<Set<String>>() {

        @Override
        public Set<String> doInRedis(RedisConnection connection) throws DataAccessException {
            // 以 ~keys 作为后缀，标识 cache name
            // we are using the ~keys postfix as defined in RedisCache#setName
            Set<byte[]> keys = connection.keys(redisOperations.getKeySerializer().serialize("*~keys"));
            Set<String> cacheKeys = new LinkedHashSet<String>();

            if (!CollectionUtils.isEmpty(keys)) {
                for (byte[] key : keys) {
                    cacheKeys.add(redisOperations.getKeySerializer().deserialize(key).toString().replace("~keys", ""));
                }
            }

            return cacheKeys;
        }
    });
}
```

```java
private RedisCachePrefix cachePrefix = new DefaultRedisCachePrefix();

/**
* 如果未指定一组缓存名称，则此RedisCacheManager为“动态”模式，会创建新的cache。
*/
@Override
protected Cache getMissingCache(String name) {
    return this.dynamic ? createCache(name) : null;
}

/**
* 构造Cache对象
*/
protected RedisCache createCache(String cacheName) {
    long expiration = computeExpiration(cacheName);
    // 如果设置usePrefix为true，则使用“[cacheName]:”格式前缀
    return new RedisCache(cacheName, (usePrefix ? cachePrefix.prefix(cacheName) : null), redisOperations, expiration,
            cacheNullValues);
}
```

```java
/**
* 将setTransactionAware(boolean)设置为true会强制将Cache装饰为TransactionAwareCacheDecorator，
* 因此只有在成功提交周围事务后才会将值写入缓存。
*/
@Override
protected Cache decorateCache(Cache cache) {

    if (isCacheAlreadyDecorated(cache)) {
        return cache;
    }

    return super.decorateCache(cache);
}

protected boolean isCacheAlreadyDecorated(Cache cache) {
    return isTransactionAware() && cache instanceof TransactionAwareCacheDecorator;
}
```

---

```java
// 只提供RedisTemplate入参，cacheNames设置为空的List集合
public RedisCacheManager(RedisOperations redisOperations) {
    this(redisOperations, Collections.<String> emptyList());
}
```

```java
public RedisCacheManager(RedisOperations redisOperations, Collection<String> cacheNames) {
    this(redisOperations, cacheNames, false);
}
```

```java
/**
* 构建"静态"模式的RedisCacheManager，仅管理指定缓存名称的缓存。
* 
* 注意：启用cacheNullValue时，请确保RedisOperations使用的RedisSerializer能够序列化NullValue。
*/
public RedisCacheManager(RedisOperations redisOperations, Collection<String> cacheNames, boolean cacheNullValues) {
    this.redisOperations = redisOperations;
    this.cacheNullValues = cacheNullValues;
    setCacheNames(cacheNames);
}
```

```java
/**
* 为此CacheManager的“静态”模式指定一组缓存名称。
* 
* 在调用此方法后，缓存的数量及其名称将固定不变，并且在运行时不会创建更多的缓存区域。
* 
* 使用null或空集合参数调用，会将模式重置为“动态”，从而允许再次创建缓存。
*/
public void setCacheNames(Collection<String> cacheNames) {

    Set<String> newCacheNames = CollectionUtils.isEmpty(cacheNames) ? Collections.<String> emptySet()
            : new HashSet<String>(cacheNames);

    this.configuredCacheNames = newCacheNames;
    this.dynamic = newCacheNames.isEmpty();
}
```

## DefaultRedisCachePrefix

```java
public class DefaultRedisCachePrefix implements RedisCachePrefix {

	private final RedisSerializer serializer = new StringRedisSerializer();
	private final String delimiter;

	public DefaultRedisCachePrefix() {
		this(":");
	}

	public DefaultRedisCachePrefix(String delimiter) {
		this.delimiter = delimiter;
	}

	public byte[] prefix(String cacheName) {
		return serializer.serialize((delimiter != null ? cacheName.concat(delimiter) : cacheName.concat(":")));
	}
}
```

## AbstractTransactionSupportingCacheManager

```java
@Override
protected Cache decorateCache(Cache cache) {
    return (isTransactionAware() ? new TransactionAwareCacheDecorator(cache) : cache);
}
```
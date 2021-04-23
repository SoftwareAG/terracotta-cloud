/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */

import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.AppenderBase;
import com.terracottatech.ehcache.clustered.client.config.builders.ClusteredRestartableResourcePoolBuilder;
import com.terracottatech.ehcache.clustered.client.config.builders.EnterpriseClusteringServiceConfigurationBuilder;
import org.ehcache.Cache;
import org.ehcache.PersistentCacheManager;
import org.ehcache.Status;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.MemoryUnit;
import org.ehcache.core.config.store.StoreStatisticsConfiguration;
import org.ehcache.management.registry.DefaultManagementRegistryConfiguration;
import org.ehcache.management.registry.DefaultManagementRegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.model.context.Context;
import org.terracotta.management.model.stats.ContextualStatistics;
import org.terracotta.management.registry.ResultSet;

import java.io.Serializable;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.IntStream.range;

/**
 * Run an ehcache3 based client, against the Terracotta Server
 */
public class ClientDoingInsertionsAndRetrievals {

  private static final Logger LOGGER = LoggerFactory.getLogger(ClientDoingInsertionsAndRetrievals.class);
  private static final int KB = 1024;

  private static final URI clusterUri = URI.create("terracotta://" + System.getenv("TERRACOTTA_SERVER_URL") + "/");
  private static final String cacheManagerEntityName = System.getenv("CACHE_MANAGER_ENTITY_NAME");
  private static final String[] tags = new String[]{"container", "ehcache", System.getenv("CUSTOM_TAG")};
  private static final String alias = System.getenv("CUSTOM_ALIAS");
  private static final String datarootResourceName = System.getenv("DATAROOT_RESOURCE_NAME");
  private static final String offheapResourceName = System.getenv("OFFHEAP_RESOURCE_NAME");
  private static final String resourcePoolName = System.getenv("RESOURCE_POOL_NAME");
  private static final int resourcePoolSize = Integer.valueOf(System.getenv("RESOURCE_POOL_SIZE"));
  private static final String resourcePoolSizeUnit = System.getenv("RESOURCE_POOL_SIZE_UNIT");
  private static final int clusteredDedicatedSize = Integer.valueOf(System.getenv("CLUSTERED_DEDICATED_SIZE"));
  private static final String clusteredDedicatedSizeUnit = System.getenv("CLUSTERED_DEDICATED_SIZE_UNIT");
  private static final int entryValuesMeanSizeKb = Integer.valueOf(System.getenv("ENTRY_VALUES_MEAN_SIZE_KB"));
  private static final int entryKeysSpread = Integer.valueOf(System.getenv("ENTRY_KEYS_SPREAD"));
  private static final int getPutRatio = Integer.valueOf(System.getenv("GET_PUT_RATIO"));
  private static final int poundingIntervalMs = Integer.valueOf(System.getenv("POUNDING_INTERVAL_MS"));
  private static final int additionalCaches = Integer.valueOf(System.getenv("ADDITIONAL_CACHES"));
  private static final String DEDICATED_CACHE = "dedicatedcache";
  private static final String SHARED_CACHE_1 = "shared-cache-1";
  private static final String SHARED_CACHE_2 = "shared-cache-2";
  private DefaultManagementRegistryService managementRegistryService;
  private PersistentCacheManager cacheManager;

  public static void main(String[] args) {

    LOGGER.info("**** Starting caching client with those settings : ****\n" +
        "clusterUri = " + clusterUri + "\n"
        + "cacheManagerEntityName = " + cacheManagerEntityName + "\n"
        + "tags = " + Arrays.toString(tags) + "\n"
        + "datarootResourceName = " + datarootResourceName + "\n"
        + "offheapResourceName = " + offheapResourceName + "\n"
        + "resourcePoolName = " + resourcePoolName + "\n"
        + "resourcePoolSize = " + resourcePoolSize + "\n"
        + "resourcePoolSizeUnit = " + resourcePoolSizeUnit + "\n"
        + "clusteredDedicatedSize = " + clusteredDedicatedSize + "\n"
        + "clusteredDedicatedSizeUnit = " + clusteredDedicatedSizeUnit + "\n"
        + "entryValuesMeanSizeKb = " + entryValuesMeanSizeKb + "\n"
        + "entryKeysSpread = " + entryKeysSpread + "\n"
        + "getPutRatio = " + getPutRatio + "\n"
        + "poundingIntervalMs = " + poundingIntervalMs);

    ClientDoingInsertionsAndRetrievals clientDoingInsertionsAndRetrievals = new ClientDoingInsertionsAndRetrievals();


    clientDoingInsertionsAndRetrievals.initialize();
    clientDoingInsertionsAndRetrievals.periodicallyDisplayStats();
    clientDoingInsertionsAndRetrievals.startSimulation();

  }

  private void initialize() {
    LOGGER.info("**** Setting up CacheManager and caches, connecting to : " + clusterUri + " ****");

    managementRegistryService = new DefaultManagementRegistryService(
        new DefaultManagementRegistryConfiguration()
            .setCacheManagerAlias(alias)
            .addTags(tags)
    );

    // from http://www.ehcache.org/documentation/3.3/clustered-cache.html
    CacheManagerBuilder<PersistentCacheManager> clusteredCacheManagerBuilder =
        CacheManagerBuilder.newCacheManagerBuilder()
            .with(EnterpriseClusteringServiceConfigurationBuilder.enterpriseCluster(clusterUri.resolve(cacheManagerEntityName))
                .autoCreate()
                .defaultServerResource(offheapResourceName)
                .resourcePool(resourcePoolName, resourcePoolSize, MemoryUnit.valueOf(resourcePoolSizeUnit))
                .restartable(datarootResourceName))
            .using(managementRegistryService)
            .withCache(DEDICATED_CACHE, CacheConfigurationBuilder.newCacheConfigurationBuilder(Integer.class, byte[].class,
                ResourcePoolsBuilder.newResourcePoolsBuilder()
                    .with(ClusteredRestartableResourcePoolBuilder.clusteredRestartableDedicated(offheapResourceName, clusteredDedicatedSize, MemoryUnit.valueOf(clusteredDedicatedSizeUnit))))
                .add(new StoreStatisticsConfiguration(true)))
            .withCache(SHARED_CACHE_1, CacheConfigurationBuilder.newCacheConfigurationBuilder(Integer.class, byte[].class,
                ResourcePoolsBuilder.newResourcePoolsBuilder()
                    .with(ClusteredRestartableResourcePoolBuilder.clusteredRestartableShared(resourcePoolName)))
                .add(new StoreStatisticsConfiguration(true)))
            .withCache(SHARED_CACHE_2, CacheConfigurationBuilder.newCacheConfigurationBuilder(Integer.class, byte[].class,
                ResourcePoolsBuilder.newResourcePoolsBuilder()
                    .with(ClusteredRestartableResourcePoolBuilder.clusteredRestartableShared(resourcePoolName)))
                .add(new StoreStatisticsConfiguration(true)));

    for (int i = 0; i < additionalCaches; i++) {
      clusteredCacheManagerBuilder = clusteredCacheManagerBuilder
          .withCache("more-cache-" + (4 + i), CacheConfigurationBuilder.newCacheConfigurationBuilder(
              Integer.class,
              byte[].class,
              ResourcePoolsBuilder.newResourcePoolsBuilder()
                  .with(ClusteredRestartableResourcePoolBuilder.clusteredRestartableDedicated(offheapResourceName, clusteredDedicatedSize, MemoryUnit.valueOf(clusteredDedicatedSizeUnit)))));
    }

    cacheManager = clusteredCacheManagerBuilder.build(true);
    LOGGER.info("**** CacheManager is initialized ! **** ");
  }

  private void startSimulation() {
    try {
      List<Cache<Integer, byte[]>> caches = Stream.concat(
          Stream.of(
              cacheManager.getCache(DEDICATED_CACHE, Integer.class, byte[].class),
              cacheManager.getCache(SHARED_CACHE_1, Integer.class, byte[].class),
              cacheManager.getCache(SHARED_CACHE_2, Integer.class, byte[].class)),
          range(4, 4 + additionalCaches)
              .boxed()
              .map(idx -> cacheManager.getCache("more-cache-" + idx, Integer.class, byte[].class))
      ).collect(Collectors.toList());
      Random random = new Random();
      LOGGER.info("**** Starting inserting / getting elements **** ");
      while (!Thread.currentThread().isInterrupted() && cacheManager.getStatus() == Status.AVAILABLE) {
        caches.forEach(cache -> {
          // indexes spread between 0 and entryKeysSpread
          int index = random.nextInt(entryKeysSpread);
          if (random.nextInt(getPutRatio + 1) == 0) {
            // put - value +/- 50% entryValuesMeanSizeKb
            double entryValueSize = entryValuesMeanSizeKb * (0.5 + random.nextFloat());
            byte[] randomByteArray = new byte[(int) (KB * entryValueSize)];
            random.nextBytes(randomByteArray);
            LOGGER.debug("Inserting at key  " + index + " String of size : " + entryValueSize + " KB");
            cache.put(index, randomByteArray);
          } else {
            // get
            byte[] elementFromClusteredCache = cache.get(index);
            LOGGER.debug("Getting key  " + index + (elementFromClusteredCache == null ? ", that was a miss" : ", THAT WAS A HIT !"));
          }
        });
        TimeUnit.MILLISECONDS.sleep(poundingIntervalMs);
      }
    } catch (InterruptedException e) {
      LOGGER.error("Interrupted !", e);
    } finally {
      if (cacheManager != null) {
        cacheManager.close();
      }
    }
  }

  private void periodicallyDisplayStats() {
    ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    executor.scheduleAtFixedRate(() -> {
      LOGGER.debug("Entering periodicallyDisplayStats()");
      if (cacheManager != null) {
        LOGGER.debug("Cache Manager status is : " + cacheManager.getStatus().toString());
        if (!Thread.currentThread().isInterrupted() && cacheManager.getStatus().equals(Status.AVAILABLE)) {
          List<Context> contexts = Stream.concat(
              Stream.of(DEDICATED_CACHE, SHARED_CACHE_1, SHARED_CACHE_2),
              range(4, 4 + additionalCaches)
                  .boxed()
                  .map(idx -> "more-cache-" + idx))
              .map(name -> Context.empty()
                  .with("cacheManagerName", cacheManagerEntityName)
                  .with("cacheName", name))
              .collect(Collectors.toList());
          List<String> header = Arrays.asList("Clustered:HitCount", "Clustered:MissCount", "Clustered:PutCount", "Clustered:EvictionCount");

          ResultSet<ContextualStatistics> executeQuery = managementRegistryService.withCapability("StatisticsCapability")
              .queryStatistics(header)
              .on(contexts)
              .build()
              .execute();

          StringBuilder sb = new StringBuilder("\n              |").append(String.join("|", header)).append("|\n");
          contexts.forEach(ctx -> {
            ContextualStatistics statistics = executeQuery.getResult(ctx);
            sb.append(ctx.get("cacheName")).append("|");
            header.forEach(name -> {
              try {
                statistics.getStatistic(name)
                    .ifPresent(statistic ->
                        statistic.getLatestSample()
                            .ifPresent(latestSample ->
                                sb.append(describe(name, latestSample.getSample())).append("|")));
              } catch (NoSuchElementException ignored) {
                sb.append(describe(name, null)).append("|");
              }
            });
            sb.append("\n");
          });
          LOGGER.info(sb.toString());
        } else {
          LOGGER.warn("CacheManager is not available, can't display statistics");
          executor.shutdownNow();
        }
      } else {
        LOGGER.warn("Cache Manager is null");
      }

    }, 10, 10, TimeUnit.SECONDS);
  }

  private String describe(String statName, Serializable statistic) {
    String val = statistic == null ? "null" : statistic.toString();
    // pad left
    return String.format("%" + statName.length() + "s", val);
  }

  /**
   * This appender looks for LoggingRobustResilienceStrategy, the default (and only!) resilience strategy, error logs
   * If it thinks it found enough evidence that ehcache3 won't be able to reconnect, it exits the JVM
   */
  public static class KillerAppender extends AppenderBase {
    @Override
    protected void append(Object eventObject) {
      if (eventObject instanceof LoggingEvent) {
        LoggingEvent loggingEvent = ((LoggingEvent) eventObject);
        if (loggingEvent.getMessage().contains("possible inconsistent")) {
          System.out.println("EXITING THE JVM - Ehcache can't reconnect to the cluster");
          System.exit(2);
        }
      }
    }
  }

}

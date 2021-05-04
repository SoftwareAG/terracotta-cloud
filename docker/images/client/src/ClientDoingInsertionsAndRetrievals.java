/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import net.sf.ehcache.Status;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.config.MemoryUnit;
import net.sf.ehcache.config.TerracottaClientConfiguration;
import net.sf.ehcache.config.TerracottaConfiguration;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Random;

public class ClientDoingInsertionsAndRetrievals {

  /**
   * Run a test with BigMemory using a programmatic configuration
   *
   * @throws java.io.IOException
   */
  public static void main(String[] args) throws IOException {

    String terracottaServerUrl = System.getenv("TERRACOTTA_SERVER_URL");

    if(terracottaServerUrl == null || terracottaServerUrl.trim().equals("")) {
      System.out.println("The environment variable TERRACOTTA_SERVER_URL was not set; using terracotta:9510 as the cluster url.");
      terracottaServerUrl = "terracotta:9510";
    }

    System.out.println("**** Programatically configure an instance, configured to connect to : " + terracottaServerUrl + " ****");

    Configuration managerConfiguration = new Configuration()
      .name("bigmemory-config")
      .terracotta(new TerracottaClientConfiguration().url(terracottaServerUrl).rejoin(true))
      .cache(new CacheConfiguration()
        .name("bigMemory")
        .maxEntriesLocalHeap(50)
        .maxBytesLocalOffHeap(128, MemoryUnit.MEGABYTES)
        .copyOnRead(true)
        .eternal(true)
        .terracotta(new TerracottaConfiguration())
      );

    CacheManager manager = CacheManager.create(managerConfiguration);
    try {
      Cache bigMemory = manager.getCache("bigMemory");
      //bigMemory is now ready.

      Random random = new Random();
      if (bigMemory.getSize() > 0) {
        System.out.println("**** We found some data in the cache ! I guess some other client inserted data in BigMemory ! **** ");
      }
      System.out.println("**** Starting inserting / getting elements **** ");
      while (!Thread.currentThread().isInterrupted() && manager.getStatus() == Status.STATUS_ALIVE) {
        int n = random.nextInt(1000);
        if (random.nextInt(10) < 3 && bigMemory.getSize() < 1000) {
          // put
          String s = new BigInteger(1024 * 128 * (1 + random.nextInt(10)), random).toString(16);
          System.out.println("Inserting at key  " + n + " String of size : " + s.length() + " bytes");
          bigMemory.put(new Element(n, s)); // construct a big string of 256k data
        } else {
          // get
          Element element = bigMemory.get(n);
          System.out.println("Getting key  " + n + (element == null ? ", that was a miss" : ", THAT WAS A HIT !"));
        }
        Thread.sleep(100);

      }
    } catch (InterruptedException e) {
      e.printStackTrace();
    } finally {
      if (manager != null) manager.shutdown();
    }
  }

}
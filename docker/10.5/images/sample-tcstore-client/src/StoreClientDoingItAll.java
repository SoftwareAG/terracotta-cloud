/*
 * Copyright (c) 2011-2019 Software AG, Darmstadt, Germany and/or Software AG USA Inc., Reston, VA, USA, and/or its subsidiaries and/or its affiliates and/or their licensors.
 * Use, reproduction, transfer, publication or disclosure is prohibited except as specifically provided for in your License Agreement with Software AG.
 */

import com.terracottatech.store.Dataset;
import com.terracottatech.store.DatasetReader;
import com.terracottatech.store.DatasetWriterReader;
import com.terracottatech.store.Record;
import com.terracottatech.store.StoreException;
import com.terracottatech.store.Type;
import com.terracottatech.store.UpdateOperation;
import com.terracottatech.store.configuration.DatasetConfiguration;
import com.terracottatech.store.definition.BoolCellDefinition;
import com.terracottatech.store.definition.BytesCellDefinition;
import com.terracottatech.store.definition.CellDefinition;
import com.terracottatech.store.definition.CharCellDefinition;
import com.terracottatech.store.definition.DoubleCellDefinition;
import com.terracottatech.store.definition.IntCellDefinition;
import com.terracottatech.store.definition.LongCellDefinition;
import com.terracottatech.store.definition.StringCellDefinition;
import com.terracottatech.store.indexing.IndexSettings;
import com.terracottatech.store.internal.InternalDatasetManager;
import com.terracottatech.store.manager.DatasetManager;
import com.terracottatech.store.statistics.StatisticsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class StoreClientDoingItAll {

  private static final Logger LOGGER = LoggerFactory.getLogger(StoreClientDoingItAll.class);
  private static final int KB = 1024;

  private static final String datarootResourceName = System.getenv("DATAROOT_RESOURCE_NAME");
  private static final String offheapResourceName = System.getenv("OFFHEAP_RESOURCE_NAME");
  private static final URI clusterUri = URI.create("terracotta://" + System.getenv("TERRACOTTA_SERVER_URL") + "/");
  private static final String datasetName = System.getenv("DATASET_NAME");
  private static final String[] tags = new String[]{"container", "tcstore", System.getenv("CUSTOM_TAG")};
  private static final String alias = System.getenv("CUSTOM_ALIAS");
  private static final int numberOfRecords = Integer.valueOf(System.getenv("NUMBER_OF_RECORDS"));
  private static final int entryValuesMeanSizeKb = Integer.valueOf(System.getenv("ENTRY_VALUES_MEAN_SIZE_KB"));
  private static final int poundingIntervalMs = Integer.valueOf(System.getenv("POUNDING_INTERVAL_MS"));
  private static final int poundingStreamInterval = Integer.valueOf(System.getenv("POUNDING_STREAM_INTERVAL"));
  private static final int datasetCount = Integer.valueOf(System.getenv("DATASET_COUNT"));
  private static final int instancesPerDataset = Integer.valueOf(System.getenv("INSTANCES_PER_DATASET"));

  private DatasetManager datasetManager;
  private LongCellDefinition longCellDefinition = CellDefinition.defineLong("longCell");
  private StringCellDefinition stringCellDefinition = CellDefinition.defineString("stringCell");
  private BytesCellDefinition bytesCellDefinition = CellDefinition.defineBytes("bytesCell");
  private BoolCellDefinition booleanCellDefinition = CellDefinition.defineBool("booleanCell");
  private CharCellDefinition charCellDefinition = CellDefinition.defineChar("charCell");
  private DoubleCellDefinition doubleCellDefinition = CellDefinition.defineDouble("doubleCell");
  private IntCellDefinition intCellDefinition = CellDefinition.defineInt("intCell");
  private List<Dataset<String>> instances = new ArrayList<>();
  private Thread simulatorNormalOpsThread;
  private Thread simulatorStreamOpsThread;

  public static void main(String[] args) {

    LOGGER.info("**** Starting store client with those settings : ****\n" +
        "clusterUri = " + clusterUri + "\n"
        + "datasetName = " + datasetName + "\n"
        + "datarootResourceName = " + datarootResourceName + "\n"
        + "offheapResourceName = " + offheapResourceName + "\n"
        + "entryValuesMeanSizeKb = " + entryValuesMeanSizeKb + "\n"
        + "numberOfRecords = " + numberOfRecords + "\n"
        + "datasetCount = " + datasetCount + "\n"
        + "instancesPerDataset = " + instancesPerDataset + "\n"
        + "poundingStreamInterval = " + poundingStreamInterval + "\n"
        + "poundingIntervalMs = " + poundingIntervalMs);

    LOGGER.info("**** Programmatically configure an instance, configured to connect to : " + clusterUri + " ****");
    StoreClientDoingItAll storeClientDoingItAll = new StoreClientDoingItAll();
    storeClientDoingItAll.start();
  }

  private void start() {
    try {
      datasetManager = DatasetManager.clustered(clusterUri)
          .withClientAlias(alias)
          .withClientTags(tags)
          .build();

      DatasetConfiguration configuration = datasetManager.datasetConfiguration()
          .offheap(offheapResourceName)
          .disk(datarootResourceName)
          .index(longCellDefinition, IndexSettings.BTREE)
          .index(charCellDefinition, IndexSettings.BTREE)
          .index(booleanCellDefinition, IndexSettings.BTREE)
          .build();

      for (int i = 1; i <= datasetCount; i++) {
        datasetManager.newDataset(datasetName + "-" + i, Type.STRING, configuration);
        for (int j = 1; j <= instancesPerDataset; j++) {
          Dataset<String> instance = datasetManager.getDataset(datasetName + "-" + i, Type.STRING);
          instances.add(instance);
          if(instance.reader().records().count() == 0 ) {
            preFillDataset(instance);
          }
        }
      }

      simulatorNormalOpsThread = new Thread(this::startNormalOpsSimulation, "simulatorNormalOpsThread");
      simulatorNormalOpsThread.start();

      simulatorStreamOpsThread = new Thread(this::startStreamOpsSimulation, "simulatorStreamOpsThread");
      simulatorStreamOpsThread.start();

      startStatPrinting();
    } catch (StoreException e) {
      LOGGER.error("Something went wrong with the store", e);
      closeAll();
    }
  }

  private void preFillDataset(Dataset<String> dataset) {
    DatasetWriterReader<String> datasetWriterReader = dataset.writerReader();
    Random random = new Random();
    LOGGER.info("Starting inserting " + numberOfRecords + " records to dataset ");
    for (int i = 0; i < numberOfRecords; i++) {
      datasetWriterReader.add("key-" + i,
          stringCellDefinition.newCell("value" + random.nextInt()),
          longCellDefinition.newCell(random.nextLong()),
          charCellDefinition.newCell(Character.forDigit(random.nextInt(), 36)),
          booleanCellDefinition.newCell(random.nextBoolean()),
          doubleCellDefinition.newCell(random.nextDouble()),
          intCellDefinition.newCell(random.nextInt()),
          bytesCellDefinition.newCell(generateRandomByArray(random, entryValuesMeanSizeKb))
      );
    }
    LOGGER.info("Ended inserting " + numberOfRecords + " records to dataset");
  }

  public void startNormalOpsSimulation() {
    Random random = new Random();
    while (!Thread.currentThread().isInterrupted()) {
      for (Dataset<String> dataset : instances) {
        final DatasetWriterReader<String> datasetWriterReader = dataset.writerReader();
        int n = random.nextInt(numberOfRecords);
        String key = "key-" + String.valueOf(n);
        int action = random.nextInt(8);
        try {
          switch (action) {
            case 0:
              datasetWriterReader.add(key,
                  stringCellDefinition.newCell("value" + random.nextInt()),
                  longCellDefinition.newCell(random.nextLong()),
                  charCellDefinition.newCell((char)(random.nextInt(128))),
                  booleanCellDefinition.newCell(random.nextBoolean()),
                  doubleCellDefinition.newCell(random.nextDouble()),
                  intCellDefinition.newCell(random.nextInt()),
                  bytesCellDefinition.newCell(generateRandomByArray(random, entryValuesMeanSizeKb))
              );
              LOGGER.debug("Added record with key : " + key);
              break;
            case 1:
              Optional<Record<String>> cells = datasetWriterReader.get(key);
              LOGGER.debug(cells.isPresent() ? ("Got record with key : " + key) : "Failed getting record with key : " + key);
              break;
            case 2:
              boolean update = datasetWriterReader.update(key, UpdateOperation.remove(longCellDefinition));
              LOGGER.debug(update ? ("Updated record with key : " + key) : "Failed updating record with key : " + key);
              break;
            case 3:
              boolean delete = datasetWriterReader.delete(key);
              LOGGER.debug(delete ? ("Deleted record with key : " + key) : "Failed deleting record with key : " + key);
              break;
            case 4:
              Optional<?> conditionalUpdate = datasetWriterReader.on(key).iff(booleanCellDefinition.isTrue()).update(UpdateOperation.write(booleanCellDefinition).value(false));
              LOGGER.debug(conditionalUpdate.isPresent() ? ("iff updated record with key : " + key) : "Failed iff updating record with key : " + key);
              break;
            case 5:
              Optional<Record<String>> conditionalDelete = datasetWriterReader.on(key).iff(booleanCellDefinition.isFalse()).delete();
              LOGGER.debug(conditionalDelete.isPresent() ? ("iff deleted record with key : " + key) : "Failed iff deleting record with key : " + key);
              break;
            case 6:
              try {
                generateRandomFailure(datasetWriterReader);
              } catch (Exception ignored) {
                // we know that would trigger an exception
              }
              break;
          }
          Thread.sleep(poundingIntervalMs);
        } catch (Exception e) {
          LOGGER.error("Something went wrong with the dataset instance -  exiting the loop", e);
          break;
        }
      }
    }
    closeAll();
  }

  public void startStreamOpsSimulation() {
    Random random = new Random();
    while (!Thread.currentThread().isInterrupted()) {
      instances.stream().findAny().ifPresent(dataset -> {
        final DatasetWriterReader<String> datasetWriterReader = dataset.writerReader();
        int action = random.nextInt(8);
        try {
          long count = 0L;
          switch (action) {
            case 0:
              //booleanCell is indexed
              count = datasetWriterReader.records().filter(booleanCellDefinition.isFalse()).count();
              LOGGER.debug("Counted records with booleanCell set to false : " + count);
              break;
            case 1:
              //longCell is indexed
              count = datasetWriterReader.records().filter(longCellDefinition.value().isGreaterThan(0L)).count();
              LOGGER.debug("Counted records with longCell greater than 0 : " + count);
              break;
            case 2:
              //charCell is indexed
              count = datasetWriterReader.records().filter(charCellDefinition.value().isGreaterThan('A')).count();
              LOGGER.debug("Counted records with charCell greater than 'A' : " + count);
              break;
            case 3:
              //intCell is NOT indexed !
              count = datasetWriterReader.records().filter(intCellDefinition.value().isGreaterThan(0)).count();
              LOGGER.debug("Counted records with intCell greater than 0 : " + count);
              break;
            case 4:
              //they are all indexed
              count = datasetWriterReader.records()
                  .filter(charCellDefinition.value().isLessThan('A'))
                  .filter(booleanCellDefinition.isTrue())
                  .filter(longCellDefinition.value().isLessThan(0L))
                  .count();
              LOGGER.debug("Counted records with multiple indexed cells filters : " + count);
              break;

          }
          Thread.sleep(poundingStreamInterval * 1000);
        } catch (Exception e) {
          LOGGER.error("Something went wrong with the dataset instance -  exiting the loop", e);
        }
      });
    }
    closeAll();
  }

  private void generateRandomFailure(DatasetWriterReader<String> datasetWriterReader) throws Exception {
    Random random = new Random();
    int nextInt = ThreadLocalRandom.current().nextInt(0, 5);
    switch (nextInt) {
      case 0:
        datasetWriterReader.add(null, stringCellDefinition.newCell("value" + random.nextInt()), longCellDefinition.newCell(random.nextLong()), charCellDefinition.newCell(Character.forDigit(random.nextInt(), 36)), booleanCellDefinition.newCell(random.nextBoolean()), doubleCellDefinition.newCell(random.nextDouble()), intCellDefinition.newCell(random.nextInt()), bytesCellDefinition.newCell(new BigInteger(8 * 10 * (1 + random.nextInt(10)), random).toByteArray()));
        break;
      case 1:
        datasetWriterReader.update(null, UpdateOperation.remove(longCellDefinition));
        break;
      case 2:
        datasetWriterReader.get(null);
        break;
      case 3:
        datasetWriterReader.delete(null);
        break;
      case 4:
        DatasetReader original = swapUnderlying(datasetWriterReader, null);
        try {
          datasetWriterReader.records().filter(booleanCellDefinition.isFalse()).count();
        } finally {
          swapUnderlying(datasetWriterReader, original);
        }
        break;
    }
  }

  void startStatPrinting() {
    Thread statPrintingThread = new Thread(() -> {
      while (true) {
        try {
          StatisticsService service = ((InternalDatasetManager) datasetManager).getStatisticsService();
          service.getDatasetStatistics().stream().forEach(datasetStatistics -> {
            LOGGER.info(datasetStatistics.getInstanceName());
            datasetStatistics.readAllStatistics((datasetOutcomes, number) -> {
              LOGGER.info("\t" + datasetOutcomes.getClass().getSimpleName() + datasetOutcomes + number);
            });
          });
          Thread.sleep(10_000);
        } catch (Exception e) {
          LOGGER.error("Something went wrong with the statPrintingThread -  exiting the loop", e);
          break;
        }
      }
    }, "statPrintingThread");
    statPrintingThread.setDaemon(true);
    statPrintingThread.start();
  }

  public void closeAll() {
    simulatorNormalOpsThread.interrupt();
    simulatorStreamOpsThread.interrupt();

    for (Dataset<String> instance : instances) {
      instance.close();
    }
    if (datasetManager != null) {
      datasetManager.close();
    }
  }

  /**
   * Originally from Henri Tremblay, to create failures while streaming records
   *
   * @param datasetReader
   * @param underlyingToReplaceWith
   * @return
   * @throws Exception
   */
  private DatasetReader<?> swapUnderlying(DatasetReader datasetReader, DatasetReader<?> underlyingToReplaceWith) throws Exception {
    Class<?> statisticsDatasetReaderClass = this.getClass().getClassLoader().loadClass(("com.terracottatech.store.statistics.StatisticsDatasetReader"));
    Field underlying = statisticsDatasetReaderClass.getDeclaredField("underlying");
    underlying.setAccessible(true);
    DatasetReader<?> current = (DatasetReader<?>) underlying.get(datasetReader);
    underlying.set(datasetReader, underlyingToReplaceWith);
    return current;
  }

  private byte[] generateRandomByArray(Random random, int entryValuesMeanSizeKb) {
    // byte array size - value +/- 50% entryValuesMeanSizeKb
    byte[] randomByteArray = new byte[(int) (KB * entryValuesMeanSizeKb * (0.5 + random.nextFloat()))];
    random.nextBytes(randomByteArray);
    return randomByteArray;
  }

}

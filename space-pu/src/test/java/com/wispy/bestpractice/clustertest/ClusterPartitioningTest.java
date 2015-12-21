package com.wispy.bestpractice.clustertest;

import com.gigaspaces.async.AsyncFuture;
import com.gigaspaces.async.AsyncResult;
import com.wispy.bestpractice.clustertest.model.Person;
import com.wispy.bestpractice.clustertest.test.GigaspacesTestUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.cluster.ClusterInfo;
import org.openspaces.core.cluster.ClusterInfoAware;
import org.openspaces.core.executor.AutowireTask;
import org.openspaces.core.executor.DistributedTask;
import org.openspaces.core.executor.TaskGigaSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.Serializable;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.*;

/**
 * Demonstrates that data is being stored and processed in several partitions.
 *
 * @author Leonid_Poliakov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:cluster-test-config.xml"})
public class ClusterPartitioningTest {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterPartitioningTest.class);
    private static int objectsCount = 100;
    private static String[] names = {"John", "Steven", "Paul", "MIKE", "LEO"};
    private static int waitSeconds = 5;
    private static long waitDelay = 100;

    @Autowired
    private GigaSpace space;

    @Before
    public void setUp() {
        space.clear(new Object());
    }

    @Test
    public void testCluster() throws ExecutionException, InterruptedException {
        writeData();
        waitProcessing();

        LOG.info("Sending distributed task");
        AsyncFuture<Map<Integer, Integer>> future = space.execute(new ClusterTask());
        Map<Integer, Integer> actual = future.get();

        LOG.info("Task execution result: {}", actual);
        assertNotNull(actual);
        assertTrue(actual.keySet().containsAll(asList(1, 2)));
        for (Integer objectsInPartition : actual.values()) {
            assertTrue(objectsInPartition > 0);
        }
    }

    private void writeData() {
        LOG.info("Generating {} objects", objectsCount);
        Random random = new Random();
        List<Person> data = new ArrayList<>(objectsCount);
        for (int index = 0; index < objectsCount; index++) {
            Person person = new Person();
            person.setProcessed(false);
            person.setName(names[random.nextInt(names.length)]);
            data.add(person);
        }

        LOG.info("Writing {} objects", objectsCount);
        space.writeMultiple(data.toArray());
    }

    private void waitProcessing() throws InterruptedException {
        LOG.info("Waiting {} seconds for objects to be processed", waitSeconds);
        Person template = new Person();
        template.setProcessed(true);
        int currentCount = GigaspacesTestUtils.waitAtLeastCount(space, template, objectsCount, SECONDS.toMillis(waitSeconds), waitDelay);
        assertEquals(objectsCount, currentCount);
        LOG.info("Done waiting for objects to be processed");
    }

    @AutowireTask
    public static class ClusterTask implements DistributedTask<ClusterTaskResult, Map<Integer, Integer>>, ClusterInfoAware {
        @TaskGigaSpace
        private transient GigaSpace space;
        private transient ClusterInfo clusterInfo;

        @Override
        public void setClusterInfo(ClusterInfo clusterInfo) {
            LOG.info("Cluster info {}", clusterInfo);
            this.clusterInfo = clusterInfo;
        }

        @Override
        public ClusterTaskResult execute() throws Exception {
            LOG.info("Executing task {}", clusterInfo);
            return new ClusterTaskResult(clusterInfo.getInstanceId(), space.count(new Object()));
        }

        @Override
        public Map<Integer, Integer> reduce(List<AsyncResult<ClusterTaskResult>> list) throws Exception {
            Map<Integer, Integer> results = new HashMap<>();
            for (AsyncResult<ClusterTaskResult> result : list) {
                if (result.getException() != null) {
                    throw result.getException();
                }
                ClusterTaskResult taskResult = result.getResult();
                results.put(taskResult.getPartitionId(), taskResult.getObjectsCount());
            }
            return results;
        }
    }

    public static class ClusterTaskResult implements Serializable {
        private int partitionId;
        private int objectsCount;

        public ClusterTaskResult() {
        }

        public ClusterTaskResult(int partitionId, int objectsCount) {
            this.partitionId = partitionId;
            this.objectsCount = objectsCount;
        }

        public int getPartitionId() {
            return partitionId;
        }

        public void setPartitionId(int partitionId) {
            this.partitionId = partitionId;
        }

        public int getObjectsCount() {
            return objectsCount;
        }

        public void setObjectsCount(int objectsCount) {
            this.objectsCount = objectsCount;
        }
    }
}
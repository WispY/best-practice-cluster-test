package com.wispy.bestpractice.clustertest;

import com.gigaspaces.async.AsyncResult;
import com.wispy.bestpractice.clustertest.model.City;
import com.wispy.bestpractice.clustertest.model.Company;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.executor.AutowireTask;
import org.openspaces.core.executor.DistributedTask;
import org.openspaces.core.executor.TaskGigaSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.*;

/**
 * @author Leonid_Poliakov
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:cluster-test-config.xml"})
public class DistributedTaskDirectModifyTest {
    private static final Logger LOG = LoggerFactory.getLogger(DistributedTaskDirectModifyTest.class);

    @Autowired
    private GigaSpace space;

    @Before
    public void setUp() {
        space.clear(new Object());
    }

    @Test
    public void testDistributedTask() throws ExecutionException, InterruptedException {
        writeData();

        LOG.info("Sending distributed task");
        space.execute(new ModifyTask()).get();

        LOG.info("Task was successfully executed");
        List<Company> companies = Arrays.asList(space.readMultiple(new Company()));
        for (Company company : companies) {
            // should fail
            assertEquals("iterator", company.getCity().getName());
            assertEquals("read multiple", company.getCity().getCode());
        }
    }

    private void writeData() {
        List<Company> companies = new LinkedList<>();
        for (String name : new String[]{"ActivTrak", "BlueTalon", "Ereflect", "InterCall", "MartianCraft"}) {
            Company company = new Company();
            company.setName(name);
            company.setCity(new City());
            companies.add(company);
        }
        LOG.info("Writing {} companies", companies.size());
        space.writeMultiple(companies.toArray());
    }

    @AutowireTask
    public static class ModifyTask implements DistributedTask<Integer, Integer> {
        @TaskGigaSpace
        private transient GigaSpace space;

        @Override
        public Integer execute() throws Exception {
            for (Company company : space.iterator(new Company())) {
                company.getCity().setName("iterator");
            }
            for (Company company : space.readMultiple(new Company())) {
                company.getCity().setCode("read multiple");
            }
            return null;
        }

        @Override
        public Integer reduce(List<AsyncResult<Integer>> list) throws Exception {
            for (AsyncResult<Integer> result : list) {
                if (result.getException() != null) {
                    throw result.getException();
                }
            }
            return null;
        }
    }

}
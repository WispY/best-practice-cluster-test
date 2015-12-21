package com.wispy.bestpractice.clustertest.test;

import org.openspaces.core.cluster.ClusterInfo;
import org.openspaces.pu.container.ProcessingUnitContainer;
import org.openspaces.pu.container.integrated.IntegratedProcessingUnitContainerProvider;
import org.openspaces.pu.container.spi.ApplicationContextProcessingUnitContainer;
import org.openspaces.pu.container.support.CompoundProcessingUnitContainer;
import org.openspaces.pu.container.support.ResourceApplicationContext;
import org.openspaces.pu.sla.SLA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * @author Leonid_Poliakov
 */
public class TestCluster {
    private static final Logger LOG = LoggerFactory.getLogger(TestCluster.class);

    private SLA sla;
    private String configPath;

    public void setSla(SLA sla) {
        this.sla = sla;
    }

    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }

    @PostConstruct
    public void init() throws IOException {
        LOG.info("Starting the cluster: config = {}, sla = {}", configPath, sla);
        long time = System.currentTimeMillis();

        ClusterInfo clusterInfo = new ClusterInfo();
        clusterInfo.setSchema(sla.getClusterSchema());
        clusterInfo.setNumberOfInstances(sla.getNumberOfInstances());
        clusterInfo.setNumberOfBackups(sla.getNumberOfBackups());

        IntegratedProcessingUnitContainerProvider provider = new IntegratedProcessingUnitContainerProvider();
        provider.addConfigLocation(configPath);
        provider.setClusterInfo(clusterInfo);
        ProcessingUnitContainer container = provider.createContainer();

        checkContext(container);

        time = System.currentTimeMillis() - time;
        LOG.info("Cluster initialization finished in {} seconds", oneDigit(time / 1000.0));
    }

    private void checkContext(ProcessingUnitContainer container) {
        if (container instanceof CompoundProcessingUnitContainer) {
            for (ProcessingUnitContainer actualContainer : ((CompoundProcessingUnitContainer) container).getProcessingUnitContainers()) {
                checkContext(actualContainer);
            }
        } else if (container instanceof ApplicationContextProcessingUnitContainer) {
            ResourceApplicationContext context = (ResourceApplicationContext) ((ApplicationContextProcessingUnitContainer) container).getApplicationContext();
            LOG.info("Cluster member context state: active = {}, running = {}", context.isActive(), context.isRunning());
        }
    }

    public static String oneDigit(double value) {
        return new DecimalFormat("#0.0", DecimalFormatSymbols.getInstance(Locale.US)).format(value);
    }

}
package com.wispy.bestpractice.clustertest.test;

import org.openspaces.core.GigaSpace;

/**
 * @author Leonid_Poliakov
 */
public class GigaspacesTestUtils {
    public static <T> int waitAtLeastCount(GigaSpace gigaSpace, T template, int count, long timeout, long delay) throws InterruptedException {
        long startTime = System.currentTimeMillis();
        int currentCount = 0;
        while (true) {
            Thread.sleep(delay);
            if (System.currentTimeMillis() - startTime > timeout) {
                return currentCount;
            }

            currentCount = gigaSpace.count(template);
            if (currentCount >= count) {
                return currentCount;
            }
        }
    }
}
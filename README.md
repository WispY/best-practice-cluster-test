# Best practice - Cluster integration tests
##### _This is a field engineering repository for best practice on testing partitioned spaces_
-----------------------------------------

[Introduction](#introduction)

[Unit test code](#unit-test-code)

[Unit test configuration files](#unit-test-configuration-files)

[Declaring a cluster](#declaring-a-cluster)

[Running the test](#running-the-test)

[Multiple spaces](#multiple-spaces)

## Introduction

When developing applications that run on top of Gigaspaces XAP and use a lot of XAP features, one may run into the need to test how code behaves on partitioned spaces. Usually any kind of testing is done manually or via unit tests against single-partitioned embedded space. For example, if you want to test if polling container is triggered properly, you write a test with an embedded space and a polling container in the context. In the test cases you write something to space and wait for the processing to be done, then check the results.

That works really nice for simple code, where routing and partitioning are not the goal of testing. But when the polling containers are using `ClusterInfo` and you want to test multiple of them at the same time, you will run into an issue - `ClusterInfo` or any cluster features (like routing, task distribution, cross-partition operations) are just not available in simple tests against embedded spaces. You either have to mock these features yourself to test out other parts of code, or you just give up on unit testing the cluster features at all.

This small project brings a handy way to utilize `IntegratedProcessingUnitContainer`s to run unit tests against one or multiple partitioned spaces.

## Unit test code

The goal of this project is to keep unit tests code clean and clear. So unit tests won't contain any cluster declarations, just pure test cases that utilize already running cluster. Here is an example of unit test that we want to have:

```java
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:cluster-test-config.xml"})
public class MyTest {
    @Autowired
    private GigaSpace space;

    @Before
    public void setUp() {
        space.clear(new Object());
    }

    @Test
    public void testSomething() {
        // some space interactions
    }
}
```

## Unit test configuration files

When testing partitioned spaces, it naturally appears that two (and `+1` per other clusters) configurations are required for the unit test.

One being **the configuration for cluster member** that includes definitions of polling containers, embedded space, local views, etc. This may be an exact replica of `pu.xml` file you have for the actual space in your project, but just with another name for convenience. Here is an example of cluster member configuration (you can find it as `cluster-member-config.xml` under test resources folder):

```xml
<beans xmlns="http://www.springframework.org/schema/beans" ...>

  <os-events:annotation-support/>

  <!-- Enable scan for OpenSpaces and Spring components -->
  <context:component-scan base-package="com.wispy.bestpractice.clustertest"/>

  <!-- Embedded space. -->
  <os-core:space id="space" url="/./test-space"/>
  <os-core:giga-space id="gigaSpace" space="space"/>

</beans>
```

Another **configuration is for the test itself**. It may declare multiple clusters and space proxies, so that test can interact with spaces. Here is an example of test configuration for one cluster and a space proxy to the cluster (`cluster-test-config.xml` under test resources folder):

```xml
<beans xmlns="http://www.springframework.org/schema/beans" ...>

  <os-events:annotation-support/>

  <bean id="space-cluster" class="com.wispy.bestpractice.clustertest.test.TestCluster">
    <property name="configPath" value="classpath:cluster-member-config.xml"/>
    <property name="sla">
      <os-sla:sla cluster-schema="partitioned-sync2backup" number-of-instances="2" number-of-backups="0"/>
    </property>
  </bean>

  <bean id="space" class="org.openspaces.core.space.UrlSpaceFactoryBean" depends-on="space-cluster">
    <property name="url" value="jini://*/*/test-space"/>
  </bean>
  <os-core:giga-space id="gigaSpace" space="space"/>

</beans>
```

If you or CI/CD process will run the test, the cluster with 2 primary and 0 backup partitions will be started locally (each partition will have active polling container) and only after the cluster is up and running, the actual unit test will go off. You may notice in test configuration that it is achieved by declaring the space proxy being dependent on the cluster using `depends-on` feature. Unit test autowires the `GigaSpace` bean from context, so it will wait for cluster initialization.

## Declaring a cluster

To declare a cluster in unit tests configuration, the use of standard SLA definition is suggested. You may find a familiar SLA bean in next cluster declaration:

```xml
<bean id="space-cluster" class="com.wispy.bestpractice.clustertest.test.TestCluster">
  <property name="configPath" value="classpath:cluster-member-config.xml"/>
  <property name="sla">
    <os-sla:sla cluster-schema="partitioned-sync2backup" number-of-instances="2" number-of-backups="0"/>
  </property>
</bean>
```

Here you can use any syntax of SLA definition acceptable for normal spaces.

## Running the test

Gigaspaces XAP product comes with a free tier that allows you to start single-partitioned embedded spaces. When you run tests that start a partitioned cluster, you will have to provide a proper `gslicense.xml` that would allow using standard XAP features. To do that, specify an additional VM argument that points to XAP home folder (it may be an empty folder with licence file):

```
-Dcom.gs.home=d:\xap\gigaspaces-xap-premium-10.2.0-ga
```

For example, if you are using `Maven` on your CI/CD server, the project build command would look something like this:

```
mvn clean install -Dcom.gs.home=d:\xap\gigaspaces-xap-premium-10.2.0-ga
```

## Multiple spaces

Depending on your needs when you have multiple spaces used by application, you may want to have a set of tests that work with multiple spaces simultaneously, or have one set of tests per space. Either way it is good practice to have a separate `test-utils` module in your project and put shared test-only classes there. For example, in this project you may find `TestCluster` and `GigaspacesTestUtils` classes in this module, and the use of them in `space-pu` module by declaring next dependency in `pom.xml`:

```xml
<dependency>
  <groupId>com.wispy.best-practice.cluster-test</groupId>
  <artifactId>test-utils</artifactId>
  <version>1.0</version>
  <scope>test</scope>
</dependency>
```

As for running several spaces in one test, you can declare multiple clusters with different SLAs since you are not limited to have one SLA definition within configuration (notice how space proxies are dependent on clusters):

```xml
<beans xmlns="http://www.springframework.org/schema/beans" ...>

  <os-events:annotation-support/>

  <bean id="first-space-cluster" class="com.wispy.bestpractice.clustertest.test.TestCluster">
    <property name="configPath" value="classpath:first-cluster-member-config.xml"/>
    <property name="sla">
      <os-sla:sla cluster-schema="partitioned-sync2backup" number-of-instances="2" number-of-backups="0"/>
    </property>
  </bean>

  <bean id="firstSpace" class="org.openspaces.core.space.UrlSpaceFactoryBean" depends-on="first-space-cluster">
    <property name="url" value="jini://*/*/test-first-space"/>
  </bean>
  <os-core:giga-space id="firstGigaSpace" space="firstSpace"/>


  <bean id="second-space-cluster" class="com.wispy.bestpractice.clustertest.test.TestCluster">
    <property name="configPath" value="classpath:second-cluster-member-config.xml"/>
    <property name="sla">
      <os-sla:sla cluster-schema="partitioned-sync2backup" number-of-instances="4" number-of-backups="1"/>
    </property>
  </bean>

  <bean id="secondSpace" class="org.openspaces.core.space.UrlSpaceFactoryBean" depends-on="second-space-cluster">
    <property name="url" value="jini://*/*/test-second-space"/>
  </bean>
  <os-core:giga-space id="secondGigaSpace" space="secondSpace"/>

</beans>
```

> Note that running multiple big clusters for tests is time and memory consuming process. You would have to tweak your environment configuration so that JVM does not run out of memory during the cluster initialization.

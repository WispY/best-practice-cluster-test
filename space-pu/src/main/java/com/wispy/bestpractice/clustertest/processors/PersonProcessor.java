package com.wispy.bestpractice.clustertest.processors;

import com.wispy.bestpractice.clustertest.model.Person;
import org.openspaces.events.EventDriven;
import org.openspaces.events.EventTemplate;
import org.openspaces.events.adapter.SpaceDataEvent;
import org.openspaces.events.polling.Polling;

/**
 * Processes space data.
 *
 * @author Leonid_Poliakov
 */
@EventDriven
@Polling(concurrentConsumers = 2, maxConcurrentConsumers = 8)
public class PersonProcessor {
    @EventTemplate
    public Person template() {
        Person template = new Person();
        template.setProcessed(false);
        return template;
    }

    @SpaceDataEvent
    public Person process(Person person) {
        person.setProcessed(true);
        person.setName(person.getName().toLowerCase());
        return person;
    }
}
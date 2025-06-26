package org.example.listener;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.map.listener.EntryLoadedListener;
import lombok.extern.slf4j.Slf4j;
import org.example.model.Customer;

@Slf4j
public class CustomerListener implements EntryLoadedListener<Integer, Customer>
{
    @Override
    public void entryLoaded(EntryEvent<Integer, Customer> event)
    {
        log.info("Customer loaded: {}", event.getValue());
    }
}

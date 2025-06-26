package org.example.listener;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.map.listener.EntryAddedListener;
import lombok.extern.slf4j.Slf4j;
import org.example.model.Price;
import org.example.task.PriceValidator;

@Slf4j
public class PriceListener implements EntryAddedListener<Long, Price>
{
    @Override
    public void entryAdded(EntryEvent<Long, Price> event)
    {
        this.processPrice(event.getValue());
    }

    private void processPrice(Price price)
    {
        Hazelcast.getAllHazelcastInstances()
                 .iterator()
                 .next()
                 .getExecutorService("price-processing")
                 .execute(new PriceValidator(price.getProductId()));
        log.info("Called price validator for product: {}", price.getProductId());
    }
}

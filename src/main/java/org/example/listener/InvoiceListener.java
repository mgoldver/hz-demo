package org.example.listener;

import java.util.Objects;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.IExecutorService;
import com.hazelcast.map.listener.EntryAddedListener;
import com.hazelcast.map.listener.EntryUpdatedListener;
import lombok.extern.slf4j.Slf4j;
import org.example.model.Invoice;
import org.example.task.VerySlowAndComplexCalculation;

@Slf4j
public class InvoiceListener implements EntryAddedListener<Integer, Invoice>,
                                              EntryUpdatedListener<Integer, Invoice>
{
    private IExecutorService executor;

    @Override
    public void entryAdded(EntryEvent<Integer, Invoice> event)
    {
        log.info("Invoice added: {}", event.getValue());
    }

    @Override
    public void entryUpdated(EntryEvent<Integer, Invoice> event)
    {
        log.info("Invoice updated: {}", event.getValue());
        this.performCalculation(event.getValue());
    }

    private void performCalculation(Invoice invoice)
    {
        this.getExecutor()
            .execute(new VerySlowAndComplexCalculation(invoice));
    }

    private IExecutorService getExecutor()
    {
        if (Objects.isNull(this.executor))
        {
            this.executor = Hazelcast.getAllHazelcastInstances()
                                     .iterator()
                                     .next()
                                     .getExecutorService("order-calculation");
        }
        return this.executor;
    }
}

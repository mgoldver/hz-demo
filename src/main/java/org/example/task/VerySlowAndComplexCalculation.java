package org.example.task;

import java.io.Serial;
import java.io.Serializable;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceAware;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.model.Invoice;

@Slf4j
@RequiredArgsConstructor
public class VerySlowAndComplexCalculation implements Runnable, HazelcastInstanceAware, Serializable
{
    @Serial
    private static final long serialVersionUID = -5656136045423546710L;

    private transient HazelcastInstance hazelcastInstance;
    private final Invoice invoice;

    @Override
    public void run()
    {
        try
        {
            log.info("Started calculation for invoice {}.", this.invoice);
            Thread.sleep(2000);
            this.hazelcastInstance.<Invoice>getQueue("invoice-queue")
                                  .add(this.invoice);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread()
                  .interrupt();
        }
        log.info("Performed a very complex calculation for invoice {} and submitted to the queue.", this.invoice);
    }

    @Override
    public void setHazelcastInstance(HazelcastInstance hazelcastInstance)
    {
        this.hazelcastInstance = hazelcastInstance;
    }
}

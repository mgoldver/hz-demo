package org.example.task;

import java.io.Serial;
import java.io.Serializable;

import com.hazelcast.aggregation.Aggregators;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceAware;
import com.hazelcast.map.IMap;
import com.hazelcast.query.Predicates;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.model.Price;

@Slf4j
@RequiredArgsConstructor
public class PriceValidator implements Runnable, HazelcastInstanceAware, Serializable
{
    @Serial
    private static final long serialVersionUID = -5656136045423546710L;

    private transient HazelcastInstance hazelcastInstance;
    private final int productId;

    @Override
    public void setHazelcastInstance(HazelcastInstance hazelcastInstance)
    {
        this.hazelcastInstance = hazelcastInstance;
    }

    @Override
    public void run()
    {
        IMap<Long, Price> map = hazelcastInstance.getMap("price");
        long count = map.aggregate(Aggregators.count(), Predicates.equal("productId", productId));

        if (count > 10)
        {
            log.info("PRICE UPDATE THRESHOLD BREACHED FOR PRODUCT {}. count: {}", productId, count);
        }
    }
}

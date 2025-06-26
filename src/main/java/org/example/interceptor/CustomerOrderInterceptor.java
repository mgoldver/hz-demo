package org.example.interceptor;

import java.io.Serial;

import com.hazelcast.map.MapInterceptor;
import com.hazelcast.nio.serialization.genericrecord.GenericRecord;
import org.example.model.CustomerOrder;

public class CustomerOrderInterceptor implements MapInterceptor
{
    @Serial
    private static final long serialVersionUID = 5694635270740682792L;

    @Override
    public Object interceptGet(Object value)
    {
        if (value instanceof GenericRecord genericRecord)
        {
            return CustomerOrder.toCustomerOrder(genericRecord);
        }
        return null;
    }

    @Override
    public void afterGet(Object value)
    {
    }

    @Override
    public Object interceptPut(Object oldValue, Object newValue)
    {
        if (newValue instanceof CustomerOrder order)
        {
            return CustomerOrder.toGenericRecord(order);
        }
        return null;
    }

    @Override
    public void afterPut(Object value)
    {
    }

    @Override
    public Object interceptRemove(Object removedValue)
    {
        return null;
    }

    @Override
    public void afterRemove(Object oldValue)
    {
    }
}

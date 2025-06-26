package org.example;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.example.interceptor.CustomerOrderInterceptor;
import org.example.model.CustomerOrder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@RequiredArgsConstructor
public class Application
{
    private final HazelcastInstance hazelcastInstance;

    public static void main(String[] args)
    {
        SpringApplication.run(Application.class, args);
    }

    @PostConstruct
    public void init()
    {
        IMap<Object, Object> customers = hazelcastInstance.getMap("customer");
        IMap<Object, Object> products = hazelcastInstance.getMap("product");
        IMap<Integer, CustomerOrder> orders = hazelcastInstance.getMap("customer-order");
        orders.addInterceptor(new CustomerOrderInterceptor());
    }
}

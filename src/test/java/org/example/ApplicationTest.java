package org.example;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Random;
import java.util.Set;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.example.model.Customer;
import org.example.model.CustomerOrder;
import org.example.model.Invoice;
import org.example.model.Price;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@Slf4j
class ApplicationTest
{
    private static HazelcastInstance instance;

    @BeforeAll
    static void beforeAll()
    {
        instance = HazelcastClient.newHazelcastClient(new ClientConfig().setClusterName("hz-demo"));
    }

    @AfterAll
    static void afterAll()
    {
        instance.shutdown();
    }

    @Test
    @DisplayName("GenericMapLoader: Check customer map is loaded with correct data")
    void checkCustomerMapIsLoaded() throws InterruptedException
    {
        IMap<Integer, Customer> map = instance.getMap("customer");
        map.evictAll();
        Assertions.assertThat(map.size())
                  .isEqualTo(0);

        map.loadAll(true);

        Thread.sleep(300);
        Assertions.assertThat(map.size())
                  .isEqualTo(1000);
        Assertions.assertThat(map.get(100))
                  .isInstanceOf(Customer.class);
    }

    @Test
    @DisplayName("GenericMapStore: Check customer orders map is loaded with correct data")
    void checkOrdersMapIsLoadedWithCorrectType()
    {
        IMap<Integer, CustomerOrder> orders = instance.getMap("customer-order");

        Object order = orders.get(100);
        log.info("Order: {}", order);
        Assertions.assertThat(order)
                  .isInstanceOf(CustomerOrder.class)
                  .hasFieldOrPropertyWithValue("id", 100);
    }

    @Test
    @DisplayName("GenericMapStore: update customer order in the map propagated to the DB")
    void updatingOrderData() throws InterruptedException
    {
        IMap<Integer, CustomerOrder> orders = instance.getMap("customer-order");

        orders.put(99999, CustomerOrder.builder()
                                       .id(99999)
                                       .customerId(20)
                                       .productId(15)
                                       .quantity(10)
                                       .orderDate(LocalDate.now())
                                       .build());

        Thread.sleep(Duration.ofSeconds(2)
                             .toMillis());

        orders.evict(99999);

        Assertions.assertThat(orders.get(99999))
                  .isInstanceOf(CustomerOrder.class)
                  .hasFieldOrPropertyWithValue("id", 99999)
                  .hasFieldOrPropertyWithValue("quantity", 10);
    }

    @Test
    @DisplayName("Customer Streaming using Journal with Jet Pipeline")
    void customerStreamingWithJetPipeline() throws InterruptedException
    {
        IMap<Integer, Customer> map = instance.getMap("customer");
        Random random = new Random(17);

        for (int i = 0; i < 100; i++)
        {
            int id = random.nextInt(1000);
            Customer customer = Customer.builder()
                                        .id(id)
                                        .name("Customer " + i)
                                        .email("customer" + i + "@example.com")
                                        .age(18 + random.nextInt(80))
                                        .build();

            map.put(id, customer);
            log.info("Customer added: {}", customer);
            Thread.sleep(100);
        }
    }

    @Test
    @DisplayName("When a record is changed, perform a complex computation and send a result to some topic")
    void useCaseOne_reactingToEvents() throws Exception
    {
        IMap<Integer, Invoice> map = instance.getMap("invoice");
        map.put(1, Invoice.builder()
                          .id(1)
                          .customerId(5)
                          .productId(20)
                          .price(20.5)
                          .invoiceDate(LocalDate.now())
                          .dueDate(LocalDate.now()
                                            .plusDays(3))
                          .build());

        Invoice invoice = map.get(1);

        invoice.setQuantity(200);
        map.put(1, invoice);

        Thread.sleep(3000);

        Assertions.assertThat(instance.getQueue("invoice-queue")
                                      .size())
                  .isEqualTo(1);
    }

    @Test
    @DisplayName("Accumulate changes based on product id and perform an operation when some threshold is breached")
    void useCase2_triggeringBasedOnCondition() throws Exception
    {
        IMap<Long, Price> map = instance.getMap("price");
        Random random = new Random(17);

        for (int i = 0; i < 200; i++)
        {
            int id = random.nextInt(10, 20);
            map.put(System.nanoTime(), Price.builder()
                                            .productId(id)
                                            .price(random.nextDouble() * 1000)
                                            .build());
            Thread.sleep(50);
        }
    }
}
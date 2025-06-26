package org.example.model;

import java.time.LocalDate;

import com.hazelcast.nio.serialization.genericrecord.GenericRecord;
import com.hazelcast.nio.serialization.genericrecord.GenericRecordBuilder;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CustomerOrder
{
    private int id;
    private int customerId;
    private int productId;
    private int quantity;
    private LocalDate orderDate;

    public static CustomerOrder toCustomerOrder(GenericRecord genericRecord)
    {
        return builder()
                       .id(genericRecord.getInt32("id"))
                       .customerId(genericRecord.getInt32("customer_id"))
                       .productId(genericRecord.getInt32("product_id"))
                       .quantity(genericRecord.getInt32("quantity"))
                       .orderDate(genericRecord.getDate("order_date"))
                       .build();
    }

    public static GenericRecord toGenericRecord(CustomerOrder order)
    {
        return GenericRecordBuilder.compact("customer-order")
                                   .setInt32("id", order.getId())
                                   .setInt32("customer_id", order.getCustomerId())
                                   .setInt32("product_id", order.getProductId())
                                   .setInt32("quantity", order.getQuantity())
                                   .setDate("order_date", order.getOrderDate())
                                   .build();
    }
}

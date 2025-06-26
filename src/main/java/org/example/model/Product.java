package org.example.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.hazelcast.nio.serialization.genericrecord.GenericRecord;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Product
{
    private int id;
    private String name;
    private String description;
    private double price;
    private LocalDateTime last_modified;
}

package org.example.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Price
{
    private int productId;
    private double price;
}

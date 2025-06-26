package org.example.model;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Invoice implements Serializable
{
    @Serial
    private static final long serialVersionUID = -6610460977919175896L;

    private int id;
    private int customerId;
    private int productId;
    private int quantity;
    private double price;
    private LocalDate invoiceDate;
    private LocalDate dueDate;
    private double totalPrice;
}

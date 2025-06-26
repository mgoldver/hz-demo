package org.example.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Customer
{
    private int id;
    private String name;
    private String email;
    private int age;
}

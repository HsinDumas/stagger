package com.example.stagger.demo;

/**
 * Payload for creating an order.
 */
public class CreateOrderRequest {

    /** Customer identifier. */
    private String customerId;

    /** Order amount in cents. */
    private Long amount;

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }
}

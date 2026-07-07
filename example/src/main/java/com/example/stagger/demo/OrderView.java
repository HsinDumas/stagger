package com.example.stagger.demo;

/**
 * Returned order view object.
 */
public class OrderView {

    /** Order ID. */
    private Long id;

    /** Current order status. */
    private String status;

    /** Amount in cents. */
    private Long amount;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getAmount() {
        return amount;
    }

    public void setAmount(Long amount) {
        this.amount = amount;
    }
}

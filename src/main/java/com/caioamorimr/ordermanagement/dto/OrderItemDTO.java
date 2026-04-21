package com.caioamorimr.ordermanagement.dto;

import com.caioamorimr.ordermanagement.entities.OrderItem;

import java.math.BigDecimal;

public class OrderItemDTO {

    private ProductDTO product;
    private Integer quantity;
    private BigDecimal price;
    private BigDecimal subTotal;

    public OrderItemDTO() {
    }

    public OrderItemDTO(OrderItem item) {
        this.product = new ProductDTO(item.getProduct());
        this.quantity = item.getQuantity();
        this.price = item.getPrice();
        this.subTotal = item.getSubTotal();
    }

    public ProductDTO getProduct() {
        return product;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getSubTotal() {
        return subTotal;
    }
}

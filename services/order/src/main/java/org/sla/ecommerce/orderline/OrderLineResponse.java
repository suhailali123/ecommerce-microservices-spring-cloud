package org.sla.ecommerce.orderline;

public record OrderLineResponse(
        Integer id,
        double quantity
) {
}
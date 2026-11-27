package org.sla.ecommerce.kafka;

import org.sla.ecommerce.customer.CustomerResponse;
import org.sla.ecommerce.order.PaymentMethod;
import org.sla.ecommerce.product.PurchaseResponse;

import java.math.BigDecimal;
import java.util.List;

public record OrderConfirmation(
        String orderReference,
        BigDecimal totalAmount,
        PaymentMethod paymentMethod,
        CustomerResponse customer,
        List<PurchaseResponse> products
) {
}
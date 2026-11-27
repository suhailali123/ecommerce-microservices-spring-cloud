package org.sla.ecommerce.payment;

import org.sla.ecommerce.customer.CustomerResponse;
import org.sla.ecommerce.order.PaymentMethod;

import java.math.BigDecimal;

public record PaymentRequest(
        BigDecimal amount,
        PaymentMethod paymentMethod,
        Integer orderId,
        String orderReference,
        CustomerResponse customer
) {
}
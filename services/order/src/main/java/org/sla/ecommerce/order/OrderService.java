package org.sla.ecommerce.order;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.sla.ecommerce.customer.CustomerClient;
import org.sla.ecommerce.exception.BusinessException;
import org.sla.ecommerce.kafka.OrderConfirmation;
import org.sla.ecommerce.kafka.OrderProducer;
import org.sla.ecommerce.orderline.OrderLineRequest;
import org.sla.ecommerce.orderline.OrderLineService;
import org.sla.ecommerce.payment.PaymentClient;
import org.sla.ecommerce.payment.PaymentRequest;
import org.sla.ecommerce.product.ProductClient;
import org.sla.ecommerce.product.PurchaseRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository repository;
    private final OrderMapper mapper;
    private final CustomerClient customerClient;
    private final PaymentClient paymentClient;
    private final ProductClient productClient;
    private final OrderLineService orderLineService;
    private final OrderProducer orderProducer;

    @Transactional
    public Integer createOrder(OrderRequest request) {
        var customer = this.customerClient.findCustomerById(request.customerId())
                .orElseThrow(() -> new BusinessException("Cannot create order:: No customer exists with the provided ID"));

        var purchasedProducts = productClient.purchaseProducts(request.products());

        BigDecimal totalAmount = calculateOrderAmount(purchasedProducts, request.amount());

        var order = this.repository.save(mapper.toOrder(request, totalAmount));

        for (PurchaseRequest purchaseRequest : request.products()) {
            orderLineService.saveOrderLine(
                    new OrderLineRequest(
                            null,
                            order.getId(),
                            purchaseRequest.productId(),
                            purchaseRequest.quantity()
                    )
            );
        }

        var paymentRequest = new PaymentRequest(
                totalAmount,
                request.paymentMethod(),
                order.getId(),
                order.getReference(),
                customer
        );
        paymentClient.requestOrderPayment(paymentRequest);

        orderProducer.sendOrderConfirmation(
                new OrderConfirmation(
                        request.reference(),
                        totalAmount,
                        request.paymentMethod(),
                        customer,
                        purchasedProducts
                )
        );

        return order.getId();
    }

    public List<OrderResponse> findAllOrders() {
        return this.repository.findAll()
                .stream()
                .map(this.mapper::fromOrder)
                .collect(Collectors.toList());
    }

    public OrderResponse findById(Integer id) {
        return this.repository.findById(id)
                .map(this.mapper::fromOrder)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("No order found with the provided ID: %d", id)
                ));
    }

    private BigDecimal calculateOrderAmount(List<org.sla.ecommerce.product.PurchaseResponse> purchasedProducts,
                                            BigDecimal fallbackAmount) {
        if (purchasedProducts == null || purchasedProducts.isEmpty()) {
            return fallbackAmount;
        }
        return purchasedProducts.stream()
                .map(product -> product.price().multiply(BigDecimal.valueOf(product.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
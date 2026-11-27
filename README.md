# E-commerce Microservices

Event-driven Spring Boot and Spring Cloud microservices for placing orders, processing
payments and sending notifications, with centralized configuration, service discovery,
distributed tracing and a local Docker infrastructure.

## Architecture

```
Client → API Gateway (:8222) → Order Service ──Kafka (:9092)──► Notification Service (:8040)
              │                     │                              ├─► MailDev (:1080 UI / :1025 SMTP)
              │                     └─ OpenFeign ─► Customer /     └─► MongoDB (:27017, db: notification)
              │                                     Product / Payment ─► PostgreSQL (:5432)
   Eureka Discovery (:8761) · Config Server (:8888) · Keycloak (:9098) · Zipkin (:9411)
```

![Architecture](docs/architecture.png)

## Tech stack
- **Runtime:** Java 17, Spring Boot <version from pom.xml>, Spring Cloud (Config, Eureka, LoadBalancer, OpenFeign)
- **Messaging:** Apache Kafka 7.5 (Confluent) with Zookeeper
- **Databases:** PostgreSQL 16 (domain services), MongoDB 7.0 (notifications)
- **Observability and email:** Micrometer Tracing (Brave) + Zipkin, MailDev
- **Security:** Keycloak 24.0.1 (OAuth 2.0 / OpenID Connect)
- **Tooling and containers:** Docker Compose, pgAdmin, Mongo Express

## Services (Spring Boot, run locally)

| Service | Port | Responsibility |
|---|---|---|
| config-server | 8888 | Serves externalized YAML configuration to the microservices |
| discovery | 8761 | Eureka service registry and instance health tracking |
| gateway | 8222 | Entry-point routing and token validation |
| customer-service | <port> | Customer data |
| product-service | <port> | Product catalog and stock |
| order-service | <port> | Creates orders (`POST /api/v1/orders`) and publishes order and payment events |
| payment-service | <port> | Payment processing and payment events |
| notification-service | 8040 | Consumes `order-topic` and `payment-topic`, stores notifications in MongoDB, sends emails via MailDev |

## Infrastructure (Docker Compose)

| Container | Image | Host port | Purpose |
|---|---|---|---|
| keycloak-ms | keycloak:24.0.1 | 9098 (→ 8080) | Identity provider (OAuth 2.0 / OIDC) |
| ms_kafka | cp-kafka:7.5.0 | 9092 | Event streaming |
| zookeeper | cp-zookeeper:7.5.0 | 22181 (→ 2181) | Kafka coordination |
| ms_pg_sql | postgres:16-alpine | 5432 | Relational database |
| ms_pgadmin | pgadmin4 | 5050 (→ 80) | PostgreSQL admin UI |
| ms_mongo_db | mongo:7.0 | 27017 | Notification store |
| ms_mongo_express | mongo-express | 8081 | MongoDB admin UI |
| ms_mail_dev | maildev | 1080 (UI), 1025 (SMTP) | Local email testing |
| zipkin | openzipkin/zipkin | 9411 | Distributed tracing UI |

## Getting started

**Prerequisites:** JDK 17, Maven, Docker and Docker Compose

```bash
git clone https://github.com/suhailali123/e-commerce-app.git
cd e-commerce-app/services
docker compose up -d
docker compose ps        # all containers should be Up / healthy
```

Start the Spring services in this order: config-server → discovery → gateway → customer, product, payment, order → notification-service.

| UI | URL |
|---|---|
| Eureka dashboard | http://localhost:8761 |
| Keycloak admin | http://localhost:9098 |
| MailDev inbox | http://localhost:1080 |
| Zipkin | http://localhost:9411 |
| Mongo Express | http://localhost:8081 |
| pgAdmin | http://localhost:5050 |

Notes:
- **MongoDB:** authenticated access (`authSource=admin`); credentials are served through Config Server.
- **Keycloak:** import `<realm-file>.json`, and use the test user `<username>` (local development only).
- Credentials in this project are for local development only. Change them for any shared environment.

## Try it

1. Get a token (the password grant is used here for local testing only):
   ```bash
   curl -X POST "http://localhost:9098/realms/<realm>/protocol/openid-connect/token" \
     -H "Content-Type: application/x-www-form-urlencoded" \
     -d "grant_type=password&client_id=<client-id>&username=<username>&password=<password>"
   ```
2. Place an order through the gateway:
   ```bash
   curl -X POST "http://localhost:8222/api/v1/orders" \
     -H "Content-Type: application/json" \
     -H "Authorization: Bearer <token>" \
     -d '{
       "orderReference": "ORD-20261122-106",
       "amount": 149.99,
       "paymentMethod": "CREDIT_CARD",
       "customerId": "<customer-id>",
       "products": [{ "productId": 152, "quantity": 1 }]
     }'
   ```
3. Verify the result:
   - notification-service logs show `order-topic` and `payment-topic` consumption
   - Mongo Express (`notification` database) shows the saved notification records
   - MailDev shows the Order Confirmation and Payment Confirmation emails
4. Open Zipkin, select a service, set the lookback window and click **Run Query** to inspect the trace.

## What I changed and fixed
- **Upgrade:** <old version> → Spring Boot <new version> on Java 17, with Spring Cloud Config and Eureka integration.
- **MongoDB authentication (error 13, `credential=null`):** bound the credentials through Config Server with `authSource=admin`, so inserts are authenticated.
- **MailDev SMTP timeouts:** raised connection, read and write timeouts so concurrent order and payment emails no longer fail.
- **Zipkin span export:** added `spring-boot-starter-zipkin` and configured sampling and the endpoint (`http://localhost:9411/api/v2/spans`) so spans appear in Zipkin.

## Known limitations
- Trace context is not yet propagated across every HTTP client and Kafka producer/consumer.
- <State honestly whether the gateway enforces Keycloak tokens on all routes.>

## Roadmap
- Transactional outbox pattern for reliable order event publishing
- Unit and integration tests (JUnit 5, Mockito, Testcontainers)
- React front end for order placement

## Credits
Architecture based on a public Spring Cloud tutorial series, then upgraded, debugged and extended by me.

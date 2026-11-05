# Saga Choreography Demo

A demo of the **choreography-based Saga pattern** with Spring Boot microservices that communicate asynchronously over Apache Kafka.

There is no central coordinator. Each service listens to one Kafka topic, does its own step, and publishes a message for the next service. The steps of a purchase are:

1. reserve stock (`product-service`),
2. create an order (`order-service`),
3. process the payment (`payment-service`),
4. update the order to `PAID`, or cancel the order and restore the stock if something failed.

Compensation is also choreographed: when a step fails, the failing service publishes a message that causes the earlier services to undo their work.

Every purchase carries an `operationId` UUID through all the messages, so the whole flow can be traced. A separate `log-journal-service` records every message and exports traces to Jaeger.

> **Current state:** `PaymentServiceImpl.pay` always throws `RuntimeException("Payment failed")`, and the working code is commented out. Every purchase therefore takes the compensation path. See [Known limitations](#known-limitations).

---

## Table of contents

- [Architecture](#architecture)
- [Modules](#modules)
- [Project structure](#project-structure)
- [Tech stack](#tech-stack)
- [REST API](#rest-api)
- [Kafka topics](#kafka-topics)
- [Messages (DTO module)](#messages-dto-module)
- [Saga flow](#saga-flow)
- [Order and stock state](#order-and-stock-state)
- [Logging and tracing](#logging-and-tracing)
- [Data model and seed data](#data-model-and-seed-data)
- [Configuration reference](#configuration-reference)
- [Running locally](#running-locally)
- [Running the tests](#running-the-tests)
- [Trying it out](#trying-it-out)
- [Build notes](#build-notes)
- [Known limitations](#known-limitations)

---

## Architecture

```
  HTTP POST /product/buy
 ─────────────────────────►┌──────────────────────┐
                           │  product-service     │  :8081  stock
                           └──────────┬───────────┘
                                      │ CreateOrderAction         ▲ ProductRollbackAction
                                      ▼ (order-topic)             │ (product-topic)
                           ┌──────────────────────┐               │
                           │  order-service       │  :8082  orders
                           └──────────┬───────────┘───────────────┘
                                      │ ProcessPaymentAction      ▲ CancelOrderAction,
                                      ▼ (payment-topic)           │ UpdateOrderToPaidStatusAction
                           ┌──────────────────────┐               │ (order-topic)
                           │  payment-service     │  :8083  payments
                           └──────────────────────┘───────────────┘

   log-journal-service (:8085) is an extra consumer on all three topics
   (its own consumer group). It journals messages and exports traces.
```

Key design points:

- **No central orchestrator.** Each service knows only the topic of the next step, not the overall flow.
- **Each service owns its data.** Each business service has its own in-memory H2 database.
- **Compensation is also choreographed.** A failure travels back along the topics: payment → order → product.
- **One topic per service.** A service consumes only its own topic, and other services publish to it.
- **Operation ID for correlation.** Every message extends `BaseTracing` and carries the same `operationId`.

---

## Modules

The project is a Maven multi-module build. The parent POM is `pom.xml` at the root.

| Module | Port | Role | Persistence | REST | Consumes | Publishes to |
|---|---|---|---|---|---|---|
| `dto` | – | Shared message contracts and topic constants. Library | – | – | – | – |
| `product-service` | **8081** | Stock: reserves and restores quantity | H2 `jdbc:h2:mem:product` | `POST /product/buy` | `product-topic` | `order-topic` |
| `order-service` | **8082** | Order lifecycle (`NEW`, `PAID`, `CANCELLED`) | H2 `jdbc:h2:mem:order` | H2 console only | `order-topic` | `payment-topic`, `product-topic` |
| `payment-service` | **8083** | Payment processing (currently always fails) | H2 `jdbc:h2:mem:payment` | H2 console only | `payment-topic` | `order-topic` |
| `log-journal-service` | **8085** | Journals every message in H2 and exports traces to Jaeger | H2 `jdbc:h2:mem:logjournal` | H2 console only | all three topics | – |

Ports come from each module's `application.properties` (`server.port`).

---

## Project structure

```
saga-choreography-demo/
├── pom.xml                                  # parent POM: Spring Boot 3.4.4, module list, dependency versions
├── dto/                                     # shared messages (library)
│   └── src/main/java/com/saga/dto/
│       ├── constant/KafkaConstants.java     # REPLICAS = 3, PARTITIONS = 3
│       ├── trace/BaseTracing.java           # operationId, parent of every message
│       ├── product/                         # ProductReserveAction, ProductRollbackAction
│       ├── order/                           # CreateOrderAction, CancelOrderAction, UpdateOrderToPaidStatusAction
│       └── payment/                         # ProcessPaymentAction
├── product-service/
│   └── src/main/java/com/saga/product/
│       ├── controller/ProductController.java   # POST /product/buy
│       ├── kafka/ProductHandler.java           # reserve and rollback stock
│       ├── service/ (ProductService, impl/)
│       ├── dao/ProductRepository.java
│       ├── entity/ProductEntity.java
│       └── dto/BuySomethingRequest.java        # REST request body
├── order-service/
│   └── src/main/java/com/saga/order/
│       ├── kafka/OrderHandler.java             # create, cancel, mark as paid
│       ├── service/ (OrderService, impl/)
│       ├── dao/OrderRepository.java
│       ├── entity/ (OrderEntity, OrderStatus)
│       └── util/CreditCardUtils.java           # random card number generator
├── payment-service/
│   └── src/main/java/com/saga/payment/
│       ├── kafka/PaymentHandler.java           # process payment, reply to order-topic
│       ├── service/ (PaymentService, impl/)
│       ├── dao/PaymentRepository.java
│       └── entity/PaymentEntity.java
└── log-journal-service/
    ├── README.md                               # module-level README (see note in Known limitations)
    └── src/main/java/com/saga/log/
        ├── kafka/KafkaLogListener.java         # consumes all three topics, opens spans
        ├── service/ (KafkaLogService, impl/)   # saves messages, query methods
        ├── repository/KafkaMessageRepository.java
        ├── entity/KafkaMessageEntity.java
        └── config/ (JacksonConfig, JaegerConfig, KafkaConfig)
```

Each business service follows the same layering: `kafka` (listener) → `service` → `dao` → `entity`. Each service's `config/KafkaConfiguration` creates its own topic at startup.

---

## Tech stack

- Java 27 (`java.version`), compiled with `maven.compiler.release` 25 (see [Build notes](#build-notes))
- Spring Boot 3.4.4 (parent POM), with Spring Web, Spring Data JPA, Spring for Apache Kafka 3.3.4
- H2 2.3.232 (in-memory)
- Lombok 1.18.48 (`@SuperBuilder` on the message classes)
- JUnit 6.1.3 and Mockito 5.24.0 for unit tests (pinned in the parent POM)
- OpenTelemetry SDK 1.49.0 and the Jaeger exporter 1.34.1 (log-journal only)
- Maven (multi-module)

---

## REST API

Only `product-service` has an HTTP endpoint for business operations.

### `POST /product/buy`

Starts a purchase by reserving stock for a product.

- **Base URL:** `http://localhost:8081`
- **Content-Type:** `application/json`

**Request body**

| Field | Type | Required | Description |
|---|---|---|---|
| `productId` | UUID string | yes | ID of the product to reserve |
| `quantity` | integer | yes | Number of units to buy |

**Responses**

| Status | When | Body |
|---|---|---|
| `200 OK` | The `ProductReserveAction` was handed to Kafka | empty |
| `400 Bad Request` | Body is not valid JSON, or `productId` is not a UUID | – |

**Important:** `200 OK` only means the saga was *started*. The controller does not wait for the outcome or check the Kafka send result. A generated `operationId` is attached to the message, so you can find the flow in the logs and in the log journal.

**Example**

```bash
curl -i -X POST http://localhost:8081/product/buy \
  -H 'Content-Type: application/json' \
  -d '{"productId": "00000000-0000-0000-0000-000000000001", "quantity": 2}'
```

```
HTTP/1.1 200
```

The other services expose no business endpoints. `order-service` and `payment-service` have web starters, so their H2 consoles are available, but they have no controllers.

---

## Kafka topics

| Topic | Created by | Consumed by | Messages it carries |
|---|---|---|---|
| `product-topic` | product-service | product-service | `ProductReserveAction`, `ProductRollbackAction` |
| `order-topic` | order-service | order-service | `CreateOrderAction`, `CancelOrderAction`, `UpdateOrderToPaidStatusAction` |
| `payment-topic` | payment-service | payment-service | `ProcessPaymentAction` |

Each service publishes the next step's message to the topic of the service that handles it:

| Published by | Message | Sent to |
|---|---|---|
| product-service | `CreateOrderAction` | `order-topic` |
| order-service | `ProcessPaymentAction` | `payment-topic` |
| order-service | `ProductRollbackAction` | `product-topic` |
| payment-service | `UpdateOrderToPaidStatusAction` | `order-topic` |
| payment-service | `CancelOrderAction` | `order-topic` |

**Topic settings.** Each service creates its topic with **3 partitions and 3 replicas**, from `KafkaConstants`. This needs at least 3 brokers.

**Message format.** Messages are JSON. Producers use `StringSerializer` for keys and `JsonSerializer` for values. Consumers use `StringDeserializer` and `JsonDeserializer`, trusting `com.saga.dto.*`. Messages are sent without a key, so ordering across messages of the same operation is not guaranteed.

**Consumer groups.** Each service uses its own group (`product-service`, `order-service`, `payment-service`). `log-journal-service` consumes all three topics under its own group.

---

## Messages (DTO module)

All messages live in `dto/src/main/java/com/saga/dto/`. Each extends `BaseTracing`, which holds the `operationId`. They use Lombok `@Data` and `@SuperBuilder`, and `@EqualsAndHashCode(callSuper = true)`.

| Class | Fields (besides `operationId`) | Consumed by | Purpose |
|---|---|---|---|
| `ProductReserveAction` | `productId`, `quantity` | product-service | Reserve stock |
| `ProductRollbackAction` | `productId`, `quantity` | product-service | Restore stock |
| `CreateOrderAction` | `productId`, `quantity` | order-service | Create an order in `NEW` |
| `ProcessPaymentAction` | `orderId`, `productId`, `amountToBePaid`, `creditCardNumber`, `quantity` | payment-service | Charge the order |
| `UpdateOrderToPaidStatusAction` | `orderId` | order-service | Mark the order `PAID` |
| `CancelOrderAction` | `orderId`, `productId`, `quantity` | order-service | Mark the order `CANCELLED`, then trigger stock restore |

There are no separate event classes. Each service's reply is an action on the next service's topic.

---

## Saga flow

### Happy path (intended)

The payment service's success branch is commented out in the current code, so this path cannot run as-is. It shows the intended flow.

```
Client      product-service      order-service      payment-service
  │ POST /product/buy │                  │                   │
  │──────────────────►│ buyProduct       │                   │
  │                   │  stock -= qty    │                   │
  │                   │ CreateOrderAction│                   │
  │                   │─────────────────►│ save order NEW    │
  │                   │                  │ ProcessPaymentAction (amount = qty × 1.5)
  │                   │                  │──────────────────►│ save payment
  │                   │                  │ UpdateOrderToPaidStatusAction
  │                   │                  │◄──────────────────│
  │                   │                  │ payOrder → PAID   │
```

### Current flow (payment always fails)

This is what happens with the code as it is:

```
Client      product-service      order-service      payment-service
  │ POST /product/buy │                  │                   │
  │──────────────────►│ buyProduct       │                   │
  │                   │  stock -= qty    │                   │
  │                   │ CreateOrderAction│                   │
  │                   │─────────────────►│ createOrder → NEW │
  │                   │                  │ ProcessPaymentAction
  │                   │                  │──────────────────►│ pay() throws
  │                   │                  │ CancelOrderAction │ (caught in PaymentHandler)
  │                   │                  │◄──────────────────│
  │                   │                  │ cancelOrder → CANCELLED
  │                   │ ProductRollbackAction                │
  │                   │◄─────────────────│                   │
  │                   │  stock += qty    │                   │
```

Net effect of one purchase: the stock is back to its original value, the order is `CANCELLED`, and no payment row is saved.

### Failure points

| Where it fails | What the service does | Next message |
|---|---|---|
| `product-service`: `buyProduct` throws | Logs the error and stops. No compensation | none |
| `order-service`: `createOrder` throws | Sends `ProductRollbackAction` | stock restored |
| `order-service`: sending `ProcessPaymentAction` throws | Sends `ProductRollbackAction`. The order is **not** cancelled | stock restored, order stays `NEW` |
| `payment-service`: `pay` throws (always, currently) | Sends `CancelOrderAction` | order cancelled, then stock restored |
| `order-service`: `cancelOrder` throws | Swallows the exception and still sends `ProductRollbackAction` | stock restored |
| `product-service`: `rollbackProduct` throws | Swallows the exception | none |

### Handler reference

| Service | Handler | Message | Action |
|---|---|---|---|
| product-service | `ProductHandler.handleProductAction` | `ProductReserveAction` | `buyProduct`, then `CreateOrderAction` |
| product-service | `ProductHandler.handleProductAction` | `ProductRollbackAction` | `rollbackProduct` |
| order-service | `OrderHandler.handleOrderAction` | `CreateOrderAction` | `createOrder`, then `ProcessPaymentAction` |
| order-service | `OrderHandler.handleOrderAction` | `CancelOrderAction` | `cancelOrder`, then `ProductRollbackAction` |
| order-service | `OrderHandler.handleOrderAction` | `UpdateOrderToPaidStatusAction` | `payOrder` |
| payment-service | `PaymentHandler.handlePaymentAction` | `ProcessPaymentAction` | `pay`, then `UpdateOrderToPaidStatusAction`; on exception `CancelOrderAction` |

---

## Order and stock state

**Order statuses** (`order-service`, `OrderStatus`)

```
  createOrder           payOrder (intended)
  ───────────► NEW ─────────────────────► PAID
                │
                │ cancelOrder
                └──────────────────────► CANCELLED
```

**Stock** (`product-service`, `ProductEntity.quantity`)

- `buyProduct(productId, quantity)`: `stock − quantity`
- `rollbackProduct(productId, quantity)`: `stock + quantity`
- No check for going below zero, and no transaction or locking (see [Known limitations](#known-limitations)).

**Amount**: `OrderHandler` charges `quantity × 1.5`.

---

## Logging and tracing

`log-journal-service` is a passive observer. It consumes all three topics and does three things for each message:

1. **Journals the message.** `KafkaLogServiceImpl.logKafkaMessage` serialises the message to JSON with `ObjectMapper` and inserts a row into `kafka_messages`.
2. **Opens a trace span** named `Buy something flow. OperationId: <uuid>`, with attributes `event.class.name`, `event.operation_id`, `event.topic` and `event.json`.
3. **Exports the span to Jaeger** through the OpenTelemetry SDK and a gRPC exporter at `localhost:14250`.

Only `log-journal-service` uses OpenTelemetry. The other services only log, and their log lines include the `operationId`, which is the common key for following one purchase across services.

Message fields are taken from `BaseTracing.operationId`. If a message has no `operationId`, the listener generates a random UUID, which breaks correlation.

---

## Data model and seed data

Each service uses Hibernate with the default naming strategy, so camel-case fields become snake-case columns.

| Service | Table | Columns |
|---|---|---|
| product-service | `product_entity` | `id` UUID (PK), `name` VARCHAR(255) NOT NULL, `quantity` INT NOT NULL. Created by `schema.sql` |
| order-service | `order_entity` | `order_id` UUID (PK), `product_id` UUID, `quantity` INT, `order_status` VARCHAR |
| payment-service | `payment_entity` | `payment_id` UUID (PK), `product_id` UUID, `order_id` UUID, `amount` DOUBLE |
| log-journal-service | `kafka_messages` | `id` BIGINT identity (PK), `topic`, `message_type`, `operation_id` UUID, `message_content` TEXT, `timestamp` TIMESTAMP, `service_source` VARCHAR. Created by `schema.sql` with indexes on `operation_id`, `topic`, `message_type` and `service_source` |

**Seed products** (`product-service/src/main/resources/data.sql`)

| ID | Name | Initial quantity |
|---|---|---|
| `00000000-0000-0000-0000-000000000001` | Product A | 100 |
| `00000000-0000-0000-0000-000000000002` | Product B | 50 |
| `00000000-0000-0000-0000-000000000003` | Product C | 200 |

All databases are in memory, so restarting a service resets its data.

---

## Configuration reference

Each module has `src/main/resources/application.properties`.

| Property | Modules | Example | Purpose |
|---|---|---|---|
| `server.port` | all | `8081` | HTTP port |
| `spring.application.name` | all | `product-service` | Application name. Used as the trace service name and as the journal's `serviceSource` (see Known limitations) |
| `spring.kafka.bootstrap-servers` | all | `localhost:9092,localhost:9094,localhost:9096` | Kafka brokers |
| `spring.kafka.producer.*` | all | `acks=all`, `enable.idempotence=true`, `delivery.timeout.ms=120000` | Producer reliability |
| `spring.kafka.consumer.group-id` | all | `product-service` | Consumer group |
| `spring.kafka.consumer.*` | all | String key, JSON value, trusted packages `com.saga.dto.*` | Deserialization |
| `spring.datasource.url` | all DB services | `jdbc:h2:mem:product;DB_CLOSE_DELAY=-1` | In-memory database |
| `spring.h2.console.enabled` / `path` | all DB services | `true` / `/h2-console` | H2 web console |
| `kafka.topic.name.<service>.topic` | all | `product-topic` | Topic names. Each service lists the topics it uses |
| `otel.exporter.jaeger.endpoint` | log-journal | `http://localhost:14250` | Jaeger gRPC collector |
| `otel.service.name` | log-journal | `log-journal-service` | Service name in Jaeger |
| `logging.level.com.saga.log` | log-journal | `DEBUG` | Log level for the journal |

---

## Running locally

### Prerequisites

- **JDK 25 or later** (the project is built with JDK 27; see [Build notes](#build-notes))
- **Maven 3.6.3+**
- **A Kafka cluster with 3 brokers** at `localhost:9092`, `localhost:9094` and `localhost:9096`. The repository does not include Kafka or Docker files. The topics use replication factor 3.
- **Jaeger** (optional) with its gRPC collector on `localhost:14250`. Without it, the journal still stores messages, but traces are not exported.

### Build

```bash
mvn clean install
```

### Start the services

Start the journal first, so it is subscribed before any purchase is made. Then start the business services. Each command runs in its own terminal.

```bash
mvn -pl log-journal-service spring-boot:run
mvn -pl product-service spring-boot:run
mvn -pl order-service spring-boot:run
mvn -pl payment-service spring-boot:run
```

---

## Running the tests

```bash
mvn test                              # all modules
mvn -pl order-service -am test        # one module, with its dependencies
```

Each service has unit tests in `src/test/java`. They mock `KafkaTemplate`, the repositories and the OpenTelemetry tracer, so no Kafka cluster, database or running service is needed.

The tests cover the Kafka handlers, the services, the product controller and the `log-journal-service` listener and log service. `PaymentServiceImplTest` asserts the current behaviour, where `pay` always throws. When the success branch is restored, that test must be updated.

---

## Trying it out

1. Open the H2 consoles (optional):

   | Service | URL | JDBC URL | User | Password |
   |---|---|---|---|---|
   | product | http://localhost:8081/h2-console | `jdbc:h2:mem:product` | `sa` | (empty) |
   | order | http://localhost:8082/h2-console | `jdbc:h2:mem:order` | `sa` | (empty) |
   | payment | http://localhost:8083/h2-console | `jdbc:h2:mem:payment` | `sa` | (empty) |
   | log journal | http://localhost:8085/h2-console | `jdbc:h2:mem:logjournal` | `sa` | (empty) |

2. Buy two units of Product A:

   ```bash
   curl -X POST http://localhost:8081/product/buy \
     -H 'Content-Type: application/json' \
     -d '{"productId": "00000000-0000-0000-0000-000000000001", "quantity": 2}'
   ```

3. Watch the logs. With the current code you should see, in order: `handleProductEvent. Received ProductReserveAction`, `Sent CreateOrderAction`, `handleOrderAction. Sent ProcessPaymentAction`, `Paying order with id ...`, `handlePaymentAction. Sent CancelOrderAction`, `handleOrderAction. Sent ProductRollbackAction`, `handleProductEvent. Received ProductRollbackAction`.

4. Check the result. `product_entity` should be back at 100 for Product A, `order_entity` should have one row with status `CANCELLED`, and `payment_entity` should be empty. `kafka_messages` should have one row per message, all with the same `operation_id`.

Tracing a single purchase in Jaeger: search for the service `log-journal-service` and the operation name `Buy something flow. OperationId: <uuid>`.

---

## Build notes

- `java.version` is **27**, and the root POM sets `maven.compiler.release` to **25**. Spring Boot 3.4.4's repackage plugin cannot read Java 27 class files (major version 71), so the compiler targets 25.
- Lombok is **1.18.48**. Version 1.18.36 fails with a `TypeTag :: UNKNOWN` error on JDK 26 and later.
- `mvn clean install` builds all modules successfully on JDK 27.
- Code is formatted with **google-java-format** (v2.0.0 at the time of writing).
- Unit tests use JUnit 6 and Mockito 5, pinned in the parent POM. `byte-buddy.version` is pinned to 1.18.17 for Java 27 support.
- There are no integration tests.

---

## Known limitations

**Payment**

- `PaymentServiceImpl.pay` always throws `RuntimeException("Payment failed")`. The success branch, which saves the payment and returns its ID, is commented out. As a result no purchase can succeed, and no payment rows are ever written.
- `PaymentHandler` replies with `CancelOrderAction` on any exception, including a failed Kafka send that happens after a successful payment.

**Error handling and compensation**

- `OrderHandler` (order-service) swallows exceptions from `cancelOrder` and `payOrder`, and still sends the next message. Those `catch` blocks are marked `HERE WE SHOULD THROW A RETRYABLE EXCEPTION`.
- `ProductHandler` (product-service) does the same for `rollbackProduct`. A failed rollback is silent.
- In `OrderHandler`, if the order is created but the `ProcessPaymentAction` send fails, the handler sends `ProductRollbackAction` only. The order stays `NEW` because nothing cancels it.
- In `ProductHandler`, if `buyProduct` succeeds but the `CreateOrderAction` send fails, the stock stays decremented and nothing restores it.
- If `buyProduct` fails, the error is logged, but no message is sent. Nothing else in the chain learns about it, so the saga just stops.

**Consistency and concurrency**

- Stock is read, modified and saved without a transaction or locking. Concurrent purchases can lose updates.
- Stock can go negative. `buyProduct` does not check the available quantity.
- For a missing product, `buyProduct` only logs, but `CreateOrderAction` is still sent. Later the order is created, payment is attempted, and the rollback adds stock to a product that does not exist, which is also only logged.
- Consumers use at-least-once delivery and the handlers are not idempotent. A redelivered `ProductReserveAction` decrements stock twice.
- The producer is idempotent, but that protects only against duplicate sends by the producer, not by the business logic.
- `ProductController` returns `200 OK` without checking the Kafka send result. A null `quantity` causes a `NullPointerException` inside `buyProduct`, which the handler catches and logs.

**Logging and data**

- `KafkaLogListener` passes `spring.application.name` (`log-journal-service`) as `serviceSource` for every row. The `service_source` column therefore never identifies the service that produced the message, and `getMessagesByServiceSource` does not do what its name suggests.
- Messages are serialised in full into `kafka_messages.message_content`, including `creditCardNumber`. The same card number is also in the `ProcessPaymentAction` log lines of the services.
- `KafkaLogServiceImpl` swallows its own exceptions and logs them. A failed write is only visible in the logs.
- The journal's query methods have no REST layer. `log-journal-service` has a web starter but no controller.

**Simulated values**

- Credit card numbers are random 16-digit numbers from `CreditCardUtils`. They are not validated.
- The payment amount is a hard-coded `quantity × 1.5`.

**Other**

- `log-journal-service/README.md` documents `GET /api/logs/...` endpoints, but no controller exists for them. Its description of the schema and config is otherwise accurate.
- `ProductServiceImpl` injects a `KafkaTemplate` that it never uses.
- `ProductController.getProducts` is misnamed. It sends a reserve action and does not return products.
- `spring.application.name` is `PaymentService` in payment-service, while the other services use lowercase names.
- Message keys are `null`, so messages for the same operation may be processed out of order when a topic has several partitions.
- No integration tests, no CI and no containerisation. The unit tests do not check Kafka serialisation or the real topic wiring.

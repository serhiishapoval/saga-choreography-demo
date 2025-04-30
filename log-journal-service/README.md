# Log Journal Service

This service is responsible for logging all Kafka messages sent between services in the saga choreography demo. It listens to all Kafka topics and stores the messages in a database for later retrieval.

## Features

- Logs all Kafka messages to a database
- Provides REST endpoints to query logs by various criteria
- Supports tracing of related messages using operation IDs

## API Endpoints

The service exposes the following REST endpoints:

- `GET /api/logs/operation/{operationId}` - Get all logs for a specific operation
- `GET /api/logs/topic/{topic}` - Get all logs for a specific Kafka topic
- `GET /api/logs/type/{messageType}` - Get all logs for a specific message type
- `GET /api/logs/service/{serviceSource}` - Get all logs from a specific service

## Database Schema

The service uses an H2 in-memory database with the following schema:

```sql
CREATE TABLE kafka_messages (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  topic VARCHAR(255) NOT NULL,
  message_type VARCHAR(255) NOT NULL,
  operation_id UUID,
  message_content TEXT,
  timestamp TIMESTAMP NOT NULL,
  service_source VARCHAR(255) NOT NULL
);
```

## Configuration

The service is configured with the following properties:

- Server port: 8085
- Database: H2 in-memory database
- Kafka: Connects to Kafka brokers at localhost:9092, localhost:9094, localhost:9096
- Kafka consumer group ID: log-journal-service

## How It Works

1. The service listens to all Kafka topics (order-topic, payment-topic, product-topic)
2. When a message is received, it extracts the message details (topic, type, operation ID, etc.)
3. The message is stored in the database with a timestamp and the source service name
4. The message can be retrieved later using the REST API

## Usage

To use this service, simply start it along with the other services in the saga choreography demo. It will automatically log all Kafka messages.

To query the logs, use the REST API endpoints described above.
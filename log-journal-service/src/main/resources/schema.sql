CREATE TABLE IF NOT EXISTS kafka_messages (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  topic VARCHAR(255) NOT NULL,
  message_type VARCHAR(255) NOT NULL,
  operation_id UUID,
  message_content TEXT,
  timestamp TIMESTAMP NOT NULL,
  service_source VARCHAR(255) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_kafka_messages_operation_id ON kafka_messages(operation_id);
CREATE INDEX IF NOT EXISTS idx_kafka_messages_topic ON kafka_messages(topic);
CREATE INDEX IF NOT EXISTS idx_kafka_messages_message_type ON kafka_messages(message_type);
CREATE INDEX IF NOT EXISTS idx_kafka_messages_service_source ON kafka_messages(service_source);
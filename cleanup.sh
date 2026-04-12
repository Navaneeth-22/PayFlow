#!/bin/bash

echo "⚠️  Make sure your Spring Boot apps are stopped, but Docker containers are running!"
sleep 2

echo "🧹 Clearing Databases..."
docker exec -t payflow-postgres-payment psql -U payflow -d payflow_payment -c "TRUNCATE saga_states, outbox_events, idempotency_keys, payments, accounts CASCADE;"
docker exec -t payflow-postgres-ledger psql -U payflow -d payflow_ledger -c "TRUNCATE journal_entries, outbox_events, cancelled_payments CASCADE;"
docker exec -t payflow-postgres-fraud psql -U payflow -d payflow_fraud -c "TRUNCATE fraud_evaluations, outbox_events CASCADE;"
docker exec -t payflow-postgres-notification psql -U payflow -d payflow_notification -c "TRUNCATE webhooks, webhook_deliveries CASCADE;"

echo "🧹 Clearing Redis..."
docker exec -t payflow-redis redis-cli FLUSHALL

echo "🧹 Deleting Kafka topics..."
docker exec -t payflow-kafka kafka-topics --bootstrap-server localhost:9092 --delete --topic payment.initiated 2>/dev/null || true
docker exec -t payflow-kafka kafka-topics --bootstrap-server localhost:9092 --delete --topic fraud.cleared 2>/dev/null || true
docker exec -t payflow-kafka kafka-topics --bootstrap-server localhost:9092 --delete --topic fraud.flagged 2>/dev/null || true
docker exec -t payflow-kafka kafka-topics --bootstrap-server localhost:9092 --delete --topic ledger.debited 2>/dev/null || true
docker exec -t payflow-kafka kafka-topics --bootstrap-server localhost:9092 --delete --topic ledger.failed 2>/dev/null || true
docker exec -t payflow-kafka kafka-topics --bootstrap-server localhost:9092 --delete --topic ledger.reversed 2>/dev/null || true
docker exec -t payflow-kafka kafka-topics --bootstrap-server localhost:9092 --delete --topic payment.completed 2>/dev/null || true
docker exec -t payflow-kafka kafka-topics --bootstrap-server localhost:9092 --delete --topic payment.failed 2>/dev/null || true
docker exec -t payflow-kafka kafka-topics --bootstrap-server localhost:9092 --delete --topic payment.cancelled 2>/dev/null || true
docker exec -t payflow-kafka kafka-topics --bootstrap-server localhost:9092 --delete --topic payment.reversal.needed 2>/dev/null || true

echo "✅ Environment completely reset!"
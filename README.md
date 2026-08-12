# user-cron

Run-once Spring Boot job that materialises Smart Match decks (batches of 30) for active users.

Mirrors [event-cron](../event-cron): EventBridge Scheduler → Lambda → `ApplicationRunner` → exit.

## Layout

**shared-user:** `SmartMatch` entity + DTOs (including persisted `score` / `rank`)

**user-cron:** ranking + batch write

- `SmartMatchUserRanker`
- `SmartMatchBatchService`
- `GenerateSmartMatchBatchJob`

**user-service:** GET `/api/smart-match` reads precomputed unseen rows only (no live ranking).

## Local

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

## Lambda

Handler: `com.fancia.backend.user.LambdaHandler::handleRequest`

```bash
./gradlew lambdaZip
```

Infra: register repo `user-cron` with `is_cron: true` and the handler above (default cron handler is event-cron's).

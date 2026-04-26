# One More Subscriber

Telegram-bot for managing shared subscriptions (VPN, Spotify, YouTube, etc.). Members track their own payments, the bot calculates how long each person is covered and sends reminders when it's time to pay.

## Features

- Shared subscription management with automatic per-member cost splitting
- Balance-based billing with daily deductions
- VPN key management via AmneziaWG integration (auto-generate, rotate, revoke)
- Admin panel: reports, user management, billing controls
- Payment reminders and overdue notifications
- Audit logging for all actions

## Stack

Spring Boot 4, Java 25, PostgreSQL 16, Flyway, Docker Compose

## Quick Start

```bash
cp .env.example .env
# edit .env with your bot token and admin ID

# start PostgreSQL
docker compose -f docker-compose-local.yml up -d

# run the app
./gradlew bootRun
```

## Deploy

```bash
docker compose up -d --build
```

## Configuration

All settings via environment variables (see `.env.example`):
- `BOT_TOKEN` — Telegram bot token
- `BOT_ADMIN_IDS` — comma-separated admin Telegram IDs
- `AWG_API_URL` / `AWG_API_TOKEN` — optional, for VPN key management

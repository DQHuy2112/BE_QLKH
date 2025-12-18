# Gmail SMTP setup (auth-service)

Project default uses **MailHog** for local testing (emails show in `http://localhost:8025`).

To send real emails via **Gmail**, set environment variables for `auth-service` (recommended via `.env` file in `BE_QLKH/`).

## 1) Create Gmail App Password

- Enable 2-Step Verification for your Google account
- Create an **App Password** (16-char) for “Mail”

## 2) Create `BE_QLKH/.env`

Create a file named `.env` next to `docker-compose.yml` and add:

```env
APP_EMAIL_ENABLED=true
APP_EMAIL_FROM=your_gmail@gmail.com
APP_FRONTEND_BASE_URL=http://localhost:3000

SPRING_MAIL_HOST=smtp.gmail.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=your_gmail@gmail.com
SPRING_MAIL_PASSWORD=YOUR_GMAIL_APP_PASSWORD
SPRING_MAIL_SMTP_AUTH=true
SPRING_MAIL_SMTP_STARTTLS_ENABLE=true
```

## 3) Restart auth-service

From `BE_QLKH/`:

```bash
docker compose up -d --build auth-service
```

Now `forgot-password` / `resend-verification` should deliver to Gmail inbox.



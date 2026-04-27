# 🚀 Hosting Your Full-Stack Platform

This guide walks you through deploying the **Expense Splitter Pro** to a production-ready environment using **Vercel** and **Neon/Supabase**.

---

## 🏗️ Deployment Architecture

```mermaid
graph TD
    User((User)) -->|HTTPS| Vercel[Vercel Edge Network]
    subgraph Vercel_Cloud [Vercel Project]
        Vercel -->|Serves| Frontend[React / Vite SPA]
        Vercel -->|Proxies /api/*| Backend[Node.js Serverless Functions]
    end
    Backend -->|Prisma Client| DB[(PostgreSQL Database)]
    Frontend -->|OAuth 2.0| Auth0[Auth0 Identity Provider]
    Backend -->|JWT Validation| Auth0
```

---

## 🏁 Phase 1: Provision a Cloud Database

Vercel's filesystem is ephemeral and read-only. You must move from **SQLite** to a managed **PostgreSQL** instance.

### 1. Create a Database
*   **Neon (Recommended):** [neon.tech](https://neon.tech) - Best for serverless projects with "Scale to Zero" free tier.
*   **Supabase:** [supabase.com](https://supabase.com) - Excellent all-in-one platform.

### 2. Update Your Schema
Change the datasource provider in `backend/prisma/schema.prisma`:

```prisma
datasource db {
  provider = "postgresql"
  url      = env("DATABASE_URL")
}
```

### 3. Run Initial Migration
```powershell
cd backend
# Create a new migration for Postgres
npx prisma migrate dev --name init_postgres
```

---

## 🔐 Phase 2: Auth0 Production Setup

1.  **Dashboard**: Go to your Auth0 Dashboard.
2.  **Allowed Origins**: Add your Vercel production URL (e.g., `https://expense-splitter.vercel.app`) to:
    *   *Allowed Callback URLs*
    *   *Allowed Logout URLs*
    *   *Allowed Web Origins*
3.  **API Identifier**: Ensure your API Identifier matches `AUTH0_AUDIENCE` in your env vars.

---

## ⛵ Phase 3: Deploy to Vercel (Monorepo)

The project is already configured with a `vercel.json` in the root to handle the monorepo routing.

### 1. Connect to GitHub
Push your latest changes to a GitHub repository.

### 2. Import Project
1.  Go to [Vercel Dashboard](https://vercel.com/new).
2.  Import your repository.
3.  **Framework Preset**: Select `Vite` (Vercel should detect this automatically).
4.  **Root Directory**: Keep as `./` (the project root).

### 3. Configure Environment Variables
Add the following variables in the Vercel Project Settings:

| Variable | Value Example |
| :--- | :--- |
| `DATABASE_URL` | `postgresql://user:pass@ep-cool-name.neon.tech/neondb` |
| `AUTH0_ISSUER_BASE_URL` | `https://your-domain.auth0.com/` |
| `AUTH0_AUDIENCE` | `https://expensesplitter.api` |
| `VITE_AUTH0_DOMAIN` | `your-domain.auth0.com` |
| `VITE_AUTH0_CLIENT_ID` | `YOUR_CLIENT_ID` |
| `VITE_AUTH0_AUDIENCE` | `https://expensesplitter.api` |
| `VITE_API_BASE_URL` | `/api` (This uses the Vercel proxy) |

### 4. Deploy!
Click **Deploy**. Vercel will build the frontend and backend simultaneously.

---

## 🛠️ Phase 4: Post-Deployment Checklist

### ✅ Database Migrations
Vercel won't automatically run migrations on every deploy unless configured in `package.json`. You can run them manually once:
```powershell
# From your local machine, pointing to the PROD DATABASE_URL
npx prisma migrate deploy
```

### ✅ CORS Configuration
In `backend/src/index.ts`, ensure your CORS allows your production domain:
```typescript
app.use(cors({
  origin: ['https://your-app.vercel.app', 'http://localhost:5173']
}));
```

### ✅ Health Check
Visit `https://your-app.vercel.app/api/health` to verify the backend is responsive.

---

> [!TIP]
> **Performance Optimization**: Use **Neon's Connection Pooling** (port 6543) if you experience connection limits with serverless functions.

> [!IMPORTANT]
> **Security**: Never commit your `.env` file to GitHub. Always use Vercel's Environment Variables dashboard for secrets.

---
*Created with ✨ Antigravity*

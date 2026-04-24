# 🚀 Hosting Your Full-Stack TypeScript Platform

This guide provides a strategy to deploy your **TypeScript Expense Splitter Pro** using modern cloud platforms.

---

## 🏗️ Step 1: Secure a Cloud Database (Supabase/Neon)

Since Vercel and other serverless platforms have read-only filesystems, you must move from local SQLite to a managed PostgreSQL database.

1.  **Sign Up**: Create an account at [Supabase](https://supabase.com) or [Neon.tech](https://neon.tech).
2.  **Connection String**: Copy your PostgreSQL connection string. It should look like:
    `postgresql://postgres:[PASSWORD]@db.[ID].supabase.co:5432/postgres`
3.  **Update Prisma**: In `backend/prisma/schema.prisma`, change the provider:
    ```prisma
    datasource db {
      provider = "postgresql"
      url      = env("DATABASE_URL")
    }
    ```
4.  **Migrate**: Run `npx prisma migrate deploy` in your production environment.

---

## 🔐 Step 2: Configure Auth0

1.  **Application (Frontend)**: Ensure "Allowed Callback URLs" include your production domain.
2.  **API (Backend)**:
    *   Identifier: `https://expensesplitter.api` (or your chosen identifier).
    *   Ensure the `AUTH0_AUDIENCE` and `AUTH0_ISSUER_BASE_URL` env vars are set in your backend hosting.

---

## ⛵ Step 3: Deployment Options

### Option A: Vercel (Free Monorepo Setup)

You can host both your Frontend and Backend on Vercel's free tier as a single project.

1.  **Project Structure**: The root `vercel.json` I created handles the routing:
    *   `/api/*` requests are sent to the Node.js backend.
    *   All other requests serve the React frontend.
2.  **Deployment Steps**:
    *   Push your code to GitHub.
    *   In Vercel, click **"New Project"** and import your repo.
    *   Vercel will detect the `vercel.json` and configure the build.
3.  **Database (Required)**: 
    *   Vercel does not support SQLite. You **must** use a free PostgreSQL database like **Supabase**.
    *   Add your `DATABASE_URL` to Vercel's **Environment Variables**.
4.  **Auth0**:
    *   Add `AUTH0_AUDIENCE`, `AUTH0_ISSUER_BASE_URL`, and the `VITE_` variables to Vercel's **Environment Variables**.
5.  **Build Command**: Vercel handles this via the `vercel.json` configuration, but ensure your `frontend` has a `build` script and your `backend` is ready for Node.js execution.

---

### Option B: Railway / Render (Recommended for Express)

Best for persistent, always-on backends.

1.  **New Service**: Connect your GitHub repo.
2.  **Root Directory**: Set to `backend`.
3.  **Build Command**: `npm install && npm run build`
4.  **Start Command**: `npm start`
5.  **Env Vars**: Add `DATABASE_URL`, `AUTH0_AUDIENCE`, `AUTH0_ISSUER_BASE_URL`, etc.

---

## 🍱 Environment Variables Checklist

| Variable | Source | Used In |
| :--- | :--- | :--- |
| `DATABASE_URL` | Supabase/Neon | Backend |
| `AUTH0_AUDIENCE` | Auth0 API | Backend |
| `AUTH0_ISSUER_BASE_URL` | Auth0 Domain | Backend |
| `VITE_AUTH0_DOMAIN` | Auth0 App | Frontend |
| `VITE_AUTH0_CLIENT_ID` | Auth0 App | Frontend |
| `VITE_API_BASE_URL` | Your Backend URL | Frontend |

---

## 💡 Troubleshooting

*   **Prisma Client Error**: Ensure `npx prisma generate` runs during your build step.
*   **CORS Issues**: Add your production frontend URL to the `cors()` middleware in `backend/src/index.ts`.
*   **Cold Starts**: Free tiers on Render/Railway may take ~30s to wake up.

---

**Now, push your changes and go live!** 🛡️✨

# 🚀 How to Deploy Your Collaborative Platform for Free

This guide provides a zero-cost, "Free for Life" strategy to deploy your **Collaborative Expense Splitter Pro** using the most robust free tiers available.

---

## 🏗️ Step 1: Secure Your Free Cloud Database (Supabase)

Supabase offers a free, high-performance PostgreSQL database that never expires.

1. **Sign Up**: Create an account at [supabase.com](https://supabase.com).
2. **New Project**: Create a new project (e.g., "ExpenseSplitter").
3. **Get JDBC URL**:
   - Go to **Project Settings** > **Database**.
   - Copy the **Connection String** for JDBC. It will look like: 
     `jdbc:postgresql://db.[id].supabase.co:5432/postgres?sslmode=require`
4. **Database Password**: Make sure you remember the password you set during project creation.

## 🔐 Step 2: Configure Your Free Identity (Auth0)

1. **New SPA Application**: Create a "Single Page App" in [Auth0 Dashboard](https://manage.auth0.com/).
   - **Allowed Callback URLs**: `http://localhost:5173, https://your-app-url.onrender.com`
   - **Allowed Logout URLs**: `http://localhost:5173, https://your-app-url.onrender.com`
2. **New API**: Create an API with identifier `https://expensesplitter.api`.
3. **Copy Keys**: Copy your `Domain`, `Client ID`, and `Audience`.

## ⛵ Step 3: Deploy on Render.com (Using Docker)

Render will build and run your entire project automatically using the provided `Dockerfile`.

1. **New Web Service**: Connect your GitHub repo to a new Render "Web Service."
2. **Configuration**:
   - **Instance Type**: `Free ($0/month)`.
   - **Runtime**: `Docker`.
3. **Environment Variables**: Add these in the "Environment" tab:
   - `SPRING_DATASOURCE_URL`: (Your Supabase JDBC URI, ensure it ends with `?sslmode=require`)
   - `SPRING_DATASOURCE_USERNAME`: `postgres`
   - `SPRING_DATASOURCE_PASSWORD`: (Your Supabase Password)
   - `AUTH0_ISSUER_URI`: `https://[YOUR_DOMAIN].auth0.com/` 
   - `AUTH0_AUDIENCE`: `https://expensesplitter.api`
   - `VITE_AUTH0_DOMAIN`: `[YOUR_DOMAIN].auth0.com`
   - `VITE_AUTH0_CLIENT_ID`: `[YOUR_CLIENT_ID]`
   - `VITE_API_BASE_URL`: `https://your-app-name.onrender.com`

---

## 🌩️ Why Use Render + Docker?

- **Zero Configuration**: The `Dockerfile` handles everything (Java, Node, Build).
- **Portability**: Your app works anywhere Docker is supported.
- **Performance**: High reliability and professional grade infrastructure.

---

## 💡 Troubleshooting

- **Spinner on Login**: Check "Allowed Callback URLs" in Auth0.
- **Spin-up Speed**: Render's free tier takes ~30s to "wake up" after inactivity.
- **Port Error**: Render automatically detects Port 8080. If it fails, ensure `PORT=8080` is set in environment.

---

**Now, push these changes to GitHub and proceed to Render.com!** 🛡️✨

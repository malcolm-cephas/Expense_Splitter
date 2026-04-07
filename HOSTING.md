# 🚀 How to Deploy Your Collaborative Platform for Free

This guide provides a zero-cost, "Free for Life" strategy to deploy your **Collaborative Expense Splitter Pro** using the most robust free tiers available.

---

## 🏗️ Step 1: Secure Your Free Cloud Database (MongoDB Atlas)

1. **Sign Up**: Create an account at [mongodb.com/cloud/atlas](https://www.mongodb.com/cloud/atlas).
2. **Build Cluster**: Select the **M0 (FREE)** shared tier.
3. **Database Access**: Create a user with a `username` and `password`.
4. **Network Access**: Add `0.0.0.0/0` (Allow Access from Anywhere) temporarily to the IP Access List.
5. **Get URI**: Copy the "Connect with Drivers" connection string.

## 🔐 Step 2: Configure Your Free Identity (Auth0)

1. **New SPA Application**: Create a "Single Page App" in [Auth0 Dashboard](https://manage.auth0.com/).
   - **Callback URLs**: `http://localhost:5173, https://your-app-url.onrender.com`
   - **Logout URLs**: `http://localhost:5173, https://your-app-url.onrender.com`
2. **New API**: Create an API with identifier `https://expensesplitter.api`.
3. **Copy Keys**: Copy your `Domain`, `Client ID`, and `Audience`.

## ⛵ Step 3: Deploy on Render.com (Using Docker)

Render will build and run your entire project automatically using the provided `Dockerfile`.

1. **GitHub Setup**: Ensure your project is in a Private GitHub repo.
2. **New Web Service**: Connect your GitHub repo to a new Render "Web Service."
3. **Configuration**:
   - **Instance Type**: `Free ($0/month)`.
   - **Runtime**: `Docker`.
4. **Secret Management**: Add these Environment Variables:
   - `SPRING_DATASOURCE_URL`: (Your MongoDB Atlas URI)
   - `AUTH0_ISSUER_URI`: (Your Auth0 Domain URL)
   - `VITE_AUTH0_DOMAIN`: (Your Auth0 Domain)
   - `VITE_AUTH0_CLIENT_ID`: (Your Auth0 Client ID)
   - `VITE_API_BASE_URL`: `https://your-app-name.onrender.com`

---

## 🌩️ Why Use Render + Docker?

- **Zero Configuration**: The `Dockerfile` handles installing Node.js, building the React app, and packaging the Java backend into a single container.
- **Portability**: If you decide to leave Render, your Docker container will work instantly on **AWS**, **Google Cloud**, or **Azure**.
- **Performance**: While the free tier might sleep after 15 minutes of inactivity, it provides a full world-class environment when active.

---

## 💡 Troubleshooting

- **Spinner on Login**: Ensure your "Allowed Callback URLs" in Auth0 *exactly* match your live URL.
- **Spin-up Speed**: Render's free tier takes ~30-60 seconds to "wake up" the first time you visit.
- **Persistency**: If not using MongoDB, Render's disk is ephemeral. **Using MongoDB Atlas is strongly recommended for persistence.**

---

**Now, push your code to GitHub and launch your platform!** 🛡️✨

# 💸 Expense Splitter Pro: Collaborative Web Platform

[![Web Architecture](https://img.shields.io/badge/Architecture-Web--First-blueviolet?logo=react&logoColor=white)](https://react.dev/)
[![Security: Auth0](https://img.shields.io/badge/Security-Auth0-eb5424?logo=auth0&logoColor=white)](https://auth0.com/)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

**Expense Splitter Pro** is a world-class financial platform designed for high-performance group collaboration. Decoupled from its original desktop roots, it now scales to the cloud with **Auth0 Identity** and **Real-Time Debt Simplification**.

---

## 🚀 The Cloud-First Experience

This project is a dedicated **Cloud Web Application** designed for seamless, multi-device collaboration through **Auth0 Identity** and **Real-Time Debt Simplification**.

---

## 🔥 Key Technical Innovations

### 🛡️ Crypto-Isolated Sessions
Every user session is cryptographically bound to an **Auth0 Identity**. Our backend ensures **absolute data isolation**—you can only see groups you've been invited to.

### 📉 Smart Settle Algorithm
Our greedy optimization engine reduces complex group "webs of debt" into the minimum number of transactions, supporting **100+ Currencies** with live exchange rates.

### 🍱 High-Aesthetics UI
- **Premium UX**: Built with React + Vite using Glassmorphic panels and a curated deep charcoal design system.
- **Micro-animations**: Smooth, interactive transitions powered by `framer-motion`.

---

## 🛠️ Tech Stack

- **Frontend**: React 18, Vite, Auth0 SDK, Framer Motion, Axios.
- **Backend**: Spring Boot 3.2, Spring Security (OAuth2), JPA/Hibernate, MongoDB Support.
- **DevOps**: Docker Multi-Stage Builds, GitHub Actions.

---

## 🚀 Getting Started

### 1. Environmental Setup
Copy `.env.example` to `.env` and fill in your **Auth0** and **Database** credentials.

```bash
cp .env.example .env
```

### 2. Local Development

**For the Backend (API):**
```bash
cd expense-splitter
mvn spring-boot:run
```

**For the Frontend (UI):**
```bash
cd frontend
npm install && npm run dev
```

---

## 🐳 Deployment (Free for Life!)

The project includes a **Multi-Stage Dockerfile** optimized for Render.com and MongoDB Atlas. 

**For full hosting instructions, see [HOSTING.md](HOSTING.md).**

---

### 🤝 Contributing
Contributions are welcome! Please fork this repository and open a Pull Request for any features or UI enhancements.

### 👨‍💻 Author
*Malcolm Cephas*  
- GitHub: [@malcolm-cephas](https://github.com/malcolm-cephas)

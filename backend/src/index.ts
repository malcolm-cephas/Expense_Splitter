import 'dotenv/config';
import express from 'express';
import cors from 'cors';
import { auth } from 'express-oauth2-jwt-bearer';
import prisma from './db.js';

const app = express();
const port = process.env.PORT || 8080;

app.use(cors({
  origin: '*', // Allow all for now to avoid CORS issues
  methods: ['GET', 'POST', 'PATCH', 'DELETE', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'Authorization']
}));
app.use(express.json());

// Logging middleware
app.use((req, res, next) => {
  console.log(`[${new Date().toISOString()}] ${req.method} ${req.url}`);
  if (req.body && Object.keys(req.body).length > 0) console.log('Body:', JSON.stringify(req.body, null, 2));
  if (req.query && Object.keys(req.query).length > 0) console.log('Query:', JSON.stringify(req.query, null, 2));
  next();
});

// Auth0 Middleware
const checkJwt = auth({
  audience: process.env.AUTH0_AUDIENCE || 'https://expensesplitter.api',
  issuerBaseURL: process.env.AUTH0_ISSUER_BASE_URL || 'https://dev-placeholder.auth0.com/',
  tokenSigningAlg: 'RS256'
});

// Helper to get or create user from Auth0 sub
const getCurrentUser = async (auth0Id: string, email: string, name: string) => {
  let user = await prisma.user.findUnique({
    where: { auth0Id }
  });

  if (!user) {
    user = await prisma.user.create({
      data: {
        auth0Id,
        email,
        name
      }
    });
  }
  return user;
};

// Middleware to inject user into request
const userMiddleware = async (req: any, res: any, next: any) => {
  if (!req.auth) return next();

  try {
    // In a real app, you'd fetch user info from Auth0 or trust the token's email
    // For now, let's assume we have email in the token or we fetch it
    const auth0Id = req.auth.payload.sub as string;
    // We might need to call Auth0 /userinfo if email isn't in token
    // For simplicity, let's just use the sub as the identifier for now
    // and assume email is provided by the frontend in a registration step 
    // or we use a placeholder if not available.

    // Better: frontend sends user details on first load
    next();
  } catch (error) {
    next(error);
  }
};

// Routes
import groupRoutes from './routes/groupRoutes.js';
import expenseRoutes from './routes/expenseRoutes.js';

app.use('/api/groups', checkJwt, groupRoutes);
app.use('/api/expenses', checkJwt, expenseRoutes);

app.get('/api/health', async (req, res) => {
  try {
    // Simple check to verify DB connectivity
    await prisma.user.findFirst();
    res.json({ status: 'ok', database: 'connected' });
  } catch (err: any) {
    console.error('Health check failed:', err);
    res.json({ status: 'partial', database: 'error', error: err.message });
  }
});

app.listen(port, () => {
  console.log(`Server running on port ${port}`);
});

export default app;

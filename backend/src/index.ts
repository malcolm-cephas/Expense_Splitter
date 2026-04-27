import 'dotenv/config';
import express from 'express';
import cors from 'cors';
import { auth } from 'express-oauth2-jwt-bearer';
import prisma from './db.js';

const app = express();
const port = process.env.PORT || 8080;

app.use(cors());
app.use(express.json());

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

app.use('/groups', checkJwt, groupRoutes);
app.use('/expenses', checkJwt, expenseRoutes);

app.get('/health', (req, res) => {
  res.json({ status: 'ok' });
});

app.listen(port, () => {
  console.log(`Server running on port ${port}`);
});

export default app;

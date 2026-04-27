import { Router } from 'express';
import prisma from '../db.js';

const router = Router();

// Get all groups for user
router.get('/', async (req: any, res) => {
  const auth0Id = req.auth.payload.sub!;
  
  const groups = await prisma.group.findMany({
    where: {
      members: {
        some: {
          user: {
            auth0Id: auth0Id
          }
        }
      }
    },
    include: {
      members: {
        include: { user: true }
      },
      expenses: {
        include: {
          payments: true,
          splits: true
        }
      }
    }
  });
  
  res.json(groups);
});

// Create group
router.post('/', async (req: any, res) => {
  const { name, budget, budgetCurrency } = req.body;
  const auth0Id = req.auth.payload.sub!;
  
  // Ensure user exists
  let user = await prisma.user.findUnique({ where: { auth0Id } });
  if (!user) {
    // If user doesn't exist, create with placeholder name/email
    user = await prisma.user.create({
      data: {
        auth0Id,
        email: `${auth0Id}@placeholder.com`,
        name: req.auth.payload.name || 'Unknown User'
      }
    });
  }

  const group = await prisma.group.create({
    data: {
      name,
      budget: parseFloat(budget || 0),
      budgetCurrency: budgetCurrency || 'INR',
      createdById: user.id,
      members: {
        create: { userId: user.id }
      }
    },
    include: {
      members: {
        include: { user: true }
      }
    }
  });
  
  res.json(group);
});

// Get group by ID
router.get('/:id', async (req: any, res) => {
  const { id } = req.params;
  const group = await prisma.group.findUnique({
    where: { id },
    include: {
      members: true,
      expenses: {
        include: {
          payments: {
            include: { user: true }
          },
          splits: {
            include: { user: true }
          }
        }
      }
    }
  });
  
  if (!group) return res.status(404).json({ error: 'Group not found' });
  res.json(group);
});

// Add member to group
router.post('/:id/members', async (req: any, res) => {
  const { id } = req.params;
  const { email } = req.query;
  
  if (!email) return res.status(400).json({ error: 'Email is required' });
  
  // Find or create user by email
  let user = await prisma.user.findUnique({ where: { email: email as string } });
  if (!user) {
    user = await prisma.user.create({
      data: {
        email: email as string,
        name: (email as string).split('@')[0] || 'Unknown'
      }
    });
  }
  
  const group = await prisma.group.update({
    where: { id },
    data: {
      members: {
        create: { userId: user.id }
      }
    },
    include: {
      members: {
        include: { user: true }
      }
    }
  });
  
  res.json(group);
});

// Settle debts (Debt Graph)
import { calculateSimplifiedDebts } from '../services/settlementService.js';

router.get('/:id/debt-graph', async (req: any, res) => {
  const { id } = req.params;
  try {
    const transactions = await calculateSimplifiedDebts(id);
    res.json({ edges: transactions });
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: 'Failed to calculate debts' });
  }
});

// Toggle Family Grouping
router.patch('/:id/family-grouping', async (req: any, res) => {
  const { id } = req.params;
  const group = await prisma.group.findUnique({ where: { id } });
  if (!group) return res.status(404).json({ error: 'Group not found' });
  
  const updated = await prisma.group.update({
    where: { id },
    data: {
      familyGroupingEnabled: !group.familyGroupingEnabled
    },
    include: {
      members: true
    }
  });
  
  res.json(updated);
});

// Delete Group
router.delete('/:id', async (req: any, res) => {
  const { id } = req.params;
  await prisma.group.delete({ where: { id } });
  res.status(204).send();
});

// Set Family Name for Member
router.patch('/members/:memberId/family', async (req: any, res) => {
  const { memberId } = req.params;
  const { familyName } = req.query;
  
  const user = await prisma.user.update({
    where: { id: memberId },
    data: { familyName: familyName as string }
  });
  
  res.json(user);
});

export default router;

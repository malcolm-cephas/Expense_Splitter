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

// Helper to parse Java-style date arrays [YYYY, MM, DD, HH, mm, ss, ns]
const parseJavaDate = (arr: any) => {
  if (!Array.isArray(arr)) return new Date();
  const [y, m, d, hh = 0, mm = 0, ss = 0] = arr;
  return new Date(y, m - 1, d, hh, mm, ss);
};

router.post('/import', async (req: any, res) => {
  const data = req.body;
  const auth0Id = req.auth.payload.sub!;

  try {
    // 1. Ensure the importer exists
    const importer = await prisma.user.findUnique({ where: { auth0Id } });
    if (!importer) return res.status(404).json({ error: 'Importer not found' });

    // 2. Create the Group
    const group = await prisma.group.create({
      data: {
        name: data.name || 'Imported Group',
        description: data.description,
        budget: parseFloat(data.budget || 0),
        budgetCurrency: data.budgetCurrency || 'INR',
        familyGroupingEnabled: !!data.familyGroupingEnabled,
        createdById: importer.id
      }
    });

    // 3. Process Members
    const emailToUuid: Record<string, string> = {};
    if (Array.isArray(data.members)) {
      for (const m of data.members) {
        let user = await prisma.user.findUnique({ where: { email: m.email } });
        if (!user) {
          user = await prisma.user.create({
            data: {
              email: m.email,
              name: m.name,
              familyName: m.familyName,
              currencyPreference: m.currencyPreference
            }
          });
        }
        emailToUuid[m.email] = user.id;

        // Connect to group
        await prisma.groupMember.upsert({
          where: { groupId_userId: { groupId: group.id, userId: user.id } },
          create: { groupId: group.id, userId: user.id },
          update: {}
        });
      }
    }

    // 4. Process Expenses
    if (Array.isArray(data.expenses)) {
      for (const e of data.expenses) {
        const expense = await prisma.expense.create({
          data: {
            description: e.description,
            amount: parseFloat(e.amount),
            currency: e.currency,
            category: e.category,
            paymentMode: e.paymentMode,
            splitType: e.splitType,
            expenseDate: parseJavaDate(e.expenseDate),
            createdAt: parseJavaDate(e.createdAt),
            groupId: group.id
          }
        });

        // Add Payments
        if (Array.isArray(e.payments)) {
          for (const p of e.payments) {
            const userId = emailToUuid[p.userEmail];
            if (userId) {
              await prisma.expensePayment.create({
                data: { amount: parseFloat(p.amount), expenseId: expense.id, userId }
              });
            }
          }
        }

        // Add Splits
        if (Array.isArray(e.splits)) {
          for (const s of e.splits) {
            const userId = emailToUuid[s.userEmail];
            if (userId) {
              await prisma.expenseSplit.create({
                data: {
                  owedAmount: parseFloat(s.owedAmount),
                  paidAmount: parseFloat(s.paidAmount || 0),
                  isPaid: !!s.isPaid,
                  expenseId: expense.id,
                  userId
                }
              });
            }
          }
        }
      }
    }

    // Return the full group with relations
    const finalGroup = await prisma.group.findUnique({
      where: { id: group.id },
      include: {
        members: { include: { user: true } },
        expenses: { include: { payments: true, splits: true } }
      }
    });

    res.json(finalGroup);
  } catch (error) {
    console.error(error);
    res.status(500).json({ error: 'Import failed: ' + (error as Error).message });
  }
});

export default router;

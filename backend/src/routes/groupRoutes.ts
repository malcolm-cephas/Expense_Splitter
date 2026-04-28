import { Router } from 'express';
import prisma from '../db.js';

const router = Router();

// Helper to transform Prisma Group to Frontend Group
const formatGroup = (g: any) => ({
  ...g,
  members: (g.membersList || []).map((m: any) => m.user),
  expenses: (g.expenses || []).map((e: any) => ({
    ...e,
    paidBy: e.payments?.[0]?.user,
    splitMembers: (e.splits || []).map((s: any) => s.user)
  }))
});

// Get all groups for user
router.get('/', async (req: any, res) => {
  try {
    const auth0Id = req.auth.payload.sub!;

    const groups = await prisma.group.findMany({
      where: {
        membersList: {
          some: {
            user: {
              auth0Id: auth0Id
            }
          }
        }
      },
      include: {
        membersList: {
          include: { user: true }
        },
        expenses: {
          include: {
            payments: { include: { user: true } },
            splits: { include: { user: true } }
          }
        }
      }
    });

    res.json(groups.map(formatGroup));
  } catch (error) {
    console.error('Error fetching groups:', error);
    res.status(500).json({ error: 'Failed to fetch groups' });
  }
});

// Create group
router.post('/', async (req: any, res) => {
  try {
    const { name, budget, budgetCurrency, description } = { ...req.query, ...req.body };
    const auth0Id = req.auth?.payload?.sub;

    console.log('Creating group with data:', { name, budget, auth0Id });

    if (!auth0Id) {
      console.warn('Unauthorized attempt to create group: No Auth0 ID');
      return res.status(401).json({ error: 'Unauthorized: No Auth0 ID found in token' });
    }
    if (!name) return res.status(400).json({ error: 'Group name is required' });

    // Ensure user exists
    let user = await prisma.user.findUnique({ where: { auth0Id } });
    if (!user) {
      console.log('User not found in DB, creating new user for auth0Id:', auth0Id);
      user = await prisma.user.create({
        data: {
          auth0Id,
          email: req.auth.payload.email || `${auth0Id}@placeholder.com`,
          name: req.auth.payload.name || req.auth.payload.nickname || 'Unknown User'
        }
      });
    }

    const group = await prisma.group.create({
      data: {
        name,
        description: description || '',
        budget: parseFloat(budget || 0),
        budgetCurrency: budgetCurrency || 'INR',
        createdById: user.id,
        membersList: {
          create: {
            user: { connect: { id: user.id } }
          }
        }
      },
      include: {
        membersList: {
          include: { user: true }
        },
        expenses: {
          include: {
            payments: { include: { user: true } },
            splits: { include: { user: true } }
          }
        }
      }
    });

    console.log('Group created successfully:', group.id);
    res.json(formatGroup(group));
  } catch (error) {
    console.error('Error creating group:', error);
    res.status(500).json({ error: 'Failed to create group', details: (error as Error).message });
  }
});

// Get group by ID
router.get('/:id', async (req: any, res) => {
  const { id } = req.params;
  const group = await prisma.group.findUnique({
    where: { id },
    include: {
      membersList: {
        include: { user: true }
      },
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
  res.json(formatGroup(group));
});

// Add member to group
router.post('/:id/members', async (req: any, res) => {
  try {
    const { id } = req.params;
    const { email, name } = { ...req.query, ...req.body };

    console.log(`Adding member to group ${id}:`, { email, name });

    if (!email) {
      console.warn('Attempt to add member without email');
      return res.status(400).json({ error: 'Email is required' });
    }

    let user = await prisma.user.findUnique({ where: { email: email as string } });
    if (!user) {
      console.log('Creating new user for invite:', email);
      user = await prisma.user.create({
        data: {
          email: email as string,
          name: (name as string) || (email as string).split('@')[0] || 'Unknown'
        }
      });
    }

    // Check if already a member
    const existingMember = await prisma.groupMember.findUnique({
      where: { groupId_userId: { groupId: id, userId: user.id } }
    });

    if (existingMember) {
      console.log(`User ${email} already in group ${id}`);
      return res.status(400).json({ error: 'User is already a member of this group' });
    }

    const group = await prisma.group.update({
      where: { id },
      data: {
        membersList: {
          create: {
            user: { connect: { id: user.id } }
          }
        }
      },
      include: {
        membersList: {
          include: { user: true }
        },
        expenses: {
          include: {
            payments: { include: { user: true } },
            splits: { include: { user: true } }
          }
        }
      }
    });

    console.log(`User ${email} added to group ${id} successfully`);
    res.json(formatGroup(group));
  } catch (error) {
    console.error('Error adding member:', error);
    res.status(500).json({ error: 'Failed to add member', details: (error as Error).message });
  }
});

// Settle debts
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
      membersList: {
        include: { user: true }
      }
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
  try {
    const { memberId } = req.params;
    const { familyName } = { ...req.query, ...req.body };

    if (familyName === undefined) return res.status(400).json({ error: 'Family name is required' });

    const user = await prisma.user.update({
      where: { id: memberId },
      data: { familyName: familyName as string }
    });

    res.json(user);
  } catch (error) {
    console.error('Error setting family name:', error);
    res.status(500).json({ error: 'Failed to set family name', details: (error as Error).message });
  }
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
    const importer = await prisma.user.findUnique({ where: { auth0Id } });
    if (!importer) return res.status(404).json({ error: 'Importer not found' });

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

        await prisma.groupMember.upsert({
          where: { groupId_userId: { groupId: group.id, userId: user.id } },
          create: {
            group: { connect: { id: group.id } },
            user: { connect: { id: user.id } }
          },
          update: {}
        });
      }
    }

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

        if (Array.isArray(e.payments)) {
          for (const p of e.payments) {
            const userId = emailToUuid[p.userEmail];
            if (userId) {
              await prisma.expensePayment.create({
                data: {
                  amount: parseFloat(p.amount),
                  expense: { connect: { id: expense.id } },
                  user: { connect: { id: userId } }
                }
              });
            }
          }
        }

        if (Array.isArray(e.splits)) {
          for (const s of e.splits) {
            const userId = emailToUuid[s.userEmail];
            if (userId) {
              await prisma.expenseSplit.create({
                data: {
                  owedAmount: parseFloat(s.owedAmount),
                  paidAmount: parseFloat(s.paidAmount || 0),
                  isPaid: !!s.isPaid,
                  expense: { connect: { id: expense.id } },
                  user: { connect: { id: userId } }
                }
              });
            }
          }
        }
      }
    }

    const finalGroup = await prisma.group.findUnique({
      where: { id: group.id },
      include: {
        membersList: { include: { user: true } },
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

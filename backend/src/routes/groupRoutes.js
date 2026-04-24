import { Router } from 'express';
import prisma from '../db.js';
const router = Router();
// Get all groups for user
router.get('/', async (req, res) => {
    const auth0Id = req.auth.payload.sub;
    const groups = await prisma.group.findMany({
        where: {
            members: {
                some: {
                    id: auth0Id
                }
            }
        },
        include: {
            members: true,
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
router.post('/', async (req, res) => {
    const { name, budget, budgetCurrency } = req.body;
    const auth0Id = req.auth.payload.sub;
    // Ensure user exists
    let user = await prisma.user.findUnique({ where: { id: auth0Id } });
    if (!user) {
        // If user doesn't exist, create with placeholder name/email
        // In real app, sync this from Auth0
        user = await prisma.user.create({
            data: {
                id: auth0Id,
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
            createdById: auth0Id,
            members: {
                connect: { id: auth0Id }
            }
        },
        include: {
            members: true
        }
    });
    res.json(group);
});
// Get group by ID
router.get('/:id', async (req, res) => {
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
    if (!group)
        return res.status(404).json({ error: 'Group not found' });
    res.json(group);
});
// Add member to group
router.post('/:id/members', async (req, res) => {
    const { id } = req.params;
    const { email } = req.query;
    if (!email)
        return res.status(400).json({ error: 'Email is required' });
    // Find or create user by email
    let user = await prisma.user.findUnique({ where: { email: email } });
    if (!user) {
        user = await prisma.user.create({
            data: {
                email: email,
                name: email.split('@')[0] || 'Unknown'
            }
        });
    }
    const group = await prisma.group.update({
        where: { id },
        data: {
            members: {
                connect: { id: user.id }
            }
        },
        include: {
            members: true
        }
    });
    res.json(group);
});
// Settle debts (Debt Graph)
import { calculateSimplifiedDebts } from '../services/settlementService.js';
router.get('/:id/debt-graph', async (req, res) => {
    const { id } = req.params;
    try {
        const transactions = await calculateSimplifiedDebts(id);
        res.json({ edges: transactions });
    }
    catch (error) {
        console.error(error);
        res.status(500).json({ error: 'Failed to calculate debts' });
    }
});
// Toggle Family Grouping
router.patch('/:id/family-grouping', async (req, res) => {
    const { id } = req.params;
    const group = await prisma.group.findUnique({ where: { id } });
    if (!group)
        return res.status(404).json({ error: 'Group not found' });
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
router.delete('/:id', async (req, res) => {
    const { id } = req.params;
    await prisma.group.delete({ where: { id } });
    res.status(204).send();
});
// Set Family Name for Member
router.patch('/members/:memberId/family', async (req, res) => {
    const { memberId } = req.params;
    const { familyName } = req.query;
    const user = await prisma.user.update({
        where: { id: memberId },
        data: { familyName: familyName }
    });
    res.json(user);
});
export default router;
//# sourceMappingURL=groupRoutes.js.map
import { Router } from 'express';
import prisma from '../db.js';

const router = Router();

// Create Expense
router.post('/', async (req: any, res) => {
  try {
    const { groupId, paidById, amount, description, category, expenseDate, splitType, splitMemberIds } = { ...req.query, ...req.body };

    console.log('Creating expense:', { groupId, paidById, amount, description });

    if (!groupId || !paidById || !amount || !description) {
      console.warn('Missing required parameters for expense creation');
      return res.status(400).json({ error: 'Missing required parameters' });
    }

    const amt = parseFloat(amount as string);
    const memberIds = typeof splitMemberIds === 'string' ? splitMemberIds.split(',') : (Array.isArray(splitMemberIds) ? splitMemberIds : []);

    if (memberIds.length === 0) {
      console.warn('Empty split member list');
      return res.status(400).json({ error: 'At least one member must be involved in the split' });
    }

    console.log(`Splitting INR ${amt} between ${memberIds.length} members`);

    // Simple equal split logic for now
    const splitAmount = amt / memberIds.length;

    const expense = await prisma.expense.create({
      data: {
        groupId: groupId as string,
        amount: amt,
        description: description as string,
        category: (category as string) || 'Other',
        expenseDate: expenseDate ? new Date(expenseDate as string) : new Date(),
        createdAt: new Date(),
        splitType: (splitType as string) || 'EQUAL',
        paidById: paidById as string,
        payments: {
          create: {
            userId: paidById as string,
            amount: amt
          }
        },
        splits: {
          create: memberIds.map((id: string) => ({
            userId: id,
            owedAmount: splitAmount,
            paidAmount: 0,
            isPaid: false
          }))
        }
      }
    });

    console.log('Expense created successfully:', expense.id);
    res.json(expense);
  } catch (error) {
    console.error('Error creating expense:', error);
    res.status(500).json({ error: 'Internal server error', details: (error as Error).message });
  }
});

// Delete Expense
router.delete('/:id', async (req: any, res) => {
  const { id } = req.params;
  await prisma.expense.delete({ where: { id } });
  res.status(204).send();
});

export default router;

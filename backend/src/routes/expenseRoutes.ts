import { Router } from 'express';
import prisma from '../db.js';
import { SplitType } from '@prisma/client/index.js';

const router = Router();

// Create Expense
router.post('/', async (req: any, res) => {
  const { groupId, paidById, amount, description, category, expenseDate, splitType, splitMemberIds } = req.query;
  
  if (!groupId || !paidById || !amount || !description) {
    return res.status(400).json({ error: 'Missing required parameters' });
  }

  const amt = parseFloat(amount as string);
  const memberIds = (splitMemberIds as string).split(',');
  
  // Simple equal split logic for now
  const splitAmount = amt / memberIds.length;

  const expense = await prisma.expense.create({
    data: {
      groupId: groupId as string,
      amount: amt,
      description: description as string,
      category: (category as string) || 'Other',
      expenseDate: new Date(expenseDate as string),
      splitType: (splitType as SplitType) || 'EQUAL',
      payments: {
        create: {
          userId: paidById as string,
          amount: amt
        }
      },
      splits: {
        create: memberIds.map(id => ({
          userId: id,
          owedAmount: splitAmount,
          paidAmount: 0,
          isPaid: false
        }))
      }
    }
  });

  res.json(expense);
});

// Delete Expense
router.delete('/:id', async (req: any, res) => {
  const { id } = req.params;
  await prisma.expense.delete({ where: { id } });
  res.status(204).send();
});

export default router;

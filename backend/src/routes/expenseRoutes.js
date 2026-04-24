import { Router } from 'express';
import prisma from '../db.js';
import { SplitType } from '@prisma/client';
const router = Router();
// Create Expense
router.post('/', async (req, res) => {
    const { groupId, paidById, amount, description, category, expenseDate, splitType, splitMemberIds } = req.query;
    if (!groupId || !paidById || !amount || !description) {
        return res.status(400).json({ error: 'Missing required parameters' });
    }
    const amt = parseFloat(amount);
    const memberIds = splitMemberIds.split(',');
    // Simple equal split logic for now
    const splitAmount = amt / memberIds.length;
    const expense = await prisma.expense.create({
        data: {
            groupId: groupId,
            amount: amt,
            description: description,
            category: category || 'Other',
            expenseDate: new Date(expenseDate),
            splitType: splitType || 'EQUAL',
            payments: {
                create: {
                    userId: paidById,
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
router.delete('/:id', async (req, res) => {
    const { id } = req.params;
    await prisma.expense.delete({ where: { id } });
    res.status(204).send();
});
export default router;
//# sourceMappingURL=expenseRoutes.js.map
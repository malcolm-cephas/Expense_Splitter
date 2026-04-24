import prisma from '../db.js';
export const calculateSimplifiedDebts = async (groupId) => {
    const group = await prisma.group.findUnique({
        where: { id: groupId },
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
    if (!group)
        throw new Error('Group not found');
    const balances = {};
    // Initialize balances for all members
    group.members.forEach((m) => {
        balances[m.id] = 0;
    });
    for (const expense of group.expenses) {
        const totalExpenseAmount = expense.amount;
        if (totalExpenseAmount <= 0)
            continue;
        // Calculate total unpaid in expense
        let totalUnpaidInExpense = 0;
        for (const split of expense.splits) {
            const unpaid = split.owedAmount - split.paidAmount;
            if (unpaid > 0) {
                totalUnpaidInExpense += unpaid;
            }
        }
        // Credit payers
        for (const payment of expense.payments) {
            const payerId = payment.userId;
            const payerContribution = payment.amount;
            // Payer's share of the remaining group credit
            const credit = (totalUnpaidInExpense * payerContribution) / totalExpenseAmount;
            balances[payerId] = (balances[payerId] || 0) + credit;
        }
        // Debit members for their own remaining unpaid debt
        for (const split of expense.splits) {
            const debtorId = split.userId;
            const unpaid = split.owedAmount - split.paidAmount;
            if (unpaid > 0) {
                balances[debtorId] = (balances[debtorId] || 0) - unpaid;
            }
        }
    }
    if (group.familyGroupingEnabled) {
        const familyBalances = {};
        group.members.forEach((member) => {
            const familyKey = member.familyName?.trim() || member.name;
            familyBalances[familyKey] = (familyBalances[familyKey] || 0) + (balances[member.id] || 0);
        });
        return runSimplificationAlgorithm(familyBalances, group.members);
    }
    return runSimplificationAlgorithm(balances, group.members);
};
const runSimplificationAlgorithm = (balances, members) => {
    const creditors = [];
    const debtors = [];
    Object.entries(balances).forEach(([idOrName, amount]) => {
        const name = members.find((m) => m.id === idOrName)?.name || idOrName;
        if (amount > 0.001) {
            creditors.push({ id: idOrName, name, amount });
        }
        else if (amount < -0.001) {
            debtors.push({ id: idOrName, name, amount: Math.abs(amount) });
        }
    });
    // Sort by amount descending
    creditors.sort((a, b) => b.amount - a.amount);
    debtors.sort((a, b) => b.amount - a.amount);
    const transactions = [];
    let i = 0, j = 0;
    while (i < creditors.length && j < debtors.length) {
        const credit = creditors[i];
        const debt = debtors[j];
        if (!credit || !debt)
            break;
        const settledAmount = Math.min(credit.amount, debt.amount);
        if (settledAmount > 0) {
            transactions.push({
                fromId: debt.id,
                toId: credit.id,
                from: debt.name,
                to: credit.name,
                amount: Number(settledAmount.toFixed(2))
            });
        }
        credit.amount -= settledAmount;
        debt.amount -= settledAmount;
        if (credit.amount < 0.001)
            i++;
        if (debt.amount < 0.001)
            j++;
    }
    return transactions;
};
//# sourceMappingURL=settlementService.js.map
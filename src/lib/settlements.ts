import Decimal from 'decimal.js';

export interface SettlementTransaction {
  from: string; // userId
  to: string; // userId
  amount: string;
  currency: string;
}

export interface MemberBalance {
  userId: string;
  familyName?: string;
  balance: Decimal; // Positive means owed, Negative means owes
}

export const calculateSettlements = (
  balances: MemberBalance[],
  currency: string,
  familyGroupingEnabled: boolean = false
): SettlementTransaction[] => {
  let effectiveBalances = balances;

  if (familyGroupingEnabled) {
    // Group balances by family name
    const familyBalances: Record<string, Decimal> = {};
    const unGrouped: MemberBalance[] = [];

    balances.forEach((b) => {
      if (b.familyName) {
        familyBalances[b.familyName] = (familyBalances[b.familyName] || new Decimal(0)).plus(b.balance);
      } else {
        unGrouped.push(b);
      }
    });

    effectiveBalances = [
      ...unGrouped,
      ...Object.entries(familyBalances).map(([name, bal]) => ({
        userId: `family:${name}`,
        balance: bal,
      })),
    ];
  }

  const creditors = effectiveBalances
    .filter((b) => b.balance.gt(0))
    .sort((a, b) => b.balance.minus(a.balance).toNumber());
  
  const debtors = effectiveBalances
    .filter((b) => b.balance.lt(0))
    .map((b) => ({ ...b, balance: b.balance.abs() }))
    .sort((a, b) => b.balance.minus(a.balance).toNumber());

  const transactions: SettlementTransaction[] = [];

  let i = 0; // creditor index
  let j = 0; // debtor index

  while (i < creditors.length && j < debtors.length) {
    const creditor = creditors[i];
    const debtor = debtors[j];

    const settlementAmount = Decimal.min(creditor.balance, debtor.balance);

    if (settlementAmount.gt(0)) {
      transactions.push({
        from: debtor.userId,
        to: creditor.userId,
        amount: settlementAmount.toDecimalPlaces(2).toString(),
        currency,
      });

      creditor.balance = creditor.balance.minus(settlementAmount);
      debtor.balance = debtor.balance.minus(settlementAmount);
    }

    if (creditor.balance.isZero()) i++;
    if (debtor.balance.isZero()) j++;
  }

  return transactions;
};

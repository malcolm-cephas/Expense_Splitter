import { describe, it, expect } from 'vitest';
import { calculateSettlements, type MemberBalance } from './settlements';
import Decimal from 'decimal.js';

describe('calculateSettlements', () => {
  it('should generate minimum transactions for a simple case', () => {
    const balances: MemberBalance[] = [
      { userId: 'A', balance: new Decimal(10) }, // Creditor
      { userId: 'B', balance: new Decimal(-10) }, // Debtor
    ];

    const transactions = calculateSettlements(balances, 'USD');
    
    expect(transactions).toHaveLength(1);
    expect(transactions[0]).toEqual({
      from: 'B',
      to: 'A',
      amount: '10',
      currency: 'USD',
    });
  });

  it('should handle complex cases with multiple creditors and debtors', () => {
    const balances: MemberBalance[] = [
      { userId: 'A', balance: new Decimal(50) },
      { userId: 'B', balance: new Decimal(30) },
      { userId: 'C', balance: new Decimal(-40) },
      { userId: 'D', balance: new Decimal(-40) },
    ];

    const transactions = calculateSettlements(balances, 'USD');
    
    // Total owed: 80, Total owes: 80
    expect(transactions.length).toBeLessThan(4);
    
    const totalSettled = transactions.reduce(
      (acc, t) => acc.plus(new Decimal(t.amount)),
      new Decimal(0)
    );
    expect(totalSettled.toString()).toBe('80');
  });

  it('should group by family when enabled', () => {
    const balances: MemberBalance[] = [
      { userId: 'A1', familyName: 'Alpha', balance: new Decimal(10) },
      { userId: 'A2', familyName: 'Alpha', balance: new Decimal(-5) },
      { userId: 'B', balance: new Decimal(-5) },
    ];

    const transactions = calculateSettlements(balances, 'USD', true);
    
    // Alpha family net: +5. B net: -5.
    expect(transactions).toHaveLength(1);
    expect(transactions[0].to).toBe('family:Alpha');
    expect(transactions[0].from).toBe('B');
    expect(transactions[0].amount).toBe('5');
  });
});

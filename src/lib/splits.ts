import Decimal from 'decimal.js';

export type SplitType = 'equal' | 'exact' | 'percentage' | 'shares';

interface SplitResult {
  userId: string;
  owedAmount: string;
}

export const calculateSplits = (
  totalAmount: string,
  splitType: SplitType,
  members: { userId: string; value?: string }[]
): SplitResult[] => {
  const total = new Decimal(totalAmount);
  const results: SplitResult[] = [];

  if (splitType === 'equal') {
    const count = members.length;
    if (count === 0) return [];
    
    const perPerson = total.div(count).toDecimalPlaces(2, Decimal.ROUND_HALF_UP);
    let remaining = total;

    members.forEach((member, index) => {
      const amount = index === members.length - 1 ? remaining : perPerson;
      results.push({
        userId: member.userId,
        owedAmount: amount.toString(),
      });
      remaining = remaining.minus(amount);
    });
  } else if (splitType === 'exact') {
    members.forEach((member) => {
      results.push({
        userId: member.userId,
        owedAmount: new Decimal(member.value || '0').toString(),
      });
    });
  } else if (splitType === 'percentage') {
    let remaining = total;
    members.forEach((member, index) => {
      const percentage = new Decimal(member.value || '0').div(100);
      const amount = index === members.length - 1 
        ? remaining 
        : total.mul(percentage).toDecimalPlaces(2, Decimal.ROUND_HALF_UP);
      
      results.push({
        userId: member.userId,
        owedAmount: amount.toString(),
      });
      remaining = remaining.minus(amount);
    });
  } else if (splitType === 'shares') {
    const totalShares = members.reduce(
      (acc, m) => acc.plus(new Decimal(m.value || '0')),
      new Decimal(0)
    );
    
    if (totalShares.isZero()) return [];

    let remaining = total;
    members.forEach((member, index) => {
      const share = new Decimal(member.value || '0');
      const amount = index === members.length - 1
        ? remaining
        : total.mul(share).div(totalShares).toDecimalPlaces(2, Decimal.ROUND_HALF_UP);
      
      results.push({
        userId: member.userId,
        owedAmount: amount.toString(),
      });
      remaining = remaining.minus(amount);
    });
  }

  return results;
};

import { describe, it, expect } from 'vitest';
import { calculateSplits } from './splits';

describe('calculateSplits', () => {
  it('should split equally', () => {
    const members = [{ userId: '1' }, { userId: '2' }, { userId: '3' }];
    const results = calculateSplits('100', 'equal', members);
    
    expect(results).toHaveLength(3);
    expect(results[0].owedAmount).toBe('33.33');
    expect(results[1].owedAmount).toBe('33.33');
    expect(results[2].owedAmount).toBe('33.34'); // Remainder
  });

  it('should split by exact amount', () => {
    const members = [
      { userId: '1', value: '40' },
      { userId: '2', value: '60' },
    ];
    const results = calculateSplits('100', 'exact', members);
    
    expect(results[0].owedAmount).toBe('40');
    expect(results[1].owedAmount).toBe('60');
  });

  it('should split by percentage', () => {
    const members = [
      { userId: '1', value: '25' },
      { userId: '2', value: '75' },
    ];
    const results = calculateSplits('100', 'percentage', members);
    
    expect(results[0].owedAmount).toBe('25');
    expect(results[1].owedAmount).toBe('75');
  });

  it('should split by shares', () => {
    const members = [
      { userId: '1', value: '1' },
      { userId: '2', value: '2' },
    ];
    const results = calculateSplits('90', 'shares', members);
    
    expect(results[0].owedAmount).toBe('30');
    expect(results[1].owedAmount).toBe('60');
  });
});

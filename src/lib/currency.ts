import Decimal from 'decimal.js';

export const formatCurrency = (
  amount: string | number | Decimal,
  currencyCode: string = 'USD'
): string => {
  const value = new Decimal(amount).toNumber();
  
  // Map some common currencies to their specific locales for correct grouping
  let locale = 'en-US';
  if (currencyCode === 'INR') locale = 'en-IN';
  if (['JPY', 'CNY', 'KRW'].includes(currencyCode)) locale = 'ja-JP';

  return new Intl.NumberFormat(locale, {
    style: 'currency',
    currency: currencyCode,
  }).format(value);
};

export const convertCurrency = (
  amount: string | number | Decimal,
  fromRate: number,
  toRate: number
): string => {
  const value = new Decimal(amount);
  // amount in base = value / fromRate
  // amount in target = (value / fromRate) * toRate
  return value.div(fromRate).mul(toRate).toDecimalPlaces(2).toString();
};

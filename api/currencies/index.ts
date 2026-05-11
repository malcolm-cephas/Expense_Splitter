import { NextApiRequest, NextApiResponse } from 'next';
import connectDB from '../_db';
import CurrencyCache from '../_models/CurrencyCache';
import axios from 'axios';

const CURRENCY_API_URL = 'https://cdn.jsdelivr.net/npm/@fawazahmed0/currency-api@latest/v1/currencies.json';

export default async function handler(req: NextApiRequest, res: NextApiResponse) {
  await connectDB();

  try {
    // 1. Check cache
    let cache = await CurrencyCache.findOne();

    if (!cache) {
      // 2. Fetch fresh data
      const response = await axios.get(CURRENCY_API_URL);
      cache = await CurrencyCache.create({
        data: response.data,
        updatedAt: new Date(),
      });
    }

    return res.status(200).json({ data: cache.data });
  } catch (error) {
    console.error('Currency API Error:', error);
    return res.status(500).json({ error: 'Failed to fetch currencies' });
  }
}

import { NextApiRequest, NextApiResponse } from 'next';
import { jwtVerify, createRemoteJWKSet } from 'jose';

const AUTH0_DOMAIN = process.env.VITE_AUTH0_DOMAIN;
const AUTH0_AUDIENCE = process.env.VITE_AUTH0_AUDIENCE;

const JWKS = createRemoteJWKSet(
  new URL(`https://${AUTH0_DOMAIN}/.well-known/jwks.json`)
);

export interface AuthenticatedRequest extends NextApiRequest {
  user?: {
    sub: string;
    [key: string]: any;
  };
}

export type ApiHandler = (
  req: AuthenticatedRequest,
  res: NextApiResponse
) => Promise<any> | any;

export function withAuth(handler: ApiHandler) {
  return async (req: NextApiRequest, res: NextApiResponse) => {
    const authHeader = req.headers.authorization;

    if (!authHeader || !authHeader.startsWith('Bearer ')) {
      return res.status(401).json({ error: 'Unauthorized: No token provided' });
    }

    const token = authHeader.split(' ')[1];

    try {
      const { payload } = await jwtVerify(token, JWKS, {
        issuer: `https://${AUTH0_DOMAIN}/`,
        audience: AUTH0_AUDIENCE,
      });

      (req as AuthenticatedRequest).user = payload as any;
      return handler(req as AuthenticatedRequest, res);
    } catch (error) {
      console.error('JWT Verification Error:', error);
      return res.status(401).json({ error: 'Unauthorized: Invalid token' });
    }
  };
}

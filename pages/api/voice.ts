import type { NextApiRequest, NextApiResponse } from 'next';

export default async function handler(req: NextApiRequest, res: NextApiResponse) {
  if (req.method !== 'POST') {
    return res.status(405).json({ error: 'Method Not Allowed' });
  }

  const authHeader = req.headers.authorization;
  if (!authHeader?.startsWith('Bearer un_')) {
    return res.status(401).json({ error: 'Unauthorized' });
  }

  // Handle TTS logic here
  res.setHeader('Content-Type', 'audio/mpeg');
  return res.status(200).send(Buffer.from([]));
}

import * as admin from 'firebase-admin';

if (admin.apps.length === 0) {
  const serviceAccount = process.env.FIREBASE_SERVICE_ACCOUNT_JSON;
  if (serviceAccount) {
    try {
      const parsed = JSON.parse(serviceAccount);
      admin.initializeApp({
        credential: admin.credential.cert(parsed),
      });
    } catch (e) {
      console.error('Failed to parse FIREBASE_SERVICE_ACCOUNT_JSON, falling back to application default credentials', e);
      admin.initializeApp();
    }
  } else {
    admin.initializeApp();
  }
}

export const firebaseAuth = admin.auth();
export const firebaseAdmin = admin;

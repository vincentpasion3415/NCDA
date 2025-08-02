const functions = require('firebase-functions');
const admin = require('firebase-admin');

// Initialize the Firebase Admin SDK
admin.initializeApp();

// Callable Cloud Function to set a user's custom claims
// Only an authenticated user can call this function
exports.setAdminClaim = functions.https.onCall(async (data, context) => {
  // Check if the user is authenticated and has the admin claim
  // This is a simple security check to make sure only existing admins can create new ones.
  if (!context.auth || !context.auth.token.admin) {
    throw new functions.https.HttpsError(
      'unauthenticated',
      'Only admins can set custom claims.'
    );
  }

  // Get the user ID from the data passed in the request
  const userId = data.userId;
  if (!userId) {
    throw new functions.https.HttpsError('invalid-argument', 'The user ID must be provided.');
  }

  try {
    // Set the 'admin' custom claim to true
    await admin.auth().setCustomUserClaims(userId, { admin: true });

    return { message: `Success! User ${userId} is now an admin.` };
  } catch (error) {
    console.error('Error setting custom claim:', error);
    throw new functions.https.HttpsError('internal', 'Failed to set admin claim.');
  }
});
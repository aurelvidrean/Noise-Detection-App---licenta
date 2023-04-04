const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

exports.getAllUsers = functions.https.onCall(async (data, context) => {
try {
    const listUsersResult = await admin.auth().listUsers();
    const users = listUsersResult.users.map(user => {
        return {
            uid: user.uid,
            email: user.email,
            displayName: user.displayName
        };
    });
    return users;
} catch (error) {
    console.error(error);
    throw new functions.https.HttpsError('internal', 'Error retrieving users');
}
});

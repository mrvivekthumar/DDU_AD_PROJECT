package com.example.ddu_e_connect.controller;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.ddu_e_connect.MainActivity;
import com.example.ddu_e_connect.R;
import com.example.ddu_e_connect.model.User;
import com.example.ddu_e_connect.view.SignInActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class AuthController {
    private FirebaseAuth auth;
    private GoogleSignInClient mGoogleSignInClient;
    private Activity activity;

    public AuthController(Activity activity) {
        this.activity = activity;
        auth = FirebaseAuth.getInstance();

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(activity.getString(R.string.client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(activity, gso);
    }

    public void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        activity.startActivityForResult(signInIntent, 101);
    }

    public void handleSignInResult(Intent data) {
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult();
            if (account != null) {
                firebaseAuthWithGoogle(account);
            }
        } catch (Exception e) {
            Log.w("AuthController", "Google sign in failed", e);
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(activity, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = auth.getCurrentUser();
                            if (firebaseUser != null) {
                                User user = new User(firebaseUser.getUid(), firebaseUser.getEmail());
                                updateUI(user);
                            }
                        } else {
                            Log.w("AuthController", "signInWithCredential:failure", task.getException());
                            updateUI(null);
                        }
                    }
                });
    }


    public void signOut() {
        auth.signOut();
        mGoogleSignInClient.signOut().addOnCompleteListener(activity, task -> updateUI(null));
    }

    private void updateUI(User user) {
        if (user != null) {
            Intent intent = new Intent(activity, MainActivity.class);
            activity.startActivity(intent);
        } else {
            Intent intent = new Intent(activity, SignInActivity.class);
            activity.startActivity(intent);
        }
        activity.finish();
    }

    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }
}

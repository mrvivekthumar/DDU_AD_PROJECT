package com.example.ddu_e_connect.model;

import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.gms.tasks.Task;

public class AuthenticationModel {
    private FirebaseAuth mAuth;

    public AuthenticationModel() {
        mAuth = FirebaseAuth.getInstance();
    }

    public Task<AuthResult> signUp(String email, String password) {
        return mAuth.createUserWithEmailAndPassword(email, password);
    }

    public Task<AuthResult> signIn(String email, String password) {
        return mAuth.signInWithEmailAndPassword(email, password);
    }

    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }

    public void signOut() {
        mAuth.signOut();
    }

}

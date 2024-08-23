package com.example.ddu_e_connect.view;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.ddu_e_connect.R;
import com.example.ddu_e_connect.controller.AuthController;
import com.google.firebase.auth.FirebaseUser;

public class SignInActivity extends AppCompatActivity {

    private EditText emailEditText, passwordEditText;
    private Button signInButton;
    private AuthController authController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        emailEditText = findViewById(R.id.emailEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        signInButton = findViewById(R.id.signInButton);

        authController = new AuthController();

        signInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();

                if (isValidEmail(email)) {
                    authController.signIn(email, password, new AuthController.OnAuthCompleteListener() {
                        @Override
                        public void onSuccess(FirebaseUser user) {
                            if (user != null && user.isEmailVerified()) {
                                // Sign-in successful and email is verified, navigate to HomePageActivity
                                startActivity(new Intent(SignInActivity.this, HomePageActivity.class));
                                finish();
                            } else {
                                Toast.makeText(SignInActivity.this, "Please verify your email first.", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(String errorMessage) {
                            // Sign-in failed, show error message
                            Toast.makeText(SignInActivity.this, "Sign-in failed: " + errorMessage, Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Toast.makeText(SignInActivity.this, "Invalid email format. Please use your institutional email.", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    private boolean isValidEmail(String email) {
        String emailPattern = "^[0-9]{2}[a-zA-Z0-9._%+-]*@ddu\\.ac\\.in$";
        return email.matches(emailPattern);
    }
}

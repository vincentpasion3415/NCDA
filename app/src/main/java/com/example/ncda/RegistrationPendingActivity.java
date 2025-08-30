package com.example.ncda;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class RegistrationPendingActivity extends AppCompatActivity {

    private Button backToLoginButton;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration_pending);

        mAuth = FirebaseAuth.getInstance();
        backToLoginButton = findViewById(R.id.backToLoginButton);
        TextView titleTextView = findViewById(R.id.titleTextView);
        TextView messageTextView = findViewById(R.id.messageTextView);

        titleTextView.setText("Registration Still on Progress");
        messageTextView.setText("Your registration is still being reviewed by our team. We will notify you once it's done. Please try again later.");

        backToLoginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Sign out the user so they cannot access the app again until approved
                mAuth.signOut();
                Intent intent = new Intent(RegistrationPendingActivity.this, LoginActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }
}
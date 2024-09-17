package com.example.ddu_e_connect.views;



import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.example.ddu_e_connect.R;


public class ContactUsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contact_us);

        // Admin 1
        View admin1Card = findViewById(R.id.admin_card_1); // Assuming you set the CardView's ID as admin1_card
        ImageView admin1Image = findViewById(R.id.admin1_image);
        TextView admin1Name = findViewById(R.id.admin1_name);
        TextView admin1Email = findViewById(R.id.admin1_email);

        // Admin 2
        View admin2Card = findViewById(R.id.admin_card_2); // Assuming you set the CardView's ID as admin1_card
        ImageView admin2Image = findViewById(R.id.admin2_image);
        TextView admin2Name = findViewById(R.id.admin2_name);
        TextView admin2Email = findViewById(R.id.admin2_email);

        // Set data for Admin 1
        admin1Image.setImageResource(R.drawable.admin1_image); // Replace with your image
        admin1Name.setText("Vivek Thumar");
        admin1Email.setText("mrvivekthumar@gmail.com");

        // Set data for Admin 2
        admin2Image.setImageResource(R.drawable.admin2_image); // Replace with your image
        admin2Name.setText("Kuldip Vaghasiya");
        admin2Email.setText("kuldipvaghasiya0@gmail.com");

        // Load animations
        Animation slideInLeft = AnimationUtils.loadAnimation(this, R.anim.slide_in_left);
        Animation slideInRight = AnimationUtils.loadAnimation(this, R.anim.slide_in_right);

        // Apply animations to the cards
        admin1Card.startAnimation(slideInLeft);
        admin2Card.startAnimation(slideInRight);

    }
}

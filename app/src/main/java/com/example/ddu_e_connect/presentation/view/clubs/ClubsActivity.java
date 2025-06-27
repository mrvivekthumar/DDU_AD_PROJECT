package com.example.ddu_e_connect.presentation.view.clubs;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ddu_e_connect.R;
import com.example.ddu_e_connect.model.ClubsModel;
import com.example.ddu_e_connect.adapters.ClubAdapter;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

public class ClubsActivity extends AppCompatActivity {
    private static final String TAG = "ClubsActivity";

    private ArrayList<ClubsModel> clubsList;
    private RecyclerView recyclerView;
    private ClubAdapter adapter;
    private TextView clubCountText;
    private FloatingActionButton fabQuickActions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clubs);

        initializeComponents();
        setupUI();
        setupClubsData();
        setupRecyclerView();

        Log.d(TAG, "ClubsActivity initialized with enhanced UI");
    }

    /**
     * Initialize components
     */
    private void initializeComponents() {
        recyclerView = findViewById(R.id.recyclerview);
        clubCountText = findViewById(R.id.clubCountText);
        fabQuickActions = findViewById(R.id.fabQuickActions);
        clubsList = new ArrayList<>();
    }

    /**
     * Setup UI components and click listeners
     */
    private void setupUI() {
        // Back button click listener
        findViewById(R.id.backButton).setOnClickListener(v -> {
            onBackPressed();
        });

        // FAB click listener
        fabQuickActions.setOnClickListener(v -> {
            showQuickActionsMenu();
        });

        // Add subtle entrance animation to FAB
        fabQuickActions.setScaleX(0f);
        fabQuickActions.setScaleY(0f);
        fabQuickActions.animate()
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .setStartDelay(300)
                .start();
    }

    /**
     * Setup clubs data
     */
    private void setupClubsData() {
        Log.d(TAG, "Setting up clubs data");

        // Add CSI DDU
        clubsList.add(new ClubsModel(
                "CSI DDU",
                "Computer Society of India, DDU Chapter\n\n" +
                        "President: Nisarg Amlan\n" +
                        "Vice President: Om Unadakat\n\n" +
                        "Established: 2010\n\n" +
                        "Overview:\n\n" +
                        "Our mission is to empower IT professionals across disciplines, fostering a dynamic environment for aspiring talents in the tech industry. As part of the prestigious CSI network, we advance research, facilitate knowledge exchange, and provide unparalleled learning opportunities.\n" +
                        "\n\nVision:\n\n" +
                        "We envision a future where every IT professional excels and contributes meaningfully to the industry's growth, fostering a collaborative ecosystem that bridges the gap between seasoned experts and emerging talents.\n" +
                        "\n\nMission:\n\n" +
                        "Our mission is to support continuous growth for IT professionals through research, knowledge sharing, and skill enhancement, and to provide a platform for newcomers to seamlessly integrate into the IT community.\n",
                false, R.drawable.csi_logo,
                "https://www.linkedin.com/in/csi-ddu-76b988285/",
                "https://www.instagram.com/csi_ddu/",
                "https://www.linkedin.com/in/csi-ddu-76b988285/"
        ));

        // Add GDSC
        clubsList.add(new ClubsModel(
                "GDSC DDU",
                "Google Developer Student Club DDU\n\n" +
                        "President: Vashisth Patel\n" +
                        "Vice President: Kunj Patel\n\n" +
                        "Established: 2020\n\n" +
                        "Overview:\n\n" +
                        "Welcome to the official page of the Google Developer Student Club (GDSC) at Dharmsinh Desai University. We are a community of developers passionate about building solutions to real-world problems and sharing knowledge within our university. Join us as we learn, connect, and grow together in the exciting world of technology.\n",
                false, R.drawable.gdsc_logo,
                "https://www.linkedin.com/company/gdsc-ddu/",
                "https://www.instagram.com/gdscddu/",
                "https://gdg.community.dev/gdg-on-campus-dharmsinh-desai-university-nadiad-india/"
        ));

        // Add IETE
        clubsList.add(new ClubsModel(
                "IETE Student's Forum",
                "IETE Student's Forum DDU\n\n" +
                        "President: Nisarg Pipaliya\n" +
                        "Vice President: Tech Team Lead\n\n" +
                        "Established: 2011\n\n" +
                        "Overview:\n\n" +
                        "We are the IETE Students Forum (ISF), a vibrant community dedicated to fostering collaboration, learning, and professional development among our members. Together, we embark on a journey of discovery, learning, and growth, aiming to harness our potential, break barriers, and leave a lasting impact on the realm of technology.\n",
                false, R.drawable.iete_logo,
                "https://www.linkedin.com/company/tech-tribe-the-community/",
                "https://www.linkedin.com/company/tech-tribe-the-community/",
                "https://isf-website.vercel.app/"
        ));

        // Add Shutterbugs DDU
        clubsList.add(new ClubsModel(
                "Shutterbugs DDU",
                "Photography Club of DDU\n\n" +
                        "President: Heet Vadiya\n" +
                        "Vice President: Mahek Purohit\n\n" +
                        "Established: 2013\n\n" +
                        "Overview:\n\n" +
                        "Shutterbugs is the premier photography club of DDU, Nadiad, dedicated to enhancing and showcasing the creative photography skills of our students. We believe in capturing life's best moments through the lens of a camera. Our activities include online photo contests, exhibitions, photography trips, workshops, seminars, and photowalks, all designed to celebrate and improve our love for photography.\n" +
                        "\n📞 Contact: 6351072003\n",
                false, R.drawable.shutterbugs_ddu_logo,
                "https://www.linkedin.com/company/shutterbugs-ddu/",
                "https://www.instagram.com/shutterbugs_ddu/",
                "https://www.linkedin.com/company/shutterbugs-ddu/"
        ));

        // Add Samvaad DDU
        clubsList.add(new ClubsModel(
                "Samvaad DDU",
                "Communication Club of DDU\n\n" +
                        "Founder: Jainish Shah\n\n" +
                        "Established: October 16, 2019\n\n" +
                        "Overview:\n\n" +
                        "SAMVAAD is the communication club of DDU, Nadiad. Our motto, \"Verbalizing Minds,\" reflects our dedication to helping individuals overcome communication barriers and develop essential skills like public speaking and effective communication. Through events like group discussions, debates, extempore, seminars, and guest lectures, we provide a platform for self-development and leadership.\n",
                false, R.drawable.samvaad_logo,
                "https://www.linkedin.com/company/samvaad-ddu/",
                "https://www.instagram.com/samvaad_ddu/",
                "https://www.linkedin.com/company/samvaad-ddu/"
        ));

        // Add Malgadi-DDU
        clubsList.add(new ClubsModel(
                "Malgadi-DDU",
                "Student Startup Initiative\n\n" +
                        "President: Hitarth Patel\n\n" +
                        "Established: 2016-17\n\n" +
                        "Overview:\n\n" +
                        "Malgadi is a non-profit startup at Dharmsinh Desai University, providing students with all their engineering needs at guaranteed lowest prices. We are committed to serving our community by offering a wide range of products and services tailored to meet the needs of our fellow students.\n",
                false, R.drawable.malgadi_logo,
                "https://www.linkedin.com/company/malgadi-ddu/",
                "https://www.instagram.com/malgadi_ddu/",
                "https://www.linkedin.com/company/malgadi-ddu/"
        ));

        // Add Decrypters
        clubsList.add(new ClubsModel(
                "Decrypters-The Coding Club",
                "Competitive Programming Club\n\n" +
                        "Department: Information Technology\n\n" +
                        "Established: 2020\n\n" +
                        "Overview:\n\n" +
                        "The Decrypters Club, managed by the Department of IT, is dedicated to fostering competitive programming skills within our campus. We organize live sessions, coding contests, webinars, and meetings to spread knowledge about problem-solving skills, data structures, and algorithms. Join us to enhance your coding skills and become a part of our vibrant community.\n",
                false, R.drawable.decrypters_logo,
                "https://www.linkedin.com/company/decrypters-ddu/",
                "https://www.instagram.com/decrypters_ddu/",
                "https://www.linkedin.com/company/decrypters-ddu/"
        ));

        // Add Sports Club
        clubsList.add(new ClubsModel(
                "Sports Club FOT DDU",
                "Athletics & Sports Community\n\n" +
                        "Department: Faculty of Technology\n\n" +
                        "Established: 2021\n\n" +
                        "Overview:\n\n" +
                        "The Sports Club of DDU (FOT) is dedicated to empowering collegiate athletes with the tools and encouragement they need to realize their full physical potential. Our mission is to ignite a passion for sports among our college students, fostering a positive and healthy environment where student-athletes can compete and thrive. We are committed to promoting a culture of excellence and fairness in college sports.\n" +
                        "\nIndustry: Sports Teams and Clubs\n\n" +
                        "Company Size: 11-50 employees\n\n" +
                        "Headquarters: Nadiad, Gujarat\n\n" +
                        "LinkedIn Members: 18 associated members\n",
                false, R.drawable.sports_club_logo,
                "https://www.linkedin.com/company/sports-club-fot-ddu/",
                "https://www.instagram.com/sportsclubddu/",
                "https://www.linkedin.com/company/sports-club-fot-ddu/"
        ));

        // Update club count
        updateClubCount();

        Log.d(TAG, "Added " + clubsList.size() + " clubs to the list");
    }

    /**
     * Setup RecyclerView with adapter
     */
    private void setupRecyclerView() {
        adapter = new ClubAdapter(clubsList, ClubsActivity.this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        Log.d(TAG, "RecyclerView setup completed");
    }

    /**
     * Update club count display
     */
    private void updateClubCount() {
        if (clubCountText != null) {
            clubCountText.setText(String.valueOf(clubsList.size()));
        }
    }

    /**
     * Show quick actions menu
     */
    private void showQuickActionsMenu() {
        // Create and show a simple menu for quick actions
        String[] options = {
                "Share Clubs Info",
                "Contact Admin",
                "Refresh Data"
        };

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Quick Actions")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            shareClubsInfo();
                            break;
                        case 1:
                            contactAdmin();
                            break;
                        case 2:
                            refreshData();
                            break;
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    /**
     * Share clubs information
     */
    private void shareClubsInfo() {
        try {
            StringBuilder shareText = new StringBuilder();
            shareText.append("🎓 DDU University Clubs 🎓\n\n");
            shareText.append("Discover amazing clubs and communities at Dharmsinh Desai University:\n\n");

            for (ClubsModel club : clubsList) {
                shareText.append("• ").append(club.getClubName().trim()).append("\n");
            }

            shareText.append("\nJoin DDU E-Connect app to explore more!");

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "DDU University Clubs");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText.toString());

            startActivity(Intent.createChooser(shareIntent, "Share Clubs Info"));
            Log.d(TAG, "Clubs info shared successfully");
        } catch (Exception e) {
            Log.e(TAG, "Failed to share clubs info", e);
            showError("Failed to share information");
        }
    }

    /**
     * Contact admin
     */
    private void contactAdmin() {
        try {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
            emailIntent.setData(Uri.parse("mailto:admin@ddu.ac.in"));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "DDU Clubs - Query from E-Connect App");
            emailIntent.putExtra(Intent.EXTRA_TEXT, "Hello,\n\nI have a query regarding university clubs.\n\nBest regards,");

            startActivity(Intent.createChooser(emailIntent, "Contact Admin"));
            Log.d(TAG, "Admin contact initiated");
        } catch (Exception e) {
            Log.e(TAG, "Failed to contact admin", e);
            showError("No email app found");
        }
    }

    /**
     * Refresh data
     */
    private void refreshData() {
        // Add refresh animation to FAB
        fabQuickActions.animate()
                .rotationBy(360f)
                .setDuration(800)
                .start();

        // Simulate data refresh
        if (adapter != null) {
            adapter.notifyDataSetChanged();
            showSuccess("Clubs data refreshed!");
        }

        Log.d(TAG, "Data refreshed");
    }

    /**
     * Show success message
     */
    private void showSuccess(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    /**
     * Show error message
     */
    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onBackPressed() {
        // Add smooth back navigation
        super.onBackPressed();
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "ClubsActivity destroyed");
    }
}
package com.example.ddu_e_connect.views;

import android.os.Bundle;
import android.view.View;
import android.widget.Adapter;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.ddu_e_connect.Model;
import com.example.ddu_e_connect.R;
import com.example.ddu_e_connect.adapters.ClubAdapter;

import java.util.ArrayList;

public class ClubsActivity extends AppCompatActivity {

    ArrayList<Model> arrayList;
    RecyclerView recyclerView;
    ClubAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clubs);

        arrayList = new ArrayList<>();
        recyclerView = findViewById(R.id.recyclerview);

        arrayList.add(new Model("CSI DDU\n" , "Computer Society of India, DDU Chapter\n\n" +
                "President: Nisarg Amlan\n" +
                "Vice President: Om Unadakat\n\n" +
                "Established: 2010\n\n" +
                "Overview:\n\n" +
                "Our mission is to empower IT professionals across disciplines, fostering a dynamic environment for aspiring talents in the tech industry. As part of the prestigious CSI network, we advance research, facilitate knowledge exchange, and provide unparalleled learning opportunities.\n" +
                "\n\nVision:\n\n" +
                "We envision a future where every IT professional excels and contributes meaningfully to the industry's growth, fostering a collaborative ecosystem that bridges the gap between seasoned experts and emerging talents.\n" +
                "\n\nMission:\n\n" +
                "Our mission is to support continuous growth for IT professionals through research, knowledge sharing, and skill enhancement, and to provide a platform for newcomers to seamlessly integrate into the IT community.\n",
                false, R.drawable.csi_logo));

        arrayList.add(new Model("GDSC\n", "GDSC DDU\n\n" +
                "President: Vashisth Patel\n" +
                "Vice President: Kunj Patel\n\n" +
                "Established: [Year]\n\n" +
                "Overview:\n\n" +
                "Welcome to the official page of the Google Developer Student Club (GDSC) at Dharmsinh Desai University. We are a community of developers passionate about building solutions to real-world problems and sharing knowledge within our university. Join us as we learn, connect, and grow together in the exciting world of technology.\n",
                false, R.drawable.gdsc_logo));

        arrayList.add(new Model("IETE\n", "IETE Student's Forum DDU\n\n" +
                "President: Nisarg Pipaliya\n" +
                "Vice President: [Name]\n\n" +
                "Established: 2011\n\n" +
                "Overview:\n\n" +
                "We are the IETE Students Forum (ISF), a vibrant community dedicated to fostering collaboration, learning, and professional development among our members. Together, we embark on a journey of discovery, learning, and growth, aiming to harness our potential, break barriers, and leave a lasting impact on the realm of technology.\n",
                false, R.drawable.iete_logo));

        arrayList.add(new Model("Shutterbugs DDU\n", "Shutterbugs DDU\n\n"+"President: Heet Vadiya\n" +
                "Vice President: Mahek Purohit\n\n" +
                "Established: 2013\n\n" +
                "Overview:\n\n" +
                "Shutterbugs is the premier photography club of DDU, Nadiad, dedicated to enhancing and showcasing the creative photography skills of our students. We believe in capturing life's best moments through the lens of a camera. Our activities include online photo contests, exhibitions, photography trips, workshops, seminars, and photowalks, all designed to celebrate and improve our love for photography.\n" +
                "\n\uD83D\uDCDE Contact: 6351072003\n",
                false, R.drawable.shutterbugs_ddu_logo));

        arrayList.add(new Model("Samvaad DDU\n", "Samvaad DDU\n\n"+"Founder: Jainish Shah\n\n" +
                "Established: October 16, 2019\n" +
                "\n\nOverview:\n\n" +
                "SAMVAAD is the communication club of DDU, Nadiad. Our motto, \"Verbalizing Minds,\" reflects our dedication to helping individuals overcome communication barriers and develop essential skills like public speaking and effective communication. Through events like group discussions, debates, extempore, seminars, and guest lectures, we provide a platform for self-development and leadership.\n",
                false, R.drawable.samvaad_logo));

        arrayList.add(new Model("Malgadi-DDU\n", "Malgadi-DDU\n\n"+"President: Hitarth Patel\n\n" +
                "Established: 2016-17\n\n" +
                "Overview:\n\n" +
                "Malgadi is a non-profit startup at Dharmsinh Desai University, providing students with all their engineering needs at guaranteed lowest prices. We are committed to serving our community by offering a wide range of products and services tailored to meet the needs of our fellow students.\n",
                false, R.drawable.malgadi_logo));

        arrayList.add(new Model("Decrypters-The Coding Club\n", "Decrypters-The Coding Club\n\n"+"President: [Name]\n" +
                "Vice President: [Name]\n\n" +
                "Established: 2020\n\n" +
                "Overview:\n\n" +
                "The Decrypters Club, managed by the Department of IT, is dedicated to fostering competitive programming skills within our campus. We organize live sessions, coding contests, webinars, and meetings to spread knowledge about problem-solving skills, data structures, and algorithms. Join us to enhance your coding skills and become a part of our vibrant community.\n",
                false, R.drawable.decrypters_logo));

        arrayList.add(new Model("SPORTS CLUB FOT DDU\n", "SPORTS CLUB FOT DDU\n\n"+"President: [Name]\n" +
                "Vice President: [Name]\n\n" +
                "Established: 2021\n\n" +
                "Overview:\n\n" +
                "The Sports Club of DDU (FOT) is dedicated to empowering collegiate athletes with the tools and encouragement they need to realize their full physical potential. Our mission is to ignite a passion for sports among our college students, fostering a positive and healthy environment where student-athletes can compete and thrive. We are committed to promoting a culture of excellence and fairness in college sports. Throughout the year, our team members organize a variety of sporting activities, ensuring active student involvement and support as we build a better future together.\n" +
                "\nIndustry: Sports Teams and Clubs\n\n" +
                "Company Size: 11-50 employees\n\n" +
                "Headquarters: Nadiad, Gujarat\n\n" +
                "LinkedIn Members: 18 associated members\n",
                false, R.drawable.sports_club_logo));

        adapter = new ClubAdapter(arrayList, ClubsActivity.this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

    }
}

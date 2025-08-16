package com.example.ncda;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ncda.adapter.ReferralAdapter;
import com.example.ncda.model.GovernmentService;
import java.util.ArrayList;
import java.util.List;

public class ReferralActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_referral);

        // Set up the toolbar
        Toolbar toolbar = findViewById(R.id.referral_toolbar);
        setSupportActionBar(toolbar);

        // Enable the back button
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        // Handle the back button click
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        // Get a reference to the RecyclerView
        RecyclerView recyclerView = findViewById(R.id.referral_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Prepare the specific list of government services
        List<GovernmentService> serviceList = new ArrayList<>();

        // **Fewer Additional Important Referrals**
        // These are the most critical agencies that were missing from your previous list.
        serviceList.add(new GovernmentService("Social Security System (SSS)", "(02) 81455-8888", "member_relations@sss.gov.ph", "https://www.sss.gov.ph"));
        serviceList.add(new GovernmentService("Home Development Mutual Fund (Pag-IBIG)", "(02) 8724-4244", "contactus@pagibigfund.gov.ph", "https://www.pagibigfund.gov.ph"));

        // Add services from your original user-provided list
        serviceList.add(new GovernmentService("Philippine Statistics Authority (PSA)", "(02) 8461-0500", "info@psa.gov.ph", "https://www.psa.gov.ph"));
        serviceList.add(new GovernmentService("Land Transportation Office (LTO)", "(02) 8922-9650", "lto_pwd_concerns@gmail.com", "https://www.lto.gov.ph"));
        serviceList.add(new GovernmentService("Land Transportation Franchising and Regulatory Board (LTFRB)", "(02) 1342", "ltfrbcentral@ltfrb.gov.ph", "https://ltfrb.gov.ph"));
        serviceList.add(new GovernmentService("Commission on Higher Education (CHED)", "(02) 8441-1224", "chedcares@ched.gov.ph", "https://ched.gov.ph"));
        serviceList.add(new GovernmentService("Department of Social Welfare and Development (DSWD)", "(02) 8931-8101", "inquiries@dswd.gov.ph", "https://www.dswd.gov.ph"));
        serviceList.add(new GovernmentService("Department of Health (DOH)", "(02) 8651-7800", "callcenter@doh.gov.ph", "https://doh.gov.ph"));
        serviceList.add(new GovernmentService("Department of Education (DepEd)", "(02) 8636-1663", "action@deped.gov.ph", "https://www.deped.gov.ph"));
        serviceList.add(new GovernmentService("TESDA (Technical Education and Skills Development Authority)", "(02) 8887-7777", "contactcenter@tesda.gov.ph", "https://www.tesda.gov.ph"));
        serviceList.add(new GovernmentService("PhilHealth (Philippine Health Insurance Corporation)", "(02) 8441-7442", "info@philhealth.gov.ph", "https://www.philhealth.gov.ph"));
        serviceList.add(new GovernmentService("Department of Labor and Employment (DOLE)", "(02) 1349", "info@dole.gov.ph", "https://www.dole.gov.ph"));
        serviceList.add(new GovernmentService("Department of Transportation (DOTr)", "(02) 8790-8300", "info@dotr.gov.ph", "https://dotr.gov.ph"));
        serviceList.add(new GovernmentService("Commission on Human Rights (CHR)", "(02) 8928-5655", "ichr.main@chr.gov.ph", "https://chr.gov.ph"));

        // Create and set the adapter
        ReferralAdapter adapter = new ReferralAdapter(serviceList);
        recyclerView.setAdapter(adapter);
    }
}
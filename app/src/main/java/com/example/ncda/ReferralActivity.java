package com.example.ncda;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.widget.ImageButton; // Make sure to import ImageButton
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.ncda.chatbot.adapter.ReferralAdapter;
import com.example.ncda.model.GovernmentService;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ReferralActivity extends AppCompatActivity {

    private static final int SPEECH_INPUT_REQUEST_CODE = 100;
    private static final int PERMISSION_REQUEST_CODE = 200;
    private List<GovernmentService> serviceList;

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
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Get a reference to the RecyclerView
        RecyclerView recyclerView = findViewById(R.id.referral_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Prepare the specific list of government services with acronyms
        serviceList = new ArrayList<>();
        // Updated service list with acronyms
        serviceList.add(new GovernmentService("Social Security System", "SSS", "(02) 81455-8888", "member_relations@sss.gov.ph", "https://www.sss.gov.ph"));
        serviceList.add(new GovernmentService("Home Development Mutual Fund", "Pag-IBIG", "(02) 8724-4244", "contactus@pagibigfund.gov.ph", "https://www.pagibigfund.gov.ph"));
        serviceList.add(new GovernmentService("Philippine Statistics Authority", "PSA", "(02) 8461-0500", "info@psa.gov.ph", "https://www.psa.gov.ph"));
        serviceList.add(new GovernmentService("Land Transportation Office", "LTO", "(02) 8922-9650", "lto_pwd_concerns@gmail.com", "https://www.lto.gov.ph"));
        serviceList.add(new GovernmentService("Land Transportation Franchising and Regulatory Board", "LTFRB", "(02) 1342", "ltfrbcentral@ltfrb.gov.ph", "https://ltfrb.gov.ph"));
        serviceList.add(new GovernmentService("Commission on Higher Education", "CHED", "(02) 8441-1224", "chedcares@ched.gov.ph", "https://ched.gov.ph"));
        serviceList.add(new GovernmentService("Department of Social Welfare and Development", "DSWD", "(02) 8931-8101", "inquiries@dswd.gov.ph", "https://www.dswd.gov.ph"));
        serviceList.add(new GovernmentService("Department of Health", "DOH", "(02) 8651-7800", "callcenter@doh.gov.ph", "https://doh.gov.ph"));
        serviceList.add(new GovernmentService("Department of Education", "DepEd", "(02) 8636-1663", "action@deped.gov.ph", "https://www.deped.gov.ph"));
        serviceList.add(new GovernmentService("Technical Education and Skills Development Authority", "TESDA", "(02) 8887-7777", "contactcenter@tesda.gov.ph", "https://www.tesda.gov.ph"));
        serviceList.add(new GovernmentService("Philippine Health Insurance Corporation", "PhilHealth", "(02) 8441-7442", "info@philhealth.gov.ph", "https://www.philhealth.gov.ph"));
        serviceList.add(new GovernmentService("Department of Labor and Employment", "DOLE", "(02) 1349", "info@dole.gov.ph", "https://www.dole.gov.ph"));
        serviceList.add(new GovernmentService("Department of Transportation", "DOTr", "(02) 8790-8300", "info@dotr.gov.ph", "https://dotr.gov.ph"));
        serviceList.add(new GovernmentService("Commission on Human Rights", "CHR", "(02) 8928-5655", "ichr.main@chr.gov.ph", "https://chr.gov.ph"));


        // Create and set the adapter
        ReferralAdapter adapter = new ReferralAdapter(serviceList);
        recyclerView.setAdapter(adapter);

        // Find the new voice command button in the toolbar and set a click listener
        ImageButton voiceToolbarButton = findViewById(R.id.voice_toolbar_button);
        voiceToolbarButton.setOnClickListener(v -> checkPermissionAndStartSpeechInput());
    }

    private void checkPermissionAndStartSpeechInput() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSION_REQUEST_CODE);
        } else {
            startSpeechInput();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startSpeechInput();
            } else {
                Toast.makeText(this, "Permission denied. Voice commands are not available.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startSpeechInput() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say a command, like 'Call SSS' or 'Email DSWD'");

        try {
            startActivityForResult(intent, SPEECH_INPUT_REQUEST_CODE);
        } catch (ActivityNotFoundException a) {
            Toast.makeText(getApplicationContext(), "Speech recognition not supported on this device.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == SPEECH_INPUT_REQUEST_CODE) {
            if (resultCode == RESULT_OK && data != null) {
                ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                String spokenText = result.get(0).toLowerCase(Locale.getDefault());
                processVoiceCommand(spokenText);
            }
        }
    }

    private void processVoiceCommand(String command) {
        for (GovernmentService service : serviceList) {
            // Check for both full name and acronym
            String name = service.getName().toLowerCase(Locale.getDefault());
            String acronym = service.getAcronym().toLowerCase(Locale.getDefault());
            String hotline = service.getHotline();
            String email = service.getEmail();
            String website = service.getWebsite();

            if ((command.contains("call") || command.contains("tawag") || command.contains("tumawag") || command.contains("tawagan")) && (command.contains(name) || command.contains(acronym))) {
                if (hotline != null && !hotline.isEmpty()) {
                    Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + hotline));
                    startActivity(intent);
                    Toast.makeText(this, "Pagtawag sa " + service.getName(), Toast.LENGTH_SHORT).show();
                    return;
                }
            } else if ((command.contains("email") || command.contains("mag-email")) && (command.contains(name) || command.contains(acronym))) {
                if (email != null && !email.isEmpty()) {
                    Intent intent = new Intent(Intent.ACTION_SENDTO);
                    intent.setData(Uri.parse("mailto:" + email));
                    startActivity(Intent.createChooser(intent, "Magpadala ng email gamit ang:"));
                    return;
                }
            } else if ((command.contains("visit") || command.contains("bisita") || command.contains("buksan")) && (command.contains(name) || command.contains(acronym))) {
                if (website != null && !website.isEmpty()) {
                    Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(website));
                    startActivity(intent);
                    Toast.makeText(this, "Pagbisita sa " + service.getName() + " website", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        }
        Toast.makeText(this, "Hindi naiintindihan ang utos. Pakisubukang muli.", Toast.LENGTH_SHORT).show();
    }
}
package com.example.emergencyapp.communities;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.emergencyapp.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

import java.util.ArrayList;

public class CommunitiesActivity extends AppCompatActivity  {

    public EditText communityText, memberText, zipCodeText;
    public Button communityButton, memberButton;
    public ProgressBar progressBar;

    private FirebaseDatabase rootNode;
    private FirebaseAuth mAuth;
    private DatabaseReference communityDB;
    private DatabaseReference userDBEntry;
    private String username, userID;

    private ListView listView;
    private ArrayList<String> nameArrayList;
    private ArrayList<String> idArrayList;
    private ArrayAdapter<String> arrayAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_communities);


        //Get Views
        communityText = findViewById(R.id.communityText);
        memberText = findViewById(R.id.memberText);
        zipCodeText = findViewById(R.id.zipCodeText);
        communityButton = findViewById(R.id.addCommunity);
        memberButton = findViewById(R.id.addMember);
        progressBar = findViewById(R.id.activity_communities_progressbar);

        //show the progressbar. Currently shows indefinitely until user has a community, should fix
        progressBar.setVisibility(View.VISIBLE);

        //get Database and auth
        rootNode = FirebaseDatabase.getInstance();
        mAuth = FirebaseAuth.getInstance();
        communityDB = rootNode.getReference("Communities");
        userDBEntry = rootNode.getReference("Users").child(FirebaseAuth.getInstance().getCurrentUser().getUid());

        userID = mAuth.getCurrentUser().getUid();
         userDBEntry.child("name").get().addOnCompleteListener(new OnCompleteListener<DataSnapshot>() {
             @Override
             public void onComplete(@NonNull Task<DataSnapshot> task) {
                 username = task.getResult().getValue(String.class);
             }
         });

        //Set up community list View adapter
        nameArrayList = new ArrayList<>();
        idArrayList = new ArrayList<>();

        listView = findViewById(R.id.communityList);
        arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, nameArrayList);
        listView.setAdapter(arrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int i, long id) {
                Toast.makeText(CommunitiesActivity.this, "Now operating in " + listView.getItemAtPosition(i).toString()
                        + " community; tap a different community to switch.", Toast.LENGTH_SHORT).show();

                CommunityListEntry selected = new CommunityListEntry(idArrayList.get(i), listView.getItemAtPosition(i).toString());
                userDBEntry.child("selectedCommunity").setValue(selected);
            }
        });

        memberButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(CommunitiesActivity.this, "this don't do nuthin", Toast.LENGTH_SHORT).show();
                addNewMember();
            }
        });

        communityButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkNewCommunityValues();
            }
        });

        userDBEntry.child("communityList").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                String name = "";
                String id = "";

                name = snapshot.getValue(String.class);
                Log.println(Log.INFO, "CommunitiesActivity", "value is " + name);
                id = snapshot.getKey();
                Log.println(Log.INFO, "CommunitiesActivity", "key is " + id);

                nameArrayList.add(name);
                idArrayList.add(id);
                arrayAdapter.notifyDataSetChanged();
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void checkNewCommunityValues() {

        final String community = communityText.getText().toString().trim();
        final String zipCode = zipCodeText.getText().toString().trim();


        if(community.isEmpty()){
            communityText.setError("Please enter a name for your community.");
            return;
        }

        if(community.length() < 6){
            communityText.setError("Community name must be longer than 6 characters.");
            return;
        }

        if(zipCode.isEmpty()){
            zipCodeText.setError("Please enter your community's zip code.");
            return;
        }

        if(zipCode.length() != 5){
            zipCodeText.setError("Please enter a valid 5 digit zip code.");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);

        createNewCommunity(community, zipCode);
    }

    private void createNewCommunity(final String communityName, String zipCode) {
        final String communityID = communityDB.push().getKey();

//        final MemberListEntry memberListEntry = new MemberListEntry(username, userDBEntry.getKey());
        Community newCom = new Community(communityName, Integer.parseInt(zipCode));

        communityDB.child(communityID).setValue(newCom).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {

                if (task.isSuccessful()){

                    //add user to Community member list; do this here instead of in Community constructor to give user a proper index in Firebase

                    communityDB.child(communityID).child("members").child(userID).setValue(username).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            if (task.isSuccessful()){
                                Log.println(Log.INFO, "Communities", "successfully created member list and added user.");

                            }

                            else Log.println(Log.INFO, "Communities", "Failed to create member list and added user.");
                        }
                    });

                    //Make creating user the community captain

                    communityDB.child(communityID).child("captain").child("uID").setValue(userID);
                    communityDB.child(communityID).child("captain").child("name").setValue(username);


                    //add community to user's community list on Firebase
                    userDBEntry.child("communityList").child(communityID).setValue(communityName);
//                    CommunityListEntry communityListEntry = new CommunityListEntry(communityID, communityName);
//                    userDBEntry.child("selectedCommunity").setValue(communityListEntry);
//                    userDBEntry.child("communityList").push().setValue(communityListEntry).addOnCompleteListener(new OnCompleteListener<Void>()

                    userDBEntry.child("selectedCommunity").child("cID").setValue(communityID);
                    userDBEntry.child("selectedCommunity").child("name").setValue(communityName).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            progressBar.setVisibility(View.GONE);
                            if (task.isSuccessful()) {
                                Toast.makeText(CommunitiesActivity.this, "Community successfully created and added to user's community list!", Toast.LENGTH_LONG).show();

                            }

                            else Toast.makeText(CommunitiesActivity.this, "Failed to add community to user's communities and probably failed to make community as well. Probably.", Toast.LENGTH_LONG).show();
                        }
                    });
                }
            }
        });
    }

    private void addNewMember() {

        String member = memberText.getText().toString().trim();
        String community = communityText.getText().toString().trim();

        if(member.isEmpty()){
            memberText.setError("Please enter your new member's name.");
            return;
        }

        if(member.length() < 4){
            memberText.setError("Member's name must be longer than 4 characters");
            return;
        }

        if(community.isEmpty()){
            communityText.setError("Please enter a name for your community.");
            return;
        }

        if(community.length() < 6){
            communityText.setError("Community name must be longer than 6 characters.");
            return;
        }

        Query communityQuery = communityDB.child(community);

    }
}
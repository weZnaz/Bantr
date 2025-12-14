package com.example.bantr.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.bantr.adapters.RecentConversationAdapter;
import com.example.bantr.databinding.ActivityMainBinding;
import com.example.bantr.listeners.ConversionListener;
import com.example.bantr.models.ChatMessage;
import com.example.bantr.models.User;
import com.example.bantr.utilities.Constants;
import com.example.bantr.utilities.PreferenceManager;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends BaseActivity implements ConversionListener {
private ActivityMainBinding binding;
private PreferenceManager preferenceManager;
private List<ChatMessage>conversions;
private RecentConversationAdapter conversionsAdapter;
private FirebaseFirestore database;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding=ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
       init();
        loadUserDetails();
        getToken();
        setlisteners();
        listenConversions();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {

                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }

    }

    private void init(){
        conversions=new ArrayList<>();
        conversionsAdapter=new RecentConversationAdapter(conversions,this);
        binding.conversionsRecycleerView.setAdapter(conversionsAdapter);
        database=FirebaseFirestore.getInstance();
    }

    private void setlisteners(){
        binding.imageSignOut.setOnClickListener(v ->signOut());

        binding.fabNewChat.setOnClickListener(v->startActivity(new Intent(getApplicationContext(),UsersActivity.class)));
    }

    private void loadUserDetails(){
        binding.nameText.setText(preferenceManager.getString(Constants.KEY_NAME));
        byte[] bytes= Base64.decode(preferenceManager.getString(Constants.KEY_IMAGE),Base64.DEFAULT);
        Bitmap bitmap= BitmapFactory.decodeByteArray(bytes,0,bytes.length);
        binding.imageProfile.setImageBitmap(bitmap);

    }

    private void showToast(String message){
        Toast.makeText(getApplicationContext(),message,Toast.LENGTH_SHORT).show();

    }
private void listenConversions(){
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID,preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_RECEIVER_ID,preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
}




    private final EventListener<QuerySnapshot> eventListener=(value, error) -> {
        if (error!=null){
            return;
        }if(value!=null){
            for (DocumentChange documentChange: value.getDocumentChanges()){
                if(documentChange.getType()==DocumentChange.Type.ADDED){
                    String senderId=documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    String receiverId=documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    ChatMessage chatMessage= new ChatMessage();
                    chatMessage.senderId=senderId;
                    chatMessage.receiverId=receiverId;
                    if(preferenceManager.getString(Constants.KEY_USER_ID).equals(senderId)){
                        chatMessage.conversionImage=documentChange.getDocument().getString(Constants.KEY_RECEIVER_IMAGE);
                        chatMessage.conversionName=documentChange.getDocument().getString(Constants.KEY_RECEIVER_NAME);
                        chatMessage.conversionId=documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);

                    }else{
                        chatMessage.conversionImage=documentChange.getDocument().getString(Constants.KEY_SENDER_IMAGE);
                        chatMessage.conversionName=documentChange.getDocument().getString(Constants.KEY_SENDER_NAME);
                        chatMessage.conversionId=documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    }
                    chatMessage.message=documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE);
                    chatMessage.dateObject=documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    conversions.add(chatMessage);
                } else if (documentChange.getType()==DocumentChange.Type.MODIFIED) {
                    for (int i=0;i<conversions.size();i++){
                        String senderId=documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                        String receiverId=documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                        if(conversions.get(i).senderId.equals(senderId)  && conversions.get(i).receiverId.equals(receiverId)){
                            conversions.get(i).message=documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE);
                            conversions.get(i).dateObject=documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                            break;
                        }

                    }

                }
            }
            Collections.sort(conversions,(obj1,obj2)->obj2.dateObject.compareTo(obj1.dateObject));
            conversionsAdapter.notifyDataSetChanged();
            binding.conversionsRecycleerView.smoothScrollToPosition(0);
            binding.conversionsRecycleerView.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.GONE);
        }
    };


    private void getToken(){
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(this::updateToken);

    }


    private void updateToken(String token){
        FirebaseFirestore database =FirebaseFirestore.getInstance();
        DocumentReference documentReference=database.collection(Constants.KEY_COLLECTION_USERS).document(preferenceManager.getString(Constants.KEY_USER_ID));
        documentReference.update(Constants.KEY_FCM_TOKEN,token)

                .addOnFailureListener(e -> showToast("Unable to update token"));

    }

    private void signOut(){
        showToast("Signing out...");
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        DocumentReference documentReference=
                database.collection(Constants.KEY_COLLECTION_USERS).document(preferenceManager.getString(Constants.KEY_USER_ID));
        HashMap<String,Object>updates=new HashMap<>();
        updates.put(Constants.KEY_FCM_TOKEN, FieldValue.delete());
        documentReference.update(updates)
                .addOnSuccessListener(unused -> {
                    preferenceManager.clear();
                    startActivity(new Intent(getApplicationContext(),SignInActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> showToast("Unable to Sign Out"));
    }

    @Override
    public void onConversionClicked(User user) {
        Intent intent=new Intent(getApplicationContext(), ChatActivity.class);
        intent.putExtra(Constants.KEY_USER,user);
        startActivity(intent);
    }
}
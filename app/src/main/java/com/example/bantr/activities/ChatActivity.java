package com.example.bantr.activities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.bantr.R;
import com.example.bantr.adapters.ChatAdapter;
import com.example.bantr.databinding.ActivityChatBinding;
import com.example.bantr.models.ChatMessage;
import com.example.bantr.models.User;
import com.example.bantr.utilities.Constants;
import com.example.bantr.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;


import android.util.Base64;
import android.view.View;
import android.view.textclassifier.ConversationAction;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import com.google.firebase.firestore.EventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Collections;


public class ChatActivity extends BaseActivity {

private ActivityChatBinding binding;
private User receiverUser;
private List<ChatMessage> chatMessage;
private ChatAdapter chatAdapter;
private PreferenceManager preferenceManager;
private FirebaseFirestore database;
private String conversationId=null;

private Boolean isReceiverAvailable=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
         binding=ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        loadReceiverDetails();
        init();
        setListeners();
        listenMessages();

    }
    private void init(){
        preferenceManager=new PreferenceManager(getApplicationContext());
        chatMessage=new ArrayList<>();
        chatAdapter=new ChatAdapter(
                chatMessage,
                getBitmapFromEncodedString(receiverUser.image),
                preferenceManager.getString(Constants.KEY_USER_ID)
        );
        binding.chatRecycleView.setAdapter(chatAdapter);
        database=FirebaseFirestore.getInstance();
    }
private void sendMessage(){
    HashMap<String, Object>message =new HashMap<>();
    message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
    message.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
    message.put(Constants.KEY_MESSAGE,binding.inputMessage.getText().toString());
    message.put(Constants.KEY_TIMESTAMP,new Date());
    database.collection(Constants.KEY_COLLECTION_CHAT).add(message);
   if(conversationId!=null){
       updateConversion(binding.inputMessage.getText().toString());
   }else{
       HashMap<String,Object>conversion=new HashMap<>();
       conversion.put(Constants.KEY_SENDER_ID,preferenceManager.getString(Constants.KEY_USER_ID));
       conversion.put(Constants.KEY_SENDER_NAME,preferenceManager.getString(Constants.KEY_NAME));
       conversion.put(Constants.KEY_SENDER_IMAGE,preferenceManager.getString(Constants.KEY_IMAGE));
       conversion.put(Constants.KEY_RECEIVER_ID,receiverUser.id);
       conversion.put(Constants.KEY_RECEIVER_NAME,receiverUser.name);
       conversion.put(Constants.KEY_RECEIVER_IMAGE,receiverUser.image);
       conversion.put(Constants.KEY_LAST_MESSAGE,binding.inputMessage.getText().toString());
       conversion.put(Constants.KEY_TIMESTAMP,new Date());
       addConversion(conversion);
   }
    binding.inputMessage.setText(null);
}

private void listenAvailabilityReceiver(){
        database.collection(Constants.KEY_COLLECTION_USERS).document(
                receiverUser.id
        ).addSnapshotListener(ChatActivity.this,(value, error) -> {
            if(error!=null){
                return;
            }if (value!=null){
                if(value.getLong(Constants.KEY_AVAILABILITY)!=null){
                    int availability=Objects.requireNonNull(
                            value.getLong(Constants.KEY_AVAILABILITY)
                    ).intValue();
                    isReceiverAvailable=availability==1;
                }
                receiverUser.token=value.getString(Constants.KEY_FCM_TOKEN);

            }
            if(isReceiverAvailable){
                binding.textAvailability.setVisibility(View.VISIBLE);
            }else{
                binding.textAvailability.setVisibility(View.GONE);
            }


        });
}


private void listenMessages(){
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID,preferenceManager.getString(Constants.KEY_USER_ID))
                .whereEqualTo(Constants.KEY_RECEIVER_ID,receiverUser.id)
                .addSnapshotListener(eventListener);
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID,receiverUser.id)
                .whereEqualTo(Constants.KEY_RECEIVER_ID,preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
}

private final EventListener<QuerySnapshot>eventListener=(value,error)->{
        if(error!=null){
            return ;
        }
        if(value!=null){
            int count=chatMessage.size();
            for (DocumentChange documentChange : value.getDocumentChanges()) {
                if (documentChange.getType() == DocumentChange.Type.ADDED) {
                    ChatMessage chat = new ChatMessage();
                    chat.senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    chat.receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    chat.message = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                    chat.dateTime = getReadableDateTime(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));
                    chat.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    chatMessage.add(chat);
                }
            }
            Collections.sort(chatMessage, (obj1, obj2) -> obj1.dateObject.compareTo(obj2.dateObject));
                    if(count==0){
                        chatAdapter.notifyDataSetChanged();
                    }else {
                        chatAdapter.notifyItemRangeInserted(chatMessage.size(),chatMessage.size());
                        binding.chatRecycleView.smoothScrollToPosition(chatMessage.size()-1);
                    }
                    binding.chatRecycleView.setVisibility(View.VISIBLE);
        }
        binding.progressBar.setVisibility(View.GONE);
        if(conversationId==null){
            checkForConversion();
        }
};


    private Bitmap getBitmapFromEncodedString(String encodedImage){
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes,0,bytes.length);
    }


    private void loadReceiverDetails(){
        receiverUser =(User)getIntent().getSerializableExtra(Constants.KEY_USER);
        binding.textName.setText(receiverUser.name);
    }
    private void setListeners(){

        binding.imageBack.setOnClickListener(v-> getOnBackPressedDispatcher().onBackPressed());
        binding.layoutSend.setOnClickListener(v->sendMessage());
    }
private String getReadableDateTime(Date date){
        return new SimpleDateFormat("MMMM dd,yyyy - hh:mm a", Locale.getDefault()).format(date);
}
private void addConversion(HashMap<String,Object>conversion){
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .add(conversion)
                .addOnSuccessListener(documentReference -> conversationId=documentReference.getId());
}
private void updateConversion(String message){
    DocumentReference documentReference=
            database.collection(Constants.KEY_COLLECTION_CONVERSATIONS).document(conversationId);
    documentReference.update(
            Constants.KEY_LAST_MESSAGE,message,
            Constants.KEY_TIMESTAMP,new Date()
    );
}

private void checkForConversion(){
        if(chatMessage.size() !=0){
            checkForConversionRemotrly(
                    preferenceManager.getString(Constants.KEY_USER_ID),
                    receiverUser.id
            );
            checkForConversionRemotrly(
                    receiverUser.id,
                    preferenceManager.getString(Constants.KEY_USER_ID)
            );
        }
}
private void checkForConversionRemotrly(String senderId,String receiverId){
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID,senderId)
                .whereEqualTo(Constants.KEY_RECEIVER_ID,receiverId)
                .get()
                .addOnCompleteListener(conversionOnCompleteListener);

}
private final OnCompleteListener<QuerySnapshot>conversionOnCompleteListener=task -> {
        if (task.isSuccessful() && task.getResult()!=null && task.getResult().getDocuments().size()>0){
            DocumentSnapshot documentSnapshot=task.getResult().getDocuments().get(0);
            conversationId=documentSnapshot.getId();
        }
};

    @Override
    protected void onResume() {
        super.onResume();
        listenAvailabilityReceiver();
    }
}
package com.example.bantr.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.bantr.databinding.ActivitySingnInBinding;
import com.example.bantr.utilities.Constants;
import com.example.bantr.utilities.PreferenceManager;
import com.google.firebase.Firebase;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class SignInActivity extends AppCompatActivity {
    private ActivitySingnInBinding binding;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferenceManager=new PreferenceManager(getApplicationContext());
        if(preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN)){
            Intent intent =new Intent(getApplicationContext(),MainActivity.class);
            startActivity(intent);
            finish();
        }
        binding=ActivitySingnInBinding.inflate(getLayoutInflater());

        setContentView(binding.getRoot());
       setListener();
    }


    private void setListener(){
        binding.createNewAccount.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(),SignUpActivity.class)));
binding.SignInButton.setOnClickListener(v ->{
    if(isValidSignInDetails()){
        signIn();
    }
});
    }

    private void signIn(){
loading(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo(Constants.KEY_EMAIL,binding.inputEmailSignIn.getText().toString())
                .whereEqualTo(Constants.KEY_PASSWORD,binding.inputPasswordignIn.getText().toString())
                .get()
                .addOnCompleteListener(task ->{
                    if(task.isSuccessful() && task.getResult() !=null
                    && task.getResult().getDocuments().size() >0){
                        DocumentSnapshot documentSnapshot=task.getResult().getDocuments().get(0);
                        preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN,true);
                        preferenceManager.putString(Constants.KEY_USER_ID, documentSnapshot.getId());
                        preferenceManager.putString(Constants.KEY_NAME,documentSnapshot.getString(Constants.KEY_NAME));
                        preferenceManager.putString(Constants.KEY_IMAGE,documentSnapshot.getString(Constants.KEY_IMAGE));
                        Intent intent = new Intent(getApplicationContext(),MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    }else{
                        loading(false);
                        showToast("Unable to sing in");
                    }
                });



    }

    private void loading(Boolean isLoading){
        if(isLoading){
            binding.SignInButton.setVisibility(View.INVISIBLE);
            binding.progressBarSignIn.setVisibility(View.VISIBLE);
        }else{
            binding.progressBarSignIn.setVisibility(View.INVISIBLE);
            binding.SignInButton.setVisibility(View.VISIBLE);
        }
    }



private void showToast(String message){
    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();

}

private Boolean isValidSignInDetails(){
        if (binding.inputEmailSignIn.getText().toString().trim().isEmpty()){
            showToast("Enter Email");
            return false;

        }else if (!Patterns.EMAIL_ADDRESS.matcher(binding.inputEmailSignIn.getText().toString()).matches()){
            showToast("Enter valid email");
            return false;

        }else if(binding.inputPasswordignIn.getText().toString().trim().isEmpty()){
            return false;

        }

        else{
            return true;

        }



}



}
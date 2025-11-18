package com.example.wordy.ui;
import com.example.wordy.ui.Word;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;


import com.example.wordy.R;
import com.example.wordy.ui.WordyActivity;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.List;

public class CreateWordActivity extends AppCompatActivity {
    
    private FirebaseFirestore db;
    private CollectionReference wordBankRef;


    private EditText wordInputField;
    private Button addButton;
    private Button cancelButton;
    private Button clearDbButton;
    private TextView questionLabel;


    

    //listener for adding a new word to the database
    private View.OnClickListener addWordListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            final String wordInput = wordInputField.getText().toString().toUpperCase();

            //resets label color before validation
            questionLabel.setTextColor(Color.BLACK);

            //run all required validation checks
            List<String> errors = validateWord(wordInput);

            if (errors.isEmpty()) {
                //if initial checks pass, check if already in firebase
                checkUniquenessAndAdd(wordInput);
            } else {
                handleValidationErrors(errors);
            }
        }
    };

    //listener for navigating back to the main game (Wordy Activity)
    private View.OnClickListener cancelListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(CreateWordActivity.this, WordyActivity.class);
            startActivity(intent);
            finish(); 
        }
    };

    //extra credit listener for clearing the entire database 
    private View.OnClickListener clearDbListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            clearWordBank();}
    };

    //main activity

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_word);

      
        db = FirebaseFirestore.getInstance();
        wordBankRef = db.collection("word_bank");
        
        wordInputField = findViewById(R.id.word_input_field);
        addButton = findViewById(R.id.add_button);
        cancelButton = findViewById(R.id.cancel_button);
        clearDbButton = findViewById(R.id.clear_db_button); 
        questionLabel = findViewById(R.id.word_label);

        addButton.setOnClickListener(addWordListener);
        cancelButton.setOnClickListener(cancelListener);
        clearDbButton.setOnClickListener(clearDbListener);
    }


    //validation logic
    private List<String> validateWord(String word) {
        List<String> errors = new ArrayList<>();

        //check is not empty
        if (word.isEmpty()) {
            errors.add("Word cannot be empty.");
        }

        //check if exactly 5 characters long
        if (word.length() != 5) {
            errors.add("Word must be exactly 5 letters long.");
        }

        //checks it only has alphabetical letters. uses a regex pattern. had to research to write it correctly.
        if (!Pattern.compile("^[A-Z]{5}$").matcher(word).matches()) {
            errors.add("Word must contain only alphabetical characters.");
        }

        return errors;
    }

    private void handleValidationErrors(List<String> errors) {
        questionLabel.setTextColor(Color.parseColor("#800080")); //purple

        StringBuilder errorMessage = new StringBuilder("Errors:\n");
        for (String error : errors) {
            errorMessage.append("- ").append(error).append("\n");
        }
        Toast.makeText(getApplicationContext(), errorMessage.toString().trim(), Toast.LENGTH_LONG).show();
    }


    //firebase logic

    private void checkUniquenessAndAdd(String wordInput) {
        //check same word is not already in database
        //all words stored in uppercase to compare correctly

        wordBankRef.whereEqualTo("value", wordInput)
                .limit(1)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (task.getResult().isEmpty()) {
                            //word is new and added to firebase
                            addNewWordToDatabase(wordInput);
                        } else {
                            //word is in database
                            List<String> uniquenessError = new ArrayList<>();
                            uniquenessError.add("This word already exists in the database (case insensitive).");
                            handleValidationErrors(uniquenessError);
                        }
                    } else {
                        Toast.makeText(getApplicationContext(), "Database query failed: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void addNewWordToDatabase(String wordInput) {
        Word newWord = new Word(wordInput);

        wordBankRef.add(newWord)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getApplicationContext(),
                                "Word '" + wordInput + "' stored successfully!",
                                Toast.LENGTH_SHORT).show();

                        finish(); //closes CreateWordActivity and returns to WordyActivity
                    } else {
                        Toast.makeText(getApplicationContext(),
                                "Error saving word: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }



    //extra credit logic for clearing the database
    private void clearWordBank() {
        //get all documents in the 'word_bank' collection
        wordBankRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                //use a WriteBatch for efficient bulk deletion
                WriteBatch batch = db.batch(); //researched WriteBatch to use

                if (task.getResult().isEmpty()) {
                    Toast.makeText(getApplicationContext(), "Word bank is already empty.", Toast.LENGTH_SHORT).show();
                    return;
                }

                //add all documents to the batch for deletion
                for (QueryDocumentSnapshot document : task.getResult()) {
                    batch.delete(document.getReference());
                }

                //commits the batch deletion
                batch.commit().addOnCompleteListener(commitTask -> {
                    if (commitTask.isSuccessful()) {
                        Toast.makeText(getApplicationContext(), "Successfully deleted ALL words in the word bank!", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "Failed to clear the database: " + commitTask.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });

            } else {
                Toast.makeText(getApplicationContext(), "Error retrieving words for deletion: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
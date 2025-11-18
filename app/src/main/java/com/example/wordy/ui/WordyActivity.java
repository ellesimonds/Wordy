package com.example.wordy.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;


import com.example.wordy.R;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class WordyActivity extends AppCompatActivity {


    private FirebaseFirestore db;
    private CollectionReference wordBankRef;

    private String targetWord = "";
    private int currentRow = 0;
    private final int MAX_TRIES = 6;
    private boolean gameActive = false;

    //UI
    private EditText[][] letterBoxes = new EditText[6][5];

    private Button submitButton;
    private Button restartButton;
    private Button clearButton;
    private Button addWordButton;
    private TextView gameStatusTextView;




    //listener for submitting a guess
    private View.OnClickListener submitListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!gameActive || currentRow >= MAX_TRIES || targetWord.isEmpty()) {
                Toast.makeText(getApplicationContext(), "Game is not active or word not loaded.", Toast.LENGTH_SHORT).show();
                return;
            }

            String guess = getCurrentGuess(); //this method retrieves input from the current row
            if (guess.length() != 5) {
                Toast.makeText(getApplicationContext(), "Guess must be 5 letters.", Toast.LENGTH_SHORT).show();
                return;
            }

            checkGuess(guess);
            lockRow(currentRow);

            if (guess.equalsIgnoreCase(targetWord)) {

                gameStatusTextView.setText("VICTORY! The word was " + targetWord);
                gameActive = false;

            } else if (currentRow == MAX_TRIES - 1) {

                gameStatusTextView.setText("INCORRECT! The word was " + targetWord);
                gameActive = false;

            } else {

                //move to next row
                currentRow++;
                for (int col = 0; col < 5; col++) {
                    letterBoxes[currentRow][col].setEnabled(true);
                }
            }

        }
    };

    //listener for restarting the game
    private View.OnClickListener restartListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            resetGameUI();
            retrieveRandomWord();
        }
    };

    //listener for clearing the current game
    private View.OnClickListener clearGameListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            resetGameUI();
        }
    };

    //listener for navigating to the Add Word Activity
    private View.OnClickListener navigateToAddWordListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent = new Intent(WordyActivity.this, CreateWordActivity.class);
            startActivity(intent);
        }
    };

    //main activity

    @SuppressLint({"MissingInflatedId", "ResourceType"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wordy);

        //initialize Firebase
        db = FirebaseFirestore.getInstance();
        wordBankRef = db.collection("word_bank");


        submitButton = findViewById(R.id.submit_button);
        restartButton = findViewById(R.id.restart_button);
        clearButton = findViewById(R.id.clear_button);
        addWordButton = findViewById(R.id.add_word_button);
        gameStatusTextView = findViewById(R.id.game_status_text_view);

        submitButton.setOnClickListener(submitListener);
        restartButton.setOnClickListener(restartListener);
        clearButton.setOnClickListener(clearGameListener);
        addWordButton.setOnClickListener(navigateToAddWordListener);

        retrieveRandomWord();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.layout.activity_wordy), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        //map all 6 rows of letter boxes
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 5; col++) {
                String id = "box_" + row + "_" + col;
                int resID = getResources().getIdentifier(id, "id", getPackageName());
                letterBoxes[row][col] = findViewById(resID);
            }
        }

        //lock all rows except row 0
        for (int row = 1; row < 6; row++) {
            for (int col = 0; col < 5; col++) {
                letterBoxes[row][col].setEnabled(false);
            }
        }

    }

    //firebase Logic

    private void retrieveRandomWord() {
        wordBankRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<String> words = new ArrayList<>();

                for (QueryDocumentSnapshot doc : task.getResult()) {
                    String word = doc.getString("value");
                    if (word != null) {
                        words.add(word.toUpperCase());
                    }
                }

                if (!words.isEmpty()) {
                    Random random = new Random();
                    targetWord = words.get(random.nextInt(words.size()));
                    Toast.makeText(this, "New target word loaded!", Toast.LENGTH_SHORT).show();
                    gameActive = true;
                } else {
                    Toast.makeText(this, "Word bank is empty!", Toast.LENGTH_LONG).show();
                    gameActive = false;
                }
            } else {
                Toast.makeText(this, "Failed to load words!", Toast.LENGTH_LONG).show();
                gameActive = false;
            }
        });
    }


    //game logic parts

    private String getCurrentGuess() {
        StringBuilder guess = new StringBuilder();
        for (int col = 0; col < 5; col++) {
            String letter = letterBoxes[currentRow][col].getText().toString().trim().toUpperCase();
            if (letter.isEmpty()) return "";  //missing letter
            guess.append(letter.charAt(0));
        }
        return guess.toString();
    }

    private void checkGuess(String guess) {

        char[] answer = targetWord.toUpperCase().toCharArray();
        char[] guessChars = guess.toUpperCase().toCharArray();

        //track used letters in answer to avoid over yellowing duplicates
        boolean[] used = new boolean[5];

        //first pass. mark GREENS
        for (int i = 0; i < 5; i++) {
            if (guessChars[i] == answer[i]) {
                letterBoxes[currentRow][i].setBackgroundColor(0xFF6AAA64); //green
                used[i] = true;
            }
        }

        //second pass. mark YELLOWS and GRAYS
        for (int i = 0; i < 5; i++) {

            //skip already green
            if (guessChars[i] == answer[i]) continue;

            boolean foundYellow = false;

            //look for matching letter in answer
            for (int j = 0; j < 5; j++) {
                if (!used[j] && guessChars[i] == answer[j]) {
                    foundYellow = true;
                    used[j] = true;
                    break;
                }
            }

            if (foundYellow) {
                letterBoxes[currentRow][i].setBackgroundColor(0xFFC9B458); //yellow
            } else {
                letterBoxes[currentRow][i].setBackgroundColor(0xFF787C7E); //grey
            }
        }
    }


    private void lockRow(int row) {
        for (int col = 0; col < 5; col++) {
            letterBoxes[row][col].setEnabled(false);
        }
    }

    private void resetGameUI() {

        currentRow = 0;
        gameActive = true;
        gameStatusTextView.setText("");

        //clear all 6 rows and re-enable row 0
        for (int row = 0; row < 6; row++) {
            for (int col = 0; col < 5; col++) {
                EditText box = letterBoxes[row][col];
                box.setText("");
                box.setBackgroundColor(0xFFFFFFFF); //white
                box.setEnabled(row == 0); //only row 0 editable
            }
        }
    }



}
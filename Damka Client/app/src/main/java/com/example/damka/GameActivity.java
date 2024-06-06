package com.example.damka;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.GridLayout;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.drawable.Drawable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


public class GameActivity extends AppCompatActivity implements WebSocketListener {

    private GridLayout checkersBoard;
    private TextView opponentNameView;
    private TextView textLabel;
    private Button offerDrawBtn;
    private Button chainBtn;
    private Button showEndPopupBtn;
    private String exitAlertMessage;
    private int selectedPieceIndex = -1;
    private char[][] board;
    private String color;

    private User opponent;
    private boolean lookingForGame = true;
    private boolean myTurn;
    private boolean drawOffer = false;
    private boolean opponentDrawOffer = false;
    private String endReturn;
    private PopupWindow endPopupWindow = null;

    private boolean isChainCapture = false;
    private final List<Move> moves = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);
        this.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        View rootLayout = getWindow().getDecorView().getRootView();
        rootLayout.requestLayout();

        Communicator.getInstance().setListener(this);
        Intent intent = getIntent();
        this.opponent = intent.getParcelableExtra("opponent");
        this.textLabel = findViewById(R.id.game_label);
        this.exitAlertMessage = "Are you sure you want to leave the match?";

        // Loop to create the grid of Buttons
        this.checkersBoard = findViewById(R.id.game_board_grid_view);
        for (int row = 0; row < Constants.BOARD_LENGTH; row++) {
            for (int col = 0; col < Constants.BOARD_LENGTH; col++) {
                Button square = new Button(this);
                GridLayout.LayoutParams params = new GridLayout.LayoutParams();
                params.width = 0;
                params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
                params.rowSpec = GridLayout.spec(row, 1f);
                params.columnSpec = GridLayout.spec(col, 1f);
                square.setLayoutParams(params);
                square.setBackgroundColor((row + col) % 2 == 0 ? getColor(R.color.light_square) : getColor(R.color.dark_square));

                // put pieces on the squares
                if ((row + col) % 2 == 1 && (row < 3 || row > 4)) {
                    Drawable drawable = AppCompatResources.getDrawable(this, R.drawable.soldier);
                    assert drawable != null;
                    if (row < 3) {
                        drawable.setColorFilter(getColor(R.color.dark_pieces), PorterDuff.Mode.SRC_IN);
                    } else {
                        drawable.setColorFilter(getColor(R.color.light_pieces), PorterDuff.Mode.SRC_IN);
                    }
                    square.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
                }

                // assign row and column for each button
                // Final variable for row, column indexes - saves them backwards to make the bottom left 1,1
                final int finalRow = row;
                final int finalCol = col;
                square.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        onSquareClick(finalRow, finalCol);
                    }
                });
                checkersBoard.addView(square);
            }
        }

        this.offerDrawBtn = findViewById(R.id.offer_draw_button);
        this.offerDrawBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               Communicator.getInstance().sendCode(Constants.OFFER_DRAW_CODE);
            }
        });

        this.chainBtn = findViewById(R.id.start_chain_capture);
        this.chainBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onChainBtnClick();
            }
        });

        this.showEndPopupBtn = findViewById(R.id.show_ending_button);
        this.showEndPopupBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (endPopupWindow != null) {
                    endPopupWindow.showAtLocation(getLayoutInflater().inflate(R.layout.end_game_popup, null), Gravity.CENTER, 0, 0);
                }
            }
        });

        Button exitBtn = findViewById(R.id.exit_game_button);
        exitBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backPressed();
            }
        });

        this.opponentNameView = findViewById(R.id.opponent_username_view);
        this.opponentNameView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (opponent != null) {
                    Communicator.getInstance().sendGetProfile(opponent.getUserID());
                }
            }
        });

        changeBoardInteraction(false);
        this.offerDrawBtn.setEnabled(false);
        if (opponent == null) {
            Communicator.getInstance().sendCode(Constants.FIND_GAME_CODE);
            showLabel("Loading...");
            this.endReturn = "end";
        } else {
            this.opponentNameView.setText(opponent.getUsername());
            int id = intent.getIntExtra("gameID", 0);
            sendInviteMatch(opponent.getUserID(), id);
            this.endReturn = "end_friend";
        }

        // Create an OnBackPressedCallback to handle the back button press
        OnBackPressedCallback backPressedCallback = new OnBackPressedCallback(true /* enabled by default */) {
            @Override
            public void handleOnBackPressed() {
                backPressed();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, backPressedCallback);
    }

    private int getSquareColor(int index) {
        int columns = checkersBoard.getColumnCount();
        int row = index / columns; // Calculate the row
        int col = index % columns; // Calculate the column
        return (row + col) % 2 == 0 ? getColor(R.color.light_square) : getColor(R.color.dark_square);
    }

    private void selectPiece(int row, int col, Button clickedSquare) {
        if (Objects.equals(this.color, "white")) {
            if (this.board[row][col] == Constants.WHITE_SOLDIER || this.board[row][col] == Constants.WHITE_QUEEN ) {
                this.selectedPieceIndex = row * checkersBoard.getColumnCount() + col;
                clickedSquare.setBackgroundColor(getColor(R.color.red));
            }
        } else {
            if (this.board[row][col] == Constants.BLACK_SOLDIER || this.board[row][col] == Constants.BLACK_QUEEN ) {
                this.selectedPieceIndex = row * checkersBoard.getColumnCount() + col;
                clickedSquare.setBackgroundColor(getColor(R.color.red));
            }
        }
    }

    private void onChainBtnClick() {
        if (this.isChainCapture) {
            if(!this.moves.isEmpty()) {
                sendMovePiece();
            } else {
                this.checkersBoard.setBackgroundColor(ContextCompat.getColor(this, R.color.blue));
            }
            selectedPieceIndex = -1;
            this.isChainCapture = false;
            this.chainBtn.setText("Chain Capture");
        } else {
            this.isChainCapture = true;
            this.chainBtn.setText("Done");
            this.checkersBoard.setBackgroundColor(ContextCompat.getColor(this, R.color.red));
        }
    }

    private void playMove(Button clickedSquare, int destIndex) {
        // find source square
        Button selectedPieceSquare = null;
        View child = checkersBoard.getChildAt(selectedPieceIndex);
        if (child instanceof Button) {
            selectedPieceSquare = (Button) child;
        }
        if (selectedPieceSquare == null) {
            return;
        }
        // reset source square color color
        selectedPieceSquare.setBackgroundColor(getSquareColor(selectedPieceIndex));

        // add move to list
        this.moves.add(new Move(selectedPieceIndex, destIndex));
        if (!this.isChainCapture) { // if normal move
            sendMovePiece();
            changeBoardInteraction(false);
            selectedPieceIndex = -1;
        } else { // if chainMove
            selectedPieceIndex = destIndex;
            // move the piece to destination and select it
            Drawable pieceDrawable = selectedPieceSquare.getCompoundDrawables()[0]; // get piece drawable on source square
            selectedPieceSquare.setCompoundDrawablesWithIntrinsicBounds(null, null, null, null); //clear source square

            clickedSquare.setBackgroundColor(ContextCompat.getColor(this, R.color.red));
            clickedSquare.setCompoundDrawablesWithIntrinsicBounds(pieceDrawable, null, null, null);
        }
    }

    private void onSquareClick(int row, int col) {
        int index = row * checkersBoard.getColumnCount() + col;
        Button clickedSquare = null;
        if (index >= 0 && index < checkersBoard.getChildCount()) {
            View child = checkersBoard.getChildAt(index);
            if (child instanceof Button) {
                clickedSquare = (Button) child;
            }
        }
        if (clickedSquare == null) {
            return;
        }
        if (selectedPieceIndex == -1) { // select new piece
          selectPiece(row, col, clickedSquare);
        } else if (selectedPieceIndex == index && !isChainCapture) { // unselect piece
            selectedPieceIndex = -1;
            clickedSquare.setBackgroundColor(getSquareColor(index));
        } else { // do something with piece
            playMove(clickedSquare, index);
        }
    }

    private void sendMovePiece() {
        try {
            JSONObject message = new JSONObject();
            message.put("code", Constants.MOVE_CODE);
            JSONArray movesMsg = new JSONArray();

            for (int i = 0; i < this.moves.size(); i ++) {
                Move move = this.moves.get(i);

                JSONObject src = new JSONObject();
                src.put("row", move.getSourceRow(this.color));
                src.put("column", move.getSourceColumn(this.color));

                JSONObject dst = new JSONObject();
                dst.put("row", move.getDestRow(this.color));
                dst.put("column", move.getDestColumn((this.color)));

                JSONObject moveStep = new JSONObject();
                moveStep.put("source_position", src);
                moveStep.put("destination_position", dst);

                movesMsg.put(moveStep);
            }
            message.put("moves", movesMsg);
            Communicator.getInstance().sendMessage(message.toString());
            this.moves.clear();
        } catch (JSONException e) {
            Toast.makeText(GameActivity.this, "Json Error", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void showWaitingForOpponent() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int numDots = 0;

                while (lookingForGame) {
                    numDots = (numDots + 1) % 4;
                    final String dots = new String(new char[numDots]).replace("\0", "."); // Create dots string

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            int numSpaces = 3 - dots.length();
                            //String spaces = new String(new char[numSpaces]).replace("\0", " ");
                            String msg = "Searching" + dots;
                            showLabel(msg);
                        }
                    });
                    try {
                        Thread.sleep(500); // Wait for 500 milliseconds
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    private void showLabel(String text) {
        textLabel.setText(text);
        textLabel.setVisibility(View.VISIBLE);
    }

    private void hideLabel() {
        textLabel.setVisibility(View.GONE);
    }

    private void offerDraw(JSONObject message) throws JSONException {
        if (message.getString("userID").equals(this.opponent.getUserID())) {
            this.opponentDrawOffer = !this.opponentDrawOffer;
            if (opponentDrawOffer) {
                this.opponentNameView.setText(this.opponent.getUsername() + "\n- Draw!");
            } else {
                this.opponentNameView.setText(this.opponent.getUsername());
            }
        } else {
            if (this.drawOffer) {
                hideLabel();
            } else {
                showLabel("Draw");
            }
            this.drawOffer = !this.drawOffer;
        }
    }

    private void changeBoardInteraction(boolean intractable) {
        this.checkersBoard.setClickable(intractable);
        this.checkersBoard.setFocusable(intractable);
        this.checkersBoard.setFocusableInTouchMode(intractable);
        if (intractable) {
            this.checkersBoard.setBackgroundColor(ContextCompat.getColor(this, R.color.blue));
        } else {
            this.checkersBoard.setBackgroundColor(ContextCompat.getColor(this, R.color.offline_color));
        }

        this.chainBtn.setEnabled(intractable);
        // Disable interaction with all buttons within the GridLayout
        for (int i = 0; i < checkersBoard.getChildCount(); i++) {
            View child = checkersBoard.getChildAt(i);
            if (child instanceof Button) {
                Button button = (Button) child;
                button.setClickable(intractable);
            }
        }
    }

    private void showProfile(JSONObject message) throws JSONException {
        JSONObject profile = message.getJSONObject("profile");
        User user;
        if (profile.getBoolean("private")) {
            user = new User(
                    profile.getString("userID"),
                    profile.getString("username"),
                    profile.getDouble("rating"),
                    profile.getInt("online"),
                    profile.getBoolean("private")
            );
        } else {
            user = User.parsUser(profile);
        }
        Intent intent = new Intent(GameActivity.this, ProfileActivity.class);
        intent.putExtra("user_profile", user);
        startActivity(intent);
    }

    private void setBoard(char[][] newBoard) {
        if (newBoard != board) {
            board = newBoard;
            for (int row = 0; row < Constants.BOARD_LENGTH; row++) {
                for (int col = 0; col < Constants.BOARD_LENGTH; col++) {
                    Button square = (Button)checkersBoard.getChildAt(row * checkersBoard.getColumnCount() + col);
                    Drawable drawable = null;
                    switch (newBoard[row][col]) {
                        case Constants.WHITE_SOLDIER:
                            drawable = AppCompatResources.getDrawable(this, R.drawable.soldier);
                            assert drawable != null;
                            drawable.setColorFilter(getColor(R.color.light_pieces), PorterDuff.Mode.SRC_IN);
                            break;
                        case Constants.BLACK_SOLDIER:
                            drawable = AppCompatResources.getDrawable(this, R.drawable.soldier);
                            assert drawable != null;
                            drawable.setColorFilter(getColor(R.color.dark_pieces), PorterDuff.Mode.SRC_IN);
                            break;
                            case Constants.WHITE_QUEEN:
                            drawable = AppCompatResources.getDrawable(this, R.drawable.queen);
                                assert drawable != null;
                                drawable.setColorFilter(getColor(R.color.light_pieces), PorterDuff.Mode.SRC_IN);
                            break;
                        case Constants.BLACK_QUEEN:
                            drawable = AppCompatResources.getDrawable(this, R.drawable.queen);
                            assert drawable != null;
                            drawable.setColorFilter(getColor(R.color.dark_pieces), PorterDuff.Mode.SRC_IN);
                            break;
                    }
                    square.setBackgroundColor((row + col) % 2 == 0 ? getColor(R.color.light_square) : getColor(R.color.dark_square));
                    square.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null);
                }
            }
        }
    }

    private char[][] parseBoard(JSONObject gameState) throws JSONException {
        JSONObject boardData = gameState.getJSONObject("board_state");
        JSONArray jsonArray = boardData.getJSONArray("array");

        char[][] array = new char[Constants.BOARD_LENGTH][Constants.BOARD_LENGTH];

        for (int i = 0; i < Constants.BOARD_LENGTH; i++) {
            JSONArray row = jsonArray.getJSONArray(i);
            for (int j = 0; j < Constants.BOARD_LENGTH; j++) {
                String pieceValue = row.getString(j);
                array[i][j] = (char)Integer.parseInt(pieceValue);
            }
        }

        // Flip the board for each color
        if (Objects.equals(this.color, "black")) {
            char[][] flipped = new char[Constants.BOARD_LENGTH][Constants.BOARD_LENGTH];

            for (int row = 0; row < Constants.BOARD_LENGTH; row++) {
                for (int col = 0; col < Constants.BOARD_LENGTH; col++) {
                    flipped[row][col] = array[Constants.BOARD_LENGTH - 1 - row][Constants.BOARD_LENGTH - 1 - col];
                }
            }
            return flipped;
        }

        return array;
    }

    private void updateGameState(JSONObject message) throws JSONException {
        JSONObject gameState = message.getJSONObject("game_state");
        setBoard(parseBoard(gameState));
        this.myTurn = this.color.equals(gameState.getString("turn"));
        changeBoardInteraction(this.myTurn);
    }

    private void moveResponse(JSONObject message) throws JSONException {
        if (!message.getBoolean("success")) {
            changeBoardInteraction(true);
        }
        updateGameState(message);
    }

    private void initiateGame(JSONObject message) throws JSONException {
        //extract data
        JSONObject opponentData = message.getJSONObject("opponent");
        JSONObject gameState = message.getJSONObject("game_state");
        this.opponent = new User(opponentData.getString("userID"), opponentData.getString("username"), opponentData.getDouble("rating"), 1, true);
        this.color = message.getString("color");
        this.myTurn = this.color.equals(gameState.getString("turn"));
        setBoard(parseBoard(gameState));

        //update UI
        this.lookingForGame = false;
        hideLabel();
        this.opponentNameView.setText(this.opponent.getUsername());

        sendReady(message.getInt("gameID"));
    }

    private void startGame() {
        hideLabel();
        this.offerDrawBtn.setEnabled(true);
        if (this.myTurn) {
            changeBoardInteraction(true);
        }
    }

    private void showGameResult(JSONObject message) throws JSONException {
        changeBoardInteraction(false);
        this.offerDrawBtn.setEnabled(false);
        this.exitAlertMessage = "Close";
        // this.gainTrophy;
        String result = message.getString("result");
        String title;
        int strokeColor;
        if (result.equals("1/2-1/2"))
        {
            title = "Draw";
            strokeColor = R.color.gray;
        } else {
            if ((result.equals("1-0") && this.color.equals("white")) || (result.equals("0-1") && this.color.equals("black"))) {
                title = "Win";
                strokeColor = R.color.green;
            } else {
                title = "Lose";
                strokeColor = R.color.red;
            }
        }

        View endWindow = getLayoutInflater().inflate(R.layout.end_game_popup, null);

        TextView titleView = endWindow.findViewById(R.id.end_game_title);
        TextView resultView = endWindow.findViewById(R.id.end_game_result);
        TextView reasonView = endWindow.findViewById(R.id.end_game_reason);
        Button backToMenu = endWindow.findViewById(R.id.end_game_back_to_menu_button);
        Button closePopUp = endWindow.findViewById(R.id.end_game_close_popup_button);

        // Modify the background stroke color
        GradientDrawable background = (GradientDrawable) endWindow.getBackground();
        background.setStroke(50, ContextCompat.getColor(this, strokeColor));

        this.endPopupWindow = new PopupWindow(endWindow, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
        // Set the system UI flags on the popup window's decor view
        this.endPopupWindow.getContentView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        this.endPopupWindow.showAtLocation(endWindow, Gravity.CENTER, 0, 0);

        titleView.setText(title);
        resultView.setText(result);
        reasonView.setText(message.getString("reason"));

        backToMenu.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                backPressed();
            }
        });

        closePopUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                endPopupWindow.dismiss();
            }
        });

        this.offerDrawBtn.setVisibility(View.GONE);
        this.chainBtn.setVisibility(View.GONE);
        this.showEndPopupBtn.setVisibility(View.VISIBLE);
    }

    private void handleNotification(JSONObject notification) throws JSONException {
        if (notification.getInt("type") == 5) {
            String contentData = notification.getString("content");
            JSONObject content = new JSONObject(contentData);
            if (content.getInt("code") == Constants.GAME_DETAILS_CODE) {
                initiateGame(content);
            } else if (content.getInt("code") == Constants.GAME_END_CODE) {
                showGameResult(content);
            }
        }
    }

    @Override
    public void onWebSocketConnected() {
    }

    @Override
    public void onMessageReceived(String message) {
        runOnUiThread(() -> {
            try {
                JSONObject result = new JSONObject(message);
                int code = result.getInt("code");
                switch (code) {
                    case Constants.NOTIFICATION_CODE:
                        handleNotification(result);
                        break;
                    case Constants.FAILED_INVITE_CODE:
                        failedInvite();
                        break;
                    case Constants.WAIT_FOR_OPPONENT:
                        showWaitingForOpponent();
                        break;
                    case Constants.GAME_DETAILS_CODE:
                        initiateGame(result);
                        break;
                    case Constants.START_GAME_CODE:
                        startGame();
                        break;
                    case Constants.GAME_END_CODE:
                        showGameResult(result);
                        break;
                    case Constants.OFFER_DRAW_CODE:
                        offerDraw(result);
                        break;
                    case Constants.GET_USER_PROFILE_CODE:
                        showProfile(result);
                        break;
                    case Constants.UPDATE_GAME_STATE_CODE:
                        updateGameState(result);
                        break;
                    case Constants.MOVE_CODE:
                        moveResponse(result);
                        break;
                }
            } catch (JSONException e) {
                Toast.makeText(GameActivity.this, "Json Error", Toast.LENGTH_SHORT).show();
                e.printStackTrace();
            }
        });
    }

    @Override
    public void onWebSocketClosed(String reason) {
        // WebSocket connection closed
        runOnUiThread(() -> {
            Toast.makeText(GameActivity.this, "Connection disappointed", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(GameActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        });
    }
    @Override
    public void onError(Exception ex) {
    }

    private void sendInviteMatch(String userID, int gameID) {
        String msg = "{\"code\": " + Constants.INVITE_CODE + ", \"userID\": \"" + userID +  "\", \"gameID\": " + gameID + "}";
        Communicator.getInstance().sendMessage(msg);
    }

    private void sendReady(int gameID) {
        String msg = "{\"code\": " + Constants.READY_CODE + ", \"gameID\": " + gameID + "}";
        Communicator.getInstance().sendMessage(msg);
    }

    private void failedInvite() {
        Communicator.getInstance().removeListener();
        Intent resultIntent = new Intent();
        setResult(RESULT_CANCELED, resultIntent);
        finish();
    }

    /*
    override the normal phones back button behavior
    to show a dialog (want to exit? yes/ no)
    before closing the activity
    */
    public void backPressed() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(this.exitAlertMessage)
                .setPositiveButton("Yes", (dialog, which) -> {
                    // Close the activity
                    Communicator.getInstance().sendCode(Constants.LEAVE_GAME_CODE);
                    Communicator.getInstance().removeListener();
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("result", this.endReturn);
                    setResult(RESULT_OK, resultIntent);
                    finish();
                })
                .setNegativeButton("No", (dialog, which) -> {
                    // Dismiss the dialog
                    dialog.dismiss();
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
package com.example.tictactoe;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothClass;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.*;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.nearby.connection.Strategy;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import android.content.DialogInterface;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import java.util.Map;
import android.bluetooth.BluetoothAdapter;

public class MainActivity extends AppCompatActivity {

    ImageView MyBitmap;
    Bitmap Screen;
    Canvas MyCanvas;
    Paint MyBrush;
    Integer ScreenWidth;
    Integer ScreenHeight;
    Boolean Win = false;
    Boolean Tie = false;
    TextView Current;
    Integer DevicePlayer;
    Integer CurrentPlayer = 1; //0 for o, 1 for x
    Integer AdvertisePlayer; //0 for o, 1 for x
    Integer DiscoverPlayer; //0 for o, 1 for x
    Integer LastSquare;
    Integer MyMatrix[][] = new Integer[3][3];

    Button Advertise;
    Button Discover;
    String AdvertiseUserNickName = "AdvertiseTicTacToe";
    String DiscoverUserNickName = "DiscoveryTicTacToe";
    boolean SetupCheck = false;
    boolean FinishCheck = false;
    boolean PlayAgain = false;
    boolean mIsAdvertising = false;
    Boolean mIsDiscovering = false;
    String AdvertiseConnectedEndPointId;
    String DiscoverConnectedEndPointId;
    public static final String ServiceId = "edu.cs4730.nearbyconnectiondemo";
    public static final Strategy STRATEGY = Strategy.P2P_STAR;
    ActivityResultLauncher<Intent> bluetoothActivityResultLauncher;
    private String[] REQUIRED_PERMISSIONS;
    ActivityResultLauncher<String[]> rpl;
    BluetoothAdapter mBluetoothAdapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ScreenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;        //gets screen width and height
        ScreenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;

        Permissions();

        //CurrentPlayer = 1;              //sets first player as x
        Current = findViewById(R.id.Current);

        for (int i = 0; i <= 2; i++){           //fills 2D matrix with -1 values
            for (int j = 0; j <= 2; j++){
                MyMatrix[i][j] = -1;
            }
        }

        MyBitmap = findViewById(R.id.MyBitmap);         //creates bitmap and canvas
        Screen = Bitmap.createBitmap(ScreenWidth, ScreenHeight, Bitmap.Config.ARGB_8888);
        MyCanvas = new Canvas(Screen);
        MyCanvas.drawColor(Color.WHITE);
        MyBitmap.setImageBitmap(Screen);
        MyBitmap.setOnTouchListener(new myTouchListener(this));

        MyBrush = new Paint();      //creates paint object
        MyBrush.setStrokeWidth(10);

        MyCanvas.drawLine(2 * (ScreenWidth/5), (ScreenWidth/5), 2 * (ScreenWidth/5), 4 *(ScreenWidth/5), MyBrush);          //draws grid on screen
        MyCanvas.drawLine(3 * (ScreenWidth/5), (ScreenWidth/5), 3 * (ScreenWidth/5), 4 *(ScreenWidth/5), MyBrush);
        MyCanvas.drawLine(ScreenWidth/5, 2 * (ScreenWidth/5), 4 * (ScreenWidth/5), 2 *(ScreenWidth/5), MyBrush);
        MyCanvas.drawLine(ScreenWidth/5, 3 * (ScreenWidth/5), 4 * (ScreenWidth/5), 3 *(ScreenWidth/5), MyBrush);

        Advertise = findViewById(R.id.Advertise);
        Discover = findViewById(R.id.Discover);

        Advertise.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mIsAdvertising)
                    stopAdvertising();  //already advertising, turn it off
                else
                    startAdvertising();
            }
        });

        Discover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mIsDiscovering)
                    stopDiscovering();//in discovery mode, turn it off
                else
                    startDiscovering();
            }
        });
    }

    public void Permissions(){
        bluetoothActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            // There are no request codes
                            Intent data = result.getData();
                        }
                    }
                });
        rpl = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                new ActivityResultCallback<Map<String, Boolean>>() {
                    @Override
                    public void onActivityResult(Map<String, Boolean> isGranted) {
                        boolean granted = true;
                        for (Map.Entry<String, Boolean> x : isGranted.entrySet()) {
                            if (!x.getValue()) granted = false;
                        }
                        if (granted){
                            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                            if (mBluetoothAdapter == null) {
                                // Device does not support Bluetooth
                                return;
                            }
                            //make sure bluetooth is enabled.
                            if (!mBluetoothAdapter.isEnabled()) {
                                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                                bluetoothActivityResultLauncher.launch(enableBtIntent);
                            } else {
                                //bluetooth is on, so list paired devices from here.
                            }
                        };
                    }
                }
        );

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            REQUIRED_PERMISSIONS = new String[]{android.Manifest.permission.BLUETOOTH_SCAN, android.Manifest.permission.BLUETOOTH_CONNECT, android.Manifest.permission.BLUETOOTH_ADVERTISE, android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.NEARBY_WIFI_DEVICES};
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            REQUIRED_PERMISSIONS = new String[]{android.Manifest.permission.BLUETOOTH_SCAN, android.Manifest.permission.BLUETOOTH_CONNECT, android.Manifest.permission.BLUETOOTH_ADVERTISE, android.Manifest.permission.ACCESS_FINE_LOCATION};
        } else {
            REQUIRED_PERMISSIONS = new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH};
        }
        if (!allPermissionsGranted())
            rpl.launch(REQUIRED_PERMISSIONS);
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    class myTouchListener implements View.OnTouchListener {
        public Context MyContext;

        public myTouchListener(Context context) {       //gets context for toasts
            MyContext = context;
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {     //listener for on click of screen

            if (Win == true || Tie == true){            //checks if game is won or tied, and prints toast if yes
                Toast.makeText(MyContext, "The game has ended, to keep playing press reset", Toast.LENGTH_LONG).show();
            } else{
                if (event.getAction() == MotionEvent.ACTION_UP) { //fake it for tap.
                    v.performClick();
                    Float X = event.getX();         //gets pixel coordinates of tap
                    Float Y = event.getY();
                    //finds the square inside which the tap was made
                    if (X >= 1 * (ScreenWidth/5) && X < 2 * (ScreenWidth/5) && Y >= 1 * (ScreenWidth/5) && Y < 2 * (ScreenWidth/5) && CurrentPlayer == DevicePlayer && MyMatrix[0][0] == -1){    //Top Left 1, check current player and if empty
                        LastSquare = 1;
                        if (CurrentPlayer == AdvertisePlayer){
                            Advertisesend(LastSquare.toString());
                        } else{
                            Discoversend(LastSquare.toString());
                        }
                    } else if (X >= 2 * (ScreenWidth/5) && X < 3 * (ScreenWidth/5) && Y >= 1 * (ScreenWidth/5) && Y < 2 * (ScreenWidth/5) && CurrentPlayer == DevicePlayer && MyMatrix[0][1] == -1){    //Top Middle 2, check current player and if empty
                        LastSquare = 2;
                        if (CurrentPlayer == AdvertisePlayer){
                            Advertisesend(LastSquare.toString());
                        } else{
                            Discoversend(LastSquare.toString());
                        }
                    } else if (X >= 3 * (ScreenWidth/5) && X < 4 * (ScreenWidth/5) && Y >= 1 * (ScreenWidth/5) && Y < 2 * (ScreenWidth/5) && CurrentPlayer == DevicePlayer && MyMatrix[0][2] == -1){    //Top Right 3, check current player and if empty
                        LastSquare = 3;
                        if (CurrentPlayer == AdvertisePlayer){
                            Advertisesend(LastSquare.toString());
                        } else{
                            Discoversend(LastSquare.toString());
                        }
                    } else if (X >= 1 * (ScreenWidth/5) && X < 2 * (ScreenWidth/5) && Y >= 2 * (ScreenWidth/5) && Y < 3 * (ScreenWidth/5) && CurrentPlayer == DevicePlayer && MyMatrix[1][0] == -1){    //Middle Left 4, check current player and if empty
                        LastSquare = 4;
                        if (CurrentPlayer == AdvertisePlayer){
                            Advertisesend(LastSquare.toString());
                        } else{
                            Discoversend(LastSquare.toString());
                        }
                    } else if (X >= 2 * (ScreenWidth/5) && X < 3 * (ScreenWidth/5) && Y >= 2 * (ScreenWidth/5) && Y < 3 * (ScreenWidth/5) && CurrentPlayer == DevicePlayer && MyMatrix[1][1] == -1){    //Middle Middle 5, check current player and if empty
                        LastSquare = 5;
                        if (CurrentPlayer == AdvertisePlayer){
                            Advertisesend(LastSquare.toString());
                        } else{
                            Discoversend(LastSquare.toString());
                        }
                    } else if (X >= 3 * (ScreenWidth/5) && X < 4 * (ScreenWidth/5) && Y >= 2 * (ScreenWidth/5) && Y < 3 * (ScreenWidth/5) && CurrentPlayer == DevicePlayer && MyMatrix[1][2] == -1){    //Middle Right 6, check current player and if empty
                        LastSquare = 6;
                        if (CurrentPlayer == AdvertisePlayer){
                            Advertisesend(LastSquare.toString());
                        } else{
                            Discoversend(LastSquare.toString());
                        }
                    } else if (X >= 1 * (ScreenWidth/5) && X < 2 * (ScreenWidth/5) && Y >= 3 * (ScreenWidth/5) && Y < 4 * (ScreenWidth/5) && CurrentPlayer == DevicePlayer && MyMatrix[2][0] == -1){    //Bottom Left 7, check current player and if empty
                        LastSquare = 7;
                        if (CurrentPlayer == AdvertisePlayer){
                            Advertisesend(LastSquare.toString());
                        } else{
                            Discoversend(LastSquare.toString());
                        }
                    } else if (X >= 2 * (ScreenWidth/5) && X < 3 * (ScreenWidth/5) && Y >= 3 * (ScreenWidth/5) && Y < 4 * (ScreenWidth/5) && CurrentPlayer == DevicePlayer && MyMatrix[2][1] == -1){    //Bottom Middle 8, check current player and if empty
                        LastSquare = 8;
                        if (CurrentPlayer == AdvertisePlayer){
                            Advertisesend(LastSquare.toString());
                        } else{
                            Discoversend(LastSquare.toString());
                        }
                    } else if (X >= 3 * (ScreenWidth/5) && X < 4 * (ScreenWidth/5) && Y >= 3 * (ScreenWidth/5) && Y < 4 * (ScreenWidth/5) && CurrentPlayer == DevicePlayer && MyMatrix[2][2] == -1){    //Bottom Right 9, check current player and if empty
                        LastSquare = 9;
                        if (CurrentPlayer == AdvertisePlayer){
                            Advertisesend(LastSquare.toString());
                        } else{
                            Discoversend(LastSquare.toString());
                        }
                    } else if(CurrentPlayer != DevicePlayer) {
                        Toast.makeText(MyContext, "It isn't your turn", Toast.LENGTH_LONG).show();      //catches if other player clicks
                    } else{
                        Toast.makeText(MyContext, "Please click an empty square inside the grid", Toast.LENGTH_LONG).show();      //catches if click is outside of square
                    }

                    return true;
                }
            }

            return false;
        }


    }

    public void Play(float TopLeftX, float TopLeftY){
        if (CurrentPlayer == 1){            //depending on current player, draws the appropriate symbol in the grid square
            DrawX(TopLeftX, TopLeftY);
        } else{
            DrawO(TopLeftX, TopLeftY);
        }

        if (CheckWin() == true){            //checks if win has occurred after playing move
            if (CurrentPlayer == 0){
                Current.setText("Player X Wins!");
            } else{
                Current.setText("Player O Wins!");
            }
        }
        if (Tie == true){               //checks if tie has occurred after playing move
            Current.setText("Tie!");
        }

    }

    public void ClearBoard(){               //overwrites the board, and resets all variables to their starting states
        MyCanvas.drawColor(Color.WHITE);
        Current.setText("Player X");
        MyCanvas.drawLine(2 * (ScreenWidth/5), (ScreenWidth/5), 2 * (ScreenWidth/5), 4 *(ScreenWidth/5), MyBrush);
        MyCanvas.drawLine(3 * (ScreenWidth/5), (ScreenWidth/5), 3 * (ScreenWidth/5), 4 *(ScreenWidth/5), MyBrush);
        MyCanvas.drawLine(ScreenWidth/5, 2 * (ScreenWidth/5), 4 * (ScreenWidth/5), 2 *(ScreenWidth/5), MyBrush);
        MyCanvas.drawLine(ScreenWidth/5, 3 * (ScreenWidth/5), 4 * (ScreenWidth/5), 3 *(ScreenWidth/5), MyBrush);
        MyMatrix = new Integer[3][3];
        for (int i = 0; i <= 2; i++){
            for (int j = 0; j <= 2; j++){
                MyMatrix[i][j] = -1;
            }
        }
        Tie = false;
        Win = false;
        CurrentPlayer = 1;
    }

    public void DrawX(float TopLeftX, float TopLeftY){          //draws x symbol in the box specified by the input variables
        Integer Width = ScreenWidth/5;
        Width = Width/10;
        float[] Points = {TopLeftX + Width, TopLeftY + Width, TopLeftX + (ScreenWidth/5) - Width, TopLeftY + (ScreenWidth/5) - Width, TopLeftX + (ScreenWidth/5) - Width, TopLeftY + Width, TopLeftX + Width, TopLeftY + (ScreenWidth/5) - Width};
        MyCanvas.drawLines(Points, MyBrush);
        CurrentPlayer = 0;              //updates the current player
        Current.setText("Player O");
    }

    public void DrawO(float TopLeftX, float TopLeftY){          //draws o symbol in the box specified by the input variables
        MyBrush.setStyle(Paint.Style.STROKE);
        Integer Width = ScreenWidth/5;
        Width = Width/10;
        MyCanvas.drawCircle(TopLeftX + (ScreenWidth/10), TopLeftY + (ScreenWidth/10), (ScreenWidth/10) -20,MyBrush);
        MyBrush.setStyle(Paint.Style.FILL);
        CurrentPlayer = 1;              //updates the current player
        Current.setText("Player X");
    }

    public boolean CheckWin(){          //checks to see if win or tie
        for (int x = 0; x <= 2; x++){       //vertical check
            if (MyMatrix[x][0] == 0 && MyMatrix[x][1] == 0 && MyMatrix[x][2] == 0){
                Win = true;
                return true;
            } else if (MyMatrix[x][0] == 1 && MyMatrix[x][1] == 1 && MyMatrix[x][2] == 1){
                Win = true;
                return true;
            }
        }

        for (int y = 0; y <= 2; y++){       //horizontal check
            if (MyMatrix[0][y] == 0 && MyMatrix[1][y] == 0 && MyMatrix[2][y] == 0){
                Win = true;
                return true;
            } else if (MyMatrix[0][y] == 1 && MyMatrix[1][y] == 1 && MyMatrix[2][y] == 1){
                Win = true;
                return true;
            }
        }

        if (MyMatrix[0][0] == 0 && MyMatrix[1][1] == 0 && MyMatrix[2][2] == 0){   //diagonal left to right
            Win = true;
            return true;
        } else if (MyMatrix[0][0] == 1 && MyMatrix[1][1] == 1 && MyMatrix[2][2] == 1){
            Win = true;
            return true;
        }

        if (MyMatrix[2][0] == 0 && MyMatrix[1][1] == 0 && MyMatrix[0][2] == 0){   //diagonal right to left
            Win = true;
            return true;
        } else if (MyMatrix[2][0] == 1 && MyMatrix[1][1] == 1 && MyMatrix[0][2] == 1){
            Win = true;
            return true;
        }

        Tie = true;                 //checks if any square is still -1, if none are, and the win has not been found, then it must be a tie
        for (int x = 0; x <= 2; x++) {
            for (int y = 0; y <= 2; y++){
                if (MyMatrix[x][y] == -1) {
                    Tie = false;
                }
            }
        }
        if (Tie == true){
            return false;
        }
        return false;
    }

    public Boolean CheckValid(Integer position){
        if (position == 1 && MyMatrix[0][0] == -1){    //Top Left 1
            MyMatrix[0][0] = CurrentPlayer;     //updates 2D matrix
            Play(1 * (ScreenWidth/5), ScreenWidth/5);       //calls play function with top left x and y coordinates
        } else if (position == 2 && MyMatrix[0][1] == -1){    //Top Middle 2
            MyMatrix[0][1] = CurrentPlayer;     //updates 2D matrix
            Play(2 * (ScreenWidth/5), ScreenWidth/5);       //calls play function with top left x and y coordinates
        } else if (position == 3 && MyMatrix[0][2] == -1){    //Top Right 3
            MyMatrix[0][2] = CurrentPlayer;     //updates 2D matrix
            Play(3 * (ScreenWidth/5), ScreenWidth/5);       //calls play function with top left x and y coordinates
        } else if (position == 4 && MyMatrix[1][0] == -1){    //Middle Left 4
            MyMatrix[1][0] = CurrentPlayer;     //updates 2D matrix
            Play(ScreenWidth/5, 2 * (ScreenWidth/5));       //calls play function with top left x and y coordinates
        } else if (position == 5 && MyMatrix[1][1] == -1){    //Middle Middle 5
            MyMatrix[1][1] = CurrentPlayer;     //updates 2D matrix
            Play(2 * (ScreenWidth/5), 2 * (ScreenWidth/5));       //calls play function with top left x and y coordinates
        } else if (position == 6 && MyMatrix[1][2] == -1){    //Middle Right 6
            MyMatrix[1][2] = CurrentPlayer;     //updates 2D matrix
            Play(3 * (ScreenWidth/5), 2 * (ScreenWidth/5));       //calls play function with top left x and y coordinates
        } else if (position == 7 && MyMatrix[2][0] == -1){    //Bottom Left 7
            MyMatrix[2][0] = CurrentPlayer;     //updates 2D matrix
            Play(1 * (ScreenWidth/5), 3 * (ScreenWidth/5));       //calls play function with top left x and y coordinates
        } else if (position == 8 && MyMatrix[2][1] == -1){    //Bottom Middle 8
            MyMatrix[2][1] = CurrentPlayer;     //updates 2D matrix
            Play(2 * (ScreenWidth/5), 3 * (ScreenWidth/5));       //calls play function with top left x and y coordinates
        } else if (position == 9 && MyMatrix[2][2] == -1){    //Bottom Right 9
            MyMatrix[2][2] = CurrentPlayer;     //updates 2D matrix
            Play(3 * (ScreenWidth/5), 3 * (ScreenWidth/5));       //calls play function with top left x and y coordinates
        } else{
            return false;
        }

        return true;
    }


    private final ConnectionLifecycleCallback AdvertisemConnectionLifecycleCallback = new ConnectionLifecycleCallback() {
        @Override
        public void onConnectionInitiated(@NonNull String endpointId, ConnectionInfo connectionInfo) {
            //logthis("Connection Initiated :" + endpointId + " Name is " + connectionInfo.getEndpointName());
            // Automatically accept the connection on both sides.
            // setups the callbacks to read data from the other connection.
            Nearby.getConnectionsClient(getApplicationContext()).acceptConnection(endpointId, //mPayloadCallback);
                    new PayloadCallback() {
                @Override
                public void onPayloadReceived(@NonNull String endpointId, @NonNull Payload payload) {

                    if (payload.getType() == Payload.Type.BYTES) {
                        String input = new String(payload.asBytes());

                        if (SetupCheck == false){               //setup check, if returns agree or disagree
                            if (input.equals("disagree") && PlayAgain == true) {     //ends game
                                FinishCheck = true;
                                MakeFinishDialog();
                                return;
                            }
                        }

                        boolean check = true;       //checks if it's an int
                        try {
                            Integer.parseInt(input);
                        } catch(NumberFormatException e) {
                            check = false;
                        }

                        if (check == true) {        //plays client's move
                            LastSquare = Integer.parseInt(input);
                            CurrentPlayer = DiscoverPlayer;
                            if(CheckValid(LastSquare)){     //checks if move is valid and plays move
                                CurrentPlayer = AdvertisePlayer;
                                Advertisesend("agree");
                            } else{     //if move isn't valid, send disagree
                                Advertisesend("disagree");
                                Toast.makeText(getApplicationContext(), "Client disagreed, game over", Toast.LENGTH_LONG).show();
                                FinishCheck = true;
                                MakeFinishDialog();
                                return;
                            }
                        } else if (input.equals("agree") && FinishCheck == true && PlayAgain == true) {         //if playing again
                            SetupCheck = false;
                            FinishCheck = false;
                            ClearBoard();
                            MakeStartDialog();
                        } else if (input.equals("agree") && FinishCheck == true) {      //if game is over and agreed
                            MakeFinishDialog();
                        } else if (input.equals("disagree") && FinishCheck == false && PlayAgain == false) {          //if client disagrees, game is over
                            Toast.makeText(getApplicationContext(), "Client disagreed, game over", Toast.LENGTH_LONG).show();
                            FinishCheck = true;
                            MakeFinishDialog();
                            return;
                        } else if (input.equals("disagree") && FinishCheck == true && PlayAgain == true) {      //if client doesn't want to play again
                            if (AdvertiseConnectedEndPointId != null)
                                if (AdvertiseConnectedEndPointId.compareTo("") != 0) { //connected to someone
                                    Nearby.getConnectionsClient(getApplicationContext()).disconnectFromEndpoint(AdvertiseConnectedEndPointId);
                                    AdvertiseConnectedEndPointId = "";
                                }
                            if (mIsAdvertising) {
                                stopAdvertising();
                            }
                            SetupCheck = false;
                            FinishCheck = false;
                            ClearBoard();
                            Current.setText("Please Connect");
                            Toast.makeText(getApplicationContext(), "Client didn't want to play again", Toast.LENGTH_LONG).show();
                            return;
                        } else if (input.equals("agree") && SetupCheck == true && PlayAgain == false) {         //client agrees with move and move is played, and win condition checked
                            CurrentPlayer = AdvertisePlayer;
                            CheckValid(LastSquare);
                            CurrentPlayer = DiscoverPlayer;
                            if (CheckWin() == true){
                                FinishCheck = true;
                                Advertisesend("winner");
                            } else if (Tie == true){
                                FinishCheck = true;
                                Advertisesend("tie");
                            } else{
                                Advertisesend("nowinner");
                            }
                        } else if (input.equals("agree") && PlayAgain == true) {    //part of starting new game.
                            PlayAgain = false;
                        } else if (input.equals("winner")){         //checks if client has won
                            if (CheckWin() == true){
                                FinishCheck = true;
                                Advertisesend("agree");
                                MakeFinishDialog();
                            }
                        } else if (input.equals("tie")){            //checks if there is a tie
                            if (Tie == true){
                                FinishCheck = true;
                                Advertisesend("agree");
                                MakeFinishDialog();
                            }
                        }
                        SetupCheck = true;

                    }
                }

                @Override
                public void onPayloadTransferUpdate(@NonNull String endpointId, @NonNull PayloadTransferUpdate payloadTransferUpdate) {
                    //if stream or file, we need to know when the transfer has finished.  ignoring this right now.
                }
            }
            );
        }

        public void MakeStartDialog(){          //dialog box at advertiser end of starting game
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage("Do you want to be X or O?");
            builder.setTitle("Pick a side !");
            builder.setCancelable(false);
            builder.setPositiveButton("X", (DialogInterface.OnClickListener) (dialog, which) -> {
                AdvertisePlayer = 1;
                DevicePlayer = 1;
                DiscoverPlayer = 0;
                Advertisesend("player X");
            });
            builder.setNegativeButton("O", (DialogInterface.OnClickListener) (dialog, which) -> {
                AdvertisePlayer = 0;
                DevicePlayer = 0;
                DiscoverPlayer = 1;
                Advertisesend("Player O");
            });
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        }

        public void MakeFinishDialog(){          //dialog box at advertiser end of finishing game
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage("Do you want to play again?");
            builder.setTitle("Play again?");
            builder.setCancelable(false);
            builder.setPositiveButton("Yes", (DialogInterface.OnClickListener) (dialog, which) -> {
                PlayAgain = true;
                Advertisesend("playagain");
            });
            builder.setNegativeButton("No", (DialogInterface.OnClickListener) (dialog, which) -> {
                SetupCheck = false;
                FinishCheck = false;
                ClearBoard();
                Current.setText("Please Connect");
                Toast.makeText(getApplicationContext(), "Disconnected from client", Toast.LENGTH_LONG).show();
                Advertisesend("exit");
            });
            AlertDialog alertDialog = builder.create();
            alertDialog.show();
        }

        @Override
        public void onConnectionResult(@NonNull String endpointId, ConnectionResolution result) {

            switch (result.getStatus().getStatusCode()) {
                case ConnectionsStatusCodes.STATUS_OK:
                    // We're connected! Can now start sending and receiving data.
                    AdvertiseConnectedEndPointId = endpointId;
                    stopAdvertising();
                    Toast.makeText(getApplicationContext(), "Advertise Connection Complete!", Toast.LENGTH_LONG).show();
                    Current.setText("Player X");
                    MakeStartDialog();
                    break;
                case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                    // The connection was rejected by one or both sides.
                    break;
                case ConnectionsStatusCodes.STATUS_ERROR:
                    // The connection broke before it was able to be accepted.
                    break;
            }
        }

        @Override
        public void onDisconnected(@NonNull String endpointId) {
            AdvertiseConnectedEndPointId = "";
        }
    };

    /**
     * Start advertising the nearby.  It sets the callback from above with what to once we get a connection
     * request.
     */
    private void startAdvertising() {

        Nearby.getConnectionsClient(getApplicationContext())
                .startAdvertising(
                        AdvertiseUserNickName,    //human readable name for the endpoint.
                        MainActivity.ServiceId,  //unique identifier for advertise endpoints
                        AdvertisemConnectionLifecycleCallback,  //callback notified when remote endpoints request a connection to this endpoint.
                        new AdvertisingOptions.Builder().setStrategy(MainActivity.STRATEGY).build()
                )
                .addOnSuccessListener(
                        new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unusedResult) {
                                mIsAdvertising = true;
                                Toast.makeText(getApplicationContext(), "Advertising starting!", Toast.LENGTH_LONG).show();

                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                mIsAdvertising = false;
                                // We were unable to start advertising.
                                e.printStackTrace();
                            }
                        });
    }

    /**
     * turn off advertising.  Note, you can not add success and failure listeners.
     */
    public void stopAdvertising() {
        mIsAdvertising = false;
        Nearby.getConnectionsClient(getApplicationContext()).stopAdvertising();
    }

    /**
     * Sends a {@link Payload} to all currently connected endpoints.
     */
    protected void Advertisesend(String data) {

        //basic error checking
        if (AdvertiseConnectedEndPointId.compareTo("") == 0)   //empty string, no connection
            return;

        Payload payload = Payload.fromBytes(data.getBytes());

        Nearby.getConnectionsClient(getApplicationContext()).
                sendPayload(AdvertiseConnectedEndPointId,  //end point to end to
                        payload)   //the actual payload of data to send.
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                    }
                })
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                    }
                }
        );
    }


    /**
     * Stops discovery.
     */
    protected void stopDiscovering() {
        mIsDiscovering = false;
        Nearby.getConnectionsClient(getApplicationContext()).stopDiscovery();
    }

    /**
     * Sets the device to discovery mode.  Once an endpoint is found, it will initiate a connection.
     */
    protected void startDiscovering() {
        Nearby.getConnectionsClient(getApplicationContext()).
                startDiscovery(
                        MainActivity.ServiceId,   //id for the service to be discovered.  ie, what are we looking for.

                        new EndpointDiscoveryCallback() {  //callback when we discovery that endpoint.
                            @Override
                            public void onEndpointFound(@NonNull String endpointId, @NonNull DiscoveredEndpointInfo info) {
                                //we found an end point.
                                //now make a initiate a connection to it.
                                makeConnection(endpointId);
                            }

                            @Override
                            public void onEndpointLost(@NonNull String endpointId) {
                            }
                        },
                        new DiscoveryOptions.Builder().setStrategy(MainActivity.STRATEGY).build()
                )  //options for discovery.
                .addOnSuccessListener(
                        new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unusedResult) {
                                mIsDiscovering = true;
                                Toast.makeText(getApplicationContext(), "Discovery starting!", Toast.LENGTH_LONG).show();

                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                mIsDiscovering = false;
                                e.printStackTrace();
                            }
                        }
                );

    }

    //the connection callback, both discovery and advertise use the same callback.
    private final ConnectionLifecycleCallback DiscovermConnectionLifecycleCallback =
            new ConnectionLifecycleCallback() {

                @Override
                public void onConnectionInitiated(
                        @NonNull String endpointId, @NonNull ConnectionInfo connectionInfo) {
                    // Automatically accept the connection on both sides.
                    // setups the callbacks to read data from the other connection.
                    Nearby.getConnectionsClient(getApplicationContext()).acceptConnection(endpointId, //mPayloadCallback);
                            new PayloadCallback() {
                                @Override
                                public void onPayloadReceived(@NonNull String endpointId, @NonNull Payload payload) {

                                    if (payload.getType() == Payload.Type.BYTES) {
                                        String input = new String(payload.asBytes());

                                        if (SetupCheck == false){       //setup of players
                                            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                            if (input.equals("player X")) {
                                                builder.setMessage("You are O");
                                            } else {
                                                builder.setMessage("You are X");
                                            }
                                            builder.setTitle("Choose your side!");
                                            builder.setCancelable(false);
                                            builder.setPositiveButton("Agree", (DialogInterface.OnClickListener) (dialog, which) -> {
                                                if (input.equals("player X")) {
                                                    DiscoverPlayer = 0;
                                                    DevicePlayer = 0;
                                                    AdvertisePlayer = 1;
                                                    Discoversend("agree");
                                                } else {
                                                    DiscoverPlayer = 1;
                                                    DevicePlayer = 1;
                                                    AdvertisePlayer = 0;
                                                    Discoversend("agree");
                                                }
                                            });
                                            builder.setNegativeButton("Disagree", (DialogInterface.OnClickListener) (dialog, which) -> {
                                                Discoversend("disagree");
                                                FinishCheck = true;
                                            });
                                            AlertDialog alertDialog = builder.create();
                                            alertDialog.show();

                                            SetupCheck = true;
                                        }

                                        boolean check = true;       //checks if input is an int
                                        try {
                                            Integer.parseInt(input);
                                        } catch(NumberFormatException e) {
                                            check = false;
                                        }

                                        if (check == true) {        //plays advertiser's move
                                            LastSquare = Integer.parseInt(input);
                                            CurrentPlayer = AdvertisePlayer;
                                            if(CheckValid(LastSquare)){
                                                CurrentPlayer = DiscoverPlayer;
                                                Discoversend("agree");
                                            } else{
                                                Discoversend("disagree");
                                                Toast.makeText(getApplicationContext(), "Advertiser disagreed, game over", Toast.LENGTH_LONG).show();
                                                FinishCheck = true;
                                                MakeFinishDialog();
                                                return;
                                            }

                                        } else if (input.equals("playagain") && FinishCheck == true) {      //checks if client wants to play again
                                            MakeFinishDialog();
                                        } else if (input.equals("exit")) {          //exits and disconnects if advertiser doesn't want to play again
                                            if (DiscoverConnectedEndPointId != null)
                                                if (DiscoverConnectedEndPointId.compareTo("") != 0) { //connected to someone
                                                    Nearby.getConnectionsClient(getApplicationContext()).disconnectFromEndpoint(DiscoverConnectedEndPointId);
                                                    DiscoverConnectedEndPointId = "";
                                                }
                                            if (mIsDiscovering) {
                                                stopDiscovering();
                                            }
                                            Toast.makeText(getApplicationContext(), "Disconnected from advertiser", Toast.LENGTH_LONG).show();
                                            SetupCheck = false;
                                            FinishCheck = false;
                                            ClearBoard();
                                            Current.setText("Please Connect");
                                            return;
                                        } else if (input.equals("disagree")){           //if advertiser disagrees
                                            Toast.makeText(getApplicationContext(), "Advertiser disagreed, game over", Toast.LENGTH_LONG).show();
                                            FinishCheck = true;
                                            return;
                                        } else if (input.equals("agree") && FinishCheck == false) {     //plays client's move and checks win conditions
                                            CurrentPlayer = DiscoverPlayer;
                                            CheckValid(LastSquare);
                                            CurrentPlayer = AdvertisePlayer;
                                            if (CheckWin() == true){
                                                FinishCheck = true;
                                                Discoversend("winner");
                                            } else if (Tie == true){
                                                FinishCheck = true;
                                                Discoversend("tie");
                                            } else{
                                                Discoversend("nowinner");
                                            }
                                        } else if (input.equals("winner")){
                                            if (CheckWin() == true){
                                                FinishCheck = true;
                                                Discoversend("agree");
                                            }
                                        } else if (input.equals("tie")){
                                            if (Tie == true){
                                                FinishCheck = true;
                                                Discoversend("agree");
                                            }
                                        }
                                    }
                                }

                                @Override
                                public void onPayloadTransferUpdate(@NonNull String endpointId, @NonNull PayloadTransferUpdate payloadTransferUpdate) {
                                    //if stream or file, we need to know when the transfer has finished.  ignoring this right now.
                                }
                            });
                }

                public void MakeFinishDialog(){         //client side dialog box at end of game
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage("Do you want to play again?");
                    builder.setTitle("Play again?");
                    builder.setCancelable(false);
                    builder.setPositiveButton("Yes", (DialogInterface.OnClickListener) (dialog, which) -> {
                        PlayAgain = false;
                        SetupCheck = false;
                        FinishCheck = false;
                        ClearBoard();
                        Discoversend("agree");
                    });
                    builder.setNegativeButton("No", (DialogInterface.OnClickListener) (dialog, which) -> {
                        Discoversend("disagree");
                        if (DiscoverConnectedEndPointId != null)
                            if (DiscoverConnectedEndPointId.compareTo("") != 0) { //connected to someone
                                Nearby.getConnectionsClient(getApplicationContext()).disconnectFromEndpoint(DiscoverConnectedEndPointId);
                                DiscoverConnectedEndPointId = "";
                            }
                        if (mIsDiscovering) {
                            stopDiscovering();
                        }
                        Toast.makeText(getApplicationContext(), "Disconnected from advertiser", Toast.LENGTH_LONG).show();
                        SetupCheck = false;
                        FinishCheck = false;
                        ClearBoard();
                    });
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                }

                @Override
                public void onConnectionResult(@NonNull String endpointId, ConnectionResolution result) {
                    switch (result.getStatus().getStatusCode()) {
                        case ConnectionsStatusCodes.STATUS_OK:
                            // We're connected! Can now start sending and receiving data.
                            stopDiscovering();
                            DiscoverConnectedEndPointId = endpointId;
                            Toast.makeText(getApplicationContext(), "Discover Connection Complete!", Toast.LENGTH_LONG).show();
                            Current.setText("Player X");
                            break;
                        case ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED:
                            // The connection was rejected by one or both sides.
                            break;
                        case ConnectionsStatusCodes.STATUS_ERROR:
                            // The connection broke before it was able to be accepted.
                            break;
                    }
                }

                @Override
                public void onDisconnected(@NonNull String endpointId) {
                    // We've been disconnected from this endpoint. No more data can be
                    // sent or received.
                    DiscoverConnectedEndPointId = "";
                }
            };


    /**
     * Simple helper function to initiate a connect to the end point
     * it uses the callback setup above this function.
     */

    public void makeConnection(String endpointId) {
        Nearby.getConnectionsClient(getApplicationContext());
        Nearby.getConnectionsClient(getApplicationContext())
                .requestConnection(
                        DiscoverUserNickName,   //human readable name for the local endpoint.  if null/empty, uses device name or model.
                        endpointId,
                        DiscovermConnectionLifecycleCallback)
                .addOnSuccessListener(
                        new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void unusedResult) {
                                // We successfully requested a connection. Now both sides
                                // must accept before the connection is established.
                            }
                        })
                .addOnFailureListener(
                        new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                // Nearby Connections failed to request the connection.
                                e.printStackTrace();
                            }
                        }
                );
    }

    /**
     * Sends a {@link Payload} to all currently connected endpoints.
     */
    protected void Discoversend(String data) {

        //basic error checking
        if (DiscoverConnectedEndPointId.compareTo("") == 0)   //empty string, no connection
            return;

        Payload payload = Payload.fromBytes(data.getBytes());

        // sendPayload (List<String> endpointIds, Payload payload)  if more then one connection allowed.
        Nearby.getConnectionsClient(getApplicationContext()).
                sendPayload(DiscoverConnectedEndPointId,  //end point to end to
                        payload)   //the actual payload of data to send.
                .addOnSuccessListener(new OnSuccessListener<Void>() {  //don't know if need this one.
                    @Override
                    public void onSuccess(Void aVoid) {

                    }
                })
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {

                        e.printStackTrace();
                    }
                });
    }


}
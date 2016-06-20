package com.home.weatherstation;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


public class MainActivity extends AppCompatActivity {


    Button startButton;
    Button stopButton;
    TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        status = (TextView) findViewById(R.id.status);
        startButton = (Button) findViewById(R.id.start_button);
        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                tryAuthActivity();
//                start();
            }
        });
        stopButton = (Button) findViewById(R.id.stop_button);
        status = (TextView) findViewById(R.id.status);


    }

    private void tryAuthActivity() {
        startActivityForResult(new Intent(this, AuthActivity.class), 2001);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 2001) {
            if (resultCode == RESULT_OK) {
                start();
            }
        }
    }

    private void start() {
        Intent serviceIntent = new Intent(this, ScannerService.class);
        serviceIntent.setAction(ScannerService.INITIALIZE);
        startService(serviceIntent);

        Intent scanIntent = new Intent(this, ScannerService.class);
        scanIntent.setAction(ScannerService.SCAN);
        this.startService(scanIntent);
    }

    private void stop() {
        Intent serviceIntent = new Intent(this, ScannerService.class);
        serviceIntent.setAction(ScannerService.STOP);
        startService(serviceIntent);

    }

}

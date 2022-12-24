package steven.share_with_you;

import android.content.Intent;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.CardView;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final TextView textMsg = (TextView) findViewById(R.id.tutorial_msg);
        final ImageView imageView = (ImageView) findViewById(R.id.tutorial_demo);
        final CardView cardView = (CardView) findViewById(R.id.tutorial_card);
        final TextView textTitle = (TextView) findViewById(R.id.tutorial_title);

        final View view = findViewById(R.id.activity_main);
        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.btn_start);

        view.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                if(!isInMultiWindowMode()){
                    fab.setVisibility(View.INVISIBLE);
                    textMsg.setText(getText(R.string.multi_msg));
                    imageView.setImageResource(R.drawable.multi_demo);
                    cardView.setCardBackgroundColor(getColor(R.color.colorRed));
                    textTitle.setText(getText(R.string.multi_title));
                    textTitle.setTextColor(getColor(R.color.colorWhite));
                    textMsg.setTextColor(getColor(R.color.colorWhite));
                }
                else {
                    Boolean isHalf = detectWinSize();
                    if (!isHalf) {
                        fab.setVisibility(View.INVISIBLE);
                        textMsg.setText(getText(R.string.resize_msg));
                        imageView.setImageResource(R.drawable.resize_demo);
                        cardView.setCardBackgroundColor(getColor(R.color.colorAccent));
                        textTitle.setText(getText(R.string.resize_title));
                    } else {
                        fab.setVisibility(View.VISIBLE);
                        textMsg.setText(getText(R.string.ready_msg));
                        textTitle.setText(getText(R.string.ready_title));
                        imageView.setImageResource(R.drawable.ready_demo);
                        cardView.setCardBackgroundColor(getColor(R.color.colorBlue));
                        textTitle.setTextColor(getColor(R.color.colorWhite));
                        textMsg.setTextColor(getColor(R.color.colorWhite));

                    }
                }
            }
        });
    }

    public void initMirror(View view) {
        if (!isInMultiWindowMode()) {
            Toast.makeText(this, "Launch another app in multi-window to start mirroring", Toast.LENGTH_SHORT).show();
        }
        else{
            startMirrorActivity();
        }
    }

    private void startMirrorActivity() {
        Intent iMirrorActivity = new Intent(this, startMirrorActivity.class);
        startActivity(iMirrorActivity);
        finish();
    }

    private boolean detectWinSize(){
        Boolean isHalf;
        int winOrient;
        int realHeight;
        int realWidth;
        int displayHeight;

        DisplayMetrics displayMetrics = new DisplayMetrics();
        DisplayMetrics realMetric = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(realMetric);
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        realHeight = realMetric.heightPixels;
        realWidth = realMetric.widthPixels;
        displayHeight = displayMetrics.heightPixels;

        if(realHeight>realWidth) winOrient = 0; //portrait
        else if (realHeight<realWidth) winOrient = 1; //landscape
        else winOrient = 2; //square

        if(winOrient == 0) {
            Float sizeRatio = Float.valueOf(displayHeight) / Float.valueOf(realHeight);

            isHalf = !(sizeRatio < 0.4f || sizeRatio > 0.5f);
        }
        else //no resizing in landscape mode
            isHalf = winOrient == 1;
        return isHalf;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.action_bar, menu);
        return true;
    }
}

package steven.share_with_you;

import android.content.Context;
import android.content.Intent;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;

public class startMirrorActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener {

    private static final String TAG = "ScreenMirror";
    private static final int PERMISSION_CODE = 1;
    private static final String STATE_RESULT_CODE = "result_code";
    private static final String STATE_RESULT_DATA = "intent_data";

    private int mLocation;
    private int mScreenDensity;
    private int mDisplayHeight;
    private int mDisplayWidth;
    private int mRealHeight;
    private int mRealWidth;
    private int mResultCode;
    private Intent mIntentData;
    private MediaProjection mMediaProjection;
    private MediaProjectionManager mProjectionManager;
    private VirtualDisplay mVirtualDisplay;
    private TextureView mTextureView;
    private Surface mSurface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_mirror);

        if (savedInstanceState != null) {
            mResultCode = savedInstanceState.getInt(STATE_RESULT_CODE);
            mIntentData = savedInstanceState.getParcelable(STATE_RESULT_DATA);
        }

        DisplayMetrics metrics = new DisplayMetrics();
        DisplayMetrics real_metric = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        getWindowManager().getDefaultDisplay().getRealMetrics(real_metric);

        mScreenDensity = metrics.densityDpi;
        mDisplayHeight = metrics.heightPixels;
        mDisplayWidth = metrics.widthPixels;
        mRealHeight = real_metric.heightPixels;
        mRealWidth = real_metric.widthPixels;

        mTextureView = new TextureView(this);
        mTextureView.setSurfaceTextureListener(this);
        setContentView(mTextureView);
        mTextureView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                int[] locSize = detectLocation();
                if(locSize[1] == 0){
                    stopMirroring();
                    endProjection();
                    backToMain();
                }
                mLocation = locSize[0];
            }
        });
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mIntentData != null) {
            outState.putInt(STATE_RESULT_CODE, mResultCode);
            outState.putParcelable(STATE_RESULT_DATA, mIntentData);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        if (requestCode != PERMISSION_CODE) {
            Log.e(TAG, "Unknown request code: " + requestCode);
            return;
        }
        if (resultCode != RESULT_OK) {
            Toast.makeText(this, "User denied screen sharing permission", Toast.LENGTH_SHORT).show();
            backToMain();
            return;
        }

        mResultCode = resultCode;
        mIntentData = data;

        mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
        mVirtualDisplay = mMediaProjection.createVirtualDisplay("ScreenMirror",
                mDisplayWidth, mDisplayHeight, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mSurface, null, null);
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

            mSurface = new Surface(surface);

            Matrix mTrans = calcMatrix(mLocation);
            mTextureView.setTransform(mTrans);

            mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            startActivityForResult(mProjectionManager.createScreenCaptureIntent(), PERMISSION_CODE);

    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {}

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    private void backToMain(){
        Intent iMainActivity = new Intent(this, MainActivity.class);
        startActivity(iMainActivity);
        finish();
    }

    private void stopMirroring(){
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }
    }

    private void endProjection(){
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
    }

    private Matrix calcMatrix(int locIndex){
        int navigationBarHeight = 0;
        int statusBarHeight = 0;
        Float scaleFactor;

        int navBarId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (navBarId > 0) {
            navigationBarHeight = getResources().getDimensionPixelSize(navBarId);
        }

        int statusBarId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if(statusBarId > 0) {
            statusBarHeight = getResources().getDimensionPixelSize(statusBarId);
        }

        mTextureView.setRotation(180);
        Matrix mTrans = new Matrix();

        if(locIndex == 0){
            scaleFactor = Float.valueOf(mRealHeight)/Float.valueOf(mDisplayHeight);
            mTrans.setScale(scaleFactor,scaleFactor,mDisplayWidth/2,mDisplayHeight);
            mTrans.postTranslate(0,navigationBarHeight+1);
        }
        else if(locIndex == 1){
            scaleFactor = Float.valueOf(mRealHeight)/Float.valueOf(mDisplayHeight);
            mTrans.setScale(scaleFactor,scaleFactor,mDisplayWidth/2,0);
            mTrans.postTranslate(0,-statusBarHeight-1);
        }
        else if(locIndex == 2){
            scaleFactor = Float.valueOf(mRealWidth)/Float.valueOf(mDisplayWidth);
            mTrans.setScale(scaleFactor,scaleFactor,mDisplayWidth,(mDisplayHeight-statusBarHeight)/2);
            if(Build.VERSION.SDK_INT == 24) {
                mTrans.postTranslate(navigationBarHeight + 1, -statusBarHeight);
            }
            else{
                mTrans.postTranslate(0, -statusBarHeight);
            }
        }
        else if(locIndex == 3){
            scaleFactor = Float.valueOf(mRealWidth)/Float.valueOf(mDisplayWidth);
            mTrans.setScale(scaleFactor,scaleFactor,0,(mDisplayHeight-statusBarHeight)/2);
            mTrans.postTranslate(0,-statusBarHeight);
        }

        return mTrans;
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        stopMirroring();
        endProjection();
    }

    @Override
    public void onMultiWindowModeChanged(boolean isInMultiWindowMode){
        super.onMultiWindowModeChanged(isInMultiWindowMode);
        stopMirroring();
        endProjection();
        backToMain();
    }

    @Override
    public void onBackPressed(){
        stopMirroring();
        endProjection();
        backToMain();
    }

    private int[] detectLocation(){
        int[] locSize = new int[2];
        int winOrient;

        DisplayMetrics displayMetrics = new DisplayMetrics();
        DisplayMetrics realMetric = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(realMetric);
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        mRealHeight = realMetric.heightPixels;
        mRealWidth = realMetric.widthPixels;
        mDisplayHeight = displayMetrics.heightPixels;
        mDisplayWidth = displayMetrics.widthPixels;

        if(mRealHeight>mRealWidth) winOrient = 0; //portrait
        else if (mRealHeight<mRealWidth) winOrient = 1; //landscape
        else winOrient = 2; //square

        int[] viewLocation = new int[2];
        if(winOrient == 0) {
            int viewHeight = mTextureView.getHeight();
            mTextureView.getLocationOnScreen(viewLocation);
            int viewCenter = (viewLocation[1] + viewHeight + viewLocation[1]) / 2;
            if (viewCenter < mRealHeight / 2) {
                locSize[0] = 0; //top
            } else {
                locSize[0] = 1; //bottom
            }
            Float sizeRatio = Float.valueOf(mDisplayHeight) / Float.valueOf(mRealHeight);

            if(sizeRatio<0.4f || sizeRatio>0.5f)
            {
                locSize[1] = 0;  //no half split
            }
            else locSize[1] = 1; //half split
        }
        else if (winOrient == 1){
            locSize[1] = 1; //no resizing in landscape mode
            int viewWidth = mTextureView.getWidth();
            mTextureView.getLocationOnScreen(viewLocation);
            int viewCenter = (viewLocation[0] + viewWidth + viewLocation[0]) / 2;
            if (viewCenter< mRealWidth/2) {
                locSize[0] = 2; //left
            }
            else {
                locSize[0] = 3; //right
            }
        }
        else {
            locSize[0] = 4;
            locSize[1] = 0;//square
        }
        return locSize;
    }


}

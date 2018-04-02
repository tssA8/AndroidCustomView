package com.example.kuohsuan.androidmotionarea;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class MotionAreaActivity extends AppCompatActivity {
    private final String TAG = this.getClass().getSimpleName();


    private final int ACTIONBAR_MENU_ITEM_SAVE = 0x0001;
    private final int PRIVATE_MASK_ROW_16 = 16;
    private final int PRIVATE_MASK_ROW_32 = 32;
    private final int PRIVATE_MASK_256 = PRIVATE_MASK_ROW_16*PRIVATE_MASK_ROW_16;
    private MenuItem menuSaveItem;

    //view stuff
    private Toolbar toolbar;
//    private RelativeLayout vg_snapshotContainer;
    private ConstraintLayout vg_snapshotContainer;
    private RelativeLayout vg_revert_motion_area;
    private ZoomView zoomView;
    private ImageView iv_snapShot;
    private Long deviceId =null;
    private ImageButton btn_all_unselect;
    private ImageButton btn_all_select;
    private RelativeLayout vg_revert_motion_aera;

    //draw Mask things...
    private ArrayList<Position> selectedListShow = new ArrayList<Position>();
    private ArrayList<Integer> originalMotionAreaUnDetectList = null;//privateMask from server
    private ArrayList<Integer> motionAreaUnDetectList = null;//privateMask from server

    //dealt with BitMap .....
    private String snapShotUrl ="";
    private int bitMapHigh = 0;
    private int bitMapWidth = 0;
    private float calculateRemoveImageWidth = 0;//超出圖片部分的白邊(左右)
    private float calculateRemoveImageHeight = 0;//超出圖片部分的白邊(上下)
    private float imageScaledWidth = 0;//keep ratio width
    private float imageScaledHeight = 0;//keep ratio height

    //edit private mask mode
//    private String privateMaskState ="do_nothing";
    private static final String ADD_PRIVATE_MASK = "add";
    private static final String DELETE_PRIVATE_MASK = "delete";
    private ProgressDialog progressDialog = null;

    //rect view
    private RectMaskView rectMaskView;
    private ArrayList<RectMaskView.PaintStatus> paintLists = new ArrayList<RectMaskView.PaintStatus>();
    private ArrayList<RectMaskView.PaintStatus> paintTriggerLists = new ArrayList<RectMaskView.PaintStatus>();
//    private ArrayList<RectMaskView.PaintStatus> paintTriggerLastTimeLists = new ArrayList<RectMaskView.PaintStatus>();
    private long resultId = -1; // 的callback





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.motion_area_grid_activity);

        try {
            //get  Mask to draw from bundle
            Bundle bundle = this.getIntent().getExtras();
            motionAreaUnDetectList = new ArrayList<>();
            originalMotionAreaUnDetectList = new ArrayList<>();
            deviceId = 2l;

        }catch (Exception e){
            e.printStackTrace();
        }

        if(isNetworkAvailable(MotionAreaActivity.this)==false)
            undetectListEmptyAlertDialog();

        zoomView = new ZoomView(this);
        zoomView.disableZoomView(false);



        //get snap shot Image from Server
//        if(lib == null) {
//            lib = new MyGeoUtilityLib();
//            lib.addToHandlerList(geoLibCallBackHandler);
//        }
        if(deviceId!=null)
            snapShotUrl = "https://images.idgesg.net/images/article/2017/08/android_robot_logo_by_ornecolorada_cc0_via_pixabay1904852_wide-100732483-large.jpg";

        findViews();

        setListener();

        setToolbar();

    }

    @Override
    protected void onStart() {
        super.onStart();

        /*設定畫面是否要休眠,要放在onStart去執行,不然會沒效果*/
        SharedPreferences configSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
//        boolean wakeLock = ConfigSharedPreferencesUtil.isWakelock(this, configSharedPreferences);
        //不要進入休眠,

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		/*設定畫面是否要休眠,要放在onStart去執行,不然會沒效果*/

    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void setToolbar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.clear();

        menuSaveItem = menu.add(Menu.NONE, ACTIONBAR_MENU_ITEM_SAVE, Menu.NONE
                , getString(R.string.actionbar_item_search_again));
        menuSaveItem.setIcon(R.mipmap.nav_save);
        menuSaveItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);

        return true;
    }

    @TargetApi(Build.VERSION_CODES.M)
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
//        String privateMaskState =recognizeMode();
        // Handle item selection
        switch (item.getItemId()) {
            case android.R.id.home: {
                Log.d(TAG, "onOptionsItemSelected close this activity.");
                ArrayList<Integer> unDetectList = transformTo1024View();
                if(isTwoArrayListSame(unDetectList,motionAreaUnDetectList)==false) {
                    Log.d(TAG,"AAA_onBackPressed isTwoArrayList not Same");
                    sendCommandToServerAlertDialog(unDetectList);
                } else {
                    Log.d(TAG,"AAA_onBackPressed isTwoArrayListSame");
                    super.onBackPressed();
                }
                return true;

            }case ACTIONBAR_MENU_ITEM_SAVE: {
                Log.d(TAG, "onOptionsItemSelected ACTIONBAR_MENU_ITEM_SAVE.");
                ArrayList<Integer> unDetectList = transformTo1024View();
                if (deviceId != null) {
//                    resultId = lib.modifyCloudCamConfigInfoForMotionArea(deviceId, unDetectList);
                }
                if (resultId <= 0) {
                    connectFailed();
                } else {
                    progressDialogShow();
                }

                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void findViews()  {

        vg_snapshotContainer= (ConstraintLayout) findViewById(R.id.vg_snapshot_container);
        vg_revert_motion_area = (RelativeLayout) findViewById(R.id.vg_revert_motion_area);

        iv_snapShot = (ImageView) findViewById(R.id.iv_snap_shot);

        btn_all_unselect = (ImageButton) findViewById(R.id.btn_all_unselect);
        btn_all_select = (ImageButton) findViewById(R.id.btn_all_select);
        vg_revert_motion_aera = (RelativeLayout) findViewById(R.id.vg_revert_motion_area);

        //show dialog
        if(isNetworkAvailable(MotionAreaActivity.this)==true)
            progressDialogShow();

        for(int index = 0; index < PRIVATE_MASK_256; index++) {
            paintLists.add(RectMaskView.PaintStatus.SELECT);
            Log.d(TAG,"paintLists : "+index+" "+paintLists.get(index));
        }

        if(!TextUtils.isEmpty(snapShotUrl)){

        }else{
            Log.e(TAG,"AAA_snapShotUrl is empty");
        }
        //建立一個AsyncTask執行緒進行圖片讀取動作，並帶入圖片連結網址路徑
        new AsyncTask<String, Void, Bitmap>()
        {
            @Override
            protected Bitmap doInBackground(String... params)
            {
                return getBitmapFromURL(snapShotUrl);
            }
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            protected void onPostExecute(Bitmap bitmap)
            {
                if(bitmap!=null){
                    iv_snapShot.setImageBitmap (bitmap);
                    iv_snapShot.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    Log.d(TAG,"AAA_Bitmap h: "+bitmap.getHeight()+" w: "+bitmap.getWidth());
                    Log.d(TAG,"AAA_iv_snapShot h: "+iv_snapShot.getHeight()+" w: "+iv_snapShot.getWidth());
                    bitMapHigh = bitmap.getHeight();
                    bitMapWidth = bitmap.getWidth();



                    int pWidth = vg_snapshotContainer.getWidth();
                    int pHeight = vg_snapshotContainer.getHeight();


                    if(bitMapHigh!=0 && bitMapWidth!=0){
                        float imageHeightRatio = ((float)pHeight/(float)bitMapHigh);
                        float imageWidthRatio = ((float)pWidth/(float)bitMapWidth);

                        float ratio = Math.min(imageHeightRatio,imageWidthRatio);

                        imageScaledWidth = ratio * bitMapWidth;
                        imageScaledHeight = ratio * bitMapHigh;


                        calculateRemoveImageHeight = pHeight - imageScaledHeight;
                        calculateRemoveImageWidth = pWidth - imageScaledWidth;
                        Log.d(TAG,"AAA_calculateRemoveImageHeight :"+calculateRemoveImageHeight + ",calculateRemoveImageWidth:"+calculateRemoveImageWidth);
                        Log.d(TAG,"AAA_imageScaledWidth :"+imageScaledWidth + ",imageScaledHeight:"+imageScaledHeight);
                    }


                    if(imageScaledWidth!=0 && rectMaskView==null) {
                        rectMaskView = new RectMaskView(MotionAreaActivity.this);
                        rectMaskView.setPaintColor(Color.RED, Color.YELLOW, Color.BLUE);


                        ConstraintLayout.LayoutParams par = new ConstraintLayout.LayoutParams((int) imageScaledWidth, (int) imageScaledHeight);
                        vg_snapshotContainer.addView(rectMaskView, par);
                    }

                    if(rectMaskView!=null){

                        float diffW = (imageScaledWidth/PRIVATE_MASK_ROW_16 - Math.round(imageScaledWidth / PRIVATE_MASK_ROW_16) ) *PRIVATE_MASK_ROW_16;
                        float diffH = (imageScaledHeight/PRIVATE_MASK_ROW_16 - Math.round(imageScaledHeight / PRIVATE_MASK_ROW_16) ) *PRIVATE_MASK_ROW_16;
//                        float diffW = Math.round(imageScaledWidth / PRIVATE_MASK_ROW_16) - imageScaledWidth/PRIVATE_MASK_ROW_16) *PRIVATE_MASK_ROW_16;
//                        float diffH = (Math.round(imageScaledHeight / PRIVATE_MASK_ROW_16)- imageScaledHeight/PRIVATE_MASK_ROW_16 ) *PRIVATE_MASK_ROW_16;

                        int paddingLeft = Math.round(calculateRemoveImageWidth/2 +(Math.abs(diffW)/2));
                        int paddingTop = Math.round(calculateRemoveImageHeight/2 +(Math.abs(diffH)/2));
                        Log.d(TAG,"ABS W : "+ Math.abs(diffW)/2+" H : "+ Math.abs(diffH)/2+" paddingLeft "+paddingLeft +" paddingTop "+paddingTop);
                        float paddingLeftFloat = calculateRemoveImageWidth/2 +(Math.abs(diffW)/2);
                        float paddingTopFloat = calculateRemoveImageHeight/2 +(Math.abs(diffH)/2);
                        Log.d(TAG,"ABS PADDING LEFT FLOAT "+ paddingLeftFloat +" PADDING RIGHT "+paddingTopFloat);

                        if(rectMaskView.getLayoutParams() instanceof ViewGroup.LayoutParams){
//                            LinearLayout.LayoutParams p = (LinearLayout.LayoutParams) rectMaskView.getLayoutParams();
//                            ConstraintLayout.LayoutParams  layoutParams = new ConstraintLayout.LayoutParams((int)imageScaledWidth, (int)imageScaledHeight);
//                            p.setMargins(paddingLeft,paddingTop, 0, 0);
//                            layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT, RelativeLayout.TRUE);
//                            rectMaskView.setLayoutParams(layoutParams);

                            ConstraintSet set = new ConstraintSet();
                            set.clone(vg_snapshotContainer);
//                            vg_snapshotContainer.addView(rectMaskView);
                            set.connect(rectMaskView.getId(), ConstraintSet.BOTTOM,iv_snapShot.getId(), ConstraintSet.BOTTOM,0);
                            set.connect(rectMaskView.getId(), ConstraintSet.RIGHT,iv_snapShot.getId(), ConstraintSet.RIGHT,0);
                            set.connect(rectMaskView.getId(), ConstraintSet.LEFT,iv_snapShot.getId(), ConstraintSet.LEFT,0);
                            set.connect(rectMaskView.getId(), ConstraintSet.TOP,iv_snapShot.getId(), ConstraintSet.TOP,0);
                            set.constrainHeight(rectMaskView.getId(), (int)imageScaledHeight);
                            set.constrainWidth(rectMaskView.getId(), (int)imageScaledWidth);
                            set.applyTo(vg_snapshotContainer);

//                            rectMaskView.setLayoutParams(p);
                            rectMaskView.requestLayout();
                        }

                        //第一次畫格子
                        paintLists.clear();
                        paintLists =  transformTo512View();
                        rectMaskView.onDrawRect(paintLists);
                        progressDialogCancel();

                        //set Listener
                        rectMaskView.setOnTouchListener(new View.OnTouchListener() {
                            ArrayList<RectMaskView.PaintStatus> selectPainListBackUp = new ArrayList<RectMaskView.PaintStatus>();
                            private static final int MIN_CLICK_DURATION = 100;
                            private long startClickTime;
                            private boolean longClickActive = false;
                            private ArrayList<Position> selectPositionList = new ArrayList<Position>();
                            private ArrayList<Integer> triggerSelectResultForActionUpDrawRedList = new ArrayList<Integer>();
                            private ArrayList<Integer> actionMoveRectList = new ArrayList<Integer>();

                            String privateMaskState = "";
                            @Override
                            public boolean onTouch(View view, MotionEvent motionEvent) {
                                int getXSelect = (int)motionEvent.getX();
                                int getYSelect = (int)motionEvent.getY();

                                switch (motionEvent.getAction()) {
                                    case MotionEvent.ACTION_MOVE: {
                                        Log.d(TAG, "aaa_ACTION_MOVE getX: " + getXSelect + " getY: " + getYSelect);

                                        if (longClickActive == true) {
                                            //LongClick Mode
                                            long clickDuration = Calendar.getInstance().getTimeInMillis() - startClickTime;
                                            if (clickDuration >= MIN_CLICK_DURATION) {
                                                Log.d(TAG, "aaa_ACTION_MOVE LongClick ");

                                                boolean isGet256View  =true;
                                                Position selectPosition = calculateWhichView(getXSelect,getYSelect, calculateRemoveImageWidth,isGet256View);


                                                selectPositionList.add(selectPosition);//add to List
                                                int xValue = selectPosition.xSelectPosition;
                                                int yValue = selectPosition.ySelectPosition;


                                                int triggerTarget = yValue*PRIVATE_MASK_ROW_16+xValue;


                                                Log.d(TAG,"triggerTarget " +triggerTarget);
                                                //經過的點都塗粉紅色
                                                if(paintLists!=null && triggerTarget>=0 &&triggerTarget<PRIVATE_MASK_256){
                                                    //把選到格子換掉
                                                    paintTriggerLists = new ArrayList<RectMaskView.PaintStatus>(paintLists);//copy to TriggerList
                                                    paintTriggerLists.set(triggerTarget,RectMaskView.PaintStatus.TRIGGER);
                                                    if(paintTriggerLists!=null)
                                                        rectMaskView.onDrawRect(paintTriggerLists);
                                                }


//                                            //find which part will draw mask.
                                                boolean isReturnNumber256View = true;//16*16
                                                ArrayList<Integer> selectPartList = findWhichPartYouSelectMakeASquare(selectPositionList,isReturnNumber256View);
                                                triggerSelectResultForActionUpDrawRedList=selectPartList;

                                                if(paintTriggerLists!=null){

                                                    //塗粉紅色的框框
                                                    int selectPart = 0;
                                                    for(int select=0;select<selectPartList.size();select++){

                                                        selectPart = selectPartList.get(select);
                                                        if(selectPart>0 && selectPart<PRIVATE_MASK_256){
//                                                            Log.d(TAG,"ACTION_UP x: " + selectPart);//0base

                                                            //把選到格子換掉
                                                            if(privateMaskState==ADD_PRIVATE_MASK){
                                                                paintTriggerLists.set(selectPart,RectMaskView.PaintStatus.SELECT);
                                                            }else{
                                                                paintTriggerLists.set(selectPart,RectMaskView.PaintStatus.TRIGGER);
                                                            }


                                                        }
                                                    }
//                                                    paintTriggerLastTimeLists = paintTriggerLists;//
                                                    rectMaskView.onDrawRect(paintTriggerLists);
                                                }
//
                                                if(actionMoveRectList !=null && actionMoveRectList.size()>=0){

                                                        //isTwoArrayListSame=true,兩個一樣
                                                    if(!isTwoArrayListSame(actionMoveRectList,selectPartList)){
                                                        Log.d(TAG,"AAA_isTwoArrayListSame not same ");
                                                        ArrayList<RectMaskView.PaintStatus> resultList =
                                                                filterTheMaskWhichIsSelect(triggerSelectResultForActionUpDrawRedList
                                                                        ,privateMaskState, actionMoveRectList,paintTriggerLists);


                                                        actionMoveRectList = selectPartList;//refresh selected List every Time , after color change.
                                                    }
                                                }
//
                                            }
                                        }
                                        return true;
                                    }
                                    case MotionEvent.ACTION_DOWN: {
                                        //single touch
                                        Log.d(TAG, "aaa_ACTION_DOWN getX: " + getXSelect + " getY: " + getYSelect);
                                        if (longClickActive == false) {
                                            longClickActive = true;
                                            startClickTime = Calendar.getInstance().getTimeInMillis();

                                            boolean isGet256view =true;
                                            Position selectPosition = calculateWhichView(getXSelect,getYSelect, calculateRemoveImageWidth,isGet256view);
                                            int xValue = selectPosition.xSelectPosition;
                                            int yValue = selectPosition.ySelectPosition;
                                            int numPosition = yValue*PRIVATE_MASK_ROW_16+xValue;

//                                             Log.d(TAG,"paintLists : "+paintLists.size());
                                            if(paintLists!=null && numPosition>=0 && numPosition<PRIVATE_MASK_256 && paintLists.size()==PRIVATE_MASK_256){
                                                privateMaskState = recognizeMode(numPosition);

                                                Log.d(TAG,"paintLists : "+numPosition);

                                                if(privateMaskState.equals(DELETE_PRIVATE_MASK))
                                                    paintLists.set(numPosition,RectMaskView.PaintStatus.UNSELECT);
                                                else if(privateMaskState.equals(ADD_PRIVATE_MASK))
                                                    paintLists.set(numPosition,RectMaskView.PaintStatus.SELECT);

//
                                                rectMaskView.onDrawRect(paintLists);
                                            }

                                        }
                                        return true;

                                    }
                                    case MotionEvent.ACTION_UP:{
                                        Log.d(TAG,"aaa_ACTION_UP getX: " + getXSelect  + " getY: " + getYSelect);
                                        if(longClickActive){
                                            longClickActive = false;
                                            if(triggerSelectResultForActionUpDrawRedList!=null
                                                    && triggerSelectResultForActionUpDrawRedList.size()>0 && paintLists!=null){
                                                for(int i=0; i<triggerSelectResultForActionUpDrawRedList.size(); i++){
                                                    int triggerTarget = triggerSelectResultForActionUpDrawRedList.get(i);
                                                    if(triggerTarget>=0&&triggerTarget<PRIVATE_MASK_256){
                                                        if(privateMaskState.equals(DELETE_PRIVATE_MASK)){
                                                            paintLists.set(triggerTarget,RectMaskView.PaintStatus.UNSELECT);
                                                        }else{
                                                            paintLists.set(triggerTarget,RectMaskView.PaintStatus.SELECT);
                                                        }
                                                    }

                                                }
                                                    rectMaskView.onDrawRect(paintLists);

                                            }

                                        }
                                        //clear all List which you select !
                                        selectPositionList.clear();
                                        triggerSelectResultForActionUpDrawRedList.clear();
                                        actionMoveRectList.clear();//clear every time
                                        paintTriggerLists.clear();

                                    }
                                    return true;
                                }

                                return true;

                            }
                        });


                    }

                }else{
                    Log.d(TAG,"AAA_cant get bitmap!");
                    progressDialogCancel();
                    cantGetSnapShotUrlAlertDialog();
                }

                    super.onPostExecute(bitmap);
            }
        }.execute(snapShotUrl);

    }

    //轉換成32*32的index
    private ArrayList<Integer> transformTo1024View(){
        Set<Integer> selectResultList = new HashSet<Integer>();
        if(paintLists!=null){
            for(int i=0;i<paintLists.size();i++){
                RectMaskView.PaintStatus select = paintLists.get(i);
                if(RectMaskView.PaintStatus.UNSELECT.equals(select)){

                    //PRIVATE_MASK_256 position
                    int x = i%PRIVATE_MASK_ROW_16;
                    int y = i/PRIVATE_MASK_ROW_16;

                    //PRIVATE_MASK_1024 左上(0,0)
                    int xx1 = 2*x;
                    int yy1 = 2*y;
                    int resultNumberLT = PRIVATE_MASK_ROW_32*yy1+xx1;
                    selectResultList.add(resultNumberLT);

                    //PRIVATE_MASK_1024 右上(0,1)
                    int xx2 = 2*x+1;
                    int yy2 = 2*y;
                    int resultNumberRT = PRIVATE_MASK_ROW_32*yy2+xx2;
                    selectResultList.add(resultNumberRT);

                    //PRIVATE_MASK_1024 左下(1,0)
                    int xx3 = 2*x;
                    int yy3 = 2*y+1;
                    int resultNumberLD = PRIVATE_MASK_ROW_32*yy3+xx3;
                    selectResultList.add(resultNumberLD);

                    //PRIVATE_MASK_1024 右下(1,1)
                    int xx4 = 2*x+1;
                    int yy4 = 2*y+1;
                    int resultNumberRD = PRIVATE_MASK_ROW_32*yy4+xx4;
                    selectResultList.add(resultNumberRD);
                }
            }
        }
        ArrayList<Integer> resultList = new ArrayList<Integer>(selectResultList);
        return resultList;
    }


    private ArrayList<RectMaskView.PaintStatus> transformTo512View(){
        ArrayList<RectMaskView.PaintStatus> selectPainList = new ArrayList<RectMaskView.PaintStatus>();
        ArrayList<Integer> unDetechList  =new ArrayList<Integer>();
        Set<Integer> selectResultList = new HashSet<Integer>();
        int resultUnDetectViewIndex = 0;
        //select all
        for(int index = 0; index < PRIVATE_MASK_256; index++) {
            selectPainList.add(RectMaskView.PaintStatus.SELECT);
        }

            if(selectPainList!=null)
            {
                //draw mask from server
                if(motionAreaUnDetectList!=null) {

                    for(int i=0; i<motionAreaUnDetectList.size();i++){

                        int index  = motionAreaUnDetectList.get(i);
                        Log.d(TAG,""+index);
                        Log.d(TAG,"AAA_motionAreaUnDetectList ："+index);
                        if(index>=0 && index<=1023){


                            //1024`s position
                            int row = index%PRIVATE_MASK_ROW_32;
                            int column = index/PRIVATE_MASK_ROW_32;
                            int resultRow = -1;
                            int resultColumn =-1;

//                            Log.d(TAG,"AAA_rowIndex ："+row+" columnIndex : "+column);
                            if(row>=0 && row<=31 && column>=0 && column<=31){
                                /**
                                 * 1024轉256座標校正:(x,y)校正成每四格中的第一格「例如：(1,1)->(0,0)」
                                 * */
                                //x,y分別減掉自己的Mode,會退回左上角
                                int modX = row%2;
                                int modY = column%2;
                                //左上角的(x,y)
                                resultRow = row-modX;
                                resultColumn = column-modY;
                            }
                            //PRIVATE_MASK_1024 左上
                            int xxRD = resultRow;
                            int yyRD = resultColumn;

                            //convert to 16*16 position,16*16的左上角
                            int x1 = xxRD/2;
                            int y1 = yyRD/2;
                            resultUnDetectViewIndex = y1*PRIVATE_MASK_ROW_16+x1;
                            selectResultList.add(resultUnDetectViewIndex);
//                            Log.d(TAG,"AAA_PRIVATE_MASK_ROW_16 resultNumber ："+resultUnDetectViewIndex);
                        }

                    }
                    unDetechList = new ArrayList<Integer>(selectResultList);
                    for(int unDetect=0; unDetect< unDetechList.size(); unDetect++) {
                        int unDetectIndex = unDetechList.get(unDetect);
                        selectPainList.set(unDetectIndex, RectMaskView.PaintStatus.UNSELECT);
//                        Log.d(TAG,"AAA_unDetect Index ："+unDetect);
                    }
                }
        }

        return selectPainList;
    }


    private Boolean isTwoArrayListSame(ArrayList<Integer> selectPinkLastTimeList , ArrayList<Integer> selectPinkNowList){
        boolean isSame = false;

        if(selectPinkLastTimeList.size()==selectPinkNowList.size()){
            isSame = true;
        }else{
            Collection<Integer> before = new ArrayList(selectPinkLastTimeList);
            Collection<Integer> after = new ArrayList(selectPinkNowList);

            List<Integer> beforeList = new ArrayList<Integer>(before);
            List<Integer> afterList = new ArrayList<Integer>(after);

            boolean a = beforeList.containsAll(afterList);
            boolean b = afterList.containsAll(beforeList);

            if(a==b)
                isSame = true;
            else
                isSame = false;
        }
//        Log.d(TAG,"AAA_resultList : "+isSame);

     return isSame;
    }

    //讀取網路圖片，型態為Bitmap
    private static Bitmap getBitmapFromURL(String imageUrl)
    {
        try
        {
            URL url = new URL(imageUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            Bitmap bitmap = BitmapFactory.decodeStream(input);
            return bitmap;
        }
        catch (IOException e)
        {
            e.printStackTrace();
            return null;
        }
    }

    //變成相對應的格子１６＊１６
    private Position calculateWhichView(int x , int y ,float calculateRemoveImageWidth , boolean isReturnNumber256View ){

        Position position =new Position();

        int numOfCol = (isReturnNumber256View)?PRIVATE_MASK_ROW_16:32;
        int numOfRow = (isReturnNumber256View)?PRIVATE_MASK_ROW_16:32;

        DisplayMetrics displaymetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        int screenHeight = rectMaskView.getHeight();//Pad 1536
        int screenWidth = rectMaskView.getWidth(); //Pad 2048
        //模擬１６＊１６
//        Log.d(TAG,"AAA_now Height :"+screenHeight+" Width : "+screenWidth);

        int everyViewWidth = (screenWidth- Math.round(calculateRemoveImageWidth)) / numOfCol;//KeepRatio float to intMath.round()
        //0~1w 第一格
        int everyViewHeight = screenHeight/numOfRow; //y + toolbar height
        //0~1h 第一格


//        Log.d(TAG,"AAA_now everyViewWidth :"+everyViewWidth);
//        Log.d(TAG,"AAA_now x :"+x/everyViewWidth+" y : "+y/everyViewHeight);

        x = x-(Math.round(calculateRemoveImageWidth)/2);
//        Log.d(TAG,"AAA_x 校正後 ： "+x);
        position.xSelectPosition = x/everyViewWidth;
        position.ySelectPosition = y/everyViewHeight;
//        Log.d(TAG,"AAA_xSelectPosition1 :"+position.xSelectPosition+" ySelectPosition : "+position.ySelectPosition);

        /**
         * 20170802 Peter 這邊為了防止畫超出最右邊的15格,強制把它設為最大值,最小值反之
         * **/
        if(position.xSelectPosition>=PRIVATE_MASK_ROW_16 ){
//            Log.d(TAG,"AAA_xSelectPosition2 :"+position.xSelectPosition+" ySelectPosition : "+position.ySelectPosition+" x "+x +" everyViewWidth "+everyViewWidth);
            position.xSelectPosition = PRIVATE_MASK_ROW_16-1;
        }else if(position.xSelectPosition<0){
            position.xSelectPosition = 0;
        }

        if( position.ySelectPosition>=PRIVATE_MASK_ROW_16){
            position.ySelectPosition = PRIVATE_MASK_ROW_16-1;
        }else if(position.ySelectPosition<0){
            position.ySelectPosition = 0;
        }
        return position;
    }

    private void setListener() {
        //Draw Every GridView`s Background
        vg_revert_motion_area.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void onClick(View view) {
                resetView();
            }
        });

        btn_all_unselect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Log.d(TAG,"btn_all_unselect onClick.");
                clearAllSelectView();

            }
        });

        btn_all_select.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                Log.d(TAG,"btn_all_select onClick.");
                selectAllView();

            }
        });
    }

    @Override
    public void onBackPressed() {

        ArrayList<Integer> unDetectList = transformTo1024View();
        if(isTwoArrayListSame(unDetectList,motionAreaUnDetectList)==false) {
            Log.d(TAG,"AAA_onBackPressed isTwoArrayList not Same");
            sendCommandToServerAlertDialog(unDetectList);
        } else {
            Log.d(TAG,"AAA_onBackPressed isTwoArrayListSame");
            super.onBackPressed();
        }
    }

    private String recognizeMode(int numPosition){
        String mode="";
        RectMaskView.PaintStatus select = paintLists.get(numPosition);

        if(RectMaskView.PaintStatus.SELECT.equals(select)){
            mode=DELETE_PRIVATE_MASK;
        }else if(RectMaskView.PaintStatus.UNSELECT.equals(select)){
            mode=ADD_PRIVATE_MASK;
        }
       return mode;
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    private ArrayList<RectMaskView.PaintStatus> filterTheMaskWhichIsSelect(ArrayList<Integer> triggerSelectResultForActionUpDrawRedList
            , String privateMaskState, ArrayList<Integer> pinkBeforeList, ArrayList<RectMaskView.PaintStatus> paintTriggerLists ){

        ArrayList<RectMaskView.PaintStatus> ResultList = new ArrayList<RectMaskView.PaintStatus>();

        Set<Integer> resultUnionPink = new HashSet<Integer>();//連集
        Set<Integer> resultDifferencePink = new HashSet<Integer>();//差集

        Set<Integer> pinkBeforeSet =new HashSet<Integer>();
        Set<Integer> pinkThisTimeSet =new HashSet<Integer>();
        Set<Integer> redPartSet = new HashSet<Integer>();


        //以上一次結束的當底圖
        if(paintLists!=null){
            //getRed
            for(int i=0;i<paintLists.size();i++){
                RectMaskView.PaintStatus paintStatus = paintLists.get(i);
                if(RectMaskView.PaintStatus.SELECT.equals(paintStatus)){
                    redPartSet.add(i);
//                    Log.d(TAG,"AAA_選取的 redPartSet "+i);
                }
            }
        }

       //這次粉紅色的區域
        if(triggerSelectResultForActionUpDrawRedList!=null){
            for(int j=0; j<triggerSelectResultForActionUpDrawRedList.size();j++){
                pinkThisTimeSet.add(triggerSelectResultForActionUpDrawRedList.get(j));
//                Log.d(TAG,"AAA_選取的 paintTriggerLists "+j);
            }
        }

        //取上一次粉紅色的部分
        if(pinkBeforeList!=null){
            for(int k=0; k<pinkBeforeList.size();k++ ){
                pinkBeforeSet.add(pinkBeforeList.get(k));
//                Log.d(TAG,"AAA_選取的 pinkBeforeList "+pinkBeforeList.get(k));
            }
        }

        //對AB取連集
        resultUnionPink.clear();
        resultUnionPink.addAll(pinkBeforeSet);
        resultUnionPink.addAll(pinkThisTimeSet);

        //對AB連集 與 A 取差集
        resultDifferencePink.clear();
        resultDifferencePink.addAll(resultUnionPink);
        resultDifferencePink.removeAll(pinkThisTimeSet);

        //交集
        Set<Integer> redAndPinkInterSect =new HashSet<Integer>();
        redAndPinkInterSect.clear();
        redAndPinkInterSect.addAll(redPartSet);
        redAndPinkInterSect.retainAll(resultDifferencePink);

        //HashSet to ArrayList
//        ArrayList<Integer> selectRangChangeBackToRedList = new ArrayList<Integer>(redAndPinkInterSect);

        //HashSet to ArrayList
        ArrayList<Integer> selectRangChangeToNormalList = new ArrayList<Integer>(resultDifferencePink);

        //HashSet to ArrayList
        ArrayList<Integer> lastTimeRedPartList = new ArrayList<Integer>(redPartSet);

//        if(privateMaskState.equals(ADD_PRIVATE_MASK)) {

            if(selectRangChangeToNormalList!=null){
                for(int drawStart=0;drawStart<selectRangChangeToNormalList.size();drawStart++){
                    int drawEmptyPart = selectRangChangeToNormalList.get(drawStart);
                    if(drawEmptyPart>0 && drawEmptyPart<PRIVATE_MASK_256){

                        Log.d(TAG,"AAA_原本紅色的格子 : +"+"  "+drawEmptyPart+" "+paintLists.get(drawEmptyPart));
                        //如果要變色的,剛好是原本紅色的格子,則保留
                        for(int redStart=0;redStart<lastTimeRedPartList.size();redStart++){
                            int redNum = lastTimeRedPartList.get(redStart);//每一個紅色的格子
                            if(redNum==drawEmptyPart){
                                paintTriggerLists.set(drawEmptyPart,RectMaskView.PaintStatus.SELECT);
                            }
                        }

                        for(int triggerStart=0; triggerStart<paintTriggerLists.size();triggerStart++){
                            if(paintTriggerLists.get(triggerStart).equals(RectMaskView.PaintStatus.SELECT)){

                            }else{
                                paintTriggerLists.set(drawEmptyPart,RectMaskView.PaintStatus.UNSELECT);
                                Log.d(TAG,"AAA_要塗回來 : "+drawEmptyPart);
                            }
                        }
                    }
                }
            }
//        }

        //畫回空的顏色////////////////////////////////
        if(selectRangChangeToNormalList!=null){
            if(paintTriggerLists!=null)
                rectMaskView.onDrawRect(paintTriggerLists);
        }

        return ResultList;
    }



    private ArrayList<Integer> findWhichPartYouSelectMakeASquare(ArrayList<Position> selectPositionList , Boolean isReturnNumber256View){
        ArrayList<Integer> drawRangeList = new ArrayList<Integer>();
        int xStart = 0;
        int yStart = 0;
        int xEnd = 0;
        int yEnd = 0;
        int viewRange = (isReturnNumber256View)?PRIVATE_MASK_ROW_16:32;//16*16 or 32*32
//        Log.d(TAG,"AAA_now is calling "+viewRange+"*"+viewRange);

        if(selectPositionList!=null){
            for(int i=0; i<selectPositionList.size();i++){
                xStart = selectPositionList.get(0).xSelectPosition;
                yStart = selectPositionList.get(0).ySelectPosition;
                xEnd = selectPositionList.get(i).xSelectPosition;
                yEnd = selectPositionList.get(i).ySelectPosition;
            }
//            if(xEnd>15||yEnd>15){
//                xEnd=15;
//                yEnd=15;
//            }

            //計算要塗滿的格子有哪些
            if(xStart==xEnd){
                //手勢：↓
                if(yStart<yEnd){
                    for(int y=yStart; y<=yEnd; y++){
                        drawRangeList.add(y*viewRange+xStart);//x->0base
//                        Log.d(TAG,"AAA_xStart : "+xStart+" xEnd :"+xEnd);
                    }
                }else {
                    //手勢：↑
                    for(int y=yEnd; y<=yStart; y++){
                        drawRangeList.add(y*viewRange+xStart);//x->0base
//                        Log.d(TAG,"AAA_xStart : "+xStart+" xEnd :"+xEnd);
                    }
                }

            }else if(yStart == yEnd){

                if(xStart<xEnd){
                    //手勢：→
                    for(int x=xStart; x<=xEnd; x++){
                        drawRangeList.add(yStart*viewRange+x);
                        Log.d(TAG,"AAA_xStart : "+xStart+" xEnd :"+xEnd);
                    }
                }else{
                    for(int x=xEnd; x<=xStart; x++){
                        drawRangeList.add(yStart*viewRange+x);
//                        Log.d(TAG,"AAA_xEnd :"+xEnd+" xStart : "+xStart);
                    }
                }

            }else{

                if(xEnd-xStart>0 && yEnd-yStart>0){
                    //手勢：↘ ok (+,+)
                    for(int x=xStart; x<=xEnd; x++){
                        for(int y=yStart; y<=yEnd; y++){
                            drawRangeList.add(y*viewRange+x);
                        }
                    }
                }else if(xEnd-xStart>0 && yEnd-yStart<0){
                    //手勢：↗ (-,+)
                    for(int x=xStart; x<=xEnd; x++){
                        for(int y=yEnd; y<=yStart; y++){
                            drawRangeList.add(y*viewRange+x);
                        }
                    }

                }else if(xEnd-xStart<0 && yEnd-yStart<0 ){
                    //手勢：↖ (-,-)
                    for(int x=xEnd; x<=xStart; x++){
                        for(int y=yEnd; y<=yStart; y++){
                            drawRangeList.add(y*viewRange+x);
                        }
                    }
                }else if(xEnd-xStart<0 && yEnd-yStart>0 ){
                    //手勢：↙ (+,-)
                    for(int x=xEnd; x<=xStart; x++){
                        for(int y=yStart; y<=yEnd; y++){
                            drawRangeList.add(y*viewRange+x);
                        }
                    }
                }

            }

        }

        return drawRangeList;
    }


    public class Position{
        public int xSelectPosition;
        public int ySelectPosition;
    }



    //select all
    private void selectAllView(){

        if(paintLists!=null){
            paintLists.clear();

            for(int index = 0; index < PRIVATE_MASK_256; index++) {
                paintLists.add(RectMaskView.PaintStatus.SELECT);
            }
        }
        rectMaskView.onDrawRect(paintLists);
    }

    private void clearAllSelectView(){
        if(paintLists!=null){
            paintLists.clear();

            for(int index = 0; index < PRIVATE_MASK_256; index++) {
                paintLists.add(RectMaskView.PaintStatus.UNSELECT);
            }
        }
        rectMaskView.onDrawRect(paintLists);
    }

    private void resetView(){
        //第一次畫格子
        paintLists.clear();
        paintLists =  transformTo512View();
        rectMaskView.onDrawRect(paintLists);
    }

    private void progressDialogCancel() {
        if (progressDialog != null) {
            progressDialog.cancel();
            progressDialog = null;
        }
        //isnt running- do something here
    }

    private void progressDialogShow() {
        if(progressDialog==null)
            progressDialog = ProgressDialog.show(MotionAreaActivity.this, "", getString(R.string.dialog_please_wait), true);
    }


    private void sendCommandToServerAlertDialog(final ArrayList<Integer> unDetectList) {
        new AlertDialog.Builder(MotionAreaActivity.this)
                .setTitle(R.string.schedule_update_to_server)
                .setMessage(R.string.motion_area_update_to_server)
                .setPositiveButton(R.string.motion_area_update, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
//                        resultId = lib.modifyCloudCamConfigInfoForMotionArea(deviceId, unDetectList);

                        if(resultId <= 0) {
                            connectFailed();
                        } else {
                            progressDialogShow();
                        }
                    }
                })
                .setNegativeButton(R.string.g_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        progressDialogCancel();
                        finish();
                    }
                })
                .show();
    }

    private void undetectListEmptyAlertDialog() {
        new AlertDialog.Builder(MotionAreaActivity.this)
                .setTitle(R.string.dialog_connection_msg)
                .setMessage(R.string.dialog_please_try_again)
                .setPositiveButton(R.string.g_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }).show();
    }
    private void cantGetSnapShotUrlAlertDialog() {
        new AlertDialog.Builder(MotionAreaActivity.this)
                .setTitle(R.string.dialog_connection_msg)
                .setMessage(R.string.motion_area_cant_get_snap_shot_url)
                .setPositiveButton(R.string.g_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                }).show();
    }

    private void connectFailed() {
        progressDialogCancel();
        String toastStr = this.getString(R.string.dialog_connection_msg) + ". " + this.getString(R.string.dialog_please_try_again);
        Toast.makeText(MotionAreaActivity.this, toastStr, Toast.LENGTH_SHORT).show();
    }

    private boolean isNetworkAvailable(Context context) {
        ConnectivityManager connectivity =(ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (connectivity == null) {
            return false;
        } else {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null) {
                for (int i = 0; i < info.length; i++) {
                    if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

}

package com.sepehr.sa_sh.abfacounter01;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.Log;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuItem;

import com.rey.material.widget.Spinner;
import com.sepehr.sa_sh.abfacounter01.DatabaseRepository.CounterReportService;
import com.sepehr.sa_sh.abfacounter01.DatabaseRepository.CounterStateService;
import com.sepehr.sa_sh.abfacounter01.DatabaseRepository.IKarbariRepo;
import com.sepehr.sa_sh.abfacounter01.DatabaseRepository.IOnOffloadService;
import com.sepehr.sa_sh.abfacounter01.DatabaseRepository.KarbariRepo;
import com.sepehr.sa_sh.abfacounter01.DatabaseRepository.OnOffloadService;
import com.sepehr.sa_sh.abfacounter01.infrastructure.GeoTracker;
import com.sepehr.sa_sh.abfacounter01.infrastructure.IGeoTracker;
import com.sepehr.sa_sh.abfacounter01.infrastructure.IMediaPlayerManager;
import com.sepehr.sa_sh.abfacounter01.infrastructure.IToastAndAlertBuilder;
import com.sepehr.sa_sh.abfacounter01.infrastructure.MediaPlayerManager;
import com.sepehr.sa_sh.abfacounter01.infrastructure.SimpleErrorHandler;
import com.sepehr.sa_sh.abfacounter01.infrastructure.ToastAndAlertBuilder;
import com.sepehr.sa_sh.abfacounter01.models.UiElementInActivity;
import com.sepehr.sa_sh.abfacounter01.models.sqlLiteTables.CounterReportValueKeyModel;
import com.sepehr.sa_sh.abfacounter01.models.sqlLiteTables.CounterStateValueKeyModel;
import com.sepehr.sa_sh.abfacounter01.models.sqlLiteTables.OnOffLoadModel;

import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import  android.os.Vibrator;
import retrofit2.Call;
import retrofit2.Callback;

public class DisplayViewPager extends BaseActivity {
    Context appContext;
    IToastAndAlertBuilder toastAndAlertBuilder;
    IKarbariRepo karbariManager;
    String[] items;
    public static  String bill_id;
    public static BigDecimal currentTrackNumber;
    public static int currentPosition=0;
    public static Integer globalCurrentCounterNumberForFragment;
    public static  Integer globalCurrentCounterPositionForFragment;
    public static  Integer globalCurrentCounterStateCode;
    public static AverageState averageStateGlobal;
    ViewPager viewPager;
    TextView systemDate,countFraction;
    PagerAdapter adapter;
    Boolean LOCK_INPUT=false;
    boolean isAppbarLocked=false;
    public List<OnOffLoadModel> _list =new ArrayList<>();
    OnOffLoadModel onOffLoadModel;
    int viewPagerSize;
    String token;
    String persianDate;
    Typeface face;
    public boolean isNormalList=true;//if(alal hesab || unread) then false
    String persianDate_db_format;//registration date in mobile db
    //
    IMediaPlayerManager mediaPlayerManager;
    //
    CounterReportService reportManager;
    List<CounterReportValueKeyModel> selectedReports;
    //////
    IGeoTracker geoTracker;
    IOnOffloadService onOffloadService;
    //////

    @Override
    protected UiElementInActivity getUiElementsInActivity() {
        UiElementInActivity uiElementInActivity=new UiElementInActivity();
        uiElementInActivity.setContentViewId(R.layout.activity_display_view_pager);
        return uiElementInActivity;
    }

    @Override
    protected void initialize() {
        try {
            appContext = this;
            initilizeSomeUiElements();
            items = CounterStateService.getCounterStateTitles();
            isAppbarLocked= initializeViewpager();
            drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
            karbariManager=new KarbariRepo();
            mediaPlayerManager = new MediaPlayerManager(appContext);
            toastAndAlertBuilder = new ToastAndAlertBuilder(appContext);
            geoTracker = new GeoTracker("viewPager", appContext);
            if (geoTracker.checkPlayServices()) {
                // Building the GoogleApi client
                geoTracker.buildGoogleApiClient();
                geoTracker.createLocationRequest();
            }


        } catch (Exception e) {
            Log.e("error", e.getCause().toString());
            Log.e("error", e.getStackTrace().toString());
        }
    }

    private boolean initializeViewpager(){
        Intent intent = getIntent();
        Bundle bundle = intent.getExtras();
        onOffloadService =new OnOffloadService(this);
        int readingType=0;//normal
        if (bundle != null) {
            isNormalList = false;
            readingType = (int) bundle.get("s");
        }//bundle != null
        _list = onOffloadService.getReadingList(readingType);
        viewPager = (ViewPager) findViewById(R.id.pager);

        if (_list == null || _list.size() < 1) {
            return true;
        }

        //initialize before page scroll
        bill_id = _list.get(0).billId;
        currentTrackNumber=_list.get(0).trackNumber;
        reportManager=new CounterReportService(bill_id);
        selectedReports=reportManager.getCounterReadingSelectedReports(bill_id);
        //
        // Pass results to ViewPagerAdapter Class
        //List<OnOffLoadModel> _viewPagerList=OnOffLoadModel.listAll(OnOffLoadModel.class);

        adapter = new ViewPagerAdaptor(DisplayViewPager.this,
                _list, items,getSupportFragmentManager(),selectedReports);
        // Binds the Adapter to the ViewPager
        viewPager.setAdapter(adapter);
        viewPagerSize = _list.size();
        viewPagerChangePageListener();
        return false;
    }

    private void initilizeSomeUiElements(){
        systemDate = (TextView) findViewById(R.id.systemDate);
        persianDate = DateAndTime.getPersianTodayDate();
        persianDate_db_format = DateAndTime.getPersianDbFormattedDate();
        systemDate.setText(persianDate);
        countFraction = (TextView) findViewById(R.id.countFraction);
        face = Typeface.createFromAsset(getAssets(), "fonts/BZar.ttf");
        systemDate.setTypeface(face);
        countFraction.setTypeface(face);
    }

    private void viewPagerChangePageListener(){
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if (_list != null && _list.size() > 0) {
                    int currentPosition = viewPager.getCurrentItem();
                    countFraction.setText((currentPosition + 1) + "/" +viewPagerSize);
                    if (!LOCK_INPUT) {
                        EditText counterNumber = (EditText) viewPager.findViewWithTag("counterNumber" + position);
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.showSoftInput(counterNumber, InputMethodManager.SHOW_IMPLICIT);
                    }
                }
            }

            @Override
            public void onPageSelected(int position) {
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (_list != null && _list.size() > 0) {
                    currentPosition = viewPager.getCurrentItem();
                    try {
                        bill_id = ((TextView) viewPager
                                .findViewWithTag("billId" + currentPosition))
                                .getText().toString();
                        String trackNumString = ((TextView) viewPager
                                .findViewWithTag("trackNumber" + currentPosition))
                                .getText().toString();
                        currentTrackNumber = new BigDecimal(trackNumString);
                        Log.e("billId", bill_id);
                    }
                    catch (Exception e){
                        Log.e("on page scroll",e.getMessage());
                    }
                    try{
                    Integer karbariCode = _list.get(currentPosition).karbariCod;
                    boolean hasKarbariVibration = karbariManager.HasVibrate(karbariCode);
                    if (hasKarbariVibration) {
                        vibratePlease();
                    }
                    }
                    catch (Exception e){
                        Log.e("karbari",e.getMessage());
                    }
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            Snackbar.make(viewPager, "جهت باز کردن صفحه های جدید لطفا از نوار کنار استفاده فرمایید", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.display_view_pager, menu);

        menu.add(Menu.NONE,MenuItemId.MENU_ITEM_QEIRE_MOJAZ.getValue(), Menu.NONE, R.string.menu_item_qeire_mojaz)
                .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS)
                .setIcon(R.drawable.ic_block_white_48dp);

        menu.add(Menu.NONE,MenuItemId.MENU_ITEM_MORE_INFO.getValue(), Menu.NONE, R.string.menu_item_more_info)
        .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS)
        .setIcon(R.drawable.ic_featured_play_list_white_36dp);

        menu.add(Menu.NONE,MenuItemId.MENU_ITEM_LOCATION.getValue(), Menu.NONE, R.string.menu_item_location)
                .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_IF_ROOM)
                .setIcon(R.drawable.ic_place_white_36dp);

        menu.add(Menu.NONE,MenuItemId.MENU_ITEM_REPORT.getValue(), Menu.NONE, R.string.menu_item_report)
                .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS)
                .setIcon(R.drawable.ic_warning_white_36dp);

        menu.add(Menu.NONE,MenuItemId.MENU_ITEM_CAMERA.getValue(), Menu.NONE, R.string.menu_item_report)
                .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS)
                .setIcon(R.drawable.ic_linked_camera_white_36dp);

        menu.add(Menu.NONE,MenuItemId.MENU_ITEM_SEARCH.getValue(),Menu.NONE,R.string.menu_item_search)
                .setShowAsActionFlags(MenuItem.SHOW_AS_ACTION_ALWAYS)
                .setIcon(R.drawable.ic_search_white_36dp);

        menu.add(Menu.NONE, MenuItemId.MENUE_ITEM_DISPLAY_LAST_UNREAD.getValue(), Menu.NONE, R.string.menue_item_display_last_unread)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        menu.add(Menu.NONE, MenuItemId.MENU_ITEM_LOCK_INPUT.getValue(), Menu.NONE, R.string.menu_item_lock_input)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);

        menu.add(Menu.NONE,MenuItemId.MENUE_ITEM_CONTACT_US.getValue(),Menu.NONE,R.string.menue_item_contact_us)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        if(isAppbarLocked){
            toastAndAlertBuilder.makeSimpleAlert("داده ای برای نمایش وجود ندارد");
            return false;
        }
        int id = item.getItemId();
        FragmentManager fm = getSupportFragmentManager();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            MoshtarakMoreInfoFragment dialogFragment = new MoshtarakMoreInfoFragment ();
            dialogFragment.show(fm, "اطلاعات مشترک");
            return true;
        }
        if(id==MenuItemId.MENU_ITEM_LOCATION.getValue()){

        }
        if(id==MenuItemId.MENU_ITEM_MORE_INFO.getValue()){
            PossibleInfoFragment dialogFragment=new PossibleInfoFragment();
            dialogFragment.show(fm,"اطلاعات احتمالی مشترک");
        }
        if(id==MenuItemId.MENU_ITEM_REPORT.getValue()) {
            CounterReportFragment dialogFragment = new CounterReportFragment();
            dialogFragment.show(fm, "گزارش کنتور");
        }
        if(id==MenuItemId.MENU_ITEM_CAMERA.getValue()){
            CameraFragment dialogFragment=new CameraFragment();
            dialogFragment.show(fm,"ثبت تصاویر");
        }
        if(id==MenuItemId.MENU_ITEM_SEARCH.getValue()){
            SearchFragment dialogFragment=new SearchFragment();
            dialogFragment.show(fm,"جستجوی مشترک");
        }
        if(id==MenuItemId.MENU_ITEM_LOCK_INPUT.getValue()){
            LOCK_INPUT=!LOCK_INPUT;
        }
        if(id==MenuItemId.MENU_ITEM_QEIRE_MOJAZ.getValue()){
            QeireMojazFragment dialogFragment=new QeireMojazFragment();
            dialogFragment.show(fm, "غیر مجاز");
        }
        if(id==MenuItemId.MENUE_ITEM_CONTACT_US.getValue()){
            ContactUsFragment dialogFragment=new ContactUsFragment();
            dialogFragment.show(fm,"ارتباط با ما");
        }
        if(id==MenuItemId.MENUE_ITEM_DISPLAY_LAST_UNREAD.getValue()){
            displayLastUnread();
        }
        return super.onOptionsItemSelected(item);
    }
    //
    public void setViewPagerCurrentItem(int position){
        viewPager=(ViewPager) findViewById(R.id.pager);
        viewPager.setCurrentItem(position, true);
    }
    //
    public void displayPreNumber(View view){
        try {
        onOffLoadModel = OnOffLoadModel
                .find(OnOffLoadModel.class, "BILL_ID = ? ", bill_id)
                .get(0);
        onOffLoadModel.setIsCounterNumberShown(true);
        onOffLoadModel.save();
        _list.get(currentPosition).setIsCounterNumberShown(true);

        Button preNumberButton =
                (Button) viewPager.findViewWithTag("preNumber" + currentPosition);
        preNumberButton.setText(onOffLoadModel.getPreNumber().toString());
        }
        catch (Exception e){
            Log.e("displayPreNum", e.getMessage());
        }
    }
    //
    public void registerCounterNumber(View view){

        /* 4 halat e asli vojood darad:

         1. shomare kontor e feli khali
            1.1  shomare kontor e feli khali & mavane sabt nashode :
                error + playNotSavedSound +return
            1.2  shomare kontor khali & mavane sabt shode:
                sabt ba movafaqiat

         2. shomare qabli kamtar az feli:
            2.1  shomare feli kamtar az qabli & dor mojadad:
                sabt e gozaresh va shomare kontor
            2.2   shomare feli kamtr az qabli & gozareshi sabt nashode:
                pishnahad e sabt e gozaresh e dor mojadad va dar soorate taiid sabt

         3. High ,Low ,Masraf-sefr:
            sabt ba taiid

         4.shomare feli sahih va normal :
            sabte shomare + sabte code mane =0*/

        //check if gps is enabled
        try {
            final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            boolean isGpsEnabled = manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            Log.e("gps is:", isGpsEnabled + "");
            if (!isGpsEnabled) {
                Snackbar.make(viewPager, "لطفا از روشن بودن gps اطمینان حاصل فرمایید", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                return;
            }
            //
            Integer counterNumber;
            EditText counterNumberEditText = (EditText) viewPager.findViewWithTag("counterNumber" + currentPosition);
            Spinner counterStateSpinner = (Spinner) viewPager.findViewWithTag("counterState" + currentPosition);
            int spinnerSelectedItemPosition = counterStateSpinner.getSelectedItemPosition();
            String spinnerSelectedText=counterStateSpinner.getSelectedItem().toString();
            CounterStateService counterStateService =new
                    CounterStateService(spinnerSelectedText,selectedReports);
            String counterNumberString = counterNumberEditText.getText().toString();
            onOffLoadModel = OnOffLoadModel
                    .find(OnOffLoadModel.class, "BILL_ID = ? ", bill_id)
                    .get(0);
            onOffLoadModel.offLoadState = 1;//1= sabt shode vali ersal nashode

            //region ______________________________ 1 _____________________________
         //1. shomare kontore feli khali
            if (counterNumberString.equals("") || counterNumberString.equals("رقم کنتور")) {
                // 1.1 & mavane sabt nashode
                if (counterStateService.shouldIEnterNumber()) {
                   toastAndAlertBuilder.makeSimpleToast( "همکار گرامی لطفا شماره کنتور را وارد ، یا وضعیت کنتور را در حالت مانع قرار دهید");
                    playNotSaveSound();
                    return;
                }
                //
                //1.2 & mavane sabt shode
                else {
                    _list.get(currentPosition).counterStatePosition = spinnerSelectedItemPosition;//list
                    onOffLoadModel.counterStatePosition = spinnerSelectedItemPosition;//db
                    onOffLoadModel.counterStateCode= CounterStateService.getRealState(spinnerSelectedText);
                    doSaveProccess();
                    return;
                }
            }
            //endregion
            //
            // mavane sabt shode & raqam ham sabt shode
            if(!counterStateService.canEnterNumber() && !counterStateService.shouldIEnterNumber()
                    && TextUtils.isDigitsOnly(counterNumberString)){
                toastAndAlertBuilder.makeSimpleToast("در این حالت ورود رقم کنتور صحیح نمیباشد");
                return;
            }

            //moshtarak bein e saie e halat ha
            int preNumber = Integer.valueOf(onOffLoadModel.preNumber);
            int todayNumber = Integer.valueOf(counterNumberString);
            double todayAverage = CounterNumberHelper.CalculateAverage(preNumber, todayNumber, onOffLoadModel.preDate);
            double monthAverage = todayAverage * 30;
            AverageState averageState = CounterNumberHelper.DetermineAverageState(
                    Double.valueOf(onOffLoadModel.preAverage.toString()), monthAverage, new HighLow(25, 25)
                    , onOffLoadModel.tedadMaskooni.intValue());
            averageStateGlobal = averageState;
            boolean isRestarted;
            isRestarted = CounterNumberHelper.isRestarted(preNumber, todayNumber);
            //
            //region ______________________________ 2______________________________
         //2. shomare qabli kamtar az feli
            // 2.1 & dor mojadad
            if (preNumber > todayNumber) {
                if(counterStateService.canNumberBeLessThanPre()){
                    _list.get(currentPosition).counterStatePosition = spinnerSelectedItemPosition;//list
                    onOffLoadModel.counterStatePosition =spinnerSelectedItemPosition;//db
                    onOffLoadModel.counterStateCode= CounterStateService.getRealState(spinnerSelectedText);
                    _list.get(currentPosition).counterNumber =new Integer(counterNumberString);//list
                    onOffLoadModel.counterNumber = new Integer(counterNumberString);
                    doSaveProccess();
                    return;
                }
                //2.2 & error !
                else {
                  toastAndAlertBuilder.makeSimpleToast("لطفا شماره کنتور را بررسی کرده و دوباره امتحان فرمایید");
                    return;
                }
            }//todayNumber > preNumber
            //endregion
            //
            //region ______________________________ 3______________________________
         //High , Low ,Masraf-sefr
            if (averageState == AverageState.HIGH ||
                    averageState == AverageState.LOW ||
                    averageState == AverageState.MASRAF_SEFR) {
                vibratePlease();
                //prepare to for registration from fragment
                counterNumber = new Integer(counterNumberEditText.getText().toString());
                globalCurrentCounterNumberForFragment = counterNumber;
                globalCurrentCounterPositionForFragment = new Integer(spinnerSelectedItemPosition);
                globalCurrentCounterStateCode=CounterStateService.getRealState(spinnerSelectedText);
                FragmentManager fm = getSupportFragmentManager();
                RegisterAnywayFragment dialogFragment = new RegisterAnywayFragment();
                dialogFragment.show(fm, "مصرف نامتعارف");
                return;
            }
            //endregion
            //
            //region ______________________________ 4______________________________
        //sabt e mamooli
            _list.get(currentPosition).counterStatePosition = spinnerSelectedItemPosition;//list
            onOffLoadModel.counterStatePosition = spinnerSelectedItemPosition;//db
            onOffLoadModel.counterStateCode= CounterStateService.getRealState(spinnerSelectedText);
            _list.get(currentPosition).counterNumber = new Integer(counterNumberString);//list
            onOffLoadModel.counterNumber = new Integer(counterNumberString);
            doSaveProccess();
            return;
            //endregion
        }
        catch (Exception e){
          toastAndAlertBuilder.makeSimpleToast("ثبت با خطا روبرو شد، لطفا از عددی بودن شماره کنتور اطمینان حاصل کنید");
        }
    }
    //
    public void spinnerCounterStateClick(View view){
        EditText counterNumberEditText = (EditText) viewPager.findViewWithTag("counterNumber" + currentPosition);
        if(counterNumberEditText!=null &&
                !TextUtils.isDigitsOnly(counterNumberEditText.getText().toString().trim())){
            counterNumberEditText.setText("");
        }
    }
    //
    public String getBill_id(){
        return bill_id;
    }
    //
    public BigDecimal getCurrentTrackNumber(){
        return currentTrackNumber;
    }
    //
    public void registerAnyway(){
        Location lastLocation =geoTracker.getLastLocation();
        if (lastLocation != null) {
            onOffLoadModel.latitude=new BigDecimal(lastLocation.getLatitude());
            onOffLoadModel.longitude=new BigDecimal(lastLocation.getLongitude());
        }
        _list.get(currentPosition).counterNumber=globalCurrentCounterNumberForFragment;//list
        _list.get(currentPosition).counterStateCode=globalCurrentCounterStateCode;//list
        onOffLoadModel.counterNumber=globalCurrentCounterNumberForFragment;//db
        onOffLoadModel.counterStatePosition=globalCurrentCounterPositionForFragment;//db
        onOffLoadModel.counterStateCode=globalCurrentCounterStateCode;
        onOffLoadModel.registerDateJalali=persianDate_db_format;
        onOffLoadModel.save();//save changes in db
        playSaveSound();
        saveDone();
        sendTheUnsended();
    }
    //
    private void playSaveSound(){
       mediaPlayerManager.playSound(R.raw.save);
    }
    //
    private void playNotSaveSound(){
       mediaPlayerManager.playSound(R.raw.not_save);
    }
    //
    private void saveDone(){
        Location lastLocation =geoTracker.getLastLocation();
        if (lastLocation != null) {
            if(lastLocation.hasAccuracy()){
                Integer accuracy2=Math.round(lastLocation.getAccuracy());
                onOffLoadModel.gisAccuracy=accuracy2;
            }
            onOffLoadModel.latitude=new BigDecimal(lastLocation.getLatitude());
            onOffLoadModel.longitude=new BigDecimal(lastLocation.getLongitude());
        }
        onOffLoadModel.registerDateJalali=persianDate_db_format;
        onOffLoadModel.save();
        geoTracker.displayLocation();
        if(currentPosition<viewPagerSize-1){
            int newPosition=++currentPosition;
            viewPager.setCurrentItem(newPosition);
            Log.e("currentPosition changed",(currentPosition++)+"");
        }
        Snackbar.make(viewPager, "ذخیره شماره کنتور انجام شد", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
    }
    //
    private void sendTheUnsended(){
        String deviceId= Build.SERIAL;
        final List<OnOffLoadModel> unSendeds=OnOffLoadModel
                .find(OnOffLoadModel.class, "OFF_LOAD_STATE= ? ", "1");
        final  List<CounterReadingReport> unSendedReports=CounterReadingReport
                .find(CounterReadingReport.class,"OFF_LOAD_STATE= ? ","1");
        Output output=new Output(unSendedReports,unSendeds);
        IAbfaService abfaService = IAbfaService.retrofit.create(IAbfaService.class);
        Call<Integer> call=abfaService.sendCounterReadingInfo(getToken(),output,deviceId,getUserCode(),false,new BigDecimal(-1));
        call.enqueue(new Callback<Integer>() {
            @Override
            public void onResponse(Call<Integer> call,
                                   retrofit2.Response<Integer> response) {
                int responseCode = response.code();
                String errorMessage = "";
                if (responseCode != 200) {
                    errorMessage = SimpleErrorHandler.getErrorMessage(responseCode);
                    toastAndAlertBuilder.makeSimpleAlert(errorMessage);
                    return;
                }
                Integer count = response.body();
                Log.e("count :", count + "");
                for (OnOffLoadModel unSended : unSendeds) {
                    unSended.offLoadState = 2;
                    unSended.save();
                    Log.i("save done", " 2");
                }
                //
                for (CounterReadingReport unSenderReport : unSendedReports) {
                    unSenderReport.offLoadState = 2;
                    unSenderReport.save();
                }
            }

            @Override
            public void onFailure(Call<Integer> call, Throwable t) {
                String errorMessage = SimpleErrorHandler.getErrorMessage(t);
                toastAndAlertBuilder.makeSimpleAlert(errorMessage);
                Log.e("retrofit error", t.toString());
            }
        });
    }
    //
    private void doSaveProccess(){
        playSaveSound();
        saveDone();
        sendTheUnsended();
    }
    //
    private void vibratePlease(){
        final Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(450);
    }
    //
    public AverageState getAverageStateGlobal(){
        return averageStateGlobal;
    }
    //
    private void displayLastUnread(){
        int i=0;
        for (OnOffLoadModel onOffload:_list) {
            if(onOffload.counterStatePosition==null){
                Log.d("last unread position:",i+"");
                currentPosition=i;
                setViewPagerCurrentItem(i);
                return;
            }
            i++;
        }
    }
    //
    public Collection<OnOffLoadModel> getTheList(){
        return _list;
    }
    //
    //region _____________________________ GPS __________________________________
    //
    public LatLang getLatLang(){
      return geoTracker.getLatLang();
    }

    @Override
    protected void onStart() {
        super.onStart();
       geoTracker.start();
    }
    //
    @Override
    protected void onResume() {
        super.onResume();
        geoTracker.resume();
    }
    //
    @Override
    protected void onStop() {
        super.onStop();
       geoTracker.stop();
    }
    //
    @Override
    protected void onPause() {
        super.onPause();
        geoTracker.pause();
    }
    //endregion
}

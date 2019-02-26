package com.example.jum.dailynote;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.ActionBar;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Calendar;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttachment;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

import static android.app.Activity.RESULT_OK;

public class MainFragment extends Fragment implements EasyPermissions.PermissionCallbacks {

    //***************
    //variables
    //***************

    // variables for calendar
    private int mID;
    private com.google.api.services.calendar.Calendar mService = null;
    GoogleAccountCredential mCredential;

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;
    static final int REQUEST_IMAGE_GALLARY = 1004;

    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = {CalendarScopes.CALENDAR};
    ProgressDialog mProgress;


    // variables for GPS
    LocationManager lm;
    Location location;
    Geocoder gCorder;
    List<Address> addresses;
    String placeName = "";

    // variables for interfaces
    ActionBar actionBar;
    long now = System.currentTimeMillis();
    java.util.Calendar calendar = java.util.Calendar.getInstance();

    EditText title;
    EditText place;
    EditText body;
    Button set_date;
    Button set_time1;
    Button set_time2;
    Date start_date = calendar.getTime();
    Date end_date = calendar.getTime();
    Button image_load;

    DatePickerDialog date_dialog;
    TimePickerDialog time_dialog1;
    TimePickerDialog time_dialog2;

    ArrayList<Uri> img_to_attach = new ArrayList<Uri>();

    RecyclerView mRecyclerView;
    RecyclerView.LayoutManager mLayoutManager;
    RV_Adapter mAdapter;
    // variables for permissions
    int MY_PERMISSIONS_REQUEST_FINE_LOCATION = 0;
    int MY_PERMISSIONS_REQUEST_COARSE_LOCATION = 1;
    int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 2;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
// settings for Google Calendar API
        // Google Calendar API 호출중에 표시되는 ProgressDialog
        mProgress = new ProgressDialog(getContext());
        mProgress.setMessage("Google Calendar API 호출 중입니다.");

        // Google Calendar API 사용하기 위해 필요한 인증 초기화( 자격 증명 credentials, 서비스 객체 )
        // OAuth 2.0를 사용하여 구글 계정 선택 및 인증하기 위한 준비
        mCredential = GoogleAccountCredential.usingOAuth2(
                getContext().getApplicationContext(),
                Arrays.asList(SCOPES)
        ).setBackOff(new ExponentialBackOff()); // I/O 예외 상황을 대비해서 백오프 정책 사용

// settings for GPS
        getLastLocation();  //get latest position
// settings for account permission
        // GET_ACCOUNTS 권한을 가지고 있지 않다면
        if (!(EasyPermissions.hasPermissions(getContext(), Manifest.permission.GET_ACCOUNTS))) {
            // 사용자에게 GET_ACCOUNTS 권한을 요구하는 다이얼로그를 보여준다.(주소록 권한 요청함)
            EasyPermissions.requestPermissions(
                    (Activity) getContext(),
                    "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
// settings for repository permission
        if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE)) {
            } else {
                ActivityCompat.requestPermissions(getActivity(),
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        // settings for functions
        // EditText settings
        title = view.findViewById(R.id.title);
        place = view.findViewById(R.id.place);
        body = view.findViewById(R.id.body);

        place.setText(placeName);

        // year, month, day, hour, minute
        int year = calendar.get(java.util.Calendar.YEAR);
        int month = calendar.get(java.util.Calendar.MONTH) + 1;
        int day = calendar.get(java.util.Calendar.DAY_OF_MONTH);
        int hour = calendar.get(java.util.Calendar.HOUR_OF_DAY);
        int minute = calendar.get(java.util.Calendar.MINUTE);

        // Dialogue for date and time
        date_dialog = new DatePickerDialog(getContext(), date_listener, year, month - 1, day);
        time_dialog1 = new TimePickerDialog(getContext(), time_listener1, hour, minute, false);
        time_dialog2 = new TimePickerDialog(getContext(), time_listener2, hour, minute, false);

        // button settings
        set_date = (Button) view.findViewById(R.id.date);
        set_time1 = (Button) view.findViewById(R.id.time1);
        set_time2 = (Button) view.findViewById(R.id.time2);
        image_load = (Button) view.findViewById(R.id.addImage);

        set_date.setText("" + year + "년 " + month + "월 " + day + "일");
        set_date.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                date_dialog.show();
            }
        });

        set_time1.setText("" + hour + "시 " + minute + "분");
        set_time1.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                time_dialog1.show();
            }
        });

        set_time2.setText("" + hour + "시 " + minute + "분");
        set_time2.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                time_dialog2.show();
            }
        });

        image_load.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View v) {
                // check permission and load image
                if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    // get image by using intent
                    Intent intent = new Intent();
                    intent.setType("image/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    startActivityForResult(intent, REQUEST_IMAGE_GALLARY); // onActivityResult를 자동으로 호출
                } else {
                    ActivityCompat.requestPermissions(getActivity(),
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                            MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
                }
            }//->onClick
        });

        // settings for recycler view
        mRecyclerView = view.findViewById(R.id.img_recycler_view);
        mRecyclerView.setHasFixedSize(true);
        mLayoutManager = new LinearLayoutManager(getContext());
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new RV_Adapter(img_to_attach);
        mRecyclerView.setAdapter(mAdapter);

        // settings for Floating Action Button
        FloatingActionButton fab = (FloatingActionButton) view.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mID = 1;
                getResultsFromApi();
                Snackbar.make(view, "다이어리 저장", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        return view;
    }//->onCreateView

    //***************
    //*****Listeners
    //***************
    private TimePickerDialog.OnTimeSetListener time_listener1 = new TimePickerDialog.OnTimeSetListener() {
        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            // 설정버튼 눌렀을 때
            Toast.makeText(getContext().getApplicationContext(), hourOfDay + "시 " + minute + "분", Toast.LENGTH_SHORT).show();
            set_time1.setText(hourOfDay + "시 " + minute + "분");

            calendar.set(java.util.Calendar.MILLISECOND, 0);
            calendar.set(java.util.Calendar.HOUR_OF_DAY, hourOfDay);
            calendar.set(java.util.Calendar.MINUTE, minute);
            start_date = calendar.getTime();
        }
    };
    private TimePickerDialog.OnTimeSetListener time_listener2 = new TimePickerDialog.OnTimeSetListener() {
        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            // 설정버튼 눌렀을 때
            Toast.makeText(getContext().getApplicationContext(), hourOfDay + "시 " + minute + "분", Toast.LENGTH_SHORT).show();
            set_time2.setText(hourOfDay + "시 " + minute + "분");

            calendar.set(java.util.Calendar.MILLISECOND, 0);
            calendar.set(java.util.Calendar.HOUR_OF_DAY, hourOfDay);
            calendar.set(java.util.Calendar.MINUTE, minute);
            end_date = calendar.getTime();
        }
    };
    private DatePickerDialog.OnDateSetListener date_listener = new DatePickerDialog.OnDateSetListener() {
        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            Toast.makeText(getContext().getApplicationContext(), year + "년 " + monthOfYear + "월 " + dayOfMonth + "일", Toast.LENGTH_SHORT).show();
            set_date.setText(year + "년 " + monthOfYear + "월 " + dayOfMonth + "일");
        }
    };

    //***********************
    //***** picture treatment
    //***********************
    private String getRealPathFromURI(Uri contentUri) {
        int column_index = 0;
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContext().getContentResolver().query(contentUri, proj, null, null, null);
        if (cursor.moveToFirst()) {
            column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        }

        return cursor.getString(column_index);
    }

    private int exifOrientationToDegrees(int exifOrientation) {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
            return 90;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
            return 180;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
            return 270;
        }
        return 0;
    }

    //**********************
    //***** my own functions
    //**********************
    private void saveDiary(String calendarTitle) {
        // save diary
        String calendarID = getCalendarID(calendarTitle);

        if (calendarID == null) {
            Toast.makeText(getContext().getApplicationContext(), "Calendar를 먼저 생성하세요", Toast.LENGTH_SHORT).show();
        }

        // putting things you wrote to the calendar
        Event event = new Event()
                .setSummary(title.getText().toString())
                .setLocation(place.getText().toString())
                .setDescription(body.getText().toString());

        // setting times
        // TODO: GPS정보로 세계시간 사용하기
        SimpleDateFormat simpledateformat;
        //simpledateformat = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ssZ", Locale.KOREA);
        // Z에 대응하여 +0900이 입력되어 문제 생겨 수작업으로 입력
        simpledateformat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss+09:00", Locale.KOREA);

        String datetime = simpledateformat.format(start_date);
        Log.d("@@@", datetime);
        DateTime startDateTime = new DateTime(datetime);
        EventDateTime start = new EventDateTime()
                .setDateTime(startDateTime)
                .setTimeZone("Asia/Seoul");
        event.setStart(start);

        datetime = simpledateformat.format(end_date);
        Log.d("@@@", datetime);
        DateTime endDateTime = new DateTime(datetime);
        EventDateTime end = new EventDateTime()
                .setDateTime(endDateTime)
                .setTimeZone("Asia/Seoul");
        event.setEnd(end);

        //String[] recurrence = new String[]{"RRULE:FREQ=DAILY;COUNT=2"};
        //event.setRecurrence(Arrays.asList(recurrence));

        // TODO : 그림, 시간 적용해서 저장
        addAttachments(event);

        // final uploading
        try {
            event = mService.events().insert(calendarID, event).execute();
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("Exception", "Exception : " + e.toString());
        }
        System.out.printf("Event created: %s\n", event.getHtmlLink());
        Log.e("Event", "created : " + event.getHtmlLink());
        String eventStrings = "created : " + event.getHtmlLink();

    }//->saveDiary

    private void addAttachments(Event event) {
        List<EventAttachment> attachments = event.getAttachments();
        if (attachments == null) {
            attachments = new ArrayList<EventAttachment>();
        }

        ContentResolver cr = getActivity().getContentResolver();
        for (Uri uri : img_to_attach) {
            try {
                attachments.add(new EventAttachment()
                        .setFileUrl((new URL(uri.toString())).toString())
                        .setMimeType(cr.getType(uri))
                        .setTitle(uri.getPath())
                );
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
    }

    //***********************
    //***** for Recycler View
    //***********************
    public class RV_Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>  {
        public class MyViewHolder extends RecyclerView.ViewHolder {
            View mView;
            ImageView Picture;
            TextView PictureName;

            MyViewHolder(View view){
                super(view);
                mView = view;
                Picture = view.findViewById(R.id.selected_picture);
                PictureName = view.findViewById(R.id.selected_picture_name);
            }

        }

        private ArrayList<Uri> uriArrayList;
        RV_Adapter(ArrayList<Uri> imgArrayList){
            this.uriArrayList = imgArrayList;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.revycler_view_item_image, parent, false);
            return new MyViewHolder(v);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
            MyViewHolder myViewHolder = (MyViewHolder) holder;
            myViewHolder.Picture.setImageURI(Uri.parse("file:///" + Environment.getExternalStorageDirectory() + uriArrayList.get(position)));
            myViewHolder.PictureName.setText(uriArrayList.get(position).getPath());

            ((MyViewHolder) holder).mView.setOnLongClickListener(new View.OnLongClickListener(){
                @Override
                public boolean onLongClick(View v) {
                    uriArrayList.remove(position); // ArrayList에서 제거하는 작업
                    notifyItemRemoved(position); // adapter에 삭제를 알려 refresh 하게 함
                    notifyItemRangeChanged(position, uriArrayList.size()); // position 이 변경되게 하는 함수
                    return false;
                }
            });
        }

        @Override
        public int getItemCount() {
            return uriArrayList.size();
        }
    }

    //***************
    //*****Google GPS
    //***************
    private void getLastLocation() {
        // check GPS permission, and ask user to give
        boolean check_permission = false;
        if (ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            // Toast.makeText(getContext().getApplicationContext(), "GPS 권한 오류", Toast.LENGTH_SHORT).show();
            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                        Manifest.permission.ACCESS_FINE_LOCATION)) {
                } else {
                    ActivityCompat.requestPermissions(getActivity(),
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            MY_PERMISSIONS_REQUEST_FINE_LOCATION);
                }
            }

            if (ContextCompat.checkSelfPermission(getContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                        Manifest.permission.ACCESS_COARSE_LOCATION)) {
                } else {
                    ActivityCompat.requestPermissions(getActivity(),
                            new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                            MY_PERMISSIONS_REQUEST_COARSE_LOCATION);
                }
            }
        } else {
            check_permission = true;
        }

        if (check_permission) {
            lm = (LocationManager) getContext().getSystemService(Context.LOCATION_SERVICE);
            gCorder = new Geocoder(getActivity().getBaseContext(), Locale.getDefault());


            final LocationListener mLocationListener = new LocationListener() {
                public void onLocationChanged(Location location) {
                    //여기서 위치값이 갱신되면 이벤트가 발생한다.
                    //값은 Location 형태로 리턴되며 좌표 출력 방법은 다음과 같다.
                    if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
                        //Gps 위치제공자에 의한 위치변화. 오차범위가 좁다.
                        double longitude = location.getLongitude();    //경도
                        double latitude = location.getLatitude();         //위도
                        float accuracy = location.getAccuracy();        //신뢰도
                    } else {
                        //Network 위치제공자에 의한 위치변화
                        //Network 위치는 Gps에 비해 정확도가 많이 떨어진다.
                    }
                }

                public void onProviderDisabled(String provider) {
                }

                public void onProviderEnabled(String provider) {
                }

                public void onStatusChanged(String provider, int status, Bundle extras) {
                }
            };

            lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 6000, 10, mLocationListener);

            if (lm != null) {
                location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                if (location != null) {
                    try {
                        addresses = gCorder.getFromLocation(location.getLatitude(),
                                location.getLongitude(), 1);
                        if (addresses.size() > 0)
                            System.out.println(addresses.get(0).getLocality());
                        placeName = addresses.get(0).getAddressLine(0);//getLocality() 대신 getAddressLine(INDEX) 사용한 것
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }//->if(location != null)
            }//->if (lm != null)
        }//->if (check_permission)
    }//->getLastLocation

    //************************
    //*****Google Calendar API
    //************************
//from https://webnautes.tistory.com/1217

    /**
     * 다음 사전 조건을 모두 만족해야 Google Calendar API를 사용할 수 있다.
     * <p>
     * 사전 조건
     * - Google Play Services 설치
     * - 유효한 구글 계정 선택
     * - 안드로이드 디바이스에서 인터넷 사용 가능
     * <p>
     * 하나라도 만족하지 않으면 해당 사항을 사용자에게 알림.
     */
    private String getResultsFromApi() {

        if (!isGooglePlayServicesAvailable()) { // Google Play Services를 사용할 수 없는 경우

            acquireGooglePlayServices();
        } else if (mCredential.getSelectedAccountName() == null) { // 유효한 Google 계정이 선택되어 있지 않은 경우

            chooseAccount();
        } else if (!isDeviceOnline()) {    // 인터넷을 사용할 수 없는 경우

            Toast.makeText(getContext().getApplicationContext(), "No network connection available.", Toast.LENGTH_LONG).show();
        } else {

            // Google Calendar API 호출
            new MakeRequestTask(this, mCredential).execute();
        }
        return null;
    }


    /**
     * 안드로이드 디바이스에 최신 버전의 Google Play Services가 설치되어 있는지 확인
     */
    private boolean isGooglePlayServicesAvailable() {

        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();

        final int connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(getContext());
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }


    /*
     * Google Play Services 업데이트로 해결가능하다면 사용자가 최신 버전으로 업데이트하도록 유도하기위해
     * 대화상자를 보여줌.
     */
    private void acquireGooglePlayServices() {

        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        final int connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(getContext());

        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {

            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }


    /*
     * 안드로이드 디바이스에 Google Play Services가 설치 안되어 있거나 오래된 버전인 경우 보여주는 대화상자
     */
    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode
    ) {

        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();

        Dialog dialog = apiAvailability.getErrorDialog(
                getActivity(),
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES
        );
        dialog.show();
    }


    /*
     * Google Calendar API의 자격 증명( credentials ) 에 사용할 구글 계정을 설정한다.
     *
     * 전에 사용자가 구글 계정을 선택한 적이 없다면 다이얼로그에서 사용자를 선택하도록 한다.
     * GET_ACCOUNTS 퍼미션이 필요하다.
     */
    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {

        // GET_ACCOUNTS 권한을 가지고 있다면
        if (EasyPermissions.hasPermissions(getContext(), Manifest.permission.GET_ACCOUNTS)) {


            // SharedPreferences에서 저장된 Google 계정 이름을 가져온다.
            String accountName = getActivity().getPreferences(Context.MODE_PRIVATE)
                    .getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {

                // 선택된 구글 계정 이름으로 설정한다.
                mCredential.setSelectedAccountName(accountName);
                getResultsFromApi();
            } else {


                // 사용자가 구글 계정을 선택할 수 있는 다이얼로그를 보여준다.
                startActivityForResult(
                        mCredential.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER);
            }


            // GET_ACCOUNTS 권한을 가지고 있지 않다면
        } else {


            // 사용자에게 GET_ACCOUNTS 권한을 요구하는 다이얼로그를 보여준다.(주소록 권한 요청함)
            EasyPermissions.requestPermissions(
                    (Activity) getContext(),
                    "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS,
                    Manifest.permission.GET_ACCOUNTS);
        }
    }



    /*
     * 구글 플레이 서비스 업데이트 다이얼로그, 구글 계정 선택 다이얼로그, 인증 다이얼로그에서 되돌아올때 호출된다.
     */

    @Override
    public void onActivityResult(
            int requestCode,  // onActivityResult가 호출되었을 때 요청 코드로 요청을 구분
            int resultCode,   // 요청에 대한 결과 코드
            Intent data
    ) {
        super.onActivityResult(requestCode, resultCode, data);


        switch (requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    Toast.makeText(getContext().getApplicationContext(), "앱을 실행시키려면 구글 플레이 서비스가 필요합니다. 구글 플레이 서비스를 설치 후 다시 실행하세요.", Toast.LENGTH_SHORT).show();
                } else {
                    getResultsFromApi();
                }
                break;

            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null && data.getExtras() != null) {
                    String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings = getActivity().getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);
                        getResultsFromApi();
                    }
                }
                break;

            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    getResultsFromApi();
                }
                break;

            case REQUEST_IMAGE_GALLARY:
                if (resultCode == RESULT_OK) {
                    // TODO: 이미지 넣는 코드
                    if(!img_to_attach.contains((data.getData())))
                        img_to_attach.add(data.getData());

//                    Set<Uri> uriSet = new HashSet<>(img_to_attach);
//                    img_to_attach = new ArrayList<>(uriSet);

                    mAdapter.notifyDataSetChanged();
                    /*
                    String imagePath = getRealPathFromURI(data.getData());
                    ExifInterface exif = null;
                    try {
                        exif = new ExifInterface(imagePath);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    int exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                    int exifDegree = exifOrientationToDegrees(exifOrientation);
                    */
                }
                break;
        }
    }

    /*
     * Android 6.0 (API 23) 이상에서 런타임 권한 요청시 결과를 리턴받음
     */
    @Override
    public void onRequestPermissionsResult(
            int requestCode,  //requestPermissions(android.app.Activity, String, int, String[])에서 전달된 요청 코드
            @NonNull String[] permissions, // 요청한 퍼미션
            @NonNull int[] grantResults    // 퍼미션 처리 결과. PERMISSION_GRANTED 또는 PERMISSION_DENIED
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, getContext());
    }


    /*
     * EasyPermissions 라이브러리를 사용하여 요청한 권한을 사용자가 승인한 경우 호출된다.
     */
    @Override
    public void onPermissionsGranted(int requestCode, List<String> requestPermissionList) {

        // 아무일도 하지 않음
    }


    /*
     * EasyPermissions 라이브러리를 사용하여 요청한 권한을 사용자가 거부한 경우 호출된다.
     */
    @Override
    public void onPermissionsDenied(int requestCode, List<String> requestPermissionList) {

        // 아무일도 하지 않음
    }


    /*
     * 안드로이드 디바이스가 인터넷 연결되어 있는지 확인한다. 연결되어 있다면 True 리턴, 아니면 False 리턴
     */
    private boolean isDeviceOnline() {

        ConnectivityManager connMgr = (ConnectivityManager) getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();

        return (networkInfo != null && networkInfo.isConnected());
    }

    //*************************
    //****Google Calendar Main
    //*************************
    /*
     * 캘린더 이름에 대응하는 캘린더 ID를 리턴
     */
    private String getCalendarID(String calendarTitle) {

        String id = null;

        // Iterate through entries in calendar list
        String pageToken = null;

        do {
            CalendarList calendarList = null;
            try {
                calendarList = mService.calendarList().list().setPageToken(pageToken).execute();
            } catch (UserRecoverableAuthIOException e) {
                startActivityForResult(e.getIntent(), REQUEST_AUTHORIZATION);
            } catch (IOException e) {
                e.printStackTrace();
            }
            List<CalendarListEntry> items = calendarList.getItems();

            for (CalendarListEntry calendarListEntry : items) {

                if (calendarListEntry.getSummary().toString().equals(calendarTitle)) {

                    id = calendarListEntry.getId().toString();
                }
            }
            pageToken = calendarList.getNextPageToken();
        } while (pageToken != null);
        return id;
    }

    /*
     * 비동기적으로 Google Calendar API 호출
     */
    private class MakeRequestTask extends AsyncTask<Void, Void, String> {

        private Exception mLastError = null;
        private MainFragment mActivity;
        List<String> eventStrings = new ArrayList<String>();


        public MakeRequestTask(MainFragment activity, GoogleAccountCredential credential) {

            mActivity = activity;

            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

            mService = new com.google.api.services.calendar.Calendar
                    .Builder(transport, jsonFactory, credential)
                    .setApplicationName("Google Calendar Diary")
                    .build();
        }


        @Override
        protected void onPreExecute() {
            // mStatusText.setText("");
            mProgress.show();
            Toast.makeText(getContext().getApplicationContext(), "데이터 가져오는 중...", Toast.LENGTH_SHORT).show();
        }


        /*
         * 백그라운드에서 Google Calendar API 호출 처리
         */
        @Override
        protected String doInBackground(Void... params) {
            try {
                if (mID == 1) {
                    //create calendar
                    //createCalendar("Diary");
                    //save calendar
                    saveDiary("Diary");
                    return "저장 완료";
                }
            } catch (Exception e) {
                mLastError = e;
                cancel(true);
                return null;
            }

            return null;
        }


        /*
         * CalendarTitle 이름의 캘린더에서 10개의 이벤트를 가져와 리턴
         */
        private String getEvent(String title) throws IOException {


            DateTime now = new DateTime(System.currentTimeMillis());

            String calendarID = getCalendarID(title);
            if (calendarID == null) {

                return "캘린더를 먼저 생성하세요.";
            }


            Events events = mService.events().list(calendarID)//"primary")
                    .setMaxResults(10)
                    //.setTimeMin(now)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();
            List<Event> items = events.getItems();


            for (Event event : items) {

                DateTime start = event.getStart().getDateTime();
                if (start == null) {

                    // 모든 이벤트가 시작 시간을 갖고 있지는 않다. 그런 경우 시작 날짜만 사용
                    start = event.getStart().getDate();
                }


                eventStrings.add(String.format("%s \n (%s)", event.getSummary(), start));
            }


            return eventStrings.size() + "개의 데이터를 가져왔습니다.";
        }

        /*
         * 선택되어 있는 Google 계정에 새 캘린더를 추가한다.
         */
        private void createCalendar(String title) throws IOException {

            String ids = getCalendarID(title);

            if (ids == null) {

                Toast.makeText(getContext().getApplicationContext(), "데이터 가져오는 중...", Toast.LENGTH_SHORT).show();


                // 새로운 캘린더 생성
                com.google.api.services.calendar.model.Calendar calendar = new Calendar();

                // 캘린더의 제목 설정
                calendar.setSummary(title);


                // 캘린더의 시간대 설정
                calendar.setTimeZone("Asia/Seoul");

                // 구글 캘린더에 새로 만든 캘린더를 추가
                Calendar createdCalendar = mService.calendars().insert(calendar).execute();

                // 추가한 캘린더의 ID를 가져옴.
                String calendarId = createdCalendar.getId();


                // 구글 캘린더의 캘린더 목록에서 새로 만든 캘린더를 검색
                CalendarListEntry calendarListEntry = mService.calendarList().get(calendarId).execute();

                // 캘린더의 배경색을 파란색으로 표시  RGB
                calendarListEntry.setBackgroundColor("#0000ff");

                // 변경한 내용을 구글 캘린더에 반영
                CalendarListEntry updatedCalendarListEntry =
                        mService.calendarList()
                                .update(calendarListEntry.getId(), calendarListEntry)
                                .setColorRgbFormat(true)
                                .execute();

                // 새로 추가한 캘린더의 ID를 리턴
                Toast.makeText(getContext().getApplicationContext(), ids, Toast.LENGTH_SHORT).show();
            }
        }


        @Override
        protected void onPostExecute(String output) {

            mProgress.hide();
            Toast.makeText(getContext().getApplicationContext(), output, Toast.LENGTH_SHORT).show();

            //if ( mID == 3 )   mResultText.setText(TextUtils.join("\n\n", eventStrings));
        }


        @Override
        protected void onCancelled() {
            mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            MainFragment.REQUEST_AUTHORIZATION);
                } else {
                    Toast.makeText(getContext().getApplicationContext(), "MakeRequestTask The following error occurred:\n" + mLastError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(getContext().getApplicationContext(), "요청 취소됨.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}//->MainFragment
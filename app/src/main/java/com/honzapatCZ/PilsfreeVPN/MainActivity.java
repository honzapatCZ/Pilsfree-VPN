package com.honzapatCZ.PilsfreeVPN;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.constraintlayout.widget.Group;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.text.InputType;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.dd.processbutton.iml.ActionProcessButton;
import com.github.angads25.toggle.LabeledSwitch;
import com.github.angads25.toggle.interfaces.OnToggledListener;
import com.google.android.material.navigation.NavigationView;

import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Locale;

import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.preference.*;
import de.blinkt.openvpn.LaunchVPN;
import de.blinkt.openvpn.VpnProfile;
import de.blinkt.openvpn.core.App;
import de.blinkt.openvpn.core.Connection;
import de.blinkt.openvpn.core.ConnectionStatus;
import de.blinkt.openvpn.core.IOpenVPNServiceInternal;
import de.blinkt.openvpn.core.OpenVPNManagement;
import de.blinkt.openvpn.core.OpenVPNService;
import de.blinkt.openvpn.core.ProfileManager;
import de.blinkt.openvpn.core.VpnStatus;
import com.mikhaellopez.circularprogressbar.CircularProgressBar;
import com.tomergoldst.tooltips.ToolTip;
import com.tomergoldst.tooltips.ToolTipsManager;

import static de.blinkt.openvpn.core.OpenVPNService.humanReadableByteCount;


import mehdi.sakout.aboutpage.AboutPage;
import mehdi.sakout.aboutpage.Element;


public class MainActivity extends AppCompatActivity implements VpnStatus.ByteCountListener, VpnStatus.StateListener {

    private DrawerLayout drwaer;

    ToolTipsManager mToolTipsManager;
    //private ActionProcessButton btnConnect;
    private ImageButton btnModify;
    private Group layoutSpeedMeter;
    private TextView textUpload, textDownload;

    private LabeledSwitch modeSwitch;
    private Boolean routeAllMode = false;
    private ImageButton modeSwHelpButt;

    private Button mainButt;
    private CircularProgressBar  mainBar;
    private TextView statusTxt;

    private TextView usernameTxt;

    private ViewGroup mainLayout;
    private ViewGroup advSetLayout;
    private ViewGroup aboutLayout;


    private IOpenVPNServiceInternal mService;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = IOpenVPNServiceInternal.Stub.asInterface(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mService = null;

        }
    };

    public void EditAuthDets(final Context context){
        final ProfileManager pm = ProfileManager.getInstance(this);
        final VpnProfile profile = pm.getProfileByName(Build.MODEL);//

        final EditText entry = new EditText(this);
        entry.setSingleLine();
        entry.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        entry.setTransformationMethod(new PasswordTransformationMethod());
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle(getString(R.string.pw_modify_dialog_title));
        //dialog.setMessage(getString(R.string.pw_request_dialog_prompt, profile.mName));
        @SuppressLint("InflateParams") final View userpwlayout = getLayoutInflater().inflate(R.layout.userpass, null, false);
        ((EditText) userpwlayout.findViewById(R.id.username)).setText(profile.mUsername);
        ((EditText) userpwlayout.findViewById(R.id.password)).setText(profile.mPassword);
        ((CheckBox) userpwlayout.findViewById(R.id.save_password)).setChecked(!TextUtils.isEmpty(profile.mPassword));
        ((CheckBox) userpwlayout.findViewById(R.id.show_password)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) ((EditText) userpwlayout.findViewById(R.id.password)).setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                else ((EditText) userpwlayout.findViewById(R.id.password)).setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            }
        });
        dialog.setView(userpwlayout);
        AlertDialog.Builder builder = dialog.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                profile.mUsername = ((EditText) userpwlayout.findViewById(R.id.username)).getText().toString().toLowerCase();
                String pw = ((EditText) userpwlayout.findViewById(R.id.password)).getText().toString();
                if (((CheckBox) userpwlayout.findViewById(R.id.save_password)).isChecked()) {
                    profile.mPassword = pw;
                } else {
                    profile.mPassword = null;
                }
                pm.saveProfile(context, profile);
                pm.saveProfileList(context);
                //UI
                if(profile.mUsername.isEmpty())
                {
                    usernameTxt.setText(getString(R.string.design_username));
                }
                else
                {
                    usernameTxt.setText(profile.mUsername);
                }

                //Log.d("STATE", "usr: " + mSelectedProfile.mUsername + " pass: "+ mSelectedProfile.mPassword);

            }
        });
        dialog.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        dialog.create().show();
    }

    @Override
    protected void onStop() {
        VpnStatus.removeStateListener(this);
        VpnStatus.removeByteCountListener(this);
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        VpnStatus.addStateListener(this);
        VpnStatus.addByteCountListener(this);
        Intent intent = new Intent(this, OpenVPNService.class);
        intent.setAction(OpenVPNService.START_SERVICE);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(mConnection);
    }

    @Override
    public void onBackPressed(){
        if(drwaer.isDrawerOpen(GravityCompat.START)){
            drwaer.closeDrawer(GravityCompat.START);
        }
        else{
            super.onBackPressed();
        }
    }

    public void setLayoutInvisible(ViewGroup lay) {
        if (lay.getVisibility() == View.VISIBLE) {
            lay.setVisibility(View.GONE);
        }
    }
    public void setLayoutVisible(ViewGroup lay) {
        if (lay.getVisibility() == View.GONE) {
            lay.setVisibility(View.VISIBLE);
        }
    }

    private View CreateAboutPage()
    {
        return(
                new AboutPage(this)
                .isRTL(false)
                .setImage(R.mipmap.ic_launcher_round)
                .setDescription(getString(R.string.about_descr))
                .addItem(new Element().setTitle(String.format(getString(R.string.about_version), "1.0")))
                .addGroup("Info")
                .addWebsite("http://web.pilsfree.net/vpn.do", getString(R.string.about_is_info))
                //.addPlayStore("com.pilsfree-servis.vpn", getString(R.string.about_on_gplay))
                .addGroup(getString(R.string.about_licenseText))
                .addWebsite("https://www.gnu.org/licenses/old-licenses/gpl-2.0.html","GPL v2")
                .addGitHub("yuger/VPN2018", "Fork of VPN 2018")
                .addItem(new Element().setTitle(getString(R.string.about_copyr)))
                .addGitHub("honzapatCZ/Pilsfree-VPN", String.format(getString(R.string.about_Mgithub), "honzapatCZ/Pilsfree-VPN"))
                .addGroup(getString(R.string.about_MDev))
                .addFacebook("nejcraft", "Nejcraft(honzapatCZ)")
                .addTwitter("Nejcraft", "Nejcraft(honzapatCZ)")
                .addGitHub("honzapatCZ", "honzapatCZ")
                .create()
        );
    }

    public static String dump(Object o){
        Field[] fields = o.getClass().getDeclaredFields();
        String fin = "";
        for (int i=0; i<fields.length; i++)
        {
            try
            {
                fin += fields[i].getName() + " - " + fields[i].get(o) + "\n";
            }
            catch (java.lang.IllegalAccessException excp){
                fin = fin;
            }
        }
        return fin;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mToolTipsManager = new ToolTipsManager();


        Locale locale = new Locale("cs");
        Locale.setDefault(locale);
        // Create a new configuration object
        Configuration config = new Configuration();
        // Set the locale of the new configuration
        config.locale = locale;
        // Update the configuration of the Accplication context
        getResources().updateConfiguration(
                config,
                getResources().getDisplayMetrics()
        );




        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //toolbar.setTitleTextColor(ContextCompat.getColor(this, android.R.color.white));

        drwaer = findViewById(R.id.drawer_layout);
         ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drwaer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drwaer.addDrawerListener(toggle);
        toggle.syncState();



        NavigationView navigationView = findViewById(R.id.nav_view);

        navigationView.setCheckedItem(R.id.nav_home);
        //setLayoutInvisible(aboutLayout);
        //setLayoutInvisible(advSetLayout);
        //setLayoutVisible(mainLayout);

        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem menuItem) {
                switch(menuItem.getItemId()){
                    case R.id.nav_home:
                        setLayoutInvisible(aboutLayout);
                        setLayoutInvisible(advSetLayout);
                        setLayoutVisible(mainLayout);

                        break;
                    case R.id.nav_advanced:
                        setLayoutInvisible(aboutLayout);
                        setLayoutInvisible(mainLayout);
                        setLayoutVisible(advSetLayout);
                        break;
                    case R.id.nav_about:
                        setLayoutInvisible(mainLayout);
                        setLayoutInvisible(advSetLayout);
                        setLayoutVisible(aboutLayout);
                        break;
                }
                drwaer.closeDrawer(GravityCompat.START);
                return true;
            }
        });



        usernameTxt = findViewById(R.id.usernameText);

        try {
            ProfileManager pm = ProfileManager.getInstance(this);

            VpnProfile profile = pm.getProfileByName(Build.MODEL);//
            if(profile == null || profile.mUsername.isEmpty())
            {
                usernameTxt.setText(getString(R.string.design_username));
            }
            else
            {
                usernameTxt.setText(profile.mUsername);
            }


        } catch (Exception ex) {
            usernameTxt.setText(getString(R.string.design_username));
        }


        mainLayout = findViewById(R.id.mainLayout);

        advSetLayout = findViewById(R.id.advSetLayout);
        getSupportFragmentManager().beginTransaction().add(R.id.advSetLayout, new SettFragment()).commit();

        aboutLayout = findViewById(R.id.aboutLayout);
        aboutLayout.addView(CreateAboutPage());

        modeSwitch = findViewById(R.id.mode_Switch);
        routeAllMode = modeSwitch.isOn();
        modeSwitch.setOnToggledListener(new OnToggledListener() {
            @Override
            public void onSwitched(LabeledSwitch labeledSwitch, boolean isOn) {
                // Implement your switching logic here
                routeAllMode = isOn;

                if(App.isStart)
                {
                    stopVPN();
                    App.isStart = false;
                    startVPN(false);
                    App.isStart = true;

                }
            }
        });

        modeSwHelpButt = findViewById(R.id.modeSwHelp);


        final ToolTip.Builder builder = new ToolTip.Builder(this, modeSwitch, mainLayout, getText(R.string.mode_switch), ToolTip.POSITION_ABOVE);
        builder.setBackgroundColor(getResources().getColor(R.color.colorGrayDark));
        modeSwHelpButt.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                mToolTipsManager.show(builder.build());
            }
        });

        layoutSpeedMeter = findViewById(R.id.updownGr);
        layoutSpeedMeter.setReferencedIds(new int[]{R.id.textUpload, R.id.textDownload});

        textUpload = findViewById(R.id.textUpload);
        textDownload = findViewById(R.id.textDownload);
        //btnConnect = findViewById(R.id.buttonConnect);
        btnModify = findViewById(R.id.buttonModify);





        mainBar = findViewById(R.id.MainButtProgress);
        mainButt = findViewById(R.id.MainButton);
        mainBar.setIndeterminateMode(false);
        mainBar.setProgress(0);
        statusTxt = findViewById(R.id.statusTxt);

        if (!App.isStart) {
            DataCleanManager.cleanCache(this);
            mainButt.setEnabled(false);
            mainButt.setBackground(getResources().getDrawable(R.drawable.main_button_off));
            //final ProgressBar progressBar = findViewById(R.id.progressbar);
            //progressBar.setVisibility(View.VISIBLE);
            profileAsync = new ProfileAsync(this, new ProfileAsync.OnProfileLoadListener() {
                @Override
                public void onProfileLoadSuccess() {
                    //progressBar.setVisibility(View.GONE);
                    mainButt.setEnabled(true);
                }

                @Override
                public void onProfileLoadFailed(String msg) {
                    //progressBar.setVisibility(View.GONE);
                    Toast.makeText(MainActivity.this, MainActivity.this.getString(R.string.init_fail) + msg, Toast.LENGTH_SHORT).show();
                }
            });
            profileAsync.execute();
//            rate();
        }
        mainButt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        if (!App.isStart) {
                            startVPN(false);
                            App.isStart = true;
                        } else {
                            stopVPN();
                            App.isStart = false;
                        }
                    }
                };
                r.run();
                Log.d("INFO", "Ahoj");
            }
        });
        final Context cont = getContextOfThis(this);
        btnModify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {//EditAuthDets();
                Runnable r = new Runnable() {
                    @Override
                    public void run() {
                        EditAuthDets(cont);
                    }
                };
                r.run();
            }
        });


    }

    Context getContextOfThis(Context cont){
        return  cont;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }


    private ProfileAsync profileAsync;

    @Override
    public void finish() {
        super.finish();
        if (profileAsync != null && !profileAsync.isCancelled()) {
            profileAsync.cancel(true);
        }
        changeStateButton(false);
    }

    void startVPN(Boolean forceAuth) {
        //btnConnect.setMode(ActionProcessButton.Mode.ENDLESS);
        //btnConnect.setProgress(1);
        mainBar.setIndeterminateMode(true);

        mainButt.setBackground(getResources().getDrawable(R.drawable.main_button_loading));

        try {
            ProfileManager pm = ProfileManager.getInstance(this);
            VpnProfile profile = pm.getProfileByName(Build.MODEL);//

            if(routeAllMode)
            {
                profile.mUseDefaultRoute = true;
            }
            else
            {
                profile.mUseDefaultRoute = false;
            }

            statusTxt.setText(getString(R.string.connecting));
            statusTxt.setTextColor(getResources().getColor(R.color.colorAccent));

            startVPNConnection(profile, forceAuth);
        } catch (Exception ex) {
            App.isStart = false;
        }
    }

    void stopVPN() {
        stopVPNConnection();
        //tnConnect.setMode(ActionProcessButton.Mode.ENDLESS);
        mainBar.setIndeterminateMode(false);
        mainBar.setProgress(0);
        mainButt.setBackground(getResources().getDrawable(R.drawable.main_button_off));
        //btnConnect.setProgress(0);
        //btnConnect.setText(getString(R.string.connect));

        mainButt.setBackground(getResources().getDrawable(R.drawable.main_button_off));

        statusTxt.setText(getString(R.string.state_disconnected));
        statusTxt.setTextColor(getResources().getColor(R.color.red_error));

        layoutSpeedMeter.setVisibility(View.INVISIBLE);
        layoutSpeedMeter.requestLayout();
    }


    // ------------- Functions Related to OpenVPN-------------
    public void startVPNConnection(VpnProfile vp, Boolean forceAuth) {
        Intent intent = new Intent(getApplicationContext(), LaunchVPN.class);
        intent.putExtra(LaunchVPN.EXTRA_KEY, vp.getUUID().toString());
        intent.putExtra("ForceAuthDialog", forceAuth);
        intent.setAction(Intent.ACTION_MAIN);
        startActivity(intent);
    }
    public void stopVPNConnection() {
        ProfileManager.setConntectedVpnProfileDisconnected(this);
        if (mService != null) {
            try {
                mService.stopVPN(false);
            } catch (RemoteException e) {
//                VpnStatus.logException(e);
            }
        }
    }

    @Override
    public void updateByteCount(long ins, long outs, long diffIns, long diffOuts) {
        final long diffIn = diffIns;
        final long diffOut = diffOuts;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textDownload.setText(String.format("↓: %s", humanReadableByteCount(diffIn / OpenVPNManagement.mBytecountInterval, true, getResources())));
                textUpload.setText(String.format("↑: %s", humanReadableByteCount(diffOut / OpenVPNManagement.mBytecountInterval, true, getResources())));
            }
        });
    }

    void setConnected() {
        changeStateButton(true);
    }

    void changeStateButton(Boolean state) {
        if (state) {
            //btnConnect.setMode(ActionProcessButton.Mode.ENDLESS);
            //btnConnect.setProgress(0);
            mainBar.setIndeterminateMode(false);
            mainBar.setProgress(0);

            mainButt.setBackground(getResources().getDrawable(R.drawable.main_button_on));

            statusTxt.setText(getString(R.string.connected));
            statusTxt.setTextColor(getResources().getColor(R.color.green_complete));

            layoutSpeedMeter.setVisibility(View.VISIBLE);
            layoutSpeedMeter.requestLayout();
        } else {
            //btnConnect.setMode(ActionProcessButton.Mode.ENDLESS);
            //btnConnect.setProgress(0);
            mainBar.setIndeterminateMode(false);
            mainBar.setProgress(0);

            mainButt.setBackground(getResources().getDrawable(R.drawable.main_button_off));

            statusTxt.setText(getString(R.string.state_disconnected));
            statusTxt.setTextColor(getResources().getColor(R.color.red_error));

            layoutSpeedMeter.setVisibility(View.INVISIBLE);
            layoutSpeedMeter.requestLayout();
        }
    }

    @Override
    public void updateState(final String state, String logmessage, int localizedResId, ConnectionStatus level) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (state.equals("CONNECTED")) {
                    App.isStart = true;
                    setConnected();
                    layoutSpeedMeter.setVisibility(View.VISIBLE);
                    layoutSpeedMeter.requestLayout();
                } else {
                    layoutSpeedMeter.setVisibility(View.INVISIBLE);
                    layoutSpeedMeter.requestLayout();
                }
                if(state.equals("USER_AUTH_CANCELLED"))
                {
                    stopVPN();
                    App.isStart = false;
                }
                if (state.equals("AUTH_FAILED")) {
                    Toast.makeText(getApplicationContext(), "", Toast.LENGTH_SHORT).show();

                    startVPN(true);
                    App.isStart = true;

                    //
                }
            }
        });
    }

    @Override
    public void setConnectedVPN(String uuid) {
    }
}

package nl.tue.robotseverywhere.personalassistant;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import nl.tue.robotseverywhere.personalassistant.adapters.ListMessagesAdapter;
import pma.PersonalMessagingAssistant;
import pma.chatparsers.MessageParser;
import pma.contact.Contact;
import pma.message.Message;

import static java.security.AccessController.getContext;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private PAHandler paHandler;

    private ListMessagesAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private Contact myContact = new Contact("You", "You");

    private View.OnClickListener fabProcessListener;
    private View.OnClickListener fabSendMessageListener;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final Context context = this;

        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        final EditText messageInput = findViewById(R.id.new_message_input);

        fabProcessListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                fab.setEnabled(false);
                fab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorProcessing, null)));
                fab.setImageResource(R.drawable.ic_sync);

                RotateAnimation rotate = new RotateAnimation(0, 360, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
                rotate.setDuration(900);
                rotate.setRepeatCount(Animation.INFINITE);
                fab.startAnimation(rotate);

                final long startTime = System.nanoTime();

                paHandler.process(new PAHandler.ProcessDoneListener() {
                    @Override
                    public void processDone() {
                        fab.setEnabled(true);
                        fab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorAccent, null)));
                        fab.setImageResource(R.drawable.ic_assistant);

                        fab.clearAnimation();

                        Snackbar.make(findViewById(R.id.message_list), String.format(Locale.getDefault(), "Processing took %.2fs", (System.nanoTime() - startTime) * 1. / 1000000000),
                                Snackbar.LENGTH_LONG)
                                .show();
                    }
                });
            }
        };

        fabSendMessageListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Message m = new Message(messageInput.getText().toString(), new Date().getTime(), myContact);
                paHandler.addMessage(m);
                messageInput.setText("");
            }
        };

        fab.setOnClickListener(fabProcessListener);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        Menu menu = navigationView.getMenu();
        Switch spamSwitch = (Switch) menu.findItem(R.id.nav_setting_hide_spam).getActionView().findViewById(R.id.drawer_switch);
        spamSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Switch s = (Switch) v;
                mAdapter.setHideSpam(s.isChecked());
            }
        });

        Switch whatsappSwitch = (Switch) menu.findItem(R.id.nav_setting_process_whatsapp).getActionView().findViewById(R.id.drawer_switch);
        whatsappSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Switch s = (Switch) v;

                if (s.isChecked()) {
                    ComponentName cn = new ComponentName(context, NotificationListener.class);
                    String flat = Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners");
                    final boolean enabled = flat != null && flat.contains(cn.flattenToString());

                    if (!enabled) {
                        Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                        startActivity(intent);
                    }
                }

                SharedPreferences sharedPref = getSharedPreferences("pa-prefs", Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean("process_whatsapp", s.isChecked());
                editor.apply();
            }
        });
        SharedPreferences sharedPref = getSharedPreferences("pa-prefs", Context.MODE_PRIVATE);
        boolean processWhatsapp = sharedPref.getBoolean("process_whatsapp", false);
        whatsappSwitch.setChecked(processWhatsapp);


        RecyclerView recyclerView = (RecyclerView) findViewById(R.id.message_list);

        mLayoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new ListMessagesAdapter();
        recyclerView.setAdapter(mAdapter);

        mAdapter.setHideSpam(spamSwitch.isChecked());

        paHandler = new PAHandler(this, mAdapter);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel notificationChannel = new NotificationChannel("channel01", "my_channel", importance);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.enableVibration(true);
            notificationChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
            notificationManager.createNotificationChannel(notificationChannel);
        }


        messageInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE || event != null &&
                        event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                    if (event == null || !event.isShiftPressed()) {

                        if (v.getText().length() > 0) {

                            Log.d("TESTTAG", v.getText().toString());

                            if (v.getText().toString().equals("test")) {
                                Log.d("TESTTAG","sending notification");

                                NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, "channel01");
                                mBuilder.setContentTitle("My notification");
                                mBuilder.setContentText("Notification listener example");
                                mBuilder.setTicker("ticker");
                                mBuilder.setSmallIcon(R.drawable.ic_assistant);
                                mBuilder.setAutoCancel(true);

                                NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                                notificationManager.notify((int)(System.currentTimeMillis()/1000), mBuilder.build());
                            }

                            Message m = new Message(v.getText().toString(), new Date().getTime(), myContact);
                            paHandler.addMessage(m);
                            v.setText("");
                        }

                        return true;
                    }
                }
                return false;
            }
        });

        messageInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() != 0) {
                    fab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorSendMessage, null)));
                    fab.setImageResource(R.drawable.ic_menu_send);
                    fab.setOnClickListener(fabSendMessageListener);
                } else {
                    fab.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.colorAccent, null)));
                    fab.setImageResource(R.drawable.ic_assistant);
                    fab.setOnClickListener(fabProcessListener);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_load_small) {
            try {
                paHandler.load(getAssets().open("chats/jodi-linear.txt"));
            } catch (ParseException | IOException e) {
                e.printStackTrace();
            }
        } else if (id == R.id.nav_load_small2) {
            try {
                paHandler.load(getAssets().open("chats/jodi-huis.txt"));
            } catch (ParseException | IOException e) {
                e.printStackTrace();
            }
        } else if (id == R.id.nav_load_large) {
            try {
                paHandler.load(getAssets().open("chats/emre-es.txt"));
            } catch (ParseException | IOException e) {
                e.printStackTrace();
            }
        } else if (id == R.id.nav_load_large2) {
            try {
                paHandler.load(getAssets().open("chats/jodi-eng.txt"));
            } catch (ParseException | IOException e) {
                e.printStackTrace();
            }
        } else if (id == R.id.nav_load_clear) {
            paHandler.clear();
        } else if (id == R.id.nav_setting_hide_spam) {
            return false;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}

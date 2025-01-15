package com.kenvix.rconmanager.ui.connection;

import static androidx.preference.PreferenceManager.getDefaultSharedPreferences;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
/*import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;*/
import android.os.Bundle;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;

import com.kenvix.rconmanager.ApplicationEnvironment;
import com.kenvix.rconmanager.DefaultPreferences;
import com.kenvix.rconmanager.R;
import com.kenvix.rconmanager.database.dao.ConnectionsModel;
import com.kenvix.rconmanager.database.dao.QuickCommandModel;
import com.kenvix.rconmanager.meta.QuickCommand;
import com.kenvix.rconmanager.rcon.meta.RconServer;
import com.kenvix.rconmanager.rcon.protocol.RconConnect;
import com.kenvix.rconmanager.ui.base.BaseActivity;
import com.kenvix.rconmanager.ui.main.MainActivity;
import com.kenvix.utils.annotation.ViewAutoLoad;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ConnectionActivity extends BaseActivity {
    public static final String ExtraRconServer = "rcon_server";
    public static final String ExtraPreFilledCommandText = "rcon_prefill_command";
    public static final String ExtraPreFilledCommandResultAreaText = "rcon_prefill_result_area";
    public static final String ExtraCommandHistory = "rcon_command_history";

    private RconServer rconServer;
    private RconConnect rconConnect = null;
    private int historyPosition = 0;
    private boolean allowRunCommand = false;
    private ArrayList<String> commandHistory = new ArrayList<>();
    private String preFilledCommandText="";
    private String preFilledCommandResultAreaText;
    private List<QuickCommand> quickCommands = null;
    private String[] quickCommandNames = null;

    private boolean errorRaised = false;

    private Handler handler;
    private ConnectionActivity connectionActivity;

    private ConnectionsModel connectionsModel;

    private static final String TAG =ConnectionActivity.class.getName();

    @ViewAutoLoad public Button connectionCommandPrev;
    @ViewAutoLoad public Button connectionCommandNext;
    @ViewAutoLoad public Button connectionCommandRun;
    @ViewAutoLoad public Button connectionCommandHistory;
    @ViewAutoLoad public Button connectionQuickCommand;
    @ViewAutoLoad public TextView connectionCommandArea;
    @ViewAutoLoad public EditText connectionCommandText;
    @ViewAutoLoad public Toolbar connectionToolbar;
    @ViewAutoLoad public ScrollView connectionScroll;
    @ViewAutoLoad public LinearLayout connectionCommandLayout;

    @Override
    protected void onInitialize(Bundle savedInstanceState) {
        try {
            commandHistory = new ArrayList<>();
            quickCommands = getQuickCommands();
            connectionsModel = new ConnectionsModel(this);
            preFilledCommandResultAreaText=getString(R.string.command_result_area);

            // If we have a saved state then we can restore it now
            if (savedInstanceState != null) {
                rconServer = savedInstanceState.getParcelable(ExtraRconServer);
                preFilledCommandText = savedInstanceState.getString(ExtraPreFilledCommandText);
                preFilledCommandResultAreaText =savedInstanceState.getString(ExtraPreFilledCommandResultAreaText);
                if (savedInstanceState.containsKey(ExtraCommandHistory))
                    commandHistory=savedInstanceState.getStringArrayList(ExtraCommandHistory);
            } else {

                Intent intent = getIntent();


                rconServer = intent.getParcelableExtra(ExtraRconServer);

                if (rconServer == null) {
                    Uri uriCall = intent.getData();

                    if (uriCall == null)
                        throw new IllegalArgumentException("Either ExtraRconServer or URI CAN'T be null");

                    String host = uriCall.getHost();
                    int port = uriCall.getPort();

                    if (host == null || host.isEmpty() || port < 1 || port > 65535)
                        throw new IllegalArgumentException("Illegal host or port");

                    String password = (uriCall.getAuthority() == null || uriCall.getAuthority().isEmpty()) ? "" : uriCall.getAuthority();
                    String name = uriCall.getQueryParameter(ApplicationEnvironment.getRconURINameParamKey());

                    if (name == null || name.isEmpty())
                        name = getString(R.string.title_unnamed);

                    rconServer = new RconServer(name, host, port, password);

                } else{
                    Log.d(TAG,"rescued activity from intent");
                }
                getConnectionData(rconServer.getSid());

            }

            connectionToolbar=findViewById(R.id.connection_toolbar);
            connectionToolbar.setTitle(rconServer.getName());
            setSupportActionBar(connectionToolbar);

            if(preFilledCommandText != null)
                connectionCommandText.setText(preFilledCommandText);

            if(preFilledCommandResultAreaText != null) {
                connectionCommandArea.setText(preFilledCommandResultAreaText);
                scrollCommandAreaToBottom();
                Log.d(TAG,"rescued activity from intent");
            }

            connectionCommandPrev.setOnClickListener(this::onPreviousCommandButtonClick);
            connectionCommandNext.setOnClickListener(this::onNextCommandButtonClick);
            connectionQuickCommand.setOnClickListener(this::onQuickCommandButtonClick);

            //connectionCommandArea.setText("");
            connectionCommandArea.setTextSize(Integer.parseInt(Objects.requireNonNull(getPreferences().getString(DefaultPreferences.KeyTerminalTextSize, DefaultPreferences.DefaultTerminalTextSize))));
            connectionCommandText.setImeActionLabel(getString(R.string.action_run), KeyEvent.KEYCODE_ENTER);
            connectionCommandText.setOnKeyListener((view, keyCode, event) -> {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    runCommand();
                    return true;
                }
                return false;
            });

            // this makes the connectionCommandArea lose focus, etc. when showing keyboard in portrait mode
            //connectionScroll.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> scrollCommandAreaToBottom());


            connectionActivity = this;

            // Create the Handler object for running repeating task (on the main thread by default)
            handler = new Handler();

        } catch (Exception ex) {
            exceptionToastPrompt(ex);
            raiseErrorFlag();
            finish();
        }
    }

    private void onQuickCommandButtonClick(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        List<QuickCommand> quickCommands = getQuickCommands();
        builder.setItems(quickCommandNames, ((dialog, which) -> runCommand(quickCommands.get(which).getValue())));
        builder.create().show();
    }

    public void onRconConnectionEstablished(RconConnect connect) {
        rconConnect = connect;
        setAllowRunCommand(true);
        connectionCommandArea.setTextIsSelectable(true);
        // Start the initial keepAlive runnable task by posting through the handler
        handler.post(keepAliveRunnable);
    }


    // to keep connection alive, run a dummy command less than 45 seconds after last command
    // define the code block to be executed
    private final Runnable keepAliveRunnable = new Runnable() {
        @Override
        public void run() {
            // Execute keep alive dummy command
            RconCommanderAsyncTask rconCommanderAsyncTask = new RconCommanderAsyncTask(rconConnect, connectionActivity);
            String [] array = new String[1];
            array[0]="keep-alive"; //dummy command
            rconCommanderAsyncTask.executeAsync(array);
            //Log.d("Handlers", "Called on main thread");
            // Repeat  the same runnable code block again after another 42 seconds
            // 'this' is referencing the Runnable object
            handler.postDelayed(this, 42000);
        }
    };


    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(keepAliveRunnable);
        if(rconConnect != null && !errorRaised) {
            int notifyCode = makeConnectionNotification();
            Log.d("Rcon Connection", "Frontend paused, hashcode: " + notifyCode);
            rconConnect.disconnect();
            storeConnectionData();
        }

        rconConnect = null;
    }

    private void storeConnectionData() {
        // update database
        int res= connectionsModel.updateBySid(rconServer.getSid(),
                connectionCommandText.getText().toString(), connectionCommandArea.getText().toString(),
                commandHistory.toArray(new String[0]));
        if (res==0) { //if updating fails
            // add new entry
            connectionsModel.add(rconServer.getSid(),
                    connectionCommandText.getText().toString(), connectionCommandArea.getText().toString(),
                commandHistory.toArray(new String[0]));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        RconServerConnectorAsyncTask rconServerConnectorAsyncTask = new RconServerConnectorAsyncTask(this);
        //rconServerConnectorAsyncTask.execute();
        rconServerConnectorAsyncTask.executeAsync();

        //if(rconConnect != null && !errorRaised) {
        if(rconServer != null && !errorRaised) {
            int notifyCode = rconServer.hashCode();
            Log.d("Rcon Connection", "Frontend started, hashcode: " + notifyCode);
            cleanConnectionNotification(notifyCode);
        }
    }

    public void raiseErrorFlag() {
        this.errorRaised = true;
        setResult(RESULT_CANCELED);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_connection, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.connection_clean_command_result_area:
                preFilledCommandResultAreaText=getString(R.string.command_result_area);
                connectionCommandArea.setText(preFilledCommandResultAreaText);
                return true;

            case R.id.connection_exit:
                exit();
                return true;

            case R.id.connection_back:
                finish();
                return true;

            case R.id.connection_clean_command_history:
                historyPosition = 0;
                commandHistory.clear();
                return true;

            case R.id.connection_connection_info:
                alertDialog(getString(R.string.prompt_connection_info, rconServer.getName(), rconServer.getHost(), rconServer.getPort()),
                        getString(R.string.title_connection_info), null);
                return true;

        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable(ExtraRconServer, rconServer);
        outState.putString(ExtraPreFilledCommandText, connectionCommandText.getText().toString());
        outState.putString(ExtraPreFilledCommandResultAreaText, connectionCommandArea.getText().toString());
        outState.putStringArrayList(ExtraCommandHistory, commandHistory);
    }

    @Override
    protected int getBaseLayout() {
        return R.layout.activity_connection;
    }

    @Override
    protected int getBaseContainer() {
        return R.id.connection_container;
    }

    public RconServer getRconServer() {
        return rconServer;
    }

    private void cleanConnectionNotification(int notifyCode) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(ApplicationEnvironment.NotificationChannelID.RconConnection, notifyCode);
    }

    private int makeConnectionNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        int notifyCode = rconServer.hashCode();

        Intent openActivityIntent = new Intent(this, ConnectionActivity.class);
        openActivityIntent.putExtra(ExtraRconServer, rconServer);
        //openActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        openActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        // To start an activity that includes a back stack of activities:
        // Create the TaskStackBuilder and add the intent, which inflates the back stack.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addNextIntentWithParentStack(openActivityIntent);

        //PendingIntent openActivityPendingIntent = PendingIntent.getActivity(this, 0, openActivityIntent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        // ...and call stackBuilder.getPendingIntent instead of PendingIntent.getActivity
        PendingIntent openActivityPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);


        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, ApplicationEnvironment.NotificationChannelID.RconConnection)
                .setContentTitle(getString(R.string.prompt_connectied_to) + rconServer.getName())
                .setContentText(rconServer.getHostAndPort())
                .setContentIntent(openActivityPendingIntent)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setSmallIcon(R.drawable.ic_server_3)
                .setWhen(System.currentTimeMillis())
                .setPriority(NotificationCompat.PRIORITY_LOW);

        Notification notification = notificationBuilder.build();

        notification.flags = Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(ApplicationEnvironment.NotificationChannelID.RconConnection, getString(R.string.title_connection), NotificationManager.IMPORTANCE_LOW);
            notificationChannel.setDescription(getString(R.string.title_connection));

            notificationManager.createNotificationChannel(notificationChannel);
        }

        notificationManager.notify(ApplicationEnvironment.NotificationChannelID.RconConnection, notifyCode, notification);

        return notifyCode;
    }

    public SpannableStringBuilder getCommandPrompt() {
        String commandPromptString;

        // Check if the SharedPreferences exist and are valid
        //SharedPreferences sharedPreferences = connectionActivity.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences sharedPreferences = getDefaultSharedPreferences(this);
        if (sharedPreferences != null) {
            commandPromptString = sharedPreferences.getString(DefaultPreferences.KeyCommandPrompt, getString(R.string.result_prompt));
        } else {
            // Handle the case where SharedPreferences are null
            // (e.g., log an error, use a default value)
            commandPromptString = getString(R.string.result_prompt);
            Log.e(TAG, "SharedPreferences is null");
        }
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(commandPromptString);
        //ForegroundColorSpan colorPrimaryDark = new ForegroundColorSpan(getColor(R.color.colorPrimaryDark));
        // Get the primary text color of the theme
        TypedValue typedValue = new TypedValue();
        Resources.Theme theme = this.getTheme();
        theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
        TypedArray arr =
                this.obtainStyledAttributes(typedValue.data, new int[]{
                        android.R.attr.textColorPrimary});
        int primaryColor = arr.getColor(0, -1);
        ForegroundColorSpan colorPrimaryDark = new ForegroundColorSpan(primaryColor);
        arr.recycle();

        assert commandPromptString != null;
        spannableStringBuilder.setSpan(colorPrimaryDark, 0, commandPromptString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return spannableStringBuilder;
    }


    public void onRunButtonClick(View view) {
        runCommand();
    }

    private void runCommand() {
        runCommand(connectionCommandText.getText().toString());
        connectionCommandText.setText("");
        historyPosition = 0;
    }

    private void runCommand(String commandText) {
        if(isAllowRunCommand()) {
            if(!commandText.isEmpty()) {
                RconCommanderAsyncTask rconCommanderAsyncTask = new RconCommanderAsyncTask(rconConnect, this);
                appendCommandEcho(commandText);
                pushCommandHistory(commandText);
                //rconCommanderAsyncTask.execute(commandText);
                rconCommanderAsyncTask.executeAsync(commandText);
                //Log.d(TAG, "runCommand: " + res);
            }
        }
    }

    private void scrollCommandAreaToBottom() {
        connectionScroll.fullScroll(View.FOCUS_DOWN);
    }

    public void appendCommandEcho(String command) {
        connectionCommandArea.append(getCommandPrompt());
        connectionCommandArea.append(command);
        connectionCommandArea.append("\n");
        scrollCommandAreaToBottom();
    }

    @SuppressLint("SetTextI18n")
    public void appendCommandResult(String result) {
        connectionCommandArea.append(result + "\n");
        scrollCommandAreaToBottom();
    }

    public void pushCommandHistory(String command) {
        commandHistory.add(command);
    }

    public void onHistoryButtonClick(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String[] stringArray = commandHistory.toArray(new String[0]);
        builder.setItems(stringArray, ((dialog, which) -> connectionCommandText.append(stringArray[which])));
        builder.create().show();
    }

    public void onPreviousCommandButtonClick(View view) {
        try {
            if(commandHistory.size() > historyPosition) {
                historyPosition++;
                connectionCommandText.setText(commandHistory.get(commandHistory.size() - historyPosition));
            }
        } catch (RuntimeException ex) {
            exceptionSnackbarPrompt(ex);
            ex.printStackTrace();
        }
    }

    public void onNextCommandButtonClick(View view) {
        try {
            if(historyPosition > 0) {
                historyPosition--;

                if(historyPosition > 0)
                    connectionCommandText.setText(commandHistory.get(commandHistory.size() - historyPosition));
                else
                    connectionCommandText.setText("");
            }
        } catch (RuntimeException ex) {
            exceptionSnackbarPrompt(ex);
            ex.printStackTrace();
        }
    }

    public void exit() {
        try {
            if(rconConnect != null) {
                rconConnect.disconnect();
                rconConnect = null;
                connectionsModel.deleteBySid(rconServer.getSid());
            }
        } catch (RuntimeException ex) {
            Log.i("Rcon Connection", "Stop connection failed: " + ex.getMessage());
            ex.printStackTrace();
        }

        setResult(RESULT_OK);
        Log.d("Rcon Connection", "Finishing");
        finish();
    }

    public static void startActivity(Activity activity, RconServer rconServer) {
        Intent intent = new Intent(activity, ConnectionActivity.class);
        intent.putExtra(ConnectionActivity.ExtraRconServer, rconServer);
        Log.d("Connection Activity","starting activity using intent");
        activity.startActivity(intent);
    }

    public boolean isAllowRunCommand() {
        return allowRunCommand;
    }

    public void setAllowRunCommand(boolean allowRunCommand) {
        this.allowRunCommand = allowRunCommand;
        connectionCommandRun.setEnabled(allowRunCommand);
    }

    public List<QuickCommand> getQuickCommands() {
        if(quickCommands == null) {
            QuickCommandModel quickCommandModel = new QuickCommandModel(this);
            quickCommands = quickCommandModel.getAllAsList();
            quickCommandNames = new String[quickCommands.size()];

            for (int i = 0; i < quickCommands.size(); i++)
                quickCommandNames[i] = quickCommands.get(i).getName();
        }

        return quickCommands;
    }

    private void getConnectionData(int sid){
        try (Cursor connectionCursor = connectionsModel.getBySid(sid)) {
            if (!connectionCursor.moveToFirst()) {
                // Handle the case where no data is found
                return;
            }
            int id=connectionCursor.getInt(connectionCursor.getColumnIndexOrThrow(ConnectionsModel.FieldServerID));
            preFilledCommandText=connectionCursor.getString(connectionCursor.getColumnIndexOrThrow(ConnectionsModel.FieldPreFilledCommand));
            preFilledCommandResultAreaText=connectionCursor.getString(connectionCursor.getColumnIndexOrThrow(ConnectionsModel.FieldResult));
            String commandHistoryString=connectionCursor.getString(connectionCursor.getColumnIndexOrThrow(ConnectionsModel.FieldHistory));
            commandHistory=connectionsModel.decodeToStringArray(commandHistoryString);

        } catch (Exception ex) {
            toast(getString(R.string.error_unable_load_connections) + ex.getLocalizedMessage());
            ex.printStackTrace();
        }
    }

    public void makeBackConfirm() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setMessage(getString(R.string.prompt_confirm_disconnect))
                .setNegativeButton(getString(R.string.action_exit), (dialog, which) -> exit())
                //.setOnCancelListener(dialog -> exit())
                .setPositiveButton(getString(R.string.action_run_in_background), (dialog, which) -> {
                    finish();
                });

        builder.show();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_BACK){
            makeBackConfirm();
        }
        return false;
    }
}

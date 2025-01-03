package com.kenvix.rconmanager.ui.connection;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.DialogInterface;

import com.kenvix.rconmanager.R;
import com.kenvix.rconmanager.rcon.exception.IllegalAuthorizationException;
import com.kenvix.rconmanager.rcon.protocol.RconConnect;
import com.kenvix.rconmanager.ui.base.BaseAsyncTask;

class RconServerConnectorAsyncTask extends BaseAsyncTask<Void, Void, RconConnect> {
    private ProgressDialog progressDialog;

    //private final WeakReference<ConnectionActivity> activityWeakReference;
    private final ConnectionActivity connectionActivity;

    RconServerConnectorAsyncTask(ConnectionActivity activity) {
        //this.activityWeakReference = new WeakReference<>(activity);
        this.connectionActivity = activity;
    }

    @Override
    //public RconConnect doInBackground(Void... voids) {
    public RconConnect doInBackground(Void... voids) {
        //RconConnect connect = new RconConnect(activityWeakReference.get().getRconServer());
        RconConnect connect = new RconConnect(connectionActivity.getRconServer());

        try {
            connect.connect();
        } catch (RuntimeException ex) {
            setException(ex);
        }

        return connect;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        //ConnectionActivity activity = activityWeakReference.get();
        ConnectionActivity activity = connectionActivity;

        progressDialog = new ProgressDialog(activity);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);

        progressDialog.setMessage(activity.getString(R.string.prompt_connecting, activity.getRconServer().getName(), activity.getRconServer().getHostAndPort()));
        progressDialog.setCancelable(true);
        progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, activity.getString(R.string.action_cancel), ((dialog, which) -> this.cancel(true)));
        progressDialog.setOnCancelListener(dialog -> this.cancel(true));
        progressDialog.show();
    }

    @Override
    protected void onPostExecute(RconConnect rconConnect) {
        super.onPostExecute(rconConnect);
        //ConnectionActivity activity =  activityWeakReference.get();
        ConnectionActivity activity = connectionActivity;

        if(progressDialog.isShowing())
            progressDialog.dismiss();

        if(rconConnect == null)
            setException(new IllegalStateException("Illegal RconConnect State <null>"));

        if(getException() != null) {
            String detail;

            if(getException() instanceof IllegalAuthorizationException) {
                detail = activity.getString(R.string.error_incorrect_password);
            } else {
                detail = getException().toString();
            }

            activity.raiseErrorFlag();
            activity.alertDialog(activity.getString(R.string.prompt_connection_failed, activity.getRconServer().getHostAndPort(), detail), result -> exitForError());
        }

        activity.onRconConnectionEstablished(rconConnect);
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        exitForError();
    }

    private void exitForError() {
        //ConnectionActivity activity = activityWeakReference.get();
        ConnectionActivity activity = connectionActivity;

        activity.setResult(Activity.RESULT_CANCELED);
        activity.finish();
    }
}
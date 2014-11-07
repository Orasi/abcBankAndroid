package abcbank.orasi.com.abcbankandroid;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.widget.EditText;
import android.widget.Toast;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.io.IOException;
import java.util.ArrayList;

public class SettingsActivity extends PreferenceActivity {


    private SharedPreferences preferences;
    private ListPreference user_reset;
    private CheckBoxPreference useCustom;
    private EditTextPreference customUrl;
    private String backendUrl;
    private String defaultBackend;

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        backendUrl = getIntent().getStringExtra("backend");
        defaultBackend = getIntent().getStringExtra("default_backend");
        new UpdateUserList().execute();
        addPreferencesFromResource(R.xml.preferences);
        user_reset = (ListPreference) findPreference("delete_user_id");
        customUrl = (EditTextPreference) findPreference("backend_server");
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        user_reset.setEnabled(false);

        //Disable setting url if not using a custom backend.
        if (!preferences.getBoolean("useCustomBackend", false)){
            customUrl.setEnabled(false);
            customUrl.setSelectable(false);
        }


        user_reset.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference preference, Object o) {
                preference.setEnabled(false);
                new DeleteUser().execute(o.toString());
                return false;
            }
        });

        customUrl.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener(){

            @Override
            public boolean onPreferenceChange(final  Preference preference, Object o){
                useCustom.setEnabled(false);
                useCustom.setSelectable(false);
                customUrl.setEnabled(false);
                customUrl.setSelectable(false);
                backendUrl = o.toString();
                new CheckCustomBackend().execute();

                return true;
            }

        });

        useCustom = (CheckBoxPreference) findPreference("useCustomBackend");
        useCustom.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(final Preference preference, Object o) {
                boolean value = (Boolean) o;
                if (value){
                    useCustom.setEnabled(false);
                    useCustom.setSelectable(false);
                    customUrl.setEnabled(false);
                    customUrl.setSelectable(false);

                    AlertDialog.Builder alert = new AlertDialog.Builder(SettingsActivity.this);

                    alert.setTitle("Set Backend URL");
                    alert.setMessage("Please Provide the URL to be used for the backend");

                    // Set an EditText view to get user input
                    final EditText input = new EditText(SettingsActivity.this);
                    input.setText(backendUrl);
                    alert.setView(input);

                    alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            backendUrl = input.getText().toString();
                            SharedPreferences.Editor edit = preferences.edit();
                            edit.putString("backend_server",backendUrl);
                            customUrl.setText(backendUrl);
                            edit.commit();
                            //backendUrl = preferences.getString("backend_server", "");
                            new CheckCustomBackend().execute();
                        }
                    });

                    alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            useCustom.setEnabled(true);
                            useCustom.setSelectable(true);
                            customUrl.setEnabled(true);
                            customUrl.setSelectable(true);
                        }
                    });

                    alert.show();

                }else{
                    backendUrl = defaultBackend;
                    customUrl.setEnabled(false);
                    user_reset.setEnabled(false);
                    new UpdateUserList().execute();
                }

                return true;
            }
        });


    }


    //Class operates on new Thread.
    //DoInBackground executes first and when it finishes it invokes onPostExecute
    private class UpdateUserList extends AsyncTask<String, Integer, String> {
        private String str_response;
        private JSONObject json;
        @Override
        //Is executed on end of DoInBackground method
        protected void onPostExecute(String response){
            ArrayList<String> names = new ArrayList<String>();
            ArrayList<String> ids = new ArrayList<String>();

           if (response.contains("error:")){
               //TODO: Deal with it
           }else{

               JSONArray jArray = null;
               try {
                   jArray = (JSONArray) new JSONTokener(response).nextValue();
               } catch (JSONException e) {
                   e.printStackTrace();
               }
               assert jArray != null;
               try {

                   JSONObject json = jArray.getJSONObject(0);
                   for(int i=0; i< jArray.length(); i++) {
                       names.add(jArray.getJSONObject(i).getString(("username")));
                       ids.add(jArray.getJSONObject(i).getString(("id")));
                   }
               } catch (JSONException e) {
                   e.printStackTrace();
               }
           }
            CharSequence[] namesArray = names.toArray(new CharSequence[names.size()]);
            CharSequence[] idsArray = ids.toArray(new CharSequence[ids.size()]);
            user_reset.setEntries(namesArray);
            user_reset.setEntryValues(idsArray);
            Toast.makeText(SettingsActivity.this, "User List Updated", Toast.LENGTH_SHORT).show();
            user_reset.setEnabled(true);
        }

        //Executed in Background on secondary thread
        protected String doInBackground(String... params){

            //Create connection with login server
            HttpClient httpclient = new DefaultHttpClient();
            //TODO: Move connection parameters to settings menu
            HttpGet httpGet = new HttpGet(backendUrl + "/users.json");

            try {
                // Execute HTTP Get Request
                HttpResponse response = httpclient.execute(httpGet);

                // Get Response String
                HttpEntity entity = response.getEntity();
                str_response = EntityUtils.toString(entity);
            } catch (ClientProtocolException e) {
                e.printStackTrace();
                str_response = "error:Error in HTTP Protocol";
            } catch (IOException e) {
                e.printStackTrace();
                str_response = "error:Connection Refused";
            } catch(Exception e){
                e.printStackTrace();
                str_response = "error:An Unexpected Error Occurred";
            }

            return str_response;
        }
    }
    private class DeleteUser extends AsyncTask<String, Integer, String> {
        private String str_response;
        private JSONObject json;
        @Override
        //Is executed on end of DoInBackground method
        protected void onPostExecute(String response){
           if (response.contains("error:")){
               Toast.makeText(SettingsActivity.this, response.replace("error:", ""), Toast.LENGTH_SHORT).show();
            }else{
               Toast.makeText(SettingsActivity.this, "User Reset Successfully", Toast.LENGTH_SHORT).show();
            }
            new UpdateUserList().execute();
        }

        //Executed in Background on secondary thread
        protected String doInBackground(String... params){

            //Create connection with login server
            HttpClient httpclient = new DefaultHttpClient();
            //TODO: Move connection parameters to settings menu
            HttpDelete httpDelete = new HttpDelete(backendUrl + "/users/" + params[0]);

            try {
                // Execute HTTP Get Request
                HttpResponse response = httpclient.execute(httpDelete);

                // Get Response String
                HttpEntity entity = response.getEntity();
                str_response = EntityUtils.toString(entity);
            } catch (ClientProtocolException e) {
                e.printStackTrace();
                str_response = "error:Error in HTTP Protocol";
            } catch (IOException e) {
                e.printStackTrace();
                str_response = "error:Connection Refused";
            } catch(Exception e){
                e.printStackTrace();
                str_response = "error:An Unexpected Error Occurred";
            }

            return str_response;
        }
    }
    private class CheckCustomBackend extends AsyncTask<String, Integer, String> {
        private String str_response;
        private JSONObject json;
        @Override
        //Is executed on end of DoInBackground method
        protected void onPostExecute(String response){
            useCustom.setEnabled(true);
            useCustom.setSelectable(true);
            customUrl.setEnabled(true);
            customUrl.setSelectable(true);
            if (response.contains("error:")){
                Toast.makeText(SettingsActivity.this, "Server Could Not Be Found", Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(SettingsActivity.this, "Server Found Successfully", Toast.LENGTH_SHORT).show();
                new UpdateUserList().execute();
            }

        }

        //Executed in Background on secondary thread
        protected String doInBackground(String... params){

            //Create connection with login server
            HttpClient httpclient = new DefaultHttpClient();
            //TODO: Move connection parameters to settings menu
            HttpGet httpGet = new HttpGet(backendUrl + "/users.json");

            try {
                // Execute HTTP Get Request
                HttpResponse response = httpclient.execute(httpGet);

                // Get Response String
                HttpEntity entity = response.getEntity();
                str_response = EntityUtils.toString(entity);
            } catch (ClientProtocolException e) {
                e.printStackTrace();
                str_response = "error:Error in HTTP Protocol";
            } catch (IOException e) {
                e.printStackTrace();
                str_response = "error:Connection Refused";
            } catch(Exception e){
                e.printStackTrace();
                str_response = "error:An Unexpected Error Occurred";
            }

            return str_response;
        }
    }
}

package abcbank.orasi.com.abcbankandroid;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class Login extends Activity {

    private ProgressBar spinner;
    private EditText username;
    private EditText password;
    private Button sign_in;
    private String backendUrl;
    private String defaultBackend = "http://abcbank.orasi.com";
    private SharedPreferences preferences;

    @Override
    //Invokes on creation of activity
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        setupUI(findViewById(R.id.loginView));

        //Load preferences
        preferences = PreferenceManager.getDefaultSharedPreferences(this);

        //Initialize Loading Spinner
        spinner = (ProgressBar) findViewById(R.id.progressBar1);
        spinner.setVisibility(View.GONE);

        //Sign In Button, Username text field, Password text field
        sign_in = (Button) findViewById(R.id.btnSingIn);
        username = (EditText) findViewById(R.id.etUserName);
        password = (EditText) findViewById(R.id.etPass);

        if (preferences.getBoolean("useCustomBackend", false)){
            backendUrl = preferences.getString("backend_server", defaultBackend);
            Toast.makeText(Login.this, "Custom Server Being User \n" + backendUrl, Toast.LENGTH_SHORT).show();
        }else{
            backendUrl = defaultBackend;
        }
        //Set On Click Listener for Sign on button to initiate sign in

        sign_in.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (preferences.getBoolean("useCustomBackend", false)){
                    backendUrl = preferences.getString("backend_server", defaultBackend);
                }else{
                    backendUrl = defaultBackend;
                }
                String str_username = username.getText().toString();
                String str_password = password.getText().toString();
                disableFields();
                new validate().execute(str_username, str_password);
            }
        });
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.login, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //Class operates on new Thread.
    //DoInBackground executes first and when it finishes it invokes onPostExecute
    private class validate extends AsyncTask<String, Integer, String>{
        private String str_response;

        @Override
        //Is executed on end of DoInBackground method
        protected void onPostExecute(String response){
            enableFields();
            //Do In Background will pass error message if needed.
            //Check for error message and display if found
            if (response.contains("error")){
                Toast.makeText(Login.this, response.replace("error:", ""), Toast.LENGTH_SHORT).show();
            }else {

                //Create JSON Object from response from server
                JSONObject json;
                CharSequence text;
                json = new JSONObject();
                try {
                    json = new JSONObject(response);
                } catch (JSONException e) {
                    e.printStackTrace();
                    //Additional error handling needed here
                }

                //Validate Response
                if (json.isNull("username") || json.isNull("id")) {
                    text = "Invalid Login Credentials";
                    enableFields();

                } else {

                    //If Valid login credentials Open Webview page
                    text = "Welcome, " + capitalizeName(getJSONString(json, "username")) + "!";
                    Intent intent = new Intent(Login.this, hybridView.class);
                    intent.putExtra("user_id", getJSONString(json, "id"));
                    intent.putExtra("backend", backendUrl);
                    Login.this.startActivity(intent);

                    Animation fade_out = AnimationUtils.loadAnimation(Login.this, R.anim.fade_out);
                    username.startAnimation(fade_out);
                    password.startAnimation(fade_out);
                    sign_in.startAnimation(fade_out);
                }

                //Show message for Login success/failure
                Context context = getApplicationContext();
                int duration = Toast.LENGTH_SHORT;
                Toast toast = Toast.makeText(context, text, duration);
                toast.show();
            }
        }

        //Executed in Background on secondary thread
        protected String doInBackground(String... params){

            //Create connection with login server
            HttpClient httpclient = new DefaultHttpClient();
            //TODO: Move connection parameters to settings menu
            HttpPost httppost = new HttpPost(backendUrl + "/login.json");

            try {
                // Add username and password to post request
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
                nameValuePairs.add(new BasicNameValuePair("[user][username]", params[0]));
                nameValuePairs.add(new BasicNameValuePair("[user][password]", params[1]));
                httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                // Execute HTTP Post Request
                HttpResponse response = httpclient.execute(httppost);

                // Get Response String
                HttpEntity entity = response.getEntity();
                str_response = EntityUtils.toString(entity);
                Thread.sleep(1000);
            } catch (ClientProtocolException e) {
                e.printStackTrace();
                str_response = "error:Error in HTTP Protocol";
            } catch (IOException e) {
                e.printStackTrace();
                str_response = "error:Connection Refused";
                // TODO Auto-generated catch block
            } catch(Exception e){
                e.printStackTrace();
                str_response = "error:An Unexpected Error Occurred";
            }

        return str_response;
        }
    }

    //Gets String from JSON Node
    private String getJSONString(JSONObject json, String string) {
        String returnValue;
        try {
            returnValue = json.getString(string);
        } catch (JSONException e) {
            e.printStackTrace();
            returnValue = "";
        }
        return returnValue;
    }

    //Simple capitalization of first letter in string
    private String capitalizeName(String name){
        return Character.toUpperCase(name.charAt(0)) + name.substring(1);
    }

    //Disables Username, Password, and Sign In buttons
    //Shows loading spinner
    private void disableFields(){
        spinner.setVisibility(View.VISIBLE);
        spinner.bringToFront();
        sign_in.setEnabled(false);
        username.setEnabled(false);
        username.setFocusable(false);
        password.setEnabled(false);
        password.setFocusable(false);
    }

    //Enables Username, Password, and Sign In button
    //Hides Loading Spinner
    private void enableFields(){
        spinner.setVisibility(View.GONE);
        sign_in.setEnabled(true);
        username.setEnabled(true);
        password.setEnabled(true);
        username.setFocusable(true);
        password.setFocusable(true);
    }
    public void openSettings(MenuItem item){
        Intent i = new Intent(this, SettingsActivity.class);
        i.putExtra("backend", backendUrl);
        i.putExtra("default_backend", defaultBackend);
        startActivity(i);
    }
    public void setupUI(View view) {

        //Set up touch listener for non-text box views to hide keyboard.
        if(!(view instanceof EditText)) {

            view.setOnTouchListener(new View.OnTouchListener() {

                public boolean onTouch(View v, MotionEvent event) {
                    hideSoftKeyboard(Login.this);
                    return false;
                }

            });
        }

        //If a layout container, iterate over children and seed recursion.
        if (view instanceof ViewGroup) {

            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {

                View innerView = ((ViewGroup) view).getChildAt(i);

                setupUI(innerView);
            }
        }
    }
    public static void hideSoftKeyboard(Activity activity) {
        InputMethodManager inputMethodManager = (InputMethodManager)  activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(activity.getCurrentFocus().getWindowToken(), 0);
    }
}

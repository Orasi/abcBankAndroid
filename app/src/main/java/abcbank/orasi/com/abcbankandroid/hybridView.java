package abcbank.orasi.com.abcbankandroid;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;




public class hybridView extends Activity {

    private WebView webView;
    private String user_id;
    private ProgressBar spinner;
    private String backendUrl;

    @Override
    //Initialize Activity
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hybrid_view);
        user_id = getIntent().getStringExtra("user_id");
        backendUrl = getIntent().getStringExtra("backend");
        //Set WebView Settings
        webView = (WebView) findViewById(R.id.webView);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        webView.loadUrl(backendUrl + "/users/" + user_id + "/hybrid");
        webView.setBackgroundColor(0x00000000);

        //Override WebView Client
        //This allows us to control where Links loads (default browser or inside webview)
        webView.setWebViewClient(new WebViewClient() {

            @Override
            //Sets all links to load inside webview instead of opening browser window
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }

            //This method is invoked when a page inside the webview is finished loading
            public void onPageFinished(WebView webView, String url){
                spinner.setVisibility(View.GONE);
            }

        });


        //Loading Spinner
        spinner = (ProgressBar) findViewById(R.id.progressBar1);


    }

    @Override
    //Overrides default behavior of back button.
    //While inside web view back will now try to go back a page
    //If there is no more pages available by going back it will do nothing
    //Back will not take you to login screen
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if ((keyCode == KeyEvent.KEYCODE_BACK)) {
            if (webView.canGoBack()) {
                webView.goBack();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.hybrid_view, menu);
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

    //Logs out out of the application and clears the BACK trace
    public void logout(MenuItem item){
        Intent i =  new Intent(this, Login.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(i);
        Toast.makeText(this, "Thank you for using abcBank", Toast.LENGTH_SHORT).show();
    }


}

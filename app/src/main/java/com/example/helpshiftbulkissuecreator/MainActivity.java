package com.example.helpshiftbulkissuecreator; // Replace with your package name

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

// import java.io.UnsupportedEncodingException; // Not strictly needed if using StandardCharsets
// import java.net.URLEncoder; // Not strictly needed if Volley handles param encoding
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "BulkIssueCreator";
    private static final int NUM_ISSUES_TO_CREATE = 100;
    private static final long DELAY_BETWEEN_REQUESTS_MS = 3000; // 3 seconds (from P1.3)
    private static final String HELPSHIFT_TAG = "minimal-bulk-test"; // From P1.1

    private Button btnCreateIssues;
    private TextView tvStatus;
    private RequestQueue requestQueue;
    private ExecutorService executorService;
    private Handler mainThreadHandler;

    // Credentials from BuildConfig (populated by installCreds.gradle via build.gradle.kts)
    private static final String DOMAIN_NAME = BuildConfig.HELPSHIFT_DOMAIN_NAME;
    // private static final String PLATFORM_ID = BuildConfig.HELPSHIFT_PLATFORM_ID; // May not be needed in URL for general issue creation
    private static final String API_KEY = BuildConfig.HELPSHIFT_API_KEY;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnCreateIssues = findViewById(R.id.btnCreateIssues);
        tvStatus = findViewById(R.id.tvStatus);

        requestQueue = Volley.newRequestQueue(this);
        executorService = Executors.newSingleThreadExecutor();
        mainThreadHandler = new Handler(Looper.getMainLooper());

        btnCreateIssues.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startBulkIssueCreation();
            }
        });
    }

    private void updateStatus(final String message) {
        mainThreadHandler.post(() -> tvStatus.setText("Status: " + message));
        Log.d(TAG, "Status: " + message);
    }

    private void startBulkIssueCreation() {
        btnCreateIssues.setEnabled(false);
        updateStatus("Starting bulk issue creation...");

        executorService.execute(() -> {
            for (int i = 0; i < NUM_ISSUES_TO_CREATE; i++) {
                final int issueNumber = i + 1;
                mainThreadHandler.post(() -> updateStatus("Preparing issue " + issueNumber + "/" + NUM_ISSUES_TO_CREATE + "..."));

                // Construct API URL
                // The general issue creation endpoint. Consult Helpshift docs if platform_id needs to be in URL.
                String apiUrl = "https://api.helpshift.com/v1/" + DOMAIN_NAME + "/" + BuildConfig.HELPSHIFT_PLATFORM_ID + "/issues/";

                final Map<String, String> params = new HashMap<>();
                params.put("message-body", "Automated Test Issue #" + issueNumber + " for minimal plan. Timestamp: " + System.currentTimeMillis());
                params.put("email", "testuser" + issueNumber + "@example.com"); // Unique email per issue
                params.put("author-name", "Minimal Test User " + issueNumber);
                params.put("platform-type", "android"); // Or "web" if more generic

                // Add tags
                JSONArray tagsArray = new JSONArray();
                tagsArray.put(HELPSHIFT_TAG);
                params.put("tags", tagsArray.toString()); // Send as JSON array string

                // (Optional) Add CIF if defined in P1.1
                // JSONObject metaObject = new JSONObject();
                // try {
                //     JSONObject cifValueDetails = new JSONObject();
                //     cifValueDetails.put("type", "checkbox"); // Must match CIF type on dashboard
                //     cifValueDetails.put("value", "true");    // Checkbox value is "true" or "false" as string
                //     metaObject.put("is_bulk_test", cifValueDetails); // "is_bulk_test" is the CIF key
                //     params.put("meta", metaObject.toString()); // Send as JSON object string
                // } catch (JSONException e) {
                //    Log.e(TAG, "Error creating meta JSON for CIF", e);
                // }


                StringRequest stringRequest = new StringRequest(Request.Method.POST, apiUrl,
                        response -> {
                            // This is called on the main thread by Volley
                            Log.i(TAG, "Issue " + issueNumber + " created successfully. Response: " + response.substring(0, Math.min(response.length(), 100)));
                        },
                        error -> {
                            // This is called on the main thread by Volley
                            Log.e(TAG, "Error creating issue " + issueNumber + ": " + error.toString());
                            if (error.networkResponse != null) {
                                Log.e(TAG, "Error Status Code: " + error.networkResponse.statusCode);
                                try {
                                    String responseBody = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                                    Log.e(TAG, "Error Data: " + responseBody);
                                } catch (Exception e) { // Changed from UnsupportedEncodingException
                                    Log.e(TAG, "Error parsing error data", e);
                                }
                            }
                        }) {
                    @Override
                    protected Map<String, String> getParams() {
                        // Volley URL encodes parameters from this map by default when
                        // getBodyContentType() is application/x-www-form-urlencoded
                        return params;
                    }

                    @Override
                    public Map<String, String> getHeaders() throws AuthFailureError {
                        Map<String, String> headers = new HashMap<>();
                        // Basic Authentication: API Key as username, password can be blank or 'X'
                        String credentials = API_KEY + ":"; // Password can be empty or 'X' or any char
                        String auth = "Basic " + Base64.encodeToString(credentials.getBytes(StandardCharsets.UTF_8), Base64.NO_WRAP);
                        headers.put("Authorization", auth);
                        // Volley usually sets Content-Type based on getParams() and getBodyContentType().
                        // If explicit setting is needed:
                        // headers.put("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                        return headers;
                    }

                    // Ensure Volley sends data as form-urlencoded
                    @Override
                    public String getBodyContentType() {
                        return "application/x-www-form-urlencoded; charset=" + getParamsEncoding();
                    }
                };

                // Add the request to the RequestQueue.
                // This is asynchronous. The loop will pause due to Thread.sleep.
                requestQueue.add(stringRequest);
                Log.d(TAG, "Request for issue " + issueNumber + " added to queue.");

                if (i < NUM_ISSUES_TO_CREATE - 1) { // Don't sleep after the last issue
                    try {
                        Thread.sleep(DELAY_BETWEEN_REQUESTS_MS);
                    } catch (InterruptedException e) {
                        Log.e(TAG, "Delay interrupted", e);
                        Thread.currentThread().interrupt(); // Restore interrupted status
                        mainThreadHandler.post(() -> {
                            updateStatus("Process interrupted.");
                            btnCreateIssues.setEnabled(true);
                        });
                        return; // Exit the background task
                    }
                }
            } // End of for loop

            // This message will appear after all requests have been *queued* and delays *elapsed*.
            mainThreadHandler.post(() -> {
                updateStatus("Attempted to queue " + NUM_ISSUES_TO_CREATE + " issues. Check Logcat for individual success/failure.");
                btnCreateIssues.setEnabled(true);
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow(); // Attempt to stop ongoing tasks
        }
        if (requestQueue != null) {
            requestQueue.cancelAll(TAG); // Cancel Volley requests associated with this TAG
            requestQueue.stop();
        }
    }
}

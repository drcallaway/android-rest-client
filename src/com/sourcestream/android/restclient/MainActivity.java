package com.sourcestream.android.restclient;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Adapter;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NoHttpResponseException;
import org.apache.http.StatusLine;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.util.EntityUtils;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity
{
    private static final String REQUEST_GET = "GET";
    private static final String REQUEST_POST = "POST";
    private static final String REQUEST_PUT = "PUT";
    private static final String REQUEST_DELETE = "DELETE";
    private static final String REQUEST_HEAD = "HEAD";
    private static final String REQUEST_OPTIONS = "OPTIONS";
    private static final String REQUEST_TRACE = "TRACE";

    private static final String HEADER_CUSTOM = "Custom";
    private static final String HEADER_ACCEPT = "Accept";
    private static final String HEADER_AUTHORIZATION = "Authorization";
    private static final String HEADER_AUTHORIZATION_BASIC = "Authorization (Basic)";
    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String HEADER_COOKIE = "Cookie";
    private static final String HEADER_USER_AGENT = "User-Agent";
    private static final String HEADER_USER_AGENT_ANDROID = "User-Agent (Android)";
    private static final String HEADER_USER_AGENT_IPHONE = "User-Agent (iPhone)";
    private static final String HEADER_USER_AGENT_IPAD = "User-Agent (iPad)";

    private static final String USER_AGENT_ANDROID = "Mozilla/5.0 (Linux; U; Android 2.2.1; en-us; MB525 Build/3.4.2-107_JDN-9) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile Safari/533.1";
    private static final String USER_AGENT_IPHONE = "Mozilla/5.0 (iPhone; CPU iPhone OS 5_0_1 like Mac OS X) AppleWebKit/534.46 (KHTML, like Gecko) Version/5.1 Mobile/9A405 Safari/7534.48.3";
    private static final String USER_AGENT_IPAD = "Mozilla/5.0 (iPad; U; CPU OS 4_3_1 like Mac OS X; en-us) AppleWebKit/533.17.9 (KHTML, like Gecko) Version/5.0.2 Mobile/8G4 Safari/6533.18.5";

    private static final int TAB_REQUEST = 0;
    private static final int TAB_RESPONSE = 1;

    private static final int DIALOG_ADD_HEADER = 1;
    private static final int DIALOG_DELETE_HEADER = 2;
    private static final int DIALOG_SAVE = 3;
    private static final int DIALOG_LOAD = 4;
    private static final int DIALOG_DELETE = 5;
    private static final int DIALOG_ERROR = 6;
    private static final int DIALOG_TIMEOUT_ERROR = 7;
    private static final int DIALOG_NO_HOST_ERROR = 8;
    private static final int DIALOG_NO_RESPONSE_ERROR = 9;

    private List<HttpHeader> httpHeaders;
    private TabHost mTabHost;
    private RestClient restClient;
    private DatabaseHelper databaseHelper;
    private Button saveRequestButton;
    private Button saveHeaderButton;
    private boolean deleteHeadersOnClear = false;
    private boolean trustAllCerts = true;
    private int timeoutInSeconds = 10;
    private String urlPrefillValue = "http://";
    private String responseHeaderText = "";
    private String responseBodyText = "";

    private void setupTabHost()
    {
        mTabHost = (TabHost) findViewById(android.R.id.tabhost);
        mTabHost.setup();
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        setupTabHost();

        setupTab(R.id.requestTab, getResources().getString(R.string.request_tab));
        setupTab(R.id.responseTab, getResources().getString(R.string.response_tab));

        if (isOldDevice())
        {
            findViewById(R.id.responseHeaders).setVisibility(View.VISIBLE);
            findViewById(R.id.responseHeaders2).setVisibility(View.GONE);
            findViewById(R.id.responseBody).setVisibility(View.VISIBLE);
            findViewById(R.id.responseBody2).setVisibility(View.GONE);
        }
        else
        {
            findViewById(R.id.responseHeaders).setVisibility(View.GONE);
            findViewById(R.id.responseHeaders2).setVisibility(View.VISIBLE);
            findViewById(R.id.responseBody).setVisibility(View.GONE);
            findViewById(R.id.responseBody2).setVisibility(View.VISIBLE);
        }

        if (getLastNonConfigurationInstance() != null)
        {
            RotationStorage storage = (RotationStorage) getLastNonConfigurationInstance();

            httpHeaders = storage.httpHeaders;
            mTabHost.setCurrentTab(storage.currentTab);
            responseHeaderText = storage.headerText;
            responseBodyText = storage.bodyText;

            if (isOldDevice())
            {
                ((EditText)findViewById(R.id.responseHeaders)).setText(Html.fromHtml(responseHeaderText));
                ((EditText)findViewById(R.id.responseBody)).setText(responseBodyText);
            }
            else
            {
                ((TextView)findViewById(R.id.responseHeaders2)).setText(Html.fromHtml(responseHeaderText));
                ((TextView)findViewById(R.id.responseBody2)).setText(responseBodyText);
            }
        }
        else
        {
            httpHeaders = new ArrayList<HttpHeader>();
        }

        updateHttpHeaders();

        Spinner httpMethod = (Spinner) findViewById(R.id.httpMethodSpinner);
        httpMethod.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
        {
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l)
            {
                View bodySection = findViewById(R.id.bodySection);
                String value = (String) adapterView.getItemAtPosition(pos);

                if (REQUEST_POST.equals(value) || REQUEST_PUT.equals(value))
                {
                    bodySection.setVisibility(View.VISIBLE);
                }
                else
                {
                    bodySection.setVisibility(View.GONE);
                }
            }

            public void onNothingSelected(AdapterView<?> adapterView)
            {
            }
        });
        httpMethod.setOnTouchListener(new View.OnTouchListener()
        {
            public boolean onTouch(View view, MotionEvent motionEvent)
            {
                hideKeyboard();
                return false;
            }
        });

        removeFocusFromAllControls();

        databaseHelper = new DatabaseHelper(this);

        if (getIntent().getAction() != null && getIntent().getAction().equals(SplashActivity.ACTION_PROMPT_RATING))
        {
            getIntent().setAction(null);
            startActivity(new Intent(this, RateAppActivity.class));
        }

        mTabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener()
        {
            public void onTabChanged(String s)
            {
                removeFocusFromAllControls();
            }
        });
    }

    private void setupTab(int tabViewId, final String tag)
    {
        View tabview = createTabView(mTabHost.getContext(), tag);

        TabSpec setContent = mTabHost.newTabSpec(tag).setIndicator(tabview).setContent(tabViewId);
        mTabHost.addTab(setContent);
    }

    @Override
    public Object onRetainNonConfigurationInstance()
    {
        RotationStorage storage = new RotationStorage();
        storage.httpHeaders = httpHeaders;
        storage.currentTab = mTabHost.getCurrentTab();
        storage.headerText = responseHeaderText;
        storage.bodyText = responseBodyText;

        return storage;
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        processPrefs();

        TextView url = (TextView)findViewById(R.id.url);

        if (url.getText().toString().trim().equals(""))
        {
            url.setText(urlPrefillValue);
        }
    }

    private void processPrefs()
    {
        // Get the xml/preferences.xml preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        trustAllCerts = prefs.getBoolean("trustCerts", true);
        deleteHeadersOnClear = prefs.getBoolean("deleteHeaders", true);
        timeoutInSeconds = Integer.parseInt(prefs.getString("timeoutInSeconds", "10"));

        if (restClient == null)
        {
            restClient = new RestClient(trustAllCerts, timeoutInSeconds);
        }

        if (prefs.getBoolean("defaultHttps", false))
        {
            urlPrefillValue = "https://";
        }
        else
        {
            urlPrefillValue = "http://";
        }

        if (restClient.isTrustAllCerts() != trustAllCerts)
        {
            restClient.setTrustAllCerts(trustAllCerts);
        }

        if (restClient.getTimeoutInSeconds() != timeoutInSeconds)
        {
            restClient.setTimeoutInSeconds(timeoutInSeconds);
        }
    }

    private static View createTabView(final Context context, final String text)
    {
        View view = LayoutInflater.from(context).inflate(R.layout.tabs_bg, null);
        TextView tv = (TextView) view.findViewById(R.id.tabsText);
        tv.setText(text);
        tv.setTextAppearance(context, R.style.WrapWrapNormalText);
        return view;
    }

    private String getHttpMethod()
    {
        Spinner spinner = (Spinner) findViewById(R.id.httpMethodSpinner);
        return (String) spinner.getSelectedItem();
    }

    private void setHttpMethod(String method)
    {
        Spinner spinner = (Spinner) findViewById(R.id.httpMethodSpinner);
        spinner.setSelection(getIndexFromElement(spinner.getAdapter(), method));
    }

    private String getUrl()
    {
        TextView url = ((TextView) findViewById(R.id.url));
        return url.getText().toString();
    }

    private void setUrl(String url)
    {
        TextView tv = ((TextView) findViewById(R.id.url));
        tv.setText(url);
    }

    private String getBody()
    {
        TextView body = (TextView) findViewById(R.id.body);
        return body.getText().toString();
    }

    private void setBody(String body)
    {
        TextView tv = (TextView) findViewById(R.id.body);
        tv.setText(body);
    }

    public int getIndexFromElement(Adapter adapter, String element)
    {
        for (int i = 0; i < adapter.getCount(); i++)
        {
            if (adapter.getItem(i).equals(element))
            {
                return i;
            }
        }
        return 0;
    }

    private void updateHttpHeaders()
    {
        LinearLayout headerDisplay = (LinearLayout) findViewById(R.id.headers);
        headerDisplay.removeAllViews();

        for (HttpHeader httpHeader : httpHeaders)
        {
            StringBuilder header = new StringBuilder();
            header.append("<b>").append(httpHeader.getName()).append(":</b> ").append(httpHeader.getValue());

            TextView headerLine = new TextView(MainActivity.this);
            headerLine.setText(Html.fromHtml(header.toString()));
            headerLine.setTextAppearance(this, R.style.WrapWrapNormalText);

            headerDisplay.addView(headerLine);
        }

        View deleteHeaderButton = findViewById(R.id.headerDelete);
        if (httpHeaders.size() > 0)
        {
            deleteHeaderButton.setVisibility(View.VISIBLE);
        }
        else
        {
            deleteHeaderButton.setVisibility(View.GONE);
        }
    }

    @Override
    protected Dialog onCreateDialog(int id)
    {
        final AlertDialog dialog;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        switch (id)
        {
            case DIALOG_ADD_HEADER:
            {
                LayoutInflater inflater = getLayoutInflater();
                final View addHeaderView = inflater.inflate(R.layout.add_header, null);

                Spinner headerType = (Spinner) addHeaderView.findViewById(R.id.headerType);
                headerType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
                {
                    public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id)
                    {
                        EditText headerName = (EditText) addHeaderView.findViewById(R.id.headerName);
                        headerName.setText("");

                        TextView headerValue = (TextView)addHeaderView.findViewById(R.id.headerValue);
                        headerValue.setText("");

                        View headerNameRow = addHeaderView.findViewById(R.id.headerNameRow);
                        headerNameRow.setVisibility(View.GONE);

                        View authPasswordRow = addHeaderView.findViewById(R.id.authPasswordRow);
                        authPasswordRow.setVisibility(View.GONE);

                        TextView headerValueLabel = (TextView)addHeaderView.findViewById(R.id.headerValueLabel);
                        headerValueLabel.setText(R.string.header_value);

                        String itemSelected = adapterView.getItemAtPosition(pos).toString();

                        if (HEADER_CUSTOM.equals(itemSelected))
                        {
                            headerNameRow.setVisibility(View.VISIBLE);
                            headerName.requestFocus();
                        }
                        else
                        {
                            headerName.setText(itemSelected.split(" ")[0]);
                            headerValue.requestFocus();

                            if (HEADER_AUTHORIZATION_BASIC.equals(itemSelected))
                            {
                                authPasswordRow.setVisibility(View.VISIBLE);
                                headerValueLabel.setText(R.string.auth_username);
                                ((EditText)addHeaderView.findViewById(R.id.authPassword)).setText("");
                            }
                            else if (HEADER_USER_AGENT_ANDROID.equals(itemSelected))
                            {
                                headerValue.setText(USER_AGENT_ANDROID);
                            }
                            else if (HEADER_USER_AGENT_IPHONE.equals(itemSelected))
                            {
                                headerValue.setText(USER_AGENT_IPHONE);
                            }
                            else if (HEADER_USER_AGENT_IPAD.equals(itemSelected))
                            {
                                headerValue.setText(USER_AGENT_IPAD);
                            }
                        }
                    }

                    public void onNothingSelected(AdapterView<?> adapterView)
                    {
                    }
                });

                final EditText headerName = (EditText) addHeaderView.findViewById(R.id.headerName);
                final EditText headerValue = (EditText) addHeaderView.findViewById(R.id.headerValue);
                TextWatcher addHeaderTextWatcher = new TextWatcher()
                {
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2)
                    {
                    }

                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2)
                    {
                    }

                    public void afterTextChanged(Editable editable)
                    {
                        if (saveHeaderButton != null)
                        {
                            if (headerName.getText().toString().trim().equals("") || headerValue.getText().toString().trim().equals(""))
                            {
                                saveHeaderButton.setEnabled(false);
                            }
                            else
                            {
                                saveHeaderButton.setEnabled(true);
                            }
                        }
                    }
                };
                headerName.addTextChangedListener(addHeaderTextWatcher);
                headerValue.addTextChangedListener(addHeaderTextWatcher);

                builder.setTitle(getString(R.string.header_add_title))
                    .setIcon(0)
                    .setView(addHeaderView)
                    .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialogInterface, int i)
                        {
                            dialogInterface.dismiss();

                            EditText headerName = (EditText) addHeaderView.findViewById(R.id.headerName);
                            EditText headerValue = (EditText) addHeaderView.findViewById(R.id.headerValue);
                            String headerValueString;

                            if (addHeaderView.findViewById(R.id.authPasswordRow).getVisibility() == View.VISIBLE)
                            {
                                EditText password =(EditText) addHeaderView.findViewById(R.id.authPassword);
                                String credentials = headerValue.getText().toString().trim() + ":" + password.getText().toString().trim();
                                headerValueString = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
                            }
                            else
                            {
                                headerValueString = headerValue.getText().toString().trim();
                            }

                            httpHeaders.add(new HttpHeader(headerName.getText().toString().trim(), headerValueString));

                            updateHttpHeaders();
                        }
                    })
                    .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialogInterface, int i)
                        {
                            dialogInterface.cancel();
                        }
                    });

                dialog = builder.create();

                dialog.setOnShowListener(new DialogInterface.OnShowListener()
                {
                    public void onShow(DialogInterface dialogInterface)
                    {
                        EditText headerName = (EditText) dialog.findViewById(R.id.headerName);
                        EditText headerValue = (EditText) dialog.findViewById(R.id.headerValue);
                        saveHeaderButton = ((AlertDialog) dialogInterface).getButton(Dialog.BUTTON_POSITIVE);

                        if (headerName.getText().toString().trim().equals("") || headerValue.getText().toString().trim().equals(""))
                        {
                            saveHeaderButton.setEnabled(false);
                        }
                        else
                        {
                            saveHeaderButton.setEnabled(true);
                        }
                    }
                });

                break;
            }

            case DIALOG_DELETE_HEADER:
            {
                final String[] deleteHeaders = new String[httpHeaders.size()];

                int count = 0;
                for (HttpHeader header : httpHeaders)
                {
                    deleteHeaders[count++] = header.getName() + ": " + header.getValue();
                }

                builder.setTitle(getResources().getString(R.string.delete_header))
                    .setIcon(0)
                    .setItems(deleteHeaders, new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, final int item)
                        {
                            AlertDialog.Builder confirmBuilder = new AlertDialog.Builder(MainActivity.this);
                            confirmBuilder.setTitle(getResources().getString(R.string.confirm_delete_title))
                                .setIcon(0)
                                .setMessage(String.format(getResources().getString(R.string.confirm_delete_header), deleteHeaders[item]))
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
                                {
                                    public void onClick(DialogInterface dialogInterface, int i)
                                    {
                                        httpHeaders.remove(item);
                                        updateHttpHeaders();
                                    }
                                })
                                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
                                {
                                    public void onClick(DialogInterface dialogInterface, int i)
                                    {
                                        dialogInterface.cancel();
                                    }
                                })
                                .show();
                        }
                    });

                dialog = builder.create();

                break;
            }

            case DIALOG_SAVE:
            {
                LayoutInflater inflater = getLayoutInflater();
                final View saveRequestView = inflater.inflate(R.layout.save_request, null);

                builder.setIcon(0).setView(saveRequestView);

                final Spinner existingRequestsSpinner = (Spinner) saveRequestView.findViewById(R.id.updateExisting);
                final EditText requestName = (EditText) saveRequestView.findViewById(R.id.requestName);

                existingRequestsSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
                {
                    public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l)
                    {
                        if (existingRequestsSpinner.getSelectedItemPosition() < 1)
                        {
                            if (saveRequestButton != null)
                            {
                                saveRequestButton.setEnabled(false);
                            }

                            requestName.setFocusable(true);
                            requestName.setFocusableInTouchMode(true);
                            requestName.setEnabled(true);
                            requestName.requestFocus();
                        }
                        else
                        {
                            if (saveRequestButton != null)
                            {
                                saveRequestButton.setEnabled(true);
                            }

                            requestName.setFocusable(false);
                            requestName.setFocusableInTouchMode(false);
                            requestName.setEnabled(false);
                        }
                    }

                    public void onNothingSelected(AdapterView<?> adapterView)
                    {
                    }
                });

                TextWatcher textWatcher = new TextWatcher()
                {
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2)
                    {
                    }

                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2)
                    {
                    }

                    public void afterTextChanged(Editable editable)
                    {
                        if (saveRequestButton != null)
                        {
                            if (requestName.getText().toString().trim().equals(""))
                            {
                                saveRequestButton.setEnabled(false);
                                existingRequestsSpinner.setEnabled(true);
                            }
                            else
                            {
                                saveRequestButton.setEnabled(true);
                                existingRequestsSpinner.setEnabled(false);
                            }
                        }
                    }
                };
                requestName.addTextChangedListener(textWatcher);

                final long[] requestIds;

                SQLiteDatabase db = databaseHelper.getReadableDatabase();
                Cursor cursor = db.query(DatabaseHelper.RequestTable.TABLE_NAME,
                    new String[]{DatabaseHelper.RequestTable._ID, DatabaseHelper.RequestTable.COLUMN_NAME_NAME},
                    null, null, null, null, null);

                if (cursor.getCount() > 0)
                {
                    builder.setTitle(R.string.save_replace_request);

                    requestIds = new long[cursor.getCount() + 1];
                    String[] requestNames = new String[cursor.getCount() + 1];

                    requestNames[0] = getString(R.string.update_request);

                    int requestCount = 1;
                    cursor.moveToFirst();
                    while (!cursor.isAfterLast())
                    {
                        requestIds[requestCount] = cursor.getLong(0);
                        requestNames[requestCount] = cursor.getString(1);
                        requestCount++;
                        cursor.moveToNext();
                    }

                    ArrayAdapter<String> requestAdapter = new ArrayAdapter<String>(this,
                        android.R.layout.simple_spinner_item, requestNames);
                    requestAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    existingRequestsSpinner.setAdapter(requestAdapter);
                }
                else
                {
                    builder.setTitle(R.string.save_request);

                    requestIds = new long[0];
                    saveRequestView.findViewById(R.id.updateExistingSection).setVisibility(View.GONE);
                }

                builder.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int whichButton)
                    {
                        long requestId;
                        SQLiteDatabase db = databaseHelper.getWritableDatabase();
                        ContentValues values = new ContentValues();
                        values.put(DatabaseHelper.RequestTable.COLUMN_NAME_METHOD, getHttpMethod());
                        values.put(DatabaseHelper.RequestTable.COLUMN_NAME_URL, getUrl());
                        values.put(DatabaseHelper.RequestTable.COLUMN_NAME_BODY, getBody());

                        if (existingRequestsSpinner.getSelectedItemPosition() < 1)
                        {
                            values.put(DatabaseHelper.RequestTable.COLUMN_NAME_NAME, requestName.getText().toString());
                            requestId = db.insert(DatabaseHelper.RequestTable.TABLE_NAME, null, values);
                        }
                        else
                        {
                            requestId = requestIds[existingRequestsSpinner.getSelectedItemPosition()];
                            String requestIdString = String.valueOf(requestId);

                            db.update(DatabaseHelper.RequestTable.TABLE_NAME, values,
                                DatabaseHelper.RequestTable._ID + "=?", new String[]{requestIdString});
                            db.delete(DatabaseHelper.HeaderTable.TABLE_NAME, DatabaseHelper.HeaderTable.COLUMN_NAME_REQUEST_ID +
                                "=?", new String[]{requestIdString});
                        }

                        for (HttpHeader header : httpHeaders)
                        {
                            values = new ContentValues();
                            values.put(DatabaseHelper.HeaderTable.COLUMN_NAME_NAME, header.getName());
                            values.put(DatabaseHelper.HeaderTable.COLUMN_NAME_VALUE, header.getValue());
                            values.put(DatabaseHelper.HeaderTable.COLUMN_NAME_REQUEST_ID, requestId);
                            db.insert(DatabaseHelper.HeaderTable.TABLE_NAME, null, values);
                        }
                    }
                });

                builder.setNegativeButton(getResources().getString(R.string.cancel), new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                        dialogInterface.cancel();
                    }
                });

                dialog = builder.create();

                dialog.setOnShowListener(new DialogInterface.OnShowListener()
                {
                    public void onShow(DialogInterface dialogInterface)
                    {
                        saveRequestButton = dialog.getButton(Dialog.BUTTON_POSITIVE);

                        if (requestName.getText().toString().trim().equals(""))
                        {
                            existingRequestsSpinner.setEnabled(true);

                            if (existingRequestsSpinner.getSelectedItemPosition() < 1)
                            {
                                saveRequestButton.setEnabled(false);
                            }
                            else
                            {
                                saveRequestButton.setEnabled(true);
                            }
                        }
                        else
                        {
                            existingRequestsSpinner.setEnabled(false);
                            saveRequestButton.setEnabled(true);
                        }
                    }
                });

                break;
            }

            case DIALOG_LOAD:
            {
                final SQLiteDatabase db = databaseHelper.getReadableDatabase();
                Cursor cursor = db.query(DatabaseHelper.RequestTable.TABLE_NAME,
                    new String[]{DatabaseHelper.RequestTable._ID, DatabaseHelper.RequestTable.COLUMN_NAME_NAME,
                    DatabaseHelper.RequestTable.COLUMN_NAME_METHOD, DatabaseHelper.RequestTable.COLUMN_NAME_URL,
                    DatabaseHelper.RequestTable.COLUMN_NAME_BODY}, null, null, null, null, null);

                if (cursor.getCount() > 0)
                {
                    String[] requestNames = new String[cursor.getCount()];
                    final RequestData[] requestDataItems = new RequestData[cursor.getCount()];

                    cursor.moveToFirst();
                    while (!cursor.isAfterLast())
                    {
                        requestNames[cursor.getPosition()] = cursor.getString(1);

                        RequestData requestData = new RequestData(cursor.getLong(0), cursor.getString(2),
                            cursor.getString(3), cursor.getString(4));
                        requestDataItems[cursor.getPosition()] = requestData;

                        cursor.moveToNext();
                    }

                    builder.setTitle(getResources().getString(R.string.load_request))
                        .setIcon(0)
                        .setItems(requestNames, new DialogInterface.OnClickListener()
                        {
                            public void onClick(DialogInterface dialog, int item)
                            {
                                RequestData requestData = requestDataItems[item];

                                setHttpMethod(requestData.method);
                                setUrl(requestData.url);
                                setBody(requestData.body);

                                Cursor cursorHeader = db.query(DatabaseHelper.HeaderTable.TABLE_NAME,
                                    new String[]{DatabaseHelper.HeaderTable.COLUMN_NAME_NAME,
                                    DatabaseHelper.HeaderTable.COLUMN_NAME_VALUE},
                                    DatabaseHelper.HeaderTable.COLUMN_NAME_REQUEST_ID + "=?",
                                    new String[]{String.valueOf(requestData.id)}, null, null, null);

                                httpHeaders = new ArrayList<HttpHeader>();

                                cursorHeader.moveToFirst();
                                while (!cursorHeader.isAfterLast())
                                {
                                    HttpHeader header = new HttpHeader(cursorHeader.getString(0), cursorHeader.getString(1));
                                    httpHeaders.add(header);
                                    cursorHeader.moveToNext();
                                }

                                cursorHeader.close();

                                updateHttpHeaders();

                                dialog.dismiss();
                            }
                        });
                }
                else
                {
                    builder.setMessage(getResources().getString(R.string.no_requests))
                        .setIcon(0)
                        .setPositiveButton(R.string.ok, null);
                }

                cursor.close();

                dialog = builder.create();

                break;
            }

            case DIALOG_DELETE:
            {
                SQLiteDatabase db = databaseHelper.getReadableDatabase();
                Cursor cursor = db.query(DatabaseHelper.RequestTable.TABLE_NAME,
                    new String[]{DatabaseHelper.RequestTable._ID, DatabaseHelper.RequestTable.COLUMN_NAME_NAME}, null,
                    null, null, null, null);

                if (cursor.getCount() > 0)
                {
                    final int[] requestIds = new int[cursor.getCount()];
                    final String[] requestNames = new String[cursor.getCount()];

                    cursor.moveToFirst();
                    while (!cursor.isAfterLast())
                    {
                        requestIds[cursor.getPosition()] = cursor.getInt(0);
                        requestNames[cursor.getPosition()] = cursor.getString(1);
                        cursor.moveToNext();
                    }

                    builder.setTitle(getString(R.string.delete_request))
                        .setIcon(0)
                        .setItems(requestNames, new DialogInterface.OnClickListener()
                        {
                            public void onClick(DialogInterface dialog, int pos)
                            {
                                final int requestId = requestIds[pos];
                                final String requestIdString = String.valueOf(requestId);

                                AlertDialog.Builder confirmBuilder = new AlertDialog.Builder(MainActivity.this);
                                confirmBuilder.setTitle(getResources().getString(R.string.confirm_delete_title))
                                    .setIcon(0)
                                    .setMessage(String.format(getResources().getString(R.string.confirm_delete_request), requestNames[pos]))
                                    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
                                    {
                                        public void onClick(DialogInterface dialogInterface, int i)
                                        {
                                            SQLiteDatabase db = databaseHelper.getWritableDatabase();
                                            db.delete(DatabaseHelper.RequestTable.TABLE_NAME, DatabaseHelper.RequestTable._ID + "=?",
                                                new String[]{requestIdString});
                                        }
                                    })
                                    .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
                                    {
                                        public void onClick(DialogInterface dialogInterface, int i)
                                        {
                                            dialogInterface.cancel();
                                        }
                                    })
                                    .show();
                            }
                        });
                }
                else
                {
                    builder.setMessage(getResources().getString(R.string.no_requests))
                        .setIcon(0)
                        .setPositiveButton(R.string.ok, null);
                }

                cursor.close();

                dialog = builder.create();

                break;
            }

            case DIALOG_ERROR:
            case DIALOG_TIMEOUT_ERROR:
            case DIALOG_NO_HOST_ERROR:
            case DIALOG_NO_RESPONSE_ERROR:
            {
                int errorBodyId;

                if (id == DIALOG_TIMEOUT_ERROR)
                {
                    errorBodyId = R.string.request_error_timeout_body;
                }
                else if (id == DIALOG_NO_HOST_ERROR)
                {
                    errorBodyId = R.string.request_error_no_host_body;
                }
                else if (id == DIALOG_NO_RESPONSE_ERROR)
                {
                    errorBodyId = R.string.request_error_no_response_body;
                }
                else
                {
                    errorBodyId = R.string.request_error_body;
                }

                builder.setTitle(getResources().getString(R.string.request_error_title))
                    .setIcon(0)
                    .setMessage(getResources().getString(errorBodyId))
                    .setPositiveButton(R.string.ok, null);

                dialog = builder.create();

                break;
            }

            default:
                dialog = null;
        }

        return dialog;
    }

    private void hideKeyboard()
    {
        //hide soft keyboard
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);

        if (getCurrentFocus().getWindowToken() != null)
        {
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
        }

        removeFocusFromAllControls();
    }

    private void removeFocusFromAllControls()
    {
        View nonInputView;
        if (mTabHost.getCurrentTab() == TAB_REQUEST)
        {
            nonInputView = findViewById(R.id.httpMethodLabel);
        }
        else
        {
            nonInputView = findViewById(R.id.responseHeadersLabel);
        }

        nonInputView.setFocusable(true);
        nonInputView.setFocusableInTouchMode(true);
        nonInputView.requestFocus();
    }

    public void onClick(View view)
    {
        hideKeyboard();

        if (view.getId() == R.id.headerAdd)
        {
            removeDialog(DIALOG_ADD_HEADER); //remove first since this is a dynamic dialog that needs to be recreated
            showDialog(DIALOG_ADD_HEADER);
        }
        else if (view.getId() == R.id.headerDelete)
        {
            removeDialog(DIALOG_DELETE_HEADER); //remove first since this is a dynamic dialog that needs to be recreated
            showDialog(DIALOG_DELETE_HEADER);
        }
        else if (view.getId() == R.id.sendButton)
        {
            String url = cleanUrl();

            ProgressDialog progress = ProgressDialog.show(this, null, getResources().getString(R.string.request_progress));

            RequestData requestData = new RequestData(getHttpMethod(), url, getBody());

            new SendRequest(progress).execute(requestData);
        }
        else if (view.getId() == R.id.clearButton)
        {
            Spinner method = (Spinner) findViewById(R.id.httpMethodSpinner);
            method.setSelection(0);
            ((TextView) findViewById(R.id.url)).setText(urlPrefillValue);
            ((TextView) findViewById(R.id.body)).setText("");
            ((EditText) findViewById(R.id.responseHeaders)).setText("");
            ((TextView) findViewById(R.id.responseHeaders2)).setText("");
            ((EditText) findViewById(R.id.responseBody)).setText("");
            ((TextView) findViewById(R.id.responseBody2)).setText("");
            responseHeaderText = "";
            responseBodyText = "";

            if (deleteHeadersOnClear)
            {
                httpHeaders = new ArrayList<HttpHeader>();
                updateHttpHeaders();
            }
        }
        else if (view.getId() == R.id.saveButton)
        {
            cleanUrl();
            removeDialog(DIALOG_SAVE); //remove first since this is a dynamic dialog that needs to be recreated
            showDialog(DIALOG_SAVE);
        }
        else if (view.getId() == R.id.loadButton)
        {
            removeDialog(DIALOG_LOAD); //remove first since this is a dynamic dialog that needs to be recreated
            showDialog(DIALOG_LOAD);
        }
        else if (view.getId() == R.id.deleteButton)
        {
            removeDialog(DIALOG_DELETE); //remove first since this is a dynamic dialog that needs to be recreated
            showDialog(DIALOG_DELETE);
        }
    }

    private String cleanUrl()
    {
        TextView tvUrl = (TextView) findViewById(R.id.url);
        String url = tvUrl.getText().toString().trim();

        if (!url.toLowerCase().startsWith("http"))
        {
            url = urlPrefillValue + url;
        }

        tvUrl.setText(url);

        return url;
    }

    private class SendRequest extends AsyncTask<RequestData, Void, MyHttpResponse>
    {
        ProgressDialog progress;
        Exception sendException;
        long startTime;

        public SendRequest(ProgressDialog progress)
        {
            this.progress = progress;
        }

        @Override
        protected MyHttpResponse doInBackground(RequestData... requestData)
        {
            HttpResponse response = null;
            MyHttpResponse myResponse = new MyHttpResponse();
            RequestData data = requestData[0];
            startTime = System.currentTimeMillis();

            try
            {
                if (REQUEST_GET.equals(data.method))
                {
                    response = restClient.sendGet(data.url, httpHeaders);
                }
                else if (REQUEST_POST.equals(data.method))
                {
                    response = restClient.sendPost(data.url, httpHeaders, data.body);
                }
                else if (REQUEST_PUT.equals(data.method))
                {
                    response = restClient.sendPut(data.url, httpHeaders, data.body);
                }
                else if (REQUEST_DELETE.equals(data.method))
                {
                    response = restClient.sendDelete(data.url, httpHeaders);
                }
                else if (REQUEST_HEAD.equals(data.method))
                {
                    response = restClient.sendHead(data.url, httpHeaders);
                }
                else if (REQUEST_OPTIONS.equals(data.method))
                {
                    response = restClient.sendOptions(data.url, httpHeaders);
                }
                else if (REQUEST_TRACE.equals(data.method))
                {
                    response = restClient.sendTrace(data.url, httpHeaders);
                }

                if (response != null)
                {
                    myResponse.setStatusLine(response.getStatusLine());
                    myResponse.setHeaders(response.getAllHeaders());

                    if (response.getEntity() != null)
                    {
                        myResponse.setResponseBodyText(EntityUtils.toString(response.getEntity()));
                    }
                }
            }
            catch (Exception e)
            {
                sendException = e;
            }

            return myResponse;
        }

        @Override
        protected void onPostExecute(MyHttpResponse httpResponse)
        {
            long duration = System.currentTimeMillis() - startTime;

            if (sendException != null)
            {
                int dialogCode;

                if (sendException instanceof SocketTimeoutException || sendException instanceof ConnectTimeoutException)
                {
                    dialogCode = DIALOG_TIMEOUT_ERROR;
                }
                else if (sendException instanceof UnknownHostException)
                {
                    dialogCode = DIALOG_NO_HOST_ERROR;
                }
                else if (sendException instanceof NoHttpResponseException)
                {
                    dialogCode = DIALOG_NO_RESPONSE_ERROR;
                }
                else
                {
                    dialogCode = DIALOG_ERROR;
                }

                progress.dismiss();
                showDialog(dialogCode);
                return;
            }

            TextView responseBody;
            TextView responseHeaders;
            if (isOldDevice())
            {
                responseBody = ((TextView) findViewById(R.id.responseBody));
                responseHeaders = ((TextView) findViewById(R.id.responseHeaders));
            }
            else
            {
                responseBody = ((TextView) findViewById(R.id.responseBody2));
                responseHeaders = ((TextView) findViewById(R.id.responseHeaders2));
            }

            if (httpResponse.getResponseBodyText() != null)
            {
                responseBodyText = httpResponse.getResponseBodyText();
                responseBody.setText(responseBodyText);
            }
            else
            {
                responseBodyText = "";
                responseBody.setText("");
            }

            StatusLine statusLine = httpResponse.getStatusLine();

            StringBuilder sb = new StringBuilder();
            sb.append("<b>").append(statusLine.getProtocolVersion()).append(" ").append(statusLine.getStatusCode())
                .append(" ").append(statusLine.getReasonPhrase()).append("</b>&nbsp;&nbsp;&nbsp;(").append(duration)
                .append("ms)<br><br>");
            for (Header header : httpResponse.getHeaders())
            {
                sb.append("<b>").append(header.getName()).append(":</b> ").append(header.getValue()).append("<br>");
            }

            responseHeaderText = sb.toString();

            responseHeaders.setText(Html.fromHtml(responseHeaderText));

            mTabHost.setCurrentTab(TAB_RESPONSE);

            progress.dismiss();
        }
    }

    private class MyHttpResponse
    {
        private StatusLine statusLine;
        private Header[] headers;
        private String responseBodyText;

        public StatusLine getStatusLine()
        {
            return statusLine;
        }

        public void setStatusLine(StatusLine statusLine)
        {
            this.statusLine = statusLine;
        }

        public Header[] getHeaders()
        {
            return headers;
        }

        public void setHeaders(Header[] headers)
        {
            this.headers = headers;
        }

        public String getResponseBodyText()
        {
            return responseBodyText;
        }

        public void setResponseBodyText(String responseBodyText)
        {
            this.responseBodyText = responseBodyText;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        boolean handled;

        switch (item.getItemId())
        {
            case R.id.settings:
                Intent settingsIntent = new Intent(this, SettingsActivity.class);
                settingsIntent.putExtra("timeoutInSeconds", String.valueOf(timeoutInSeconds));
                startActivity(settingsIntent);
                handled = true;
                break;

            case R.id.about:
                startActivity(new Intent(this, AboutActivity.class));
                handled = true;
                break;

            default:
                handled = super.onOptionsItemSelected(item);
        }

        return handled;
    }

    private boolean isOldDevice()
    {
        return Build.VERSION.SDK_INT < 11;
    }

    private static class RequestData
    {
        public long id;
        public String method;
        public String url;
        public String body;

        public RequestData(String method, String url, String body)
        {
            this.method = method;
            this.url = url;
            this.body = body;
        }

        public RequestData(long id, String method, String url, String body)
        {
            this(method, url, body);
            this.id = id;
        }
    }

    private static class RotationStorage
    {
        public List<HttpHeader> httpHeaders;
        public int currentTab;
        public String headerText;
        public String bodyText;
    }
}
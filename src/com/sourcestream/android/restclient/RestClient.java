package com.sourcestream.android.restclient;

import android.util.Log;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpTrace;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.SingleClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;

import java.io.IOException;
import java.security.KeyStore;
import java.util.List;

public class RestClient
{
    private static final int DEFAULT_TIMEOUT_IN_SECONDS = 10;

    private HttpClient httpClient;
    private boolean trustAllCerts;
    private int timeoutInSeconds;

    public RestClient()
    {
        this(true);
    }

    public RestClient(boolean trustAllCerts)
    {
        this(trustAllCerts, DEFAULT_TIMEOUT_IN_SECONDS);
    }

    public RestClient(boolean trustAllCerts, int timeoutInSeconds)
    {
        this.timeoutInSeconds = timeoutInSeconds;
        httpClient = getHttpClient(trustAllCerts, timeoutInSeconds);
        this.trustAllCerts = trustAllCerts;
    }

    public boolean isTrustAllCerts()
    {
        return trustAllCerts;
    }

    public void setTrustAllCerts(boolean trustAllCerts)
    {
        this.trustAllCerts = trustAllCerts;
        httpClient = getHttpClient(trustAllCerts, timeoutInSeconds);
    }

    public int getTimeoutInSeconds()
    {
        return timeoutInSeconds;
    }

    public void setTimeoutInSeconds(int timeoutInSeconds)
    {
        this.timeoutInSeconds = timeoutInSeconds;
        httpClient = getHttpClient(trustAllCerts, timeoutInSeconds);
    }

    public HttpResponse sendGet(String url, List<HttpHeader> headers) throws IOException
    {
        HttpGet httpGet = new HttpGet(url);

        for (HttpHeader header : headers)
        {
            httpGet.addHeader(header.getName(), header.getValue());
        }

        return httpClient.execute(httpGet);
    }

    public HttpResponse sendPost(String url, List<HttpHeader> headers, String body) throws IOException
    {
        HttpPost httpPost = new HttpPost(url);

        for (HttpHeader header : headers)
        {
            httpPost.addHeader(header.getName(), header.getValue());
        }

        httpPost.setEntity(new StringEntity(body));
        return httpClient.execute(httpPost);
    }

    public HttpResponse sendPut(String url, List<HttpHeader> headers, String body) throws IOException
    {
        HttpPut httpPut = new HttpPut(url);

        for (HttpHeader header : headers)
        {
            httpPut.addHeader(header.getName(), header.getValue());
        }

        httpPut.setEntity(new StringEntity(body));
        return httpClient.execute(httpPut);
    }

    public HttpResponse sendDelete(String url, List<HttpHeader> headers) throws IOException
    {
        HttpDelete httpDelete = new HttpDelete(url);

        for (HttpHeader header : headers)
        {
            httpDelete.addHeader(header.getName(), header.getValue());
        }

        return httpClient.execute(httpDelete);
    }

    public HttpResponse sendHead(String url, List<HttpHeader> headers) throws IOException
    {
        HttpHead httpHead = new HttpHead(url);

        for (HttpHeader header : headers)
        {
            httpHead.addHeader(header.getName(), header.getValue());
        }

        return httpClient.execute(httpHead);
    }

    public HttpResponse sendOptions(String url, List<HttpHeader> headers) throws IOException
    {
        HttpOptions httpOptions = new HttpOptions(url);

        for (HttpHeader header : headers)
        {
            httpOptions.addHeader(header.getName(), header.getValue());
        }

        return httpClient.execute(httpOptions);
    }

    public HttpResponse sendTrace(String url, List<HttpHeader> headers) throws IOException
    {
        HttpTrace httpTrace = new HttpTrace(url);

        for (HttpHeader header : headers)
        {
            httpTrace.addHeader(header.getName(), header.getValue());
        }

        return httpClient.execute(httpTrace);
    }

    private HttpClient getHttpClient(boolean trustAllCerts, int timeoutInSeconds)
    {
        try
        {
            SocketFactory plainSocketFactory = PlainSocketFactory.getSocketFactory();
            SocketFactory httpsSocketFactory;

            if (trustAllCerts)
            {
                KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                trustStore.load(null, null);

                SSLSocketFactory trustingSocketFactory = new EnhancedSSLSocketFactory(trustStore);
                trustingSocketFactory.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

                httpsSocketFactory = trustingSocketFactory;
            }
            else
            {
                httpsSocketFactory = plainSocketFactory;
            }

            SchemeRegistry registry = new SchemeRegistry();
            registry.register(new Scheme("http", plainSocketFactory, 80));
            registry.register(new Scheme("https", httpsSocketFactory, 443));

            int timeoutInMs = timeoutInSeconds * 1000;

            HttpParams params = new BasicHttpParams();
            params.setParameter(HttpProtocolParams.USE_EXPECT_CONTINUE, false);
            HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
            HttpConnectionParams.setConnectionTimeout(params, timeoutInMs);
            HttpConnectionParams.setSoTimeout(params, timeoutInMs);

            ClientConnectionManager ccm = new SingleClientConnManager(params, registry);

            return new DefaultHttpClient(ccm, params);
        }
        catch (Exception e)
        {
            Log.e("RestClint", "Error instantiating HTTP client");
        }

        return null;
    }
}

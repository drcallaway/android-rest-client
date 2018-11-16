package com.sourcestream.android.restclient;

import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Adds support for ignoring certificates and establishing a timeout for SSL connections.
 */
public class EnhancedSSLSocketFactory extends SSLSocketFactory
{
    private SSLContext sslContext = SSLContext.getInstance("TLS");

    public EnhancedSSLSocketFactory(KeyStore truststore) throws NoSuchAlgorithmException, KeyManagementException,
        KeyStoreException, UnrecoverableKeyException
    {
        super(truststore);

        TrustManager tm = new X509TrustManager()
        {
            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException
            {
            }

            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException
            {
            }

            public X509Certificate[] getAcceptedIssuers()
            {
                return null;
            }
        };

        sslContext.init(null, new TrustManager[]{tm}, null);
    }

    public Socket connectSocket(Socket sock, String host, int port, InetAddress localAddress, int localPort,
        HttpParams params) throws IOException
    {
        int connTimeout = HttpConnectionParams.getConnectionTimeout(params);
        int soTimeout = HttpConnectionParams.getSoTimeout(params);

        InetSocketAddress remoteAddress = new InetSocketAddress(host, port);
        SSLSocket sslsock = (SSLSocket) ((sock != null) ? sock : createSocket());

        if ((localAddress != null) || (localPort > 0))
        {
            // we need to bind explicitly
            if (localPort < 0)
            {
                localPort = 0; // indicates "any"
            }
            InetSocketAddress isa = new InetSocketAddress(localAddress, localPort);
            sslsock.bind(isa);
        }

        sslsock.connect(remoteAddress, connTimeout);
        sslsock.setSoTimeout(soTimeout);
        return sslsock;
    }

    public boolean isSecure(Socket socket) throws IllegalArgumentException
    {
        return true;
    }

    public Socket createSocket() throws IOException
    {
        return sslContext.getSocketFactory().createSocket();
    }

    /**
     * @see org.apache.http.conn.scheme.LayeredSocketFactory#createSocket(java.net.Socket,
     *      java.lang.String, int, boolean)
     */
    public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException
    {
        return sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
    }

    // -------------------------------------------------------------------
    // javadoc in org.apache.http.conn.scheme.SocketFactory says :
    // Both Object.equals() and Object.hashCode() must be overridden
    // for the correct operation of some connection managers
    // -------------------------------------------------------------------

    public boolean equals(Object obj)
    {
        return ((obj != null) && obj.getClass().equals(EnhancedSSLSocketFactory.class));
    }

    public int hashCode()
    {
        return EnhancedSSLSocketFactory.class.hashCode();
    }
}

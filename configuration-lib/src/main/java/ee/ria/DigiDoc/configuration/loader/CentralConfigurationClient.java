package ee.ria.DigiDoc.configuration.loader;

import java.io.IOException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

class CentralConfigurationClient {

    private static final int DEFAULT_TIMEOUT = 5;
    private final OkHttpClient httpClient;
    private final String centralConfigurationServiceUrl;
    private final String userAgent;

    CentralConfigurationClient(String centralConfigurationServiceUrl, String userAgent) {
        this.centralConfigurationServiceUrl = centralConfigurationServiceUrl;
        httpClient = constructHttpClient();
        this.userAgent = userAgent;
    }

    String getConfiguration() {
        return requestData(centralConfigurationServiceUrl + "/config.json");
    }

    String getConfigurationSignature() {
        return requestData(centralConfigurationServiceUrl + "/config.rsa");
    }

    String getConfigurationSignaturePublicKey() {
        return requestData(centralConfigurationServiceUrl + "/config.pub");
    }

    private String requestData(String url) {

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader("User-Agent", userAgent)
                .build();

        Call call = httpClient.newCall(request);
        try {
            Response response = call.execute();
            if (response.code() != 200) {
                throw new CentralConfigurationException("Service responded with not OK status code " + response.code());
            }
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                throw new CentralConfigurationException("Service responded with empty body");
            }
            return responseBody.string();
        } catch (IOException e) {
            throw new CentralConfigurationException("Something went wrong during fetching configuration", e);
        }
    }

    private OkHttpClient constructHttpClient() {
        try {
            OkHttpClient.Builder builder = constructClientBuilder();
            return builder.build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to construct HTTP client",e);
        }
    }

    private OkHttpClient.Builder constructClientBuilder() throws IOException {
        return new OkHttpClient.Builder()
                .sslSocketFactory(constructSSLSocketFactory(), (X509TrustManager) getTrustManagers()[0])
                .hostnameVerifier((hostname, session) -> true)
                .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .callTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS);
    }

    private SSLSocketFactory constructSSLSocketFactory() {
        try {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, getTrustManagers(), new SecureRandom());
            return sslContext.getSocketFactory();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to construct SSLSocketFactory", e);
        }
    }

    private static TrustManager[] getTrustManagers() throws IOException {
        try {
            final TrustManager[] trustManagers = new TrustManager[] {
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {}

                        @Override
                        public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {}

                        @Override
                        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                    }
            };

            return trustManagers;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    static class CentralConfigurationException extends RuntimeException {
        CentralConfigurationException(String message) {
            super(message);
        }

        CentralConfigurationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
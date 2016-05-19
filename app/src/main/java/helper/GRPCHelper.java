package helper;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import com.squareup.okhttp.ConnectionSpec;
import com.squareup.okhttp.TlsVersion;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.security.auth.x500.X500Principal;

import app.BookerApplication;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.internal.GrpcUtil;
import io.grpc.okhttp.OkHttpChannelBuilder;
import ir.cafebazaar.booker.booker.BuildConfig;
import ir.cafebazaar.booker.booker.R;
import ir.cafebazaar.booker.proto.nano.AndroidClientInfo;
import ir.cafebazaar.booker.proto.nano.RequestProperties;
import ir.cafebazaar.booker.proto.nano.ReservationGrpc;
import ir.cafebazaar.booker.proto.nano.ResourcesGrpc;

public class GRPCHelper {

    private ManagedChannel mReservationChannel, mResourcesChannel;

    static private GRPCHelper singleton;

    public synchronized static GRPCHelper getInstance() throws InterruptedException {
        singleton = new GRPCHelper();
        return singleton;
    }

    public void close() throws InterruptedException {
        if (mResourcesChannel != null) {
            mResourcesChannel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
        }
        if (mReservationChannel != null) {
            mReservationChannel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
        }
    }

    public static RequestProperties newRPWithDeviceInfo() {
        RequestProperties rp = new RequestProperties();
        rp.androidClientInfo = new AndroidClientInfo();

        Application app = BookerApplication.getInstance();
        String packageName = app.getPackageName();
        rp.clientId = packageName;
        try {
            PackageInfo pInfo = app.getPackageManager().getPackageInfo(packageName, 0);
            rp.clientVersion = pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException nnfe) {
            // Nevermind!
        }

        TelephonyManager tel = (TelephonyManager) BookerApplication.getInstance().getSystemService(Context.TELEPHONY_SERVICE);
        String networkOperator = tel.getNetworkOperator();
        if (!TextUtils.isEmpty(networkOperator)) {
            rp.androidClientInfo.mccMnc = networkOperator;
        }

        rp.userAuthToken = "TODO";

        rp.androidClientInfo.locale = Locale.getDefault().getLanguage();
        rp.androidClientInfo.country = Locale.getDefault().getCountry();

        // TODO: cache static values

        return rp;
    }

    /**
     * Creates an SSLSocketFactory which contains {@code certChainFile} as its only root certificate.
     */
    public static SSLSocketFactory newSslSocketFactoryForCa(File certChainFile) throws Exception {
        InputStream is = new FileInputStream(certChainFile);
        try {
            return newSslSocketFactoryForCa(is);
        } finally {
            is.close();
        }
    }

    /**
     * Creates an SSLSocketFactory which contains {@code certChainFile} as its only root certificate.
     */
    public static SSLSocketFactory newSslSocketFactoryForCa(InputStream certChain) throws Exception {
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        ks.load(null, null);
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        X509Certificate cert = (X509Certificate) cf.generateCertificate(
                new BufferedInputStream(certChain));
        X500Principal principal = cert.getSubjectX500Principal();
        ks.setCertificateEntry(principal.getName("RFC2253"), cert);

        // Set up trust manager factory to use our key store.
        TrustManagerFactory trustManagerFactory =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(ks);
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, trustManagerFactory.getTrustManagers(), null);
        return context.getSocketFactory();
    }

    private ManagedChannel createChannel(String host, int port, InputStream cert) {
        OkHttpChannelBuilder builder = OkHttpChannelBuilder.forAddress(host, port)
                .connectionSpec(new ConnectionSpec.Builder(OkHttpChannelBuilder.DEFAULT_CONNECTION_SPEC)
                        .tlsVersions(ConnectionSpec.MODERN_TLS.tlsVersions().toArray(new TlsVersion[0]))
                        .build());
        try {
            builder.sslSocketFactory(newSslSocketFactoryForCa(cert));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return builder.build();
    }

    public ReservationGrpc.ReservationBlockingStub getReservationGrpc() throws IOException {
        // TODO: cache
        if (mReservationChannel == null) {
            mReservationChannel = createChannel(
                    BuildConfig.RESERVATION_SRV_HOST,
                    BuildConfig.RESERVATION_SRV_PORT,
                    BookerApplication.getInstance().getResources().openRawResource(R.raw.reservation));
        }
        return ReservationGrpc.newBlockingStub(mReservationChannel);
    }

    public ResourcesGrpc.ResourcesBlockingStub getResourcesGrpc() throws IOException {
        // TODO: cache
        if (mReservationChannel == null) {
            mReservationChannel = createChannel(
                    BuildConfig.RESOURCES_SRV_HOST,
                    BuildConfig.RESOURCES_SRV_PORT,
                    BookerApplication.getInstance().getResources().openRawResource(R.raw.resources));
        }
        return ResourcesGrpc.newBlockingStub(mReservationChannel);
    }
}
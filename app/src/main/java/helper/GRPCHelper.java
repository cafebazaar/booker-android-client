package helper;

import android.app.Application;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import java.net.ssl.SSLContext;
import java.net.ssl.SSLSocketFactory;
import java.net.ssl.TrustManagerFactory;

import app.BookerApplication;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import ir.cafebazaar.booker.booker.BuildConfig;
import ir.cafebazaar.booker.proto.nano.AndroidClientInfo;
import ir.cafebazaar.booker.proto.nano.RequestProperties;
import ir.cafebazaar.booker.proto.nano.ReservationGrpc;

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

    public SSLSocketFactory getSocketFactory() {

        CertificateFactory cf = CertificateFactory.getInstance();
        InputStream caInput = new BufferedInputStream(new FileInputStream("load-der.crt"));
        Certificate ca;
        try {
            ca = cf.generateCertificate(caInput);
            System.out.println("ca=" + ((X509Certificate) ca).getSubjectDN());
        } finally {
            caInput.close();
        }

        String keyStoreType = KeyStore.getDefaultType();
        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
        keyStore.load(null, null);
        keyStore.setCertificateEntry("ca", ca);

        String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
        tmf.init(keyStore);

        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, tmf.getTrustManagers(), null);
        return context.getSocketFactory();

    }
    public ReservationGrpc.ReservationBlockingStub getReservationGrpc() {
        // TODO: cache
        if (mReservationChannel == null) {
            mReservationChannel = ManagedChannelBuilder.forAddress(BuildConfig.RESERVATION_SRV_HOST, BuildConfig.RESERVATION_SRV_PORT)
                    .build();
        }
        return ReservationGrpc.newBlockingStub(mReservationChannel);
    }

}
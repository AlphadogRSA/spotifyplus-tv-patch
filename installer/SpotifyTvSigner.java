import com.android.apksig.ApkSigner;
import com.android.apksig.ApkVerifier;
import java.io.File;
import java.nio.file.Files;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HexFormat;

/** Sign only with a key matching the existing installation's public certificate. */
public final class SpotifyTvSigner {
    static String fingerprint(X509Certificate cert) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(cert.getEncoded()));
    }
    public static void main(String[] args) {
        try { sign(args); }
        catch (Exception error) { System.err.println("STOPPED: " + error.getMessage()); System.exit(2); }
    }
    static void sign(String[] args) throws Exception {
        if (args.length != 4) throw new IllegalArgumentException("Expected INPUT_APK KEYSTORE OUTPUT_APK EXPECTED_CERT_SHA256");
        File input=new File(args[0]), storeFile=new File(args[1]), output=new File(args[2]);
        if (output.exists()) throw new IllegalArgumentException("Output already exists; move it first: " + output.getName());
        String expected=args[3].toLowerCase();
        if (!expected.matches("[a-f0-9]{64}")) throw new IllegalArgumentException("Invalid expected certificate fingerprint");
        String configured=System.getenv("SPTV_STORE_PASSWORD");
        char[] password=(configured==null ? "android" : configured).toCharArray();
        String configuredKey=System.getenv("SPTV_KEY_PASSWORD");
        char[] keyPassword=configuredKey==null ? password.clone() : configuredKey.toCharArray();
        try {
            KeyStore store=KeyStore.getInstance(storeFile,password);
            String selected=null;
            for (String alias:Collections.list(store.aliases())) {
                if (store.isKeyEntry(alias) && store.getCertificate(alias) instanceof X509Certificate cert
                        && fingerprint(cert).equals(expected)) { selected=alias; break; }
            }
            if (selected==null) throw new IllegalArgumentException("This key does not match the Spotify app on your TV. No APK was signed or installed.");
            if (!(store.getKey(selected,keyPassword) instanceof PrivateKey key)) throw new IllegalArgumentException("Matching entry has no accessible private key");
            var certificates=new ArrayList<X509Certificate>();
            for(var cert:store.getCertificateChain(selected)) certificates.add((X509Certificate)cert);
            var signer=new ApkSigner.SignerConfig.Builder("SpotifyPlusTV",key,certificates).build();
            new ApkSigner.Builder(Collections.singletonList(signer)).setInputApk(input).setOutputApk(output)
                    .setMinSdkVersion(26).setV1SigningEnabled(true).setV2SigningEnabled(true)
                    .setV3SigningEnabled(true).setV4SigningEnabled(false).build().sign();
            var verification=new ApkVerifier.Builder(output).setMinCheckedPlatformVersion(26).build().verify();
            if (!verification.isVerified() || verification.getSignerCertificates().size()!=1
                    || !fingerprint(verification.getSignerCertificates().get(0)).equals(expected)) {
                Files.deleteIfExists(output.toPath());
                throw new IllegalStateException("Signature verification failed; invalid output removed");
            }
            System.out.println("SIGNED_AND_VERIFIED: " + output.getAbsolutePath());
            System.out.println("Certificate matches the previous Spotify TV installation.");
        } finally { Arrays.fill(password,'\0'); Arrays.fill(keyPassword,'\0'); }
    }
}

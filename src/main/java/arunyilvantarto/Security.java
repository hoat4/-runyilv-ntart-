package arunyilvantarto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Security {

    private static final String SALT = "isaloler";

    public static byte[] hashPassword(String password) {
        MessageDigest messageDigest = messageDigest();
        messageDigest.update(SALT.getBytes(UTF_8));
        messageDigest.update(password.getBytes(UTF_8));
        return messageDigest.digest();
    }

    private static MessageDigest messageDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}

package com.example.springdemo.util;

import com.example.springdemo.entity.UserPosition;
import jakarta.servlet.http.HttpSession;
import org.jspecify.annotations.NonNull;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public final class BasicFunctions {
    private BasicFunctions(){}

    /**PBKDF2 迭代次数，越大计算越慢、越难被暴力破解（OWASP 当前推荐值）*/
    private static final int HASH_ITERATIONS = 600_000;
    private static final int HASH_KEY_LENGTH_BITS = 256;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();
    private static final char[] SALT_CHARS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();

    public static @NonNull String getFormatTime(){
        OffsetDateTime now = OffsetDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        return now.format(formatter);
    }

    /**
     * System.out.println(BasicFunctions.getFormatTime()+"  HttpSessionId:"+httpSession.getId()+"\t"+object);
     * */
    public static void printTimeHttpSessionAnd(@NonNull HttpSession httpSession, Object object){
        System.out.println(BasicFunctions.getFormatTime()+"  HttpSessionId: "+httpSession.getId()+"\t"+object);
    }

    public static void printTimeHttpSessionAnd(@NonNull HttpSession httpSession, @NonNull String userId, Object object){
        printTimeHttpSessionIdAnd(httpSession.getId(), userId, object);
    }

    public static void printTimeHttpSessionIdAnd(@NonNull String sessionId, @NonNull String userId, Object object){
        System.out.println(BasicFunctions.getFormatTime()+"  HttpSessionId: "+sessionId+"\tid: "+userId+"\t"+object);
    }

    public static void printTimeHttpSessionAnd(@NonNull HttpSession httpSession, @NonNull String userId1, @NonNull String userId2, Object object){
        System.out.println(BasicFunctions.getFormatTime()+"  HttpSessionId: "+httpSession.getId()+"\tid1: "+userId1+"\tid2: "+userId2+"\t"+object);
    }

    public static boolean isLoggedIn(@NonNull HttpSession httpSession){
        return httpSession.getAttribute("userId") != null;
    }

    public static boolean safeEquals(String a, String b){
        if (a == null) {
            throw new NullPointerException("参数1为null!");
        }
        if (b == null) {
            throw new NullPointerException("参数2为null!");
        }
        // MessageDigest.isEqual 只接受字节数组，先编码为 UTF-8
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }

    /**生成 16 位随机盐，用于密码哈希
     * @return 16 位随机字符串*/
    public static @NonNull String generateSalt(){
        StringBuilder stringBuilder = new StringBuilder(16);
        for (int i = 0; i < 16; i++) {
            stringBuilder.append(SALT_CHARS[RANDOM.nextInt(SALT_CHARS.length)]);
        }
        return stringBuilder.toString();
    }

    /**计算密码的加盐哈希值（PBKDF2WithHmacSHA256，迭代次数见 HASH_ITERATIONS）
     * @param password 明文密码
     * @param salt 盐；为 null 时按占位串处理（兼容数据库中尚未迁移的旧数据，此时哈希必然对不上，登录会失败但不会抛异常）
     * @return 64 位十六进制字符串
     * @throws NullPointerException 当 password 为 null 时*/
    public static @NonNull String hashPassword(String password, String salt){
        if (password == null) {
            throw new NullPointerException("密码为null!");
        }
        String actualSalt = salt == null ? "unsalted-legacy!" : salt;
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(),
                    actualSalt.getBytes(StandardCharsets.UTF_8),
                    HASH_ITERATIONS, HASH_KEY_LENGTH_BITS);
            byte[] hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
            spec.clearPassword();
            return toHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException("计算密码哈希失败", e);
        }
    }

    private static @NonNull String toHex(byte[] bytes){
        char[] result = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int value = bytes[i] & 0xFF;
            result[i * 2] = HEX_CHARS[value >>> 4];
            result[i * 2 + 1] = HEX_CHARS[value & 0x0F];
        }
        return new String(result);
    }

    public static String getPositionFromHttpSession(@NonNull HttpSession httpSession){
        return ((UserPosition)(httpSession.getAttribute("userPosition"))).getString();
    }

}

package com.example.springdemo.util;

import com.example.springdemo.entity.UserPosition;
import jakarta.servlet.http.HttpSession;
import org.jspecify.annotations.NonNull;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

public final class BasicFunctions {
    private BasicFunctions(){}

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
        System.out.println(BasicFunctions.getFormatTime()+"  HttpSessionId: "+httpSession.getId()+"\tid: "+userId+"\t"+object);
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

    public static String getPositionFromHttpSession(@NonNull HttpSession httpSession){
        return ((UserPosition)(httpSession.getAttribute("userPosition"))).getString();
    }

}

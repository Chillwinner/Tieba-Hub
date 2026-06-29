package com.aura.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class Sha256Utils
{

    private static final String SALT = "Roxy";

    // 对明文加盐后做 SHA-256 哈希，返回 64 位十六进制字符串
    public static String hash(String input)
    {
        try
        {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update(SALT.getBytes());
            byte[] digest = md.digest(input.getBytes());
            return HexFormat.of().formatHex(digest);
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new RuntimeException(e);
        }
    }

    // 校验明文与哈希值是否匹配
    public static boolean verify(String input, String hashed)
    {
        return hash(input).equals(hashed);
    }
}

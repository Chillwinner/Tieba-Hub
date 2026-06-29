package com.aura.result;

import lombok.Data;

// 统一返回对象，code=200 表示成功，其他为错误码
@Data
public class Result<T>
{
    private int code;
    private String message;
    private T data;

    private Result() {}

    // 成功，携带数据
    public static <T> Result<T> success(T data)
    {
        Result<T> r = new Result<T>();
        r.code = 200;
        r.message = "success";
        r.data = data;
        return r;
    }

    // 成功，无数据
    public static <T> Result<T> success()
    {
        return success(null);
    }

    // 失败，指定错误码和消息
    public static <T> Result<T> error(int code, String message)
    {
        Result<T> r = new Result<T>();
        r.code = code;
        r.message = message;
        return r;
    }

    // 失败，默认 500
    public static <T> Result<T> error(String message)
    {
        return error(500, message);
    }
}

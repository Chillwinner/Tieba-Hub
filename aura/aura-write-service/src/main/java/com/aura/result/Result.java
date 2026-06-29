package com.aura.result;

import lombok.Data;

// 统一返回对象
@Data
public class Result<T>
{
    private int code;
    private String message;
    private T data;

    private Result() {}

    // 返回成功结果，携带数据
    public static <T> Result<T> success(T data)
    {
        Result<T> r = new Result<T>();
        r.code = 200;
        r.message = "success";
        r.data = data;
        return r;
    }

    // 返回成功结果，无数据
    public static <T> Result<T> success()
    {
        return success(null);
    }

    // 返回失败结果，指定错误码和消息
    public static <T> Result<T> error(int code, String message)
    {
        Result<T> r = new Result<T>();
        r.code = code;
        r.message = message;
        return r;
    }

    // 返回失败结果，默认 500 错误码
    public static <T> Result<T> error(String message)
    {
        return error(500, message);
    }
}

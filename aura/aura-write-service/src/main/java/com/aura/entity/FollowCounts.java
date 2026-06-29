package com.aura.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

// 关注数统计
@Data
@AllArgsConstructor
public class FollowCounts
{
    private long followers;
    private long following;
}

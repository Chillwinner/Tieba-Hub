package com.aura.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FollowCounts
{
    private long followers;
    private long following;
}

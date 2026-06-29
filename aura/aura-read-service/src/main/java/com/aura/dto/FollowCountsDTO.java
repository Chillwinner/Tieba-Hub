package com.aura.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FollowCountsDTO
{
    private long followers;
    private long following;
}

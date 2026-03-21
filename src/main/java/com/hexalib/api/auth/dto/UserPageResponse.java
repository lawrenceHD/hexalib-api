package com.hexalib.api.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPageResponse {
    private List<UserDto> users;
    private int     page;
    private int     size;
    private long    total;
    private int     totalPages;
}
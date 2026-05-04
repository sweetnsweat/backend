package com.capstone.backend.home.dto;

import java.util.List;

public record HomeWorldBannerListResponse(
        List<HomeWorldBannerResponse> slides
) {
}

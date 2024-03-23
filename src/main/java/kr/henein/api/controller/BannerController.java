package kr.henein.api.controller;

import kr.henein.api.dto.BannerResponseDto;
import kr.henein.api.service.CommonBoardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController("/board/banner")
@RequiredArgsConstructor
public class BannerController {
    private final CommonBoardService commonBoardService;
    @GetMapping
    public List<BannerResponseDto> getBannerList() {
        return commonBoardService.getBannerList();
    }
}

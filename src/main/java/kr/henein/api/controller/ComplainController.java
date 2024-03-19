package kr.henein.api.controller;

import kr.henein.api.dto.ComplainRequestDto;
import kr.henein.api.enumCustom.ComplainReason;
import kr.henein.api.service.ComplainService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/complain")
@RequiredArgsConstructor
public class ComplainController {
    private final ComplainService complainService;

    @GetMapping("/reason")
    public Map<Integer, String> getComplainList() {
        Map<Integer, String> result = new HashMap<>();
        result.put(ComplainReason.abuse.getKey(), ComplainReason.abuse.getTitle());
        result.put(ComplainReason.advertisement.getKey(), ComplainReason.advertisement.getTitle());
        result.put(ComplainReason.obscene.getKey(), ComplainReason.obscene.getTitle());
        result.put(ComplainReason.IllegalFilm.getKey(), ComplainReason.IllegalFilm.getTitle());
        result.put(ComplainReason.another.getKey(), ComplainReason.another.getTitle());

        return result;
    }

    @PostMapping()
    public ResponseEntity<?> complainThisService(@RequestBody ComplainRequestDto complainRequestDto, HttpServletRequest request) {
        complainService.complainThisService(complainRequestDto, request);
    }
}

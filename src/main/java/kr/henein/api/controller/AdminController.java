package kr.henein.api.controller;

import io.swagger.annotations.Api;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Api(tags = {"관리자 Controller"})
public class AdminController {
    @Operation(summary = "대쉬보드 호출")
    @GetMapping("/dashboard")
    public void getDashBoard () {

    }

    @Operation(summary = "대쉬보드 호출")
    @GetMapping("/user")
    public void getUserList () {

    }

    @Operation(summary = "대쉬보드 호출")
    @GetMapping("/character")
    public void getCharacterList () {

    }
}

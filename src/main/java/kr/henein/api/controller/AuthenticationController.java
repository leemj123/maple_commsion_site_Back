package kr.henein.api.controller;

import kr.henein.api.dto.login.BasicLoginRequestDto;
import kr.henein.api.dto.login.BasicRegisterRequestDto;
import kr.henein.api.service.AmazonSMTPService;
import kr.henein.api.service.AuthenticationService;
import kr.henein.api.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;



@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@Api(tags = {"JWT 로그인 Controller"})

public class AuthenticationController {

    private final AuthenticationService authenticationService;
    private final AmazonSMTPService amazonSMTPService;


    //=======Local 로그인 관련
    @Operation(summary = "로컬 로그인 userEmail,password, reCAPTCHA 토큰")
    @PostMapping("/login")
    public ResponseEntity<?> basicLogin(@RequestBody BasicLoginRequestDto basicLoginRequestDto, HttpServletResponse response) {
        return authenticationService.basicLogin(basicLoginRequestDto,response);
    }
    @Operation(summary = "로컬 회원가입 userEmail,password")
    @PostMapping("/login/register")
    public ResponseEntity<String> basicSignUp(@RequestBody BasicRegisterRequestDto basicRegisterRequestDto, HttpServletRequest request, HttpServletResponse response) {
        return authenticationService.basicSignUp(basicRegisterRequestDto,request, response);
    }
    @Operation(summary = "인증 메일 발송 요청")
    @PostMapping("/mail/sender")
    public ResponseEntity<String> sendNaver(String requestEmail) {
        amazonSMTPService.sendToVerifyEmail(requestEmail);
        return ResponseEntity.ok("인증 메일이 발송되었습니다.");
    }
    @Operation(summary = "인증 코드 확인 API")
    @PostMapping("/mail/verify")
    public ResponseEntity<String> verifyEmail(String key, HttpServletResponse response) {
        amazonSMTPService.verifyEmailAuth(key, response);
        return ResponseEntity.ok("이메일 인증이 정상적으로 처리되었습니다.");
    }


    //=====OAuth2 관련========
    @Operation(summary = "Authorization에 accessToken, RefreshToken에 refreshToken이 들어있음")
    @GetMapping("/login/kakao")
    public ResponseEntity<?> kakaoLogin(@RequestParam("code") String code, HttpServletResponse response) {
        return authenticationService.kakaoLogin(code,response);
    }
    @Operation(summary = "AT를 재발급 받기 위한 API")
    @GetMapping("/refresh")
    public ResponseEntity<?> refreshAT(HttpServletRequest request, HttpServletResponse response) {
       return authenticationService.refreshAT(request, response);
    }

    //==========reCAPTCHA 관련===========

}

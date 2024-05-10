package kr.henein.api.controller;

import kr.henein.api.dto.board.BoardListResponseDto;
import kr.henein.api.dto.user.UserDetailInfoResponseDto;
import kr.henein.api.dto.user.UserInfoChange;
import kr.henein.api.dto.user.UserInfoResponseDto;
import kr.henein.api.dto.userchar.CharRefreshRequestDto;
import kr.henein.api.dto.userchar.UserCharacterResponse;
import kr.henein.api.dto.userchar.UserMapleApi;
import kr.henein.api.error.ErrorCode;
import kr.henein.api.error.exception.BadRequestException;
import kr.henein.api.error.exception.ForbiddenException;
import kr.henein.api.repository.UserRepository;
import kr.henein.api.service.UserService;
import io.swagger.annotations.Api;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;


@RestController
@RequestMapping("/userinfo")
@RequiredArgsConstructor
@Api(tags = {"유저정보 제공 Controller"})
@Slf4j
public class UserInfoController {
    private final UserRepository userRepository;
    private final UserService userService;

    @Operation(summary = "유저 정보에 대한 요청 API")
    @GetMapping
    public UserInfoResponseDto userInfo(HttpServletRequest request){

        return userService.userInfo(request);
    }
    @Operation(summary = "유저 정보에 대한 요청 API")
    @GetMapping("/profile")
    public UserDetailInfoResponseDto userDetailInfo(HttpServletRequest request){

        return userService.userDetailInfo(request);
    }

    @Operation(summary = "유저 이름,사진 변경 API - [form-data]")
    @PostMapping(consumes = "multipart/form-data;charset=UTF-8")
    public String userUpdate(@ModelAttribute UserInfoChange userInfoChange, HttpServletRequest request) throws IOException {
        if (!userInfoChange.getUserName().trim().isEmpty() && (userInfoChange.getUserName().length() < 2 || userInfoChange.getUserName().length() > 15) )
            throw new ForbiddenException("이름이 너무 짧거나 깁니다.", ErrorCode.BAD_REQUEST);
        else if (userInfoChange.getUserName().trim().isEmpty() && userInfoChange.getImage().isEmpty())
            throw new ForbiddenException("수정할 사항이 없습니다.",ErrorCode.BAD_REQUEST);
        else if (userRepository.existsByUserName(userInfoChange.getUserName()))
            throw new ForbiddenException("사용자가 이미 존재합니다.", ErrorCode.BAD_REQUEST);
        return userService.userUpdate(userInfoChange, request);
    }
    @Operation(summary = "익명으로 재설정하기")
    @PostMapping("/anonymous")
    public void changeToAnonymous(HttpServletRequest request) {
        userService.changeToAnonymous(request);
    }

    //=====================메이플 캐릭터 관련=========================//
    @Operation(summary = "현재 인증된 모든 캐릭터 가져오기")
    @GetMapping("/character/all")
    public List<UserCharacterResponse> getAllUserCharacterInfo(HttpServletRequest request){
        return userService.getAllUserCharacterInfo(request);
    }

    @Operation(summary = "대표 캐릭터 설정")
    @PostMapping("/character/pick")
    public void pickCharacter(@RequestParam Long id, HttpServletRequest request){
        userService.pickCharacter(id,request);
    }

    @Operation(summary = "단일 캐릭터 정보갱신 요청")
    @GetMapping("/character/update/single/{id}")
    public Mono<UserCharacterResponse> updateSingleCharacter(@PathVariable Long id, HttpServletRequest request){
        return userService.updateSingleCharacter(id, request);
    }

    @Operation(summary = "전체 캐릭터 정보갱신 요청 list<String>")
    @PostMapping("/character/update/multiple")
    public Mono<List<UserCharacterResponse>> updateMultiCharacter(@RequestBody CharRefreshRequestDto charRefreshRequestDto, HttpServletRequest request) {
        return userService.updateMultiCharacter(charRefreshRequestDto, request);
    }

    @Operation(summary = "유저가 가지고있는 캐릭터 큐브 내역으로 불러오기" )
    @PostMapping("/character/auth")
    public Mono<String> requestNexon(@RequestBody UserMapleApi userMapleApi, HttpServletRequest request){
        return userService.requestToAPIServer(request,userMapleApi);

    }


    //================내 활동 관련 =====================//
    @Operation(summary = "내가 쓴 게시글 보기")
    @GetMapping("/active/board")
    public Page<BoardListResponseDto> getMyBoardList (HttpServletRequest request,@RequestParam String name, @RequestParam int page,
                                                      @RequestParam(required = false, defaultValue = "10") int size) {
        return userService.getMyBoardList(request, name, page, size);
    }
    @Operation(summary = "댓글 작성한 게시글 보기")
    @GetMapping("/active/comment")
    public Page<BoardListResponseDto> getMyBoardsWithCommentList (HttpServletRequest request,@RequestParam String name, @RequestParam int page,
                                                                  @RequestParam(required = false, defaultValue = "10") int size) {
        return userService.getMyBoardsWithCommentList(request, name, page, size);
    }
    //==============타인 조회 ====================//
    @Operation(summary = "유저 Name 값으로 조회 작성 게시글 조회 토큰 필요없음")
    @GetMapping ("/search")
    public List<BoardListResponseDto> searchBoardByName(@RequestParam String name) {
        if (name.equals("ㅇㅇ"))
            throw new BadRequestException("익명을 검색할 수 없습니다.", ErrorCode.BAD_REQUEST);
        return userService.searchBoardByName(name);
    }

}

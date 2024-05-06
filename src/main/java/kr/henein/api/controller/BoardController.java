package kr.henein.api.controller;

import io.micrometer.core.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.v3.oas.annotations.Operation;
import kr.henein.api.dto.board.*;
import kr.henein.api.service.CommonBoardService;
import kr.henein.api.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

@RestController()
@RequestMapping(value = "/board")
@RequiredArgsConstructor
@Slf4j
@Api(tags = {"게시글 Controller"})

public class BoardController {

    private final CommonBoardService commonBoardService;
    private final S3Service s3Service;


    @Operation(summary = "메인페이지 게시글 정렬정보 리턴, ",description = "0번인덱스 전체게시판, 그 후 넘버링된 순서대로 인덱스 배정")
    @GetMapping("/main")
    public List<MainPageResponseDTO> getMainPage() {
        return commonBoardService.getMainPageService();
    }

    @ApiImplicitParams({
            @ApiImplicitParam(name= "type", value= "원하는 게시판- 게시판 종류는 종류 호출 api 사용해서 정확하게 or 'ALL'이면 공지제외 전체 조회", required = true),
            @ApiImplicitParam(name = "page", value = "원하는 페이지 값", required = true)
    })
    @GetMapping()
    @Timed(value = "board.getPage",longTask = true)
    public Page<BoardListResponseDto> getTypeOfBoard(@RequestParam("type")String type  , @RequestParam("page")int page ){
        return type.equals("ALL") ? commonBoardService.getBoardNotNotice(page) : commonBoardService.getTypeOfBoard(page, type);
    }
    //Read
    @Operation(summary = "게시판 종류 검색")
    @GetMapping("/type-list")
    public String[] getTypeList() {
        return commonBoardService.getTypeList();
    }

    @Operation(summary = "id검색")
    @GetMapping("/{id}")
    @Timed(value = "board.getOne",longTask = true)
    public BoardResponseDto getOneBoard(@PathVariable Long id, @RequestHeader(value = "Authorization",required = false)String authentication){
        return commonBoardService.getOneService(id, authentication);
    }
//    Search
    @Operation(summary = "2자 이상 검색 가능")
    @GetMapping("/search")
    public Page<BoardSearchListResponseDto> SearchByText(@RequestParam("type")String type, @RequestParam ("key") String key, @RequestParam("page") int page ) {
        return commonBoardService.SearchByText(type, key, page);
    }
    //==================================================================================
    @Operation(summary = "Json 으로 보내주세요 [보안]")
    @PostMapping() //Create
    public long addTypeOfBoard(@RequestBody BoardRequestDto boardRequestDto, HttpServletRequest request ) {
        return commonBoardService.addTypeOfBoard(boardRequestDto, request);
    }

    @Operation(summary = "[보안]")
    @PostMapping("/recommend")
    public String recommendThisBoard(@RequestBody BoardIdRequestDTO boardIdRequestDTO, HttpServletRequest request){
        return commonBoardService.recommendThisBoard(boardIdRequestDTO.getId(),request);
    }
    @Operation(summary = "사진 name return api")
    @PostMapping("/image")
    public String saveImageAndGetName(@RequestPart MultipartFile image) throws IOException {
        return s3Service.uploadImageBeforeSavedBoardEntity(image);
    }
//==================================================================================
    @Operation(summary = "[보안]")
    @PutMapping("/{id}")
    public long updateBoard(@PathVariable Long id, @RequestBody BoardUpdateDto boardUpdateDto, HttpServletRequest request){
        return commonBoardService.updateService(id, boardUpdateDto, request);
    }
    @Operation(summary = "[보안]")
    @DeleteMapping("/{id}")
    public String deleteBoard(@PathVariable("id")Long id, HttpServletRequest request){
        return commonBoardService.deleteService(id, request);
    }

}

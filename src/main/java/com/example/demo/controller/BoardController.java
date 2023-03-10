package com.example.demo.controller;

import com.example.demo.dto.board.BoardRequestDto;
import com.example.demo.dto.board.BoardResponseDto;
import com.example.demo.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;


@RestController("")
@RequestMapping(value = "/board")
@RequiredArgsConstructor
@Slf4j
public class BoardController {

    private final CommonBoardService commonBoardService;
    private final BoardTypeOfService boardTypeOfService;

    @GetMapping("/entireboard") // 전체게시판 ( 공지게시판 호출없음)
    public Page<BoardResponseDto> getEntireBoard(@RequestParam("page")int page){

        return boardTypeOfService.getEntireBoard(page);
    }
    @PostMapping("/{boardtype}/{id}/updateview")
    public String updateView(@PathVariable Long id){
        return commonBoardService.updateView(id);
    }

    @GetMapping("/{boardtype}")
    public Page<BoardResponseDto> getTypeOfBoard(@PathVariable char boardtype, @RequestParam("page")int page){
        return boardTypeOfService.getTypeOfBoard(page, boardtype);
    }
    @PostMapping("/{boardtype}") //Create
    public String addTypeOfBoard(@PathVariable char boardtype, @RequestBody BoardRequestDto boardRequestDto){
        return boardTypeOfService.addTypeOfBoard(boardtype,boardRequestDto);
    }
    @GetMapping("/{boardtype}/{id}")
    public BoardResponseDto getOneBoardOfType(@PathVariable("id")Long id){
        return commonBoardService.getOneService(id);
    }
    @GetMapping("/search/{id}") //Read
    public BoardResponseDto getOneBoard(@PathVariable("id") Long id){
        return commonBoardService.getOneService(id);
    }

    @PutMapping("/{boardtype}/{id}")
    public String updateBoardOfType(@PathVariable("id")Long id,@RequestBody BoardRequestDto boardRequestDto){
        return commonBoardService.updateService(id, boardRequestDto);
    }
    @PutMapping ("/search/{id}") //Update
    public String updateBoard(@PathVariable("id") Long id,@RequestBody BoardRequestDto boardRequestDto) {
        return commonBoardService.updateService(id, boardRequestDto);
    }
    @DeleteMapping("/{boardtype}/{id}")
    public String deleteBoardOfType(@PathVariable("id")Long id){
        return commonBoardService.deleteService(id);
    }
    @DeleteMapping("/search/{id}") //Delete
    public String deleteBoard(@PathVariable("id") Long id){
        return commonBoardService.deleteService(id);
    }

    /*@PatchMapping("/{boardtype}/{id}/recommend")
    public String recommendThisBoard(@PathVariable("id")Long id){return commonBoardService.recommendThisBoard(id);}
    @PatchMapping("/{boardtype}/{id}/unRecommend")
    public String unRecommendThisBoard(@PathVariable("id")Long id){return commonBoardService.unRecommendThisBoard(id);}*/
}

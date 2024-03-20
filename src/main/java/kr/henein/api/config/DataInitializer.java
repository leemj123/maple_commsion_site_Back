package kr.henein.api.config;

import kr.henein.api.entity.BoardTypeEntity;
import kr.henein.api.enumCustom.BoardType;
import kr.henein.api.repository.BoardTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final BoardTypeRepository boardTypeRepository;
    @Override
    public void run(String... args) {
        List<BoardTypeEntity> boardTypeEntityList = new ArrayList<>();

        if ( !boardTypeRepository.existsByName("공지") ) {
            boardTypeEntityList.add(new BoardTypeEntity("공지", BoardType.Board, 0));
        }
        if ( !boardTypeRepository.existsByName("자유") ) {
            boardTypeEntityList.add(new BoardTypeEntity("자유", BoardType.Board,1));
        }
        boardTypeRepository.saveAll(boardTypeEntityList);
    }
}

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

        if ( !boardTypeRepository.existsByName("NOTICE") ) {
            boardTypeEntityList.add(new BoardTypeEntity("NOTICE", BoardType.Board, 0));
        }
        if ( !boardTypeRepository.existsByName("FREE") ) {
            boardTypeEntityList.add(new BoardTypeEntity("FREE", BoardType.Board,1));
        }
        boardTypeRepository.saveAll(boardTypeEntityList);
    }
}

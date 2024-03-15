package kr.henein.api.config;

import kr.henein.api.entity.BoardTypeEntity;
import kr.henein.api.repository.BoardTypeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private BoardTypeRepository boardTypeRepository;
    @Override
    public void run(String... args) {
        if ( !boardTypeRepository.existsByType("NOTICE") ) {
            BoardTypeEntity boardType = new BoardTypeEntity("NOTICE");
            boardTypeRepository.save(boardType);
        }
        if ( !boardTypeRepository.existsByType("FREE") ) {
            BoardTypeEntity boardType = new BoardTypeEntity("FREE");
            boardTypeRepository.save(boardType);
        }
    }
}

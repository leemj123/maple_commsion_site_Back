package kr.henein.api.service;

import kr.henein.api.dto.ComplainRequestDto;
import kr.henein.api.error.ErrorCode;
import kr.henein.api.error.exception.NotFoundException;
import kr.henein.api.jwt.JwtTokenProvider;
import kr.henein.api.repository.BoardRepository;
import kr.henein.api.repository.CommentRepository;
import kr.henein.api.repository.ReplyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;

@Service
@RequiredArgsConstructor
public class ComplainService {

    private final JwtTokenProvider jwtTokenProvider;
    private final BoardRepository boardRepository;
    private final CommentRepository commentRepository;
    private final ReplyRepository replyRepository;
    @Transactional
    public ResponseEntity<?> complainThisService(ComplainRequestDto complainRequestDto, HttpServletRequest request) {

        switch (complainRequestDto.getComplainType()) {
            case Comment:
                if (!commentRepository.existsById(complainRequestDto.getTargetId())) {
                    throw new NotFoundException("cannot found target", ErrorCode.NOT_FOUND);
                }
            case Board:
                if (!boardRepository.existsById(complainRequestDto.getTargetId())) {
                    throw new NotFoundException("cannot found target", ErrorCode.NOT_FOUND);
                }
            case Reply:
                if (!replyRepository.existsById(complainRequestDto.getTargetId())) {
                    throw new NotFoundException("cannot found target", ErrorCode.NOT_FOUND);
                }

        }
        String userEmail = jwtTokenProvider.fetchUserEmailByHttpRequest(request);



    }
}

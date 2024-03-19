package kr.henein.api.service;

import kr.henein.api.dto.ComplainRequestDto;
import kr.henein.api.entity.*;
import kr.henein.api.error.ErrorCode;
import kr.henein.api.error.exception.BadRequestException;
import kr.henein.api.error.exception.NotFoundException;
import kr.henein.api.jwt.JwtTokenProvider;
import kr.henein.api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;

@Service
@RequiredArgsConstructor
public class ComplainService {

    private final JwtTokenProvider jwtTokenProvider;
    private final BoardRepository boardRepository;
    private final CommentRepository commentRepository;
    private final ReplyRepository replyRepository;
    private final UserRepository userRepository;
    private final ComplaintRepository complaintRepository;

    @Transactional
    public ResponseEntity<?> complainThisService(ComplainRequestDto complainRequestDto, HttpServletRequest request) {
        String userEmail = jwtTokenProvider.fetchUserEmailByHttpRequest(request);
        UserEntity complainer = userRepository.findByUserEmail(userEmail)
                .orElseThrow(()->new NotFoundException(ErrorCode.NOT_FOUND.getMessage(), ErrorCode.NOT_FOUND));
        UserEntity complainantUser;

        switch (complainRequestDto.getComplainType()) {
            case Comment:
                CommentEntity commentEntity = commentRepository.findById(complainRequestDto.getTargetId())
                        .orElseThrow(()->new NotFoundException("cannot found target", ErrorCode.NOT_FOUND));
                complainantUser = commentEntity.getBoardEntity().getUserEntity();

                saveComplaintEntity(complainRequestDto,complainer,complainantUser);

                return ResponseEntity.ok().build();
            case Board:
                BoardEntity boardEntity = boardRepository.findById(complainRequestDto.getTargetId())
                        .orElseThrow(()->new NotFoundException("cannot found target", ErrorCode.NOT_FOUND));
                complainantUser = boardEntity.getUserEntity();

                saveComplaintEntity(complainRequestDto,complainer,complainantUser);

                return ResponseEntity.ok().build();
            case Reply:
                ReplyEntity replyEntity = replyRepository.findById(complainRequestDto.getTargetId())
                        .orElseThrow(()->new NotFoundException("cannot found target", ErrorCode.NOT_FOUND));

                complainantUser = userRepository.findByUserEmail(replyEntity.getUserEmail())
                        .orElseThrow(()->new NotFoundException(ErrorCode.NOT_FOUND.getMessage(), ErrorCode.NOT_FOUND));

                saveComplaintEntity(complainRequestDto,complainer,complainantUser);

                return ResponseEntity.ok().build();
            default: throw new BadRequestException(ErrorCode.BAD_REQUEST.getMessage(), ErrorCode.BAD_REQUEST);
        }

    }

    private void saveComplaintEntity(ComplainRequestDto complainRequestDto, UserEntity complainer, UserEntity complainantUser) {
        ComplaintEntity complaintEntity = ComplaintEntity.builder()
                .complainType(complainRequestDto.getComplainType())
                .complainReason(complainRequestDto.getComplainReason())
                .targetId(complainRequestDto.getTargetId())
                .text(complainRequestDto.getText())
                .complainerId(complainer.getId())
                .complainerName(complainer.getUserName())
                .complainantId(complainantUser.getId())
                .complainantName(complainantUser.getUserName())
                .build();

        complaintRepository.save(complaintEntity);
    }
}

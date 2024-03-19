package kr.henein.api.enumCustom;

import kr.henein.api.error.ErrorCode;
import kr.henein.api.error.exception.NotFoundException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ComplainReason {
    abuse("욕설/비방",0),
    advertisement("광고/도배",1),
    obscene("음란물",2),
    IllegalFilm("불법촬영물",3),
    another("기타",4);


    private final String title;
    private final int key;

    public ComplainReason findReasonByKey(int key){
        switch (key) {
            case 0: return abuse;
            case 1: return advertisement;
            case 2: return obscene;
            case 3: return IllegalFilm;
            case 4: return another;
            default: throw new NotFoundException(ErrorCode.NOT_EXIST_TYPE.getMessage(), ErrorCode.NOT_EXIST_TYPE);
        }
    }

}

package UMC_8th.With_Run.common.apiResponse.status;

import UMC_8th.With_Run.common.apiResponse.basecode.BaseCode;
import UMC_8th.With_Run.common.apiResponse.dto.ReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum SuccessCode implements BaseCode {
    // User
    LOGIN_SUCCESS(HttpStatus.OK, "COMMON2001", "로그인에 성공했습니다."),
    LOGOUT_SUCCESS(HttpStatus.OK, "COMMON2002", "로그아웃에 성공했습니다."),
    SIGN_IN_SUCCESS(HttpStatus.CREATED, "COMMON2003", "회원가입에 성공했습니다."),
    VALIDATE_SUCCESS (HttpStatus.CONTINUE, "COMMON2004", "이메일, 닉네임 검증에 성공했습니다"),

    // Common
    IN_PROCESSING(HttpStatus.ACCEPTED, "COMMON2005", "요청 처리 중에 있습니다."),
    INQUIRY_SUCCESS (HttpStatus.OK, "COMMON200", "정보 조회에 성공했습니다."),
    REQUEST_SUCCESS (HttpStatus.OK, "COMMON200", "요청 처리가 성공했습니다"),

    // Chat
    CHAT_CREATE_SUCCESS(HttpStatus.CREATED, "CHAT2000", "채팅 생성에 성공했습니다."),
    GET_INVITE_SUCCESS(HttpStatus.OK, "CHAT2001", "채팅방 초대 목록 조회에 성공했습니다"),
    INVITE_SUCCESS(HttpStatus.OK, "CHAT2002", "사용자 초대에 성공했습니다"),
    RENAME_SUCCESS(HttpStatus.OK, "CHAT2003", "채팅방 이름 변경에 성공했습니다"),
    ENTER_CHAT_SUCCESS (HttpStatus.OK, "CHAT2004", "채팅방 진입에 성공했습니다"),
    LEAVE_CHAT_SUCCESS (HttpStatus.OK, "CHAT2005", "채팅방 나가기에 성공했습니다"),
    GET_LIST_SUCCESS (HttpStatus.OK, "CHAT2006", "참여 채팅방 목록 조회에 성공했습니다"),
    DELETE_CHAT_SUCCESS (HttpStatus.OK, "CHAT2007", "참여 채팅방 삭제에 성공했습니다"),
    CHATTING_SUCCESS (HttpStatus.OK, "CHAT2008", "메세징에 성공했습니다"),
    SHARE_COURSE_SUCCESS(HttpStatus.OK, "CHAT2009", "산책 코스 공유 메세징에 성공했습니다"),



    //Map
    CREATE_SUCCESS(HttpStatus.CREATED, "COMMON2006", "핀 생성에 성공했습니다."),
    UPDATE_SUCCESS(HttpStatus.OK, "COMMON2007", "핀 수정에 성공했습니다."),
    DELETE_SUCCESS(HttpStatus.OK, "COMMON2008", "핀 삭제에 성공했습니다.");




    private final HttpStatus status;
    private final String code;
    private final String message;


    @Override
    public ReasonDTO getReason() {
        return ReasonDTO.builder()
                .isSuccess(true)
                .code(code)
                .message(message)
                .build();
    }

    @Override
    public ReasonDTO getReasonHttpStatus() {
        return ReasonDTO.builder()
                .httpStatus(status)
                .isSuccess(true)
                .code(code)
                .message(message)
                .build();
    }
}

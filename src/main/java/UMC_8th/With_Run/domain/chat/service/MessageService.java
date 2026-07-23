package UMC_8th.With_Run.domain.chat.service;

import UMC_8th.With_Run.domain.chat.dto.ChatRequestDTO;

public interface MessageService {

    // 실질적인 채팅 Method
    // authenticatedEmail: STOMP CONNECT 시점에 검증된 Principal의 email(JwtTokenProvider.getAuthentication 참고).
    // reqDTO.getUserId()는 클라이언트가 임의로 채울 수 있어 신원 판단에 쓰지 않고, 이 값으로만 발신자를 조회한다.
    void chatting (Long chatId, ChatRequestDTO.ChattingReqDTO reqDTO, String authenticatedEmail);

    // 산책코스 공유 Method
    void shareCourse (ChatRequestDTO.ShareReqDTO reqDTO);

}

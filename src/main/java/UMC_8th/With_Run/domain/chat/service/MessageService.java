package UMC_8th.With_Run.domain.chat.service;

import UMC_8th.With_Run.domain.chat.dto.ChatRequestDTO;
import UMC_8th.With_Run.domain.chat.dto.MessagePersistResultDTO;

public interface MessageService {

    // MySQL 전용 단계(@Transactional) — authenticatedEmail: STOMP CONNECT 시점에 검증된 Principal의 email.
    // reqDTO.getUserId()는 클라이언트가 임의로 채울 수 있어 신원 판단에 쓰지 않고, 이 값으로만 발신자를 조회한다.
    // 트랜잭션이 끝나(커넥션이 반납된) 뒤에 publishToRedis를 호출할 수 있도록, Controller가 두 메서드를 순차 호출한다.
    MessagePersistResultDTO persistMessage(Long chatId, ChatRequestDTO.ChattingReqDTO reqDTO, String authenticatedEmail);

    // Redis 전용 단계(@Transactional 밖) — persistMessage 결과로 안읽은 메세지 카운팅 + lastReceivedMsg 갱신 + PUBLISH.
    void publishToRedis(Long chatId, MessagePersistResultDTO result);

    // 산책코스 공유 Method
    void shareCourse (ChatRequestDTO.ShareReqDTO reqDTO);

}

package UMC_8th.With_Run.domain.chat.dto;

import UMC_8th.With_Run.domain.chat.entity.Message;
import UMC_8th.With_Run.domain.user.entity.Profile;
import io.micrometer.core.instrument.Timer;

import java.util.List;

// persistMessage(MySQL 트랜잭션) -> publishToRedis(Redis I/O) 로 데이터를 넘기기 위한 중간 DTO.
// Controller가 두 메서드를 순차 호출하는 사이, MySQL 조회 결과를 재조회 없이 그대로 전달한다.
public record MessagePersistResultDTO(
        Long userId,
        Profile profile,
        Message message,
        List<Long> userChatList,
        Timer.Sample sample
) {
}

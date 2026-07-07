package UMC_8th.With_Run.chat.service;

import UMC_8th.With_Run.chat.dto.ChatRequestDTO;

public interface MessageService {

    // 실질적인 채팅 Method
    void chatting (Long chatId, ChatRequestDTO.ChattingReqDTO reqDTO);

    // 산책코스 공유 Method
    void shareCourse (ChatRequestDTO.ShareReqDTO reqDTO);

}

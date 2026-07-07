package UMC_8th.With_Run.notice.dto;

import UMC_8th.With_Run.notice.entity.NoticeType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NoticeResponse {
    private Long id;
    private String message;
    private Long receiverId;
    private String receiverName;
    private Long actorId;
    private String actorName;
    private NoticeType noticeType;
    private Long courseId;
    private String courseName;
}

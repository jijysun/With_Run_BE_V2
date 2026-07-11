    package UMC_8th.With_Run.domain.friend.service;

    import UMC_8th.With_Run.global.exception.GeneralException;
    import UMC_8th.With_Run.global.apiResponse.status.ErrorCode;
    import UMC_8th.With_Run.domain.user.repository.FollowRepository;
    import jakarta.transaction.Transactional;
    import lombok.RequiredArgsConstructor;
    import org.springframework.beans.factory.annotation.Value;
    import org.springframework.stereotype.Service;

    import java.net.URI;
    import java.net.http.HttpClient;
    import java.net.http.HttpRequest;
    import java.net.http.HttpResponse;


    import org.slf4j.Logger;
    import org.slf4j.LoggerFactory;

    @Service
    @RequiredArgsConstructor
    public class ReportService {

        private static final Logger logger = LoggerFactory.getLogger(ReportService.class);

        private final FollowRepository followRepository;

        @Value("${discord.webhook.url}")
        private String discordWebhookUrl;

        public void sendReportToDiscord(Long reporterId, Long reportedId, String reason) {
            try {
                String payload = String.format(
                        "{\"content\": \"🚨 사용자 신고 🚨\\n신고자 ID: %d\\n피신고자 ID: %d\\n사유: %s\"}",
                        reporterId, reportedId, reason
                );

                HttpClient client = HttpClient.newHttpClient();
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(discordWebhookUrl))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload))
                        .build();

                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                logger.info("Discord webhook response status: {}", response.statusCode());
                logger.info("Discord webhook response body: {}", response.body());

                if (response.statusCode() / 100 != 2) {
                    logger.error("Discord webhook 호출 실패, 상태 코드: {}, 응답: {}", response.statusCode(), response.body());
                    throw new GeneralException(ErrorCode.REPORT_ERROR);
                }

            } catch (Exception e) {
                logger.error("Discord webhook 호출 중 예외 발생", e);
                throw new GeneralException(ErrorCode.REPORT_ERROR);
            }
        }

        @Transactional
        public void removeFollowRelation(Long userId, Long targetId) {
            followRepository.deleteByUserIdAndTargetUserId(userId, targetId);
            followRepository.deleteByUserIdAndTargetUserId(targetId, userId);
        }
    }

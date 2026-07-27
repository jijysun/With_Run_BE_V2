package UMC_8th.With_Run.global.observer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class ObserverResponseDTO {

    @Getter @Builder @NoArgsConstructor @AllArgsConstructor
    public static class IssueTokenDTO {
        private String accessToken;
        private Long expiresIn; // ms
    }
}

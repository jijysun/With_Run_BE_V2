package UMC_8th.With_Run.common.apiResponse;


import UMC_8th.With_Run.common.apiResponse.status.SuccessCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonPropertyOrder ({"isSuccess", "code", "message", "result"})
public class StndResponse<T> {

    @JsonProperty
    private boolean success;

    private String code;

    private String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T result;

    public static <T> StndResponse<T> onSuccess(T result, SuccessCode status) {
        return new StndResponse<>(true, status.getCode(), status.getMessage(), result);
    }

    public static <T> StndResponse<T> onFailure(String code, String message, T data) {
        return new StndResponse<>(false, code, message, data);
    }

}


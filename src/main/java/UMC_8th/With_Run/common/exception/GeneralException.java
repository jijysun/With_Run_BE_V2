package UMC_8th.With_Run.common.exception;

import UMC_8th.With_Run.common.apiResponse.basecode.BaseErrorCode;
import UMC_8th.With_Run.common.apiResponse.dto.ErrorReasonDTO;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GeneralException extends RuntimeException {

    private BaseErrorCode errorCode;

    public ErrorReasonDTO getReason (){
        return this.errorCode.getReason();
    }

    public ErrorReasonDTO getReasonHttpStatus (){
        return this.errorCode.getReasonHttpStatus();
    }

}

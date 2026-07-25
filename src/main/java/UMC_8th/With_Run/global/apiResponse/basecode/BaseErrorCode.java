package UMC_8th.With_Run.global.apiResponse.basecode;

import UMC_8th.With_Run.global.apiResponse.dto.ErrorReasonDTO;

public interface BaseErrorCode {

    ErrorReasonDTO getReason();

    ErrorReasonDTO getReasonHttpStatus();

}

package UMC_8th.With_Run.common.apiResponse.basecode;

import UMC_8th.With_Run.common.apiResponse.dto.ErrorReasonDTO;

public interface BaseErrorCode {

    ErrorReasonDTO getReason();

    ErrorReasonDTO getReasonHttpStatus();

}

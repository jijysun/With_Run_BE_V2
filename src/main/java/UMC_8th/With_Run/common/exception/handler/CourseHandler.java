package UMC_8th.With_Run.common.exception.handler;

import UMC_8th.With_Run.common.apiResponse.basecode.BaseErrorCode;
import UMC_8th.With_Run.common.exception.GeneralException;

public class CourseHandler extends GeneralException {
    public CourseHandler(BaseErrorCode errorCode) {
        super(errorCode);
    }
}

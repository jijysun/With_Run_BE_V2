package UMC_8th.With_Run.global.exception.handler;

import UMC_8th.With_Run.global.apiResponse.basecode.BaseErrorCode;
import UMC_8th.With_Run.global.exception.GeneralException;

public class ObserverHandler extends GeneralException {
    public ObserverHandler(BaseErrorCode errorCode) {
        super(errorCode);
    }
}

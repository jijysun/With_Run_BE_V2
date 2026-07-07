package UMC_8th.With_Run.common.exception.handler;

import UMC_8th.With_Run.common.apiResponse.basecode.BaseErrorCode;
import UMC_8th.With_Run.common.exception.GeneralException;

public class MapHandler extends GeneralException {
    public MapHandler(BaseErrorCode errorCode) {
        super(errorCode);
    }
}

package UMC_8th.With_Run.global.exception.handler;

import UMC_8th.With_Run.global.apiResponse.basecode.BaseErrorCode;
import UMC_8th.With_Run.global.exception.GeneralException;

public class ChatHandler extends GeneralException {
    public ChatHandler(BaseErrorCode errorCode) {
        super(errorCode);
    }
}

# General Exception 사용법


Service 로직에서 보통 비즈니스 로직을 다루니, Service 코드를 기준으로 잡겠습니다.

만약 이상한 로직 상의 오류가 발생했어요!!
- db에는 없는 정보 요청
- 알 수 없는 사용자
- 이미 초대된 사용자 중복 초대
- ...

여러가지 오류가 있겠죠...

일단 UserService라는 곳에서 오류가 발생했을 때 이런 식이 될 거에요

저는 이 때 각 Handler를 만들어 놓았고, 해당 오류에 대한 에러 코드를 넘겨주기만 하면 되요!

```java
import UMC_8th.With_Run.common.apiResponse.status.ErrorCode;
import UMC_8th.With_Run.common.exception.handler.UserHandler;

public class UserService {
    if(true) { // 무조건 에러가 났다고 했을 때
        throw new UserHandler(ErrorCode.WRONG_USER);
    }
}
```

그래서 Controller 에서는
1. 무조건 성공 기준의 응답 형식만 반환하시면 됩니다
2. 에러 상황인 경우 return 대신 throw 가 호출되어 에러 응답이 반환됩니다.

추가적인 에러 상황이 필요하거나, 궁금하신 점은 24시간 김석현에게 DM 해주세요!
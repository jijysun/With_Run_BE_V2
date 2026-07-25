// GET /api/chat/{chatId}/invite 스모크 테스트.
// UserChatRepository.getCanInviteUser()의 JPQL 생성자 표현식(SELECT NEW ...)이 패키지 리팩토링 후
// 깨졌던 지점(FQCN 문자열이 domain.chat 으로 안 바뀌어 있었음)을 다시 깨뜨리지 않는지 확인하는 회귀 테스트로 사용.
//
// 실행 (레포 루트에서):
//   k6 run -e JWT_SECRET=<로컬 .env의 JWT_SECRET_KEY> src/test/k6/With_Run_Chat_GetCanInviteUser_test.js
// 다른 유저/방으로 확인하고 싶으면:
//   k6 run -e JWT_SECRET=<...> -e EMAIL=loadtest_002@loadtest.local -e CHAT_ID=2 src/test/k6/With_Run_Chat_GetCanInviteUser_test.js

import http from 'k6/http';
import { check, sleep } from 'k6';
import crypto from 'k6/crypto';
import encoding from 'k6/encoding';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.1.0/index.js';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const JWT_SECRET = __ENV.JWT_SECRET;
const EMAIL = __ENV.EMAIL || 'loadtest_001@loadtest.local';
const CHAT_ID = __ENV.CHAT_ID || 1;

export const options = {
    vus: 1,
    iterations: 1,
};

function base64url(str) {
    return encoding.b64encode(str, 'rawurl');
}

// JwtTokenProvider.generateToken 과 동일한 방식(HS256, sub=email, claim role)으로
// 로그인 API 호출 없이 로컬에서 직접 서명한다.
function generateJwt(email) {
    const header = { alg: 'HS256', typ: 'JWT' };
    const now = Math.floor(Date.now() / 1000);
    const payload = { sub: email, role: 'ROLE_USER', iat: now, exp: now + 3600 };

    const signingInput = base64url(JSON.stringify(header)) + '.' + base64url(JSON.stringify(payload));
    const signature = crypto.hmac('sha256', JWT_SECRET, signingInput, 'base64rawurl');
    return signingInput + '.' + signature;
}

export default function () {
    if (!JWT_SECRET) {
        throw new Error('JWT_SECRET 환경변수가 필요합니다 (로컬 .env의 JWT_SECRET_KEY와 동일 값)');
    }

    const token = generateJwt(EMAIL);
    const params = {
        headers: {
            Authorization: 'Bearer ' + token,
            'Content-Type': 'application/json',
        },
    };

    const res = http.get(BASE_URL + '/api/chat/' + CHAT_ID + '/invite', params);

    check(res, {
        'status 200 (JPQL SemanticException 재발 안 함)': (r) => r.status === 200,
        '응답 body가 500 에러 스택트레이스 아님': (r) => !(r.body || '').includes('SemanticException'),
    });

    sleep(1);
}

export function handleSummary(data) {
    return {
        stdout: textSummary(data, { indent: ' ', enableColors: true }),
    };
}

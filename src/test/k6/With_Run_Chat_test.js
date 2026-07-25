// STOMP 채팅 수동 스모크 테스트: 1개 커넥션으로 CONNECT → SUBSCRIBE → SEND → 수신 확인까지 눈으로 확인.
// 부하테스트(With_Run_Chat_LoadTest_*.js)와 달리 로그를 자세히 찍어서 디버깅용으로 쓴다.
//
// 실행 (레포 루트에서):
//   k6 run -e JWT_SECRET=<로컬 .env의 JWT_SECRET_KEY> src/test/k6/With_Run_Chat_test.js
// 다른 유저/방으로 확인하고 싶으면:
//   k6 run -e JWT_SECRET=<...> -e EMAIL=loadtest_002@loadtest.local -e CHAT_ID=2 src/test/k6/With_Run_Chat_test.js

import ws from 'k6/ws';
import { check } from 'k6';
import crypto from 'k6/crypto';
import encoding from 'k6/encoding';

const WS_URL = __ENV.WS_URL || 'ws://localhost:8080/api/ws';
const JWT_SECRET = __ENV.JWT_SECRET;
const EMAIL = __ENV.EMAIL || 'loadtest_001@loadtest.local';
const USER_ID = Number(__ENV.USER_ID || 1); // EMAIL과 실제로 매칭되는 유저 id로 맞출 것(현재는 인증 미비로 payload 값을 그대로 신뢰함 — Critical #1 참고)
const CHAT_ROOM_ID = __ENV.CHAT_ID || 1;

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

// STOMP 프레임은 반드시 NULL(\x00) 바이트로 끝나야 한다.
function stompFrame(command, headers, body) {
    let frame = command + '\n';
    for (const key in headers) {
        frame += key + ':' + headers[key] + '\n';
    }
    frame += '\n' + (body || '') + '\x00';
    return frame;
}

export default function () {
    if (!JWT_SECRET) {
        throw new Error('JWT_SECRET 환경변수가 필요합니다 (로컬 .env의 JWT_SECRET_KEY와 동일 값)');
    }

    const token = generateJwt(EMAIL);

    const res = ws.connect(WS_URL, {}, function (socket) {
        console.log('연결 시도: WebSocket 핸드셰이크 완료');

        socket.on('open', function () {
            socket.send(stompFrame('CONNECT', {
                'accept-version': '1.1,1.0',
                'heart-beat': '0,0',
                Authorization: 'Bearer ' + token,
            }));

            socket.send(stompFrame('SUBSCRIBE', {
                id: 'sub-0',
                destination: '/sub/' + CHAT_ROOM_ID + '/msg',
            }));
            console.log('구독 요청 전송: /sub/' + CHAT_ROOM_ID + '/msg');

            const payload = JSON.stringify({
                userId: USER_ID, // Critical #1 미해결 상태라 payload의 이 값이 그대로 신뢰됨(고친 후엔 Principal에서 추출하도록 바뀔 지점)
                message: 'k6에서 보낸 테스트 메시지',
                isCourse: false,
            });

            socket.send(stompFrame('SEND', {
                destination: '/pub/' + CHAT_ROOM_ID + '/msg',
                'content-type': 'application/json',
            }, payload));
            console.log('메시지 전송: /pub/' + CHAT_ROOM_ID + '/msg');
        });

        socket.on('message', function (msg) {
            console.log('서버로부터 수신: ' + msg);
        });

        // 3초 후 종료
        socket.setTimeout(function () {
            console.log('테스트 종료 — 소켓 닫음');
            socket.close();
        }, 3000);

        socket.on('close', function () {
            console.log('WebSocket 연결 종료됨');
        });
        socket.on('error', function (e) {
            console.error('WebSocket 오류: ' + e);
        });
    });

    check(res, {
        'STOMP WebSocket 연결 성공(101)': (r) => r && r.status === 101,
    });
}

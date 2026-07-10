// 채팅 부하테스트 베이스라인: 4인 풀방 25개(=100명)가 각자 1~2초 간격으로 계속 메시지를 보낼 때의 처리량/지연시간을 측정
//
// 준비:
// 1. .env 에서 LOADTEST_SEED_ENABLED=true 로 설정 후 docker compose up
// -> LoadTestDataSeeder 가 500명 유저 + 125개 4인방을 자동으로 채운다.
// 2. 그중 100명(25방) 매핑을 DB에서 뽑아 이 파일과 같은 디렉토리에
//      loadtest-mapping.json 으로 저장한다. 형식:
//      [{ "userId": 1, "email": "loadtest_001@loadtest.local", "chatId": 1 }, ...]
//
// 실행:
//   k6 run -e JWT_SECRET=<로컬 .env의 JWT_SECRET_KEY> src/test/With_Run_Chat_LoadTest_Baseline.js
//   (필요 시 -e WS_URL=ws://localhost:8080/api/ws 로 대상 서버 변경)

import ws from 'k6/ws';
import { check } from 'k6';
import crypto from 'k6/crypto';
import encoding from 'k6/encoding';
import { Trend, Counter, Rate } from 'k6/metrics';

const WS_URL = __ENV.WS_URL || 'ws://localhost:8080/api/ws';
const JWT_SECRET = __ENV.JWT_SECRET;

const ROOM_COUNT = 25;
const ROOM_CAPACITY = 4;
const VU_COUNT = ROOM_COUNT * ROOM_CAPACITY; // 100
const TEST_DURATION_MS = 3 * 60 * 1000; // 3분 — 개선 후 재측정 시에도 동일 기준 유지
const MIN_SEND_INTERVAL_MS = 1000;
const MAX_SEND_INTERVAL_MS = 2000;

const mapping = JSON.parse(open('./loadtest-mapping.json'));

// ===== 커스텀 메트릭 =====
const broadcastLatency = new Trend('chat_broadcast_latency_ms', true);
const messagesSent = new Counter('chat_messages_sent');
const messagesReceived = new Counter('chat_messages_received');
const connectErrors = new Counter('chat_connect_errors');
const sendErrors = new Counter('chat_send_errors');
const connectSuccessRate = new Rate('chat_connect_success_rate');

export const options = {
  scenarios: {
    chat_baseline: {
      executor: 'per-vu-iterations',
      vus: VU_COUNT,
      iterations: 1,
      maxDuration: '5m',
    },
  },
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

  const signingInput = `${base64url(JSON.stringify(header))}.${base64url(JSON.stringify(payload))}`;
  const signature = crypto.hmac('sha256', JWT_SECRET, signingInput, 'base64rawurl');
  return `${signingInput}.${signature}`;
}

function stompFrame(command, headers, body) {
  let frame = command + '\n';
  for (const key in headers) {
    frame += `${key}:${headers[key]}\n`;
  }
  frame += '\n' + (body || '') + '\x00';
  return frame;
}

export default function () {
  if (!JWT_SECRET) {
    throw new Error('JWT_SECRET 환경변수가 필요합니다 (로컬 .env의 JWT_SECRET_KEY와 동일 값)');
  }

  const me = mapping[(__VU - 1) % mapping.length];
  const token = generateJwt(me.email);
  const sentAt = {};
  let seq = 0;

  const res = ws.connect(WS_URL, {}, function (socket) {
    let buffer = '';

    socket.on('open', function () {
      socket.send(stompFrame('CONNECT', {
        'accept-version': '1.1,1.0',
        'heart-beat': '0,0',
        Authorization: `Bearer ${token}`,
      }));
    });

    socket.on('message', function (data) {
      buffer += data;
      const frames = buffer.split('\x00');
      buffer = frames.pop(); // 마지막 조각은 미완성 프레임일 수 있으니 다음 수신까지 보관

      for (const raw of frames) {
        const frame = raw.replace(/^\n+/, '');
        if (frame.length === 0) continue;

        if (frame.startsWith('CONNECTED')) {
          connectSuccessRate.add(true);

          socket.send(stompFrame('SUBSCRIBE', {
            id: 'sub-0',
            destination: `/sub/${me.chatId}/msg`,
          }));

          socket.setInterval(function () {
            seq += 1;
            const correlationId = `loadtest-${__VU}-${seq}-${Date.now()}`;
            sentAt[correlationId] = Date.now();

            const body = JSON.stringify({
              userId: me.userId,
              message: correlationId,
              isCourse: false,
            });

            socket.send(stompFrame('SEND', {
              destination: `/pub/${me.chatId}/msg`,
              'content-type': 'application/json',
            }, body));

            messagesSent.add(1);
          }, MIN_SEND_INTERVAL_MS + Math.random() * (MAX_SEND_INTERVAL_MS - MIN_SEND_INTERVAL_MS));
        } else if (frame.startsWith('MESSAGE')) {
          const bodyStart = frame.indexOf('\n\n');
          const body = bodyStart >= 0 ? frame.slice(bodyStart + 2) : '';
          messagesReceived.add(1);

          try {
            const parsed = JSON.parse(body);
            const correlationId = parsed.msg;
            if (correlationId && sentAt[correlationId]) {
              broadcastLatency.add(Date.now() - sentAt[correlationId]);
              delete sentAt[correlationId];
            }
          } catch (e) {
            // 초대/공유 등 JSON이 아니거나 상관관계 없는 시스템 메시지는 지연시간 측정에서 제외
          }
        } else if (frame.startsWith('ERROR')) {
          sendErrors.add(1);
        }
      }
    });

    socket.on('error', function () {
      connectErrors.add(1);
      connectSuccessRate.add(false);
    });

    socket.setTimeout(function () {
      socket.close();
    }, TEST_DURATION_MS);
  });

  check(res, { 'STOMP WebSocket 연결 성공(101)': (r) => r && r.status === 101 });
}

// 채팅 2차 부하테스트: 4인 풀방 50개(=200명)가 각자 1~2초 간격으로 계속 메시지를 보낼 때의
// 처리량/지연시간/동시성 이슈를 베이스라인(25방·100명, With_Run_Chat_LoadTest_Baseline.js) 대비 측정한다.
//
// 베이스라인과 달리 방/유저 수·지속시간을 전부 -e 옵션으로 바꿀 수 있게 뺐다(재측정 시 VU만 바꾸면 됨).
//
// 준비:
// 1. .env 에서 LOADTEST_SEED_ENABLED=true, LOADTEST_SEED_USER_COUNT=200 설정
//    (LoadTestDataSeeder.java 가 이 값을 읽어 유저 수를 조절함 — application.yml의 loadtest.seed.user-count 참고)
// 2. 이전 시드가 남아있으면 유저 수가 어긋나므로 볼륨까지 지우고 재기동:
//      docker compose --env-file .env -f docker/docker-compose.yml down -v
//      docker compose --env-file .env -f docker/docker-compose.yml up -d --build
//    로그에서 "유저 200명 신규 생성..., 채팅방 50개 신규 생성" 확인
// 3. 시딩된 200명(50방) 전체를 DB에서 뽑아 loadtest-mapping-200.json 으로 저장.
//    주의(Windows): -p와 비밀번호 사이에 공백을 넣지 말 것(-p<비번>). 공백을 넣으면
//    mysql이 비밀번호를 별도 인자로 오인해 파싱이 깨지고 대신 usage 도움말이 출력된다.
//      docker exec withrun-mysql mysql -uwithrun -p<비번> withrun -N -B -e "
//      SELECT JSON_ARRAYAGG(JSON_OBJECT('userId', userId, 'email', email, 'chatId', chatId))
//      FROM (SELECT u.id AS userId, u.email AS email, uc.chat_id AS chatId
//            FROM \`user\` u JOIN user_chat uc ON uc.user_id = u.id
//            WHERE u.login_id LIKE 'loadtest_%' ORDER BY u.id LIMIT 200) t;
//      " > src/test/k6/result/loadtest-mapping-200.json
// 4. 결과 저장 폴더는 이 스크립트와 같은 위치의 result/ 를 사용(이미 존재, 없으면 mkdir -p src/test/k6/result)
//
// 실행 (레포 루트에서, 기본값: 200 VU, 3분, 접속 분산 10초):
//   k6 run -e JWT_SECRET=<JWT_SECRET_KEY> src/test/k6/With_Run_Chat_LoadTest_V2.js
// 부하량을 바꿔 재측정하고 싶을 때(예: 400명):
//   k6 run -e VU_COUNT=400 -e MAPPING_FILE=./result/loadtest-mapping-400.json \
//          -e JWT_SECRET=<...> src/test/k6/With_Run_Chat_LoadTest_V2.js
// 발신 간격도 바꾸고 싶을 때(기본 1000~2000ms 무작위 폭):
//   -e MIN_SEND_INTERVAL_MS=500 -e MAX_SEND_INTERVAL_MS=1000   (0.5~1초 무작위 폭)
//   -e MIN_SEND_INTERVAL_MS=1000 -e MAX_SEND_INTERVAL_MS=1000  (MIN=MAX -> 전 VU가 정확히 1초 간격, 무작위 폭 없음)

import ws from 'k6/ws';
import { check, sleep } from 'k6';
import crypto from 'k6/crypto';
import encoding from 'k6/encoding';
import { Trend, Counter, Rate } from 'k6/metrics';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.1.0/index.js';

const WS_URL = __ENV.WS_URL || 'ws://localhost:8080/api/ws';
const JWT_SECRET = __ENV.JWT_SECRET;
// open()은 이 스크립트 파일 위치(src/test/k6/) 기준 상대경로임에 주의(handleSummary 출력 경로와는 기준이 다름)
const MAPPING_FILE = __ENV.MAPPING_FILE || './result/loadtest-mapping-200.json';

const ROOM_CAPACITY = 4;
const VU_COUNT = Number(__ENV.VU_COUNT || 200);
// 3분(180s) 고정 — 베이스라인과 동일 기준을 유지해 "부하량"만 변수로 남긴다(아래 실행 결과 설명 참고).
const TEST_DURATION_SEC = Number(__ENV.DURATION_SEC || 180);
// 200개 커넥션이 t=0에 한꺼번에 몰리는 스탬피드를 피하기 위해 접속 시각을 무작위로 흩뿌리는 폭(초).
const RAMP_SEC = Number(__ENV.RAMP_SEC || 10);
const TEST_DURATION_MS = TEST_DURATION_SEC * 1000;
// 기본값(1000/2000)은 기존 실행과 동일 — MIN=MAX로 지정하면 무작위 폭 없이 정확히 그 값으로 고정됨
// (예: MIN_SEND_INTERVAL_MS=MAX_SEND_INTERVAL_MS=1000 -> 전 VU가 정확히 1초 간격).
const MIN_SEND_INTERVAL_MS = Number(__ENV.MIN_SEND_INTERVAL_MS || 1000);
const MAX_SEND_INTERVAL_MS = Number(__ENV.MAX_SEND_INTERVAL_MS || 2000);
const LABEL = __ENV.LABEL || `${VU_COUNT}vu`;

const mapping = JSON.parse(open(MAPPING_FILE));

// ===== 커스텀 메트릭 (베이스라인과 동일한 이름 — 비교 가능하도록 유지) =====
const broadcastLatency = new Trend('chat_broadcast_latency_ms', true);
const messagesSent = new Counter('chat_messages_sent');
const messagesReceived = new Counter('chat_messages_received');
const connectErrors = new Counter('chat_connect_errors');
const sendErrors = new Counter('chat_send_errors');
const connectSuccessRate = new Rate('chat_connect_success_rate');

export const options = {
  scenarios: {
    chat_v2: {
      executor: 'per-vu-iterations',
      vus: VU_COUNT,
      iterations: 1,
      maxDuration: `${TEST_DURATION_SEC + RAMP_SEC + 60}s`,
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

  // 접속 시각을 흩뿌려 커넥션 스탬피드 자체가 정상 상태(steady-state) 지연시간 측정을 오염시키지 않게 한다.
  sleep(Math.random() * RAMP_SEC);

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

// ===== 결과 파일 저장 =====
// --summary-export 플래그는 k6 v0.30.0 이후 deprecated → 공식 권장 방식인 handleSummary() 사용.
// (참고: https://grafana.com/docs/k6/latest/results-output/end-of-test/custom-summary/)
// 주의: handleSummary()가 반환하는 파일 경로는 (open()과 달리) k6 실행 시점의 현재 작업 디렉토리(CWD) 기준.
// result/analysis가 dev_notes 레포로 이전된 뒤(3671e43)엔 레포 루트 기준 상대경로만으로 못 찾는다 —
// run_and_report.py가 -e RESULT_DIR=<절대경로>로 넘겨주므로 그 값을 그대로 쓰고, k6를 직접(하네스 없이)
// 돌릴 때만 기본값(레포 루트 기준 형제 디렉터리 가정)으로 폴백한다.
// 파일명: {RESULT_DIR}/{YYYYMMDD-HHmm}-{VU}vu.{txt,json} — 실행 시점과 부하량이 파일명만 봐도 드러나게.
// txt/json 모두 k6가 생성한 원본 그대로 저장하고, 해석/분석은 여기 담지 않는다.
const RESULT_DIR = __ENV.RESULT_DIR || '../dev_notes/With_Run_V2/result';

function timestamp() {
  const d = new Date();
  const pad = (n) => String(n).padStart(2, '0');
  return `${d.getFullYear()}${pad(d.getMonth() + 1)}${pad(d.getDate())}-${pad(d.getHours())}${pad(d.getMinutes())}`;
}

export function handleSummary(data) {
  const base = `${RESULT_DIR}/${timestamp()}-${LABEL}`;

  return {
    stdout: textSummary(data, { indent: ' ', enableColors: true }), // 콘솔에는 기존과 동일하게 출력
    [`${base}.txt`]: textSummary(data, { indent: ' ', enableColors: false }),
    [`${base}.json`]: JSON.stringify(data, null, 2), // 여러 회차 비교용 원본 수치
  };
}

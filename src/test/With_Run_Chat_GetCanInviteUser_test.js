import http from 'k6/http';
import { check, sleep } from 'k6';
// 💡 이 라인을 추가해주세요!
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';

// 테스트 옵션 설정
export const options = {
    // 100명의 가상 유저가 10초 동안 테스트를 수행합니다.
    vus: 100,
    duration: '10s',

    // 성능 목표치(Thresholds) 설정
    thresholds: {
        // HTTP 에러는 없어야 합니다.
        http_req_failed: ['rate<0.01'],
        // 95%의 요청은 200ms 안에 처리되어야 합니다.
        http_req_duration: ['p(95)<200'],
    },
};

// 메인 테스트 함수
export default function () {
    // --- 사전 준비 ---
    const chatId = 40;
    const TOKEN = ''; // 여기에 JWT 토큰 넣기
    const authToken = `Bearer ${TOKEN}`;

    const params = {
        headers: {
            'Authorization': authToken,
            'Content-Type': 'application/json',
        },
    };

    // --- API 요청 ---
    const res = http.get(`http://localhost:8080/api/chat/${chatId}/invite`, params);

    // --- 결과 검증 ---
    check(res, {
        'is status 200': (r) => r.status === 200,
    });

    // 실제 사용자처럼 1초간 대기
    sleep(1);
}

// 이 함수는 이제 정상적으로 동작합니다.
export function handleSummary(data) {
    // 콘솔에 기본 요약 출력
    console.log(textSummary(data, { indent: ' ', enableColors: true }));

    // 요약 데이터를 JSON 파일로 저장
    return {
        'summary.json': JSON.stringify(data, null, 2), // 정상적으로 파일 생성
        // stdout: textSummary(data), // 터미널에도 똑같이 출력하고 싶을 때
    };
}
import ws from 'k6/ws';
import { check } from 'k6';

const CHAT_ROOM_ID = 1;
const TOKEN = ''; // 여기에 JWT 토큰 넣기

export default function () {
    const url = 'http://localhost:8080/api/ws/websocket';

    const headers = {
        Authorization: `Bearer ${TOKEN}`,
    };

    const res = ws.connect(url, { headers }, function (socket) {
        console.log('🔗 WebSocket 연결 완료');

        // STOMP CONNECT
        socket.send(
            'CONNECT\naccept-version:1.2\nheart-beat:10000,10000\n' +
            `Authorization: Bearer ${TOKEN}\n\n\u0000`
        );

        // SUBSCRIBE
        socket.send(
            `SUBSCRIBE\nid:sub-0\ndestination:/sub/${CHAT_ROOM_ID}/msg\n\n\u0000`
        );
        console.log(`📥 /sub/${CHAT_ROOM_ID}/msg 구독 완료`);

        // SEND MESSAGE
        const payload = JSON.stringify({
            message: 'k6에서 보낸 테스트 메시지',
            isCourse: false
        });

        socket.send(
            `SEND\ndestination:/pub/${CHAT_ROOM_ID}/msg\ncontent-type:application/json\n\n${payload}\u0000`
        );
        console.log(`📤 /pub/${CHAT_ROOM_ID}/msg 로 메시지 전송 완료`);

        socket.on('message', (msg) => {
            console.log('✅ 서버로부터 받은 메시지:', msg);
        });

        // 3초 후 종료
        socket.setTimeout(() => {
            console.log('⏱ 테스트 종료');
            socket.close();
        }, 3000);

        socket.on('close', () => console.log('🔒 WebSocket 연결 종료'));
        socket.on('error', (e) => console.error('❌ WebSocket 오류:', e));
    });

    check(res, {
        'WebSocket 연결 성공 (status 101)': (r) => r && r.status === 101,
    });
}

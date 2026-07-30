-- 안읽음 메시지 카운트 원자적 증가 스크립트
-- 목적:
--   "이 유저가 지금 채팅방을 보고 있지 않으면(isChatting=false) 안읽음 카운트 ++,
--    그 유저-채팅방 조합을 '나중에 MySQL과 동기화해야 할 키' 목록에 등록"
--   라는 3단계 로직(조회 -> 조건부 증가 -> 조건부 dirty 등록)을 Redis 서버 안에서 통 짜로 원자적 실행이 필요


--   KEYS[1] : 안읽음 카운트를 확인/증가시킬 대상 Hash 키. 예) "user:42:7" (userId=42, chatId=7)
--   KEYS[2] : 자정 배치(RedisSyncScheduler)가 MySQL과 동기화할 때 참고하는
--             "변경된 키 목록" Set의 키. RedisSyncScheduler.DIRTY_USER_CHAT_KEY와 동일한 값.
--   ARGV[1] : 안읽음 카운트를 얼마나 늘릴지(항상 1이지만 하드코딩하지 않고 자바에서 넘겨받음)

local isChatting = redis.call('HGET', KEYS[1], 'isChatting')

-- Redis Hash에 해당 필드 자체가 없으면 HGET은 Lua의 false 반환
-- 이 프로젝트는 항상 문자열 "false"/"true"로 저장하므로, 문자열 비교로 충분할 듯
if isChatting == 'false' then
    redis.call('HINCRBY', KEYS[1], 'unReadMsg', ARGV[1])
    redis.call('SADD', KEYS[2], KEYS[1])
    return 1
else
    return 0
end

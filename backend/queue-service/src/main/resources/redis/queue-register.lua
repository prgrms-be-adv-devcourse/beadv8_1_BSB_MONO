-- 대기열 등록: 이미 입장(active)한 회원인지 확인하고, 아니면 줄(waiting)에 넣는다
-- 확인과 추가를 한 번에 해서, 그 사이에 승격(waiting → active)이 끼어들지 못하게 한다
-- KEYS[1] race:{raceId}:racecourse:{raceCourseId}:waiting  KEYS[2] race:{raceId}:racecourse:{raceCourseId}:active
-- KEYS[3] queue:waiting
-- ARGV[1] 회원 ID  ARGV[2] 지금(epoch ms)  ARGV[3] {raceId}:{raceCourseId}
-- 돌려주는 값: { 'ACTIVE' } 또는 { 'WAITING', 내 앞 인원(0부터), 줄 전체 인원 }
local activeUntil = redis.call('ZSCORE', KEYS[2], ARGV[1])
if activeUntil and tonumber(activeUntil) > tonumber(ARGV[2]) then
	return { 'ACTIVE' }  -- 끝 시각이 지나지 않은 active. 끝 시각이 지났으면 스케줄러가 지우기 전이라도 다시 줄을 설 수 있다
end

redis.call('ZADD', KEYS[1], 'NX', ARGV[2], ARGV[1])  -- 이미 줄에 있으면(다른 기기 등) 처음 순번을 유지한다
redis.call('SADD', KEYS[3], ARGV[3])  -- 스케줄러가 승격할 대기열 목록. 이미 들어 있으면 그대로다
return { 'WAITING', redis.call('ZRANK', KEYS[1], ARGV[1]), redis.call('ZCARD', KEYS[1]) }

-- 재발급: 들어온 토큰이 지금 쓸 수 있는 것인지 판정하고, 정상이면 새 토큰으로 바꾼다
-- 판정과 교체를 한 번에 해서, 같은 토큰으로 동시에 온 두 요청이 둘 다 정상으로 통과하지 못하게 한다
-- KEYS[1] members:{memberId}:refreshToken
-- ARGV[1] 들어온 토큰의 familyId  ARGV[2] 들어온 토큰의 jti
-- ARGV[3] 새 jti  ARGV[4] 새 토큰 원본  ARGV[5] 지금(초)  ARGV[6] 유예(초)  ARGV[7] 새 토큰 만료(초, TTL)
-- 돌려주는 값: { 판정 } 또는 GRACE면 { 'GRACE', 지금 토큰 원본 }
if redis.call('EXISTS', KEYS[1]) == 0 then
	return { 'MISSING' }  -- 로그아웃, 탈취 처리, TTL 만료
end

local saved = redis.call('HMGET', KEYS[1], 'familyId', 'currentJti', 'previousJti', 'rotatedAt', 'currentToken')
local familyId, currentJti, previousJti, rotatedAt, currentToken = saved[1], saved[2], saved[3], saved[4], saved[5]

if familyId ~= ARGV[1] then
	return { 'STALE' }  -- 다른 기기에서 새로 로그인해 family가 바뀌었다. 지금 family는 건드리지 않는다
end

if currentJti == ARGV[2] then
	redis.call('HSET', KEYS[1], 'previousJti', currentJti, 'currentJti', ARGV[3], 'currentToken', ARGV[4], 'rotatedAt', ARGV[5])
	redis.call('EXPIREAT', KEYS[1], ARGV[7])
	return { 'ROTATED' }
end

if previousJti == ARGV[2] and tonumber(ARGV[5]) - tonumber(rotatedAt) <= tonumber(ARGV[6]) then
	return { 'GRACE', currentToken }  -- 방금 회전된 토큰이 유예 안에 다시 왔다. 새로 만들지 않고 지금 토큰을 돌려준다
end

redis.call('DEL', KEYS[1])  -- 같은 family의 이미 쓴 토큰: 누가 진짜인지 모르므로 family를 끊는다
return { 'THEFT' }

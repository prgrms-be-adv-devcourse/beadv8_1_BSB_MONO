-- 로그아웃: 들어온 토큰의 family가 지금 family일 때만 지운다
-- 확인과 삭제 사이에 다른 기기의 새 로그인이 끼면 새 family를 지우게 되므로 한 번에 한다
-- KEYS[1] members:{memberId}:refreshToken
-- ARGV[1] 들어온 토큰의 familyId
-- 돌려주는 값: 지웠으면 1, 아니면 0
if redis.call('HGET', KEYS[1], 'familyId') == ARGV[1] then
	return redis.call('DEL', KEYS[1])
end
return 0

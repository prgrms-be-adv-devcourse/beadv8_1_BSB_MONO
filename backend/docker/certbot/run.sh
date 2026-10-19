#!/bin/sh
# certbot 컨테이너 실행 스크립트 (compose.prod.yml의 certbot entrypoint)
# 인증서가 없으면 처음 발급하고, 있으면 12시간마다 갱신을 시도한다 (certbot은 만료 30일 전부터만 실제로 갱신한다)
# 발급·갱신된 인증서는 볼륨(certbot-conf)에 남고, Nginx의 start-https.sh가 1분 안에 알아채 reload한다
set -u

: "${DOMAIN:?DOMAIN이 비어 있습니다. backend/.env에 적어 주세요}"
: "${CERTBOT_EMAIL:?CERTBOT_EMAIL이 비어 있습니다. backend/.env에 적어 주세요}"

WEBROOT=/var/www/certbot
CHALLENGE_DIR="$WEBROOT/.well-known/acme-challenge"
CERT="/etc/letsencrypt/live/${DOMAIN}/fullchain.pem"
RENEW_INTERVAL=43200
RETRY_INTERVAL=900

# Let's Encrypt는 실패한 확인도 횟수를 센다 (도메인당 1시간에 5번). 그래서 발급을 요청하기 전에
# "도메인 → 이 서버 → Nginx → 토큰 폴더" 길이 열렸는지 같은 방법(http://도메인/.well-known/...)으로 먼저 확인한다
# compose가 처음 뜰 때 Nginx는 게이트웨이·서비스가 모두 뜬 뒤에야 열리므로, 열릴 때까지 30초마다 다시 본다
wait_until_reachable() {
	token="preflight-$(date +%s)"
	mkdir -p "$CHALLENGE_DIR"
	echo "$token" > "$CHALLENGE_DIR/$token"
	until wget -q -T 5 -O - "http://${DOMAIN}/.well-known/acme-challenge/${token}" 2>/dev/null | grep -q "$token"; do
		echo "[certbot] http://${DOMAIN} 에 아직 닿지 않습니다. 30초 뒤 다시 확인합니다 (DNS·보안 그룹 80·Nginx 기동 확인)"
		sleep 30
	done
	rm -f "$CHALLENGE_DIR/$token"
	echo "[certbot] http://${DOMAIN} 확인 완료"
}

# 이미 계정이 있으면 계정 이메일을 .env 값으로 맞춘다. .env의 CERTBOT_EMAIL만 바꾸고 컨테이너를 다시 띄우면 반영된다
# (계정 이메일은 발급할 때 한 번 등록되므로, 이 단계가 없으면 .env를 바꿔도 Let's Encrypt 쪽 값은 그대로다)
if [ -f "$CERT" ]; then
	certbot update_account --email "$CERTBOT_EMAIL" --no-eff-email --non-interactive \
		|| echo "[certbot] 계정 이메일 변경 실패. 인증서 갱신에는 영향이 없습니다"
fi

while :; do
	if [ -f "$CERT" ]; then
		# 저장된 갱신 설정(webroot 경로 등)을 그대로 쓴다. 만료가 30일 넘게 남았으면 아무것도 하지 않는다
		certbot renew --non-interactive
	else
		wait_until_reachable
		if ! certbot certonly --webroot -w "$WEBROOT" -d "$DOMAIN" \
			--email "$CERTBOT_EMAIL" --agree-tos --no-eff-email --non-interactive; then
			echo "[certbot] 발급 실패. ${RETRY_INTERVAL}초 뒤 다시 시도합니다"
			sleep "$RETRY_INTERVAL"
			continue
		fi
	fi
	sleep "$RENEW_INTERVAL"
done

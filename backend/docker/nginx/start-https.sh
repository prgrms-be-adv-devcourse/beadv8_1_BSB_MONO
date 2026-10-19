#!/bin/sh
# 운영 Nginx 시작 스크립트 (compose.prod.yml의 nginx command)
# 인증서가 있으면 https.conf.template, 없으면 http.conf.template으로 설정을 만들고 Nginx를 띄운다
# 띄운 뒤에는 1분마다 인증서를 확인해, 처음 발급되거나 갱신되면 설정을 다시 만들고 reload한다
# (Nginx는 시작할 때 읽은 인증서를 계속 쓰므로, 파일이 바뀌어도 reload해야 새 인증서를 읽는다)
set -eu

: "${DOMAIN:?DOMAIN이 비어 있습니다. backend/.env에 적어 주세요}"

CERT="/etc/letsencrypt/live/${DOMAIN}/fullchain.pem"
TEMPLATES=/etc/nginx/crewrun
TARGET=/etc/nginx/conf.d/default.conf

# 인증서가 없으면 빈 값, 있으면 실제 파일의 수정 시각. 처음 발급되거나 갱신되면 값이 바뀐다
cert_stamp() {
	if [ -f "$CERT" ]; then
		stat -L -c %Y "$CERT"
	fi
}

render() {
	if [ -f "$CERT" ]; then
		template=https.conf.template
	else
		template=http.conf.template
	fi
	# 바꿀 변수를 DOMAIN 하나로 정해, $host 같은 Nginx 변수는 건드리지 않는다
	envsubst '${DOMAIN}' < "$TEMPLATES/$template" > "$TARGET"
	echo "[start-https] ${template} 적용 (${DOMAIN})"
}

render

(
	last=$(cert_stamp)
	while :; do
		sleep 60
		now=$(cert_stamp)
		if [ "$now" != "$last" ]; then
			render
			# 새 설정이 틀리면 reload하지 않는다. 떠 있는 Nginx는 이전 설정으로 계속 응답한다
			if nginx -t; then
				nginx -s reload
				echo "[start-https] reload 완료"
			else
				echo "[start-https] 설정 검사 실패, reload하지 않음"
			fi
			last=$now
		fi
	done
) &

exec nginx -g 'daemon off;'

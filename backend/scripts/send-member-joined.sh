#!/usr/bin/env bash
# member-service 없이 cash-service를 확인하기 위해 가짜 회원가입 이벤트(MemberJoinedEvent)를 보낸다.
# cash-service가 받으면 CashMember 저장 → queuing.cashmember.created 발행 → Wallet 생성 순으로 처리한다.
#
# 사용법: ./scripts/send-member-joined.sh [id] [nickname]
# 예:     ./scripts/send-member-joined.sh 1001 테스터

set -euo pipefail

ID="${1:-1001}"
NICKNAME="${2:-테스터${ID}}"
NOW="$(date +%Y-%m-%dT%H:%M:%S)"

# 토픽 이름은 common의 KafkaTopics.MEMBER_MEMBER_JOINED와 같아야 한다
TOPIC="queuing.member.joined"
MESSAGE="{\"id\":${ID},\"createDate\":\"${NOW}\",\"modifyDate\":\"${NOW}\",\"nickname\":\"${NICKNAME}\"}"

echo "${MESSAGE}" | docker exec -i bukang-kafka rpk topic produce "${TOPIC}" -k "${ID}"
echo "보낸 메시지: ${MESSAGE}"

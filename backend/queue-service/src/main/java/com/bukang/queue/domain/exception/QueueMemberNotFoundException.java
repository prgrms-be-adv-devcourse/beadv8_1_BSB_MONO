package com.bukang.queue.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

/**
 * 순번을 조회한 회원이 그 대회·종목의 줄(waiting)에도 없고 입장(active)한 상태도 아닐 때
 * 등록하지 않았거나, active 끝 시각이 지났거나, Redis가 비워진 경우다. 없는 대회·종목 ID로 불러도 여기로 온다
 */
public class QueueMemberNotFoundException extends BusinessException {
	public QueueMemberNotFoundException() {
		super(HttpStatus.NOT_FOUND, "대기열에 없는 회원입니다.");
	}
}

package com.bukang.queue.domain.exception;

import org.springframework.http.HttpStatus;

import com.bukang.common.global.exception.BusinessException;

/**
 * 이미 입장(active)한 회원이 같은 대회·종목의 줄을 다시 서려 할 때
 * 다시 줄에 넣으면 재승격 때 active 끝 시각이 덮어써져 활성 시간이 늘어난다
 */
public class QueueAlreadyActiveException extends BusinessException {
	public QueueAlreadyActiveException() {
		super(HttpStatus.CONFLICT, "이미 입장한 회원입니다.");
	}
}

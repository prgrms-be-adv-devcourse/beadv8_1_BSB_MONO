-- 판매자 신청 결과 확인 시각
-- 신청자가 승인·반려 결과를 처음 본 시각을 남겨, 결과 화면을 한 번만 보여 준다 (Seller.resultCheckedAt)
-- 신청서마다 두므로 반려 뒤 다시 신청하면 새 신청서는 비어 있어 새 결과를 다시 보여 준다
-- 기존 신청은 NULL(아직 안 봄)로 둔다. 타입은 requested_at·reviewed_at과 같다
ALTER TABLE member_seller ADD COLUMN result_checked_at DATETIME(6) NULL;

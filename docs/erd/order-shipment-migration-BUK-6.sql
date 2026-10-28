-- 키트 배송을 배송 표로 옮기는 SQL (BUK-6, 2026-10-28)
-- 언제: BUK-6을 배포해 주문 서비스가 새 표(order_shipment, order_shipment_item)를 만든 직후 1번 (ddl-auto: update)
-- 무엇: 주문 상품의 옛 키트 칸(kit_*)과 주문의 옛 배송지 칸(recipient_name ~ address_detail)을 배송 표로 옮긴다
-- 다시 돌려도 된다: 이미 배송이 있는 주문, 이미 상자 속 상품이 있는 배송은 건너뛴다
-- 옛 칸은 이번 배포에서 남겨 두고(새 코드는 읽지도 쓰지도 않음), 다음 배포에서 지운다
-- MySQL 8 기준. 테스트(H2 MySQL 모드)에서도 같은 파일을 돌려 확인한다 (KitDeliveryTest.migrationSql)

-- 1) 배송: 키트가 있고 결제했던 주문 상품마다 1건 (주문 하나 = 참가권 하나라 주문마다 1건)
--    배송 준비였는데 결제 완료면 발송 대기, 이미 취소됐으면 배송 취소. 배송 중·배송 완료는 그대로
--    결제 전(ORDERED)·만료된 주문은 배송이 없다 (새 코드도 결제할 때만 만든다)
INSERT INTO order_shipment (create_date, modify_date, order_id, status, carrier, tracking_number,
	recipient_name, recipient_phone, zipcode, address, address_detail, delivery_memo,
	shipped_at, delivered_at, canceled_at)
SELECT CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), i.order_id,
	CASE
		WHEN i.kit_delivery_status <> 'PREPARING' THEN i.kit_delivery_status
		WHEN i.status = 'PAID' THEN 'READY'
		ELSE 'CANCELED'
	END,
	i.kit_carrier, i.kit_tracking_number,
	o.recipient_name, o.recipient_phone, o.zipcode, o.address, o.address_detail, NULL,
	i.kit_shipped_at, i.kit_delivered_at,
	CASE WHEN i.kit_delivery_status = 'PREPARING' AND i.status <> 'PAID' THEN i.canceled_at END
FROM order_order_item i
	JOIN order_order o ON o.id = i.order_id
WHERE i.kit_delivery_status IS NOT NULL
	AND i.status <> 'ORDERED'
	AND NOT EXISTS (SELECT 1 FROM order_shipment s WHERE s.order_id = i.order_id);

-- 2) 상자 속 상품: 1)에서 만든(아직 상품이 없는) 배송에 그 주문의 키트 상품을 1개씩 넣는다
INSERT INTO order_shipment_item (create_date, modify_date, shipment_id, order_item_id, quantity)
SELECT CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6), s.id, i.id, 1
FROM order_shipment s
	JOIN order_order_item i ON i.order_id = s.order_id
WHERE i.kit_delivery_status IS NOT NULL
	AND NOT EXISTS (SELECT 1 FROM order_shipment_item si WHERE si.shipment_id = s.id);

-- 3) 확인 (결과를 눈으로 본다. 1)·2)를 돌린 뒤 둘 다 0이어야 한다)
-- 3-1) 옮겨야 하는데 배송 상자에 없는 키트 주문 상품 수
SELECT COUNT(*) AS not_migrated
FROM order_order_item i
WHERE i.kit_delivery_status IS NOT NULL
	AND i.status <> 'ORDERED'
	AND NOT EXISTS (SELECT 1 FROM order_shipment_item si WHERE si.order_item_id = i.id);

-- 3-2) 옛 칸과 새 배송의 상태·송장이 다른 줄 수
SELECT COUNT(*) AS mismatched
FROM order_order_item i
	JOIN order_shipment_item si ON si.order_item_id = i.id
	JOIN order_shipment s ON s.id = si.shipment_id
WHERE i.kit_delivery_status IS NOT NULL
	AND (s.status <> CASE
			WHEN i.kit_delivery_status <> 'PREPARING' THEN i.kit_delivery_status
			WHEN i.status = 'PAID' THEN 'READY'
			ELSE 'CANCELED'
		END
		OR COALESCE(s.tracking_number, '') <> COALESCE(i.kit_tracking_number, ''));

-- 다음 배포에서 옛 칸을 지울 때 (BUK-6 다음 이슈, 확인 쿼리가 0인 것을 본 뒤)
-- ALTER TABLE order_order_item DROP COLUMN kit_delivery_status, DROP COLUMN kit_carrier,
-- 	DROP COLUMN kit_tracking_number, DROP COLUMN kit_shipped_at, DROP COLUMN kit_delivered_at;
-- ALTER TABLE order_order DROP COLUMN recipient_name, DROP COLUMN recipient_phone, DROP COLUMN zipcode,
-- 	DROP COLUMN address, DROP COLUMN address_detail;

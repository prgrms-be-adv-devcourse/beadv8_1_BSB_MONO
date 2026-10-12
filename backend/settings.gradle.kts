rootProject.name = "bukang"

// 공통 라이브러리(common, jwt)와 서비스별 Spring Boot 앱
include("common", "jwt", "member-service", "cash-service", "payout-service", "file-service", "order-service", "product-service")

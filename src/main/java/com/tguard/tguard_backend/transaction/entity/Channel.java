package com.tguard.tguard_backend.transaction.entity;

public enum Channel {

        ONLINE,    // 웹/앱 기반 결제
        OFFLINE,   // 오프라인 매장(POS, 키오스크)
        MOBILE,    // 모바일 앱 전용 채널
        WEB,       // 웹 브라우저 전용
        POS,       // 전통적인 POS 단말
        KIOSK,     // 키오스크 단말
        UNKNOWN;   // 매핑 불가 시
}

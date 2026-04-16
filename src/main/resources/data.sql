-- =====================================================
-- TastingNote 초기 술 데이터
-- 작성일: 2026-04-16
-- 수량: 약 170개 + AlcoholAlias
-- 로컬: H2 인메모리 — 앱 시작 시 자동 실행 (매번 fresh)
-- 프로덕션 MySQL: SSH 접속 후 1회만 실행
--   scp data.sql ubuntu@13.124.79.235:~/
--   mysql -u root -p tastingnote < data.sql
-- Flyway 도입 시: V2__seed_data.sql 으로 이름만 변경
-- =====================================================

-- =====================================================
-- 1. ALCOHOL 데이터
-- =====================================================

-- --------------------------------------------------
-- 위스키 (WHISKEY) - 64개
-- --------------------------------------------------
INSERT INTO alcohol (name, name_ko, category, origin, region, abv, created_at, updated_at) VALUES

-- 스카치 블렌디드 (19개)
('Johnnie Walker Black Label', '조니워커 블랙라벨', 'WHISKEY', 'Scotland', 'Blended Scotch', 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Johnnie Walker Red Label', '조니워커 레드라벨', 'WHISKEY', 'Scotland', 'Blended Scotch', 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Johnnie Walker Blue Label', '조니워커 블루라벨', 'WHISKEY', 'Scotland', 'Blended Scotch', 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Johnnie Walker Gold Reserve', '조니워커 골드리저브', 'WHISKEY', 'Scotland', 'Blended Scotch', 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Johnnie Walker Double Black', '조니워커 더블블랙', 'WHISKEY', 'Scotland', 'Blended Scotch', 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Johnnie Walker Green Label', '조니워커 그린라벨', 'WHISKEY', 'Scotland', 'Blended Malt Scotch', 43.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Chivas Regal 12 Year', '시바스 리갈 12년', 'WHISKEY', 'Scotland', 'Blended Scotch', 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Chivas Regal 18 Year', '시바스 리갈 18년', 'WHISKEY', 'Scotland', 'Blended Scotch', 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Chivas Regal 25 Year', '시바스 리갈 25년', 'WHISKEY', 'Scotland', 'Blended Scotch', 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Ballantine''s Finest', '발렌타인 파인스트', 'WHISKEY', 'Scotland', 'Blended Scotch', 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Ballantine''s 17 Year', '발렌타인 17년', 'WHISKEY', 'Scotland', 'Blended Scotch', 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Ballantine''s 21 Year', '발렌타인 21년', 'WHISKEY', 'Scotland', 'Blended Scotch', 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Ballantine''s 30 Year', '발렌타인 30년', 'WHISKEY', 'Scotland', 'Blended Scotch', 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Monkey Shoulder', '멍키 숄더', 'WHISKEY', 'Scotland', 'Blended Malt Scotch', 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Dewar''s 12 Year', '듀어스 12년', 'WHISKEY', 'Scotland', 'Blended Scotch', 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Famous Grouse', '페이머스 그라우스', 'WHISKEY', 'Scotland', 'Blended Scotch', 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Grant''s Family Reserve', '그란츠 패밀리 리저브', 'WHISKEY', 'Scotland', 'Blended Scotch', 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('J&B Rare', '제이앤비 레어', 'WHISKEY', 'Scotland', 'Blended Scotch', 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('William Lawson''s', '윌리엄 로슨', 'WHISKEY', 'Scotland', 'Blended Scotch', 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- 싱글 몰트 스페이사이드 (14개)
('Glenfiddich 12 Year', '글렌피딕 12년', 'WHISKEY', 'Scotland', 'Speyside', 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Glenfiddich 15 Year', '글렌피딕 15년', 'WHISKEY', 'Scotland', 'Speyside', 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Glenfiddich 18 Year', '글렌피딕 18년', 'WHISKEY', 'Scotland', 'Speyside', 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Glenfiddich 21 Year', '글렌피딕 21년', 'WHISKEY', 'Scotland', 'Speyside', 43.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('The Macallan 12 Year Double Cask', '더 맥캘란 12년 더블 캐스크', 'WHISKEY', 'Scotland', 'Speyside', 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('The Macallan 12 Year Sherry Cask', '더 맥캘란 12년 쉐리 캐스크', 'WHISKEY', 'Scotland', 'Speyside', 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('The Macallan 18 Year Sherry Cask', '더 맥캘란 18년 쉐리 캐스크', 'WHISKEY', 'Scotland', 'Speyside', 43.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('The Macallan 25 Year', '더 맥캘란 25년', 'WHISKEY', 'Scotland', 'Speyside', 43.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Glenlivet 12 Year', '더 글렌리벳 12년', 'WHISKEY', 'Scotland', 'Speyside', 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Glenlivet 15 Year', '더 글렌리벳 15년', 'WHISKEY', 'Scotland', 'Speyside', 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Glenlivet 18 Year', '더 글렌리벳 18년', 'WHISKEY', 'Scotland', 'Speyside', 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Aberlour 12 Year', '에이블러 12년', 'WHISKEY', 'Scotland', 'Speyside', 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Aberlour A''Bunadh', '에이블러 어버나', 'WHISKEY', 'Scotland', 'Speyside', 61.2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Glenfarclas 15 Year', '글렌파클라스 15년', 'WHISKEY', 'Scotland', 'Speyside', 46.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- 싱글 몰트 하이랜드 (6개)
('Dalmore 12 Year', '달모어 12년', 'WHISKEY', 'Scotland', 'Highland', 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Dalmore 15 Year', '달모어 15년', 'WHISKEY', 'Scotland', 'Highland', 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Highland Park 12 Year', '하이랜드 파크 12년', 'WHISKEY', 'Scotland', 'Highland', 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Highland Park 18 Year', '하이랜드 파크 18년', 'WHISKEY', 'Scotland', 'Highland', 43.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Oban 14 Year', '오반 14년', 'WHISKEY', 'Scotland', 'Highland', 43.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Glenmorangie Original 10 Year', '글렌모렌지 오리지널 10년', 'WHISKEY', 'Scotland', 'Highland', 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- 싱글 몰트 아일라 (8개)
('Laphroaig 10 Year', '라프로익 10년', 'WHISKEY', 'Scotland', 'Islay', 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Laphroaig Quarter Cask', '라프로익 쿼터 캐스크', 'WHISKEY', 'Scotland', 'Islay', 48.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Ardbeg 10 Year', '아드벡 10년', 'WHISKEY', 'Scotland', 'Islay', 46.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Ardbeg Uigeadail', '아드벡 우가달', 'WHISKEY', 'Scotland', 'Islay', 54.2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Ardbeg Corryvreckan', '아드벡 코리브레칸', 'WHISKEY', 'Scotland', 'Islay', 57.1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Bowmore 12 Year', '보모어 12년', 'WHISKEY', 'Scotland', 'Islay', 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Lagavulin 16 Year', '라가불린 16년', 'WHISKEY', 'Scotland', 'Islay', 43.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Bruichladdich The Classic Laddie', '브룩라디 더 클래식 래디', 'WHISKEY', 'Scotland', 'Islay', 50.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- 아메리칸 버번/테네시 (9개)
('Jack Daniel''s Old No. 7', '잭다니엘 올드 넘버7', 'WHISKEY', 'USA', 'Tennessee', 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Jack Daniel''s Gentleman Jack', '잭다니엘 젠틀맨 잭', 'WHISKEY', 'USA', 'Tennessee', 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Jack Daniel''s Single Barrel', '잭다니엘 싱글 배럴', 'WHISKEY', 'USA', 'Tennessee', 45.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Jim Beam White', '짐빔 화이트', 'WHISKEY', 'USA', 'Kentucky', 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Jim Beam Black', '짐빔 블랙', 'WHISKEY', 'USA', 'Kentucky', 43.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Maker''s Mark', '메이커스 마크', 'WHISKEY', 'USA', 'Kentucky', 45.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Woodford Reserve', '우드포드 리저브', 'WHISKEY', 'USA', 'Kentucky', 43.2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Buffalo Trace', '버팔로 트레이스', 'WHISKEY', 'USA', 'Kentucky', 45.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Wild Turkey 101', '와일드 터키 101', 'WHISKEY', 'USA', 'Kentucky', 50.5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- 아이리시 (5개)
('Jameson', '제임슨', 'WHISKEY', 'Ireland', 'Irish Blended', 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Jameson Black Barrel', '제임슨 블랙 배럴', 'WHISKEY', 'Ireland', 'Irish Blended', 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Bushmills Original', '부시밀스 오리지널', 'WHISKEY', 'Ireland', 'Irish Blended', 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Bushmills Black Bush', '부시밀스 블랙 부시', 'WHISKEY', 'Ireland', 'Irish Blended', 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Tullamore D.E.W.', '털러모어 듀', 'WHISKEY', 'Ireland', 'Irish Blended', 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- 재패니즈 (6개)
('Suntory Toki', '산토리 토키', 'WHISKEY', 'Japan', 'Japanese Blended', 43.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Suntory Kakubin', '산토리 가쿠빈', 'WHISKEY', 'Japan', 'Japanese Blended', 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Nikka From the Barrel', '닛카 프롬 더 배럴', 'WHISKEY', 'Japan', 'Japanese Blended', 51.4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Hibiki Harmony', '히비키 하모니', 'WHISKEY', 'Japan', 'Japanese Blended', 43.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Yamazaki 12 Year', '야마자키 12년', 'WHISKEY', 'Japan', 'Japanese Single Malt', 43.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Hakushu 12 Year', '하쿠슈 12년', 'WHISKEY', 'Japan', 'Japanese Single Malt', 43.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- --------------------------------------------------
-- 와인 (WINE) - 32개
-- --------------------------------------------------
INSERT INTO alcohol (name, name_ko, category, origin, region, abv, created_at, updated_at) VALUES

-- 레드 와인 (18개)
('Chateau Margaux', '샤토 마르고', 'WINE', 'France', 'Bordeaux', 13.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Chateau Lafite Rothschild', '샤토 라피트 로칠드', 'WINE', 'France', 'Bordeaux', 13.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Chateau Mouton Rothschild', '샤토 무통 로칠드', 'WINE', 'France', 'Bordeaux', 13.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Opus One', '오퍼스 원', 'WINE', 'USA', 'Napa Valley', 14.5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Penfolds Grange', '펜폴즈 그레인지', 'WINE', 'Australia', 'South Australia', 14.5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Penfolds Bin 407 Cabernet Sauvignon', '펜폴즈 빈 407 까베르네 소비뇽', 'WINE', 'Australia', 'South Australia', 14.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Penfolds Bin 389 Cabernet Shiraz', '펜폴즈 빈 389 까베르네 쉬라즈', 'WINE', 'Australia', 'South Australia', 14.5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Casillero del Diablo Cabernet Sauvignon', '카시예로 델 디아블로 까베르네 소비뇽', 'WINE', 'Chile', 'Central Valley', 13.5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('1865 Cabernet Sauvignon', '1865 까베르네 소비뇽', 'WINE', 'Chile', 'Maule Valley', 14.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Santa Rita 120 Cabernet Sauvignon', '산타 리타 120 까베르네 소비뇽', 'WINE', 'Chile', 'Central Valley', 13.5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Cloudy Bay Pinot Noir', '클라우디 베이 피노 누아', 'WINE', 'New Zealand', 'Marlborough', 14.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Apothic Red', '아포딕 레드', 'WINE', 'USA', 'California', 13.5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('19 Crimes Cabernet Sauvignon', '19 크라임즈 까베르네 소비뇽', 'WINE', 'Australia', 'South Eastern Australia', 13.5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Yellow Tail Shiraz', '옐로우 테일 쉬라즈', 'WINE', 'Australia', 'South Eastern Australia', 13.5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Barefoot Cabernet Sauvignon', '베어풋 까베르네 소비뇽', 'WINE', 'USA', 'California', 13.5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Wolf Blass Yellow Label Cabernet Sauvignon', '울프 블라스 옐로우 라벨 까베르네 소비뇽', 'WINE', 'Australia', 'South Australia', 13.5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Malbec Trapiche', '트라피체 말벡', 'WINE', 'Argentina', 'Mendoza', 13.5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Clos du Val Cabernet Sauvignon', '클로 뒤 발 까베르네 소비뇽', 'WINE', 'USA', 'Napa Valley', 14.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- 화이트 와인 (8개)
('Cloudy Bay Sauvignon Blanc', '클라우디 베이 소비뇽 블랑', 'WINE', 'New Zealand', 'Marlborough', 13.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Kim Crawford Sauvignon Blanc', '킴 크로포드 소비뇽 블랑', 'WINE', 'New Zealand', 'Marlborough', 12.5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Santa Margherita Pinot Grigio', '산타 마르게리타 피노 그리지오', 'WINE', 'Italy', 'Trentino-Alto Adige', 12.5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Kendall-Jackson Vintner''s Reserve Chardonnay', '켄달 잭슨 빈트너스 리저브 샤르도네', 'WINE', 'USA', 'California', 13.5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Yellow Tail Chardonnay', '옐로우 테일 샤르도네', 'WINE', 'Australia', 'South Eastern Australia', 13.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Barefoot Pinot Grigio', '베어풋 피노 그리지오', 'WINE', 'USA', 'California', 12.5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Riesling Dr. Loosen', '닥터 루젠 리슬링', 'WINE', 'Germany', 'Mosel', 9.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Pouilly-Fume Henri Bourgeois', '푸이 퓌메 앙리 부르주아', 'WINE', 'France', 'Loire Valley', 13.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- 스파클링 (4개)
('Moet & Chandon Brut Imperial', '모에 & 샹동 브뤼 임페리얼', 'WINE', 'France', 'Champagne', 12.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Veuve Clicquot Yellow Label Brut', '브브 클리코 옐로우 라벨 브뤼', 'WINE', 'France', 'Champagne', 12.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Perrier-Jouet Grand Brut', '페리에 주에 그랑 브뤼', 'WINE', 'France', 'Champagne', 12.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Ruffino Prosecco', '루피노 프로세코', 'WINE', 'Italy', 'Veneto', 11.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- 로제 (2개)
('Whispering Angel Rose', '위스퍼링 엔젤 로제', 'WINE', 'France', 'Provence', 13.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Miraval Rose', '미라발 로제', 'WINE', 'France', 'Provence', 13.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- --------------------------------------------------
-- 맥주 (BEER) - 27개
-- --------------------------------------------------
INSERT INTO alcohol (name, name_ko, category, origin, region, abv, created_at, updated_at) VALUES

-- 수입 라거/에일 (20개)
('Heineken Lager', '하이네켄', 'BEER', 'Netherlands', NULL, 5.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Stella Artois', '스텔라 아르투아', 'BEER', 'Belgium', NULL, 5.2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Budweiser', '버드와이저', 'BEER', 'USA', NULL, 5.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Corona Extra', '코로나 엑스트라', 'BEER', 'Mexico', NULL, 4.6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Guinness Draught', '기네스 드래프트', 'BEER', 'Ireland', NULL, 4.2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Hoegaarden White', '호가든 화이트', 'BEER', 'Belgium', NULL, 4.9, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Leffe Blonde', '레페 블론드', 'BEER', 'Belgium', NULL, 6.6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Chimay Blue', '쉬마이 블루', 'BEER', 'Belgium', NULL, 9.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Duvel', '두벨', 'BEER', 'Belgium', NULL, 8.5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Weihenstephaner Hefeweissbier', '바이엔슈테파너 헤페바이스비어', 'BEER', 'Germany', 'Bavaria', 5.4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Paulaner Hefe-Weissbier', '파울라너 헤페바이스비어', 'BEER', 'Germany', 'Bavaria', 5.5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Erdinger Weissbier', '에딩거 바이스비어', 'BEER', 'Germany', 'Bavaria', 5.3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Asahi Super Dry', '아사히 수퍼 드라이', 'BEER', 'Japan', NULL, 5.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Sapporo Premium Lager', '삿포로 프리미엄 라거', 'BEER', 'Japan', NULL, 4.7, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Kirin Ichiban', '기린 이치방', 'BEER', 'Japan', NULL, 5.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Tiger Beer', '타이거 비어', 'BEER', 'Singapore', NULL, 5.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('San Miguel Pale Pilsen', '산미구엘 페일 필센', 'BEER', 'Philippines', NULL, 5.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Blue Moon Belgian White', '블루문 벨지안 화이트', 'BEER', 'USA', NULL, 5.4, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Sierra Nevada Pale Ale', '시에라 네바다 페일 에일', 'BEER', 'USA', 'California', 5.6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Beck''s', '벡스', 'BEER', 'Germany', NULL, 5.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),

-- 국산 맥주 (7개)
('Cass Fresh', '카스 프레쉬', 'BEER', 'South Korea', NULL, 4.5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Hite Extra Cold', '하이트 엑스트라 콜드', 'BEER', 'South Korea', NULL, 4.3, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Terra', '테라', 'BEER', 'South Korea', NULL, 4.6, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Kloud', '클라우드', 'BEER', 'South Korea', NULL, 5.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Hanmac', '한맥', 'BEER', 'South Korea', NULL, 4.5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('OB Golden Lager', 'OB 골든 라거', 'BEER', 'South Korea', NULL, 4.5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Fitz Super Clear', '피츠 수퍼 클리어', 'BEER', 'South Korea', NULL, 4.5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- --------------------------------------------------
-- 소주 (SOJU) - 10개
-- --------------------------------------------------
INSERT INTO alcohol (name, name_ko, category, origin, region, abv, created_at, updated_at) VALUES
('Chamisul Fresh', '참이슬 후레쉬', 'SOJU', 'South Korea', NULL, 16.5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Chamisul Original', '참이슬 오리지널', 'SOJU', 'South Korea', NULL, 20.1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Jinro Is Back', '진로이즈백', 'SOJU', 'South Korea', NULL, 16.9, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Good Day', '좋은데이', 'SOJU', 'South Korea', NULL, 16.9, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Chum Churum', '처음처럼', 'SOJU', 'South Korea', NULL, 16.5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('O2Lin', '오투린', 'SOJU', 'South Korea', NULL, 16.9, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Yipsejoo', '잎새주', 'SOJU', 'South Korea', NULL, 16.9, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Andong Soju', '안동소주', 'SOJU', 'South Korea', 'Gyeongsangbuk-do', 35.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Hwayo 41', '화요 41', 'SOJU', 'South Korea', NULL, 41.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Hwayo 25', '화요 25', 'SOJU', 'South Korea', NULL, 25.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- --------------------------------------------------
-- 막걸리 (MAKGEOLLI) - 10개
-- --------------------------------------------------
INSERT INTO alcohol (name, name_ko, category, origin, region, abv, created_at, updated_at) VALUES
('Seoul Makgeolli', '서울 막걸리', 'MAKGEOLLI', 'South Korea', 'Seoul', 6.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Jangsu Makgeolli', '장수 막걸리', 'MAKGEOLLI', 'South Korea', 'Seoul', 6.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Boksuondoga Makgeolli', '복순도가 손막걸리', 'MAKGEOLLI', 'South Korea', 'Gyeongsangnam-do', 6.5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Geumjeongsan Fortress Makgeolli', '금정산성 막걸리', 'MAKGEOLLI', 'South Korea', 'Busan', 8.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Ihwaju', '이화주', 'MAKGEOLLI', 'South Korea', NULL, 6.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Baesangmyeonbuja Rice Makgeolli', '배상면주가 쌀 막걸리', 'MAKGEOLLI', 'South Korea', NULL, 6.5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Milyang Makgeolli', '밀양 막걸리', 'MAKGEOLLI', 'South Korea', 'Gyeongsangnam-do', 6.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Gapyeong Makgeolli', '가평 막걸리', 'MAKGEOLLI', 'South Korea', 'Gyeonggi-do', 6.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Solsongju', '솔송주', 'MAKGEOLLI', 'South Korea', 'Gyeonggi-do', 13.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Makku', '막쿠', 'MAKGEOLLI', 'South Korea', NULL, 5.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- --------------------------------------------------
-- 사케 (SAKE) - 10개
-- --------------------------------------------------
INSERT INTO alcohol (name, name_ko, category, origin, region, abv, created_at, updated_at) VALUES
('Dassai 23 Junmai Daiginjo', '닷사이 23 준마이 다이긴죠', 'SAKE', 'Japan', 'Yamaguchi', 16.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Dassai 39 Junmai Daiginjo', '닷사이 39 준마이 다이긴죠', 'SAKE', 'Japan', 'Yamaguchi', 16.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Dassai 45 Junmai Daiginjo', '닷사이 45 준마이 다이긴죠', 'SAKE', 'Japan', 'Yamaguchi', 16.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Hakkaisan Junmai Ginjo', '핫카이산 준마이 긴죠', 'SAKE', 'Japan', 'Niigata', 15.5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Kubota Manju Junmai Daiginjo', '구보타 만쥬 준마이 다이긴죠', 'SAKE', 'Japan', 'Niigata', 15.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Kubota Senju Junmai Ginjo', '구보타 센쥬 준마이 긴죠', 'SAKE', 'Japan', 'Niigata', 15.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Gekkeikan Junmai', '겟케이칸 준마이', 'SAKE', 'Japan', 'Kyoto', 14.5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Ozeki Junmai', '오제키 준마이', 'SAKE', 'Japan', 'Hyogo', 14.5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Tateyama Junmai Daiginjo', '다테야마 준마이 다이긴죠', 'SAKE', 'Japan', 'Toyama', 16.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Nanbubijin Tokubetsu Junmai', '난부비진 도쿠베츠 준마이', 'SAKE', 'Japan', 'Iwate', 15.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- --------------------------------------------------
-- 보드카 (VODKA) - 6개
-- --------------------------------------------------
INSERT INTO alcohol (name, name_ko, category, origin, region, abv, created_at, updated_at) VALUES
('Absolut Vodka', '앱솔루트 보드카', 'VODKA', 'Sweden', NULL, 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Grey Goose', '그레이 구스', 'VODKA', 'France', 'Cognac', 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Belvedere Vodka', '벨베데레 보드카', 'VODKA', 'Poland', NULL, 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Ketel One Vodka', '케텔 원 보드카', 'VODKA', 'Netherlands', NULL, 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Smirnoff No. 21', '스미노프 No.21', 'VODKA', 'USA', NULL, 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Ciroc Vodka', '씨록 보드카', 'VODKA', 'France', 'Cognac', 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- --------------------------------------------------
-- 진 (GIN) - 6개
-- --------------------------------------------------
INSERT INTO alcohol (name, name_ko, category, origin, region, abv, created_at, updated_at) VALUES
('Tanqueray London Dry Gin', '탱커레이 런던 드라이 진', 'GIN', 'UK', NULL, 43.1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Bombay Sapphire', '봄베이 사파이어', 'GIN', 'UK', NULL, 47.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Hendrick''s Gin', '헨드릭스 진', 'GIN', 'Scotland', NULL, 44.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('The Botanist Islay Dry Gin', '더 보태니스트 아일라 드라이 진', 'GIN', 'Scotland', 'Islay', 46.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Roku Gin', '로쿠 진', 'GIN', 'Japan', NULL, 43.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Gordon''s London Dry Gin', '고든스 런던 드라이 진', 'GIN', 'UK', NULL, 37.5, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- --------------------------------------------------
-- 럼 (RUM) - 6개
-- --------------------------------------------------
INSERT INTO alcohol (name, name_ko, category, origin, region, abv, created_at, updated_at) VALUES
('Bacardi Superior', '바카디 수페리어', 'RUM', 'Puerto Rico', NULL, 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Captain Morgan Original Spiced Rum', '캡틴 모건 오리지널 스파이스드 럼', 'RUM', 'Jamaica', NULL, 35.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Havana Club Anejo 3 Anos', '하바나 클럽 아네호 3년', 'RUM', 'Cuba', NULL, 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Diplomatico Reserva Exclusiva', '디플로마티코 레세르바 엑스클루시바', 'RUM', 'Venezuela', NULL, 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Mount Gay Eclipse', '마운트 게이 이클립스', 'RUM', 'Barbados', NULL, 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Appleton Estate Signature Blend', '애플턴 에스테이트 시그니처 블렌드', 'RUM', 'Jamaica', NULL, 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- --------------------------------------------------
-- 테킬라 (TEQUILA) - 6개
-- --------------------------------------------------
INSERT INTO alcohol (name, name_ko, category, origin, region, abv, created_at, updated_at) VALUES
('Jose Cuervo Especial Silver', '호세 쿠엘보 에스페셜 실버', 'TEQUILA', 'Mexico', 'Jalisco', 38.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Patron Silver', '페트론 실버', 'TEQUILA', 'Mexico', 'Jalisco', 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Don Julio Blanco', '돈 훌리오 블랑코', 'TEQUILA', 'Mexico', 'Jalisco', 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Olmeca Silver', '올메카 실버', 'TEQUILA', 'Mexico', 'Jalisco', 38.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('1800 Silver Tequila', '1800 실버 데킬라', 'TEQUILA', 'Mexico', 'Jalisco', 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Herradura Silver', '에라두라 실버', 'TEQUILA', 'Mexico', 'Jalisco', 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- --------------------------------------------------
-- 브랜디 (BRANDY) - 6개
-- --------------------------------------------------
INSERT INTO alcohol (name, name_ko, category, origin, region, abv, created_at, updated_at) VALUES
('Hennessy VS', '헤네시 VS', 'BRANDY', 'France', 'Cognac', 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Hennessy VSOP', '헤네시 VSOP', 'BRANDY', 'France', 'Cognac', 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Hennessy XO', '헤네시 XO', 'BRANDY', 'France', 'Cognac', 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Martell VS', '마르텔 VS', 'BRANDY', 'France', 'Cognac', 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Remy Martin VSOP', '레미 마르탱 VSOP', 'BRANDY', 'France', 'Cognac', 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('Courvoisier VS', '쿠르부아지에 VS', 'BRANDY', 'France', 'Cognac', 40.0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);


-- =====================================================
-- 2. ALCOHOL_ALIAS 데이터
-- 서치에서 자주 쓰는 별칭 위주 (주로 위스키)
-- 형식: 술 이름으로 id를 찾아서 alias 삽입
-- =====================================================

-- 조니워커
INSERT INTO alcohol_alias (alcohol_id, alias, created_at, updated_at) VALUES
((SELECT id FROM alcohol WHERE name = 'Johnnie Walker Black Label'), '블랙라벨', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Johnnie Walker Black Label'), 'JW Black', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Johnnie Walker Black Label'), '조니블랙', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Johnnie Walker Black Label'), '조니워커블랙', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Johnnie Walker Red Label'), '레드라벨', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Johnnie Walker Red Label'), 'JW Red', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Johnnie Walker Red Label'), '조니레드', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Johnnie Walker Blue Label'), '블루라벨', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Johnnie Walker Blue Label'), 'JW Blue', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Johnnie Walker Blue Label'), '조니블루', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Johnnie Walker Double Black'), '더블블랙', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Johnnie Walker Double Black'), 'JW Double Black', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Johnnie Walker Gold Reserve'), '골드리저브', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Johnnie Walker Gold Reserve'), 'JW Gold', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Johnnie Walker Green Label'), '그린라벨', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Johnnie Walker Green Label'), 'JW Green', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 시바스 리갈
INSERT INTO alcohol_alias (alcohol_id, alias, created_at, updated_at) VALUES
((SELECT id FROM alcohol WHERE name = 'Chivas Regal 12 Year'), '시바스 12', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Chivas Regal 12 Year'), '시바스12', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Chivas Regal 18 Year'), '시바스 18', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Chivas Regal 18 Year'), '시바스18', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Chivas Regal 25 Year'), '시바스 25', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 발렌타인
INSERT INTO alcohol_alias (alcohol_id, alias, created_at, updated_at) VALUES
((SELECT id FROM alcohol WHERE name = 'Ballantine''s Finest'), '발렌타인 파인', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Ballantine''s Finest'), '발파인', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Ballantine''s 17 Year'), '발렌타인 17', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Ballantine''s 17 Year'), '발렌타인17', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Ballantine''s 17 Year'), '발17', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Ballantine''s 21 Year'), '발렌타인 21', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Ballantine''s 21 Year'), '발렌타인21', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Ballantine''s 30 Year'), '발렌타인 30', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 글렌피딕
INSERT INTO alcohol_alias (alcohol_id, alias, created_at, updated_at) VALUES
((SELECT id FROM alcohol WHERE name = 'Glenfiddich 12 Year'), '글렌피딕12', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Glenfiddich 12 Year'), '피딕 12', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Glenfiddich 15 Year'), '글렌피딕15', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Glenfiddich 15 Year'), '피딕 15', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Glenfiddich 18 Year'), '글렌피딕18', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Glenfiddich 18 Year'), '피딕 18', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Glenfiddich 21 Year'), '글렌피딕21', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 맥캘란
INSERT INTO alcohol_alias (alcohol_id, alias, created_at, updated_at) VALUES
((SELECT id FROM alcohol WHERE name = 'The Macallan 12 Year Double Cask'), '맥캘란 12 더블', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'The Macallan 12 Year Double Cask'), '맥캘란12DC', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'The Macallan 12 Year Double Cask'), '맥캘란 더블캐스크', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'The Macallan 12 Year Sherry Cask'), '맥캘란 12 쉐리', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'The Macallan 12 Year Sherry Cask'), '맥캘란12SC', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'The Macallan 12 Year Sherry Cask'), '맥캘란 쉐리캐스크', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'The Macallan 18 Year Sherry Cask'), '맥캘란 18', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'The Macallan 18 Year Sherry Cask'), '맥캘란18', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'The Macallan 25 Year'), '맥캘란 25', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 글렌리벳
INSERT INTO alcohol_alias (alcohol_id, alias, created_at, updated_at) VALUES
((SELECT id FROM alcohol WHERE name = 'Glenlivet 12 Year'), '글렌리벳12', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Glenlivet 12 Year'), '리벳 12', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Glenlivet 15 Year'), '글렌리벳15', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Glenlivet 18 Year'), '글렌리벳18', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 아일라
INSERT INTO alcohol_alias (alcohol_id, alias, created_at, updated_at) VALUES
((SELECT id FROM alcohol WHERE name = 'Laphroaig 10 Year'), '라프로익10', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Laphroaig 10 Year'), '라프', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Ardbeg 10 Year'), '아드벡10', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Ardbeg Uigeadail'), '아드벡 우가달', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Ardbeg Corryvreckan'), '아드벡 코리', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Lagavulin 16 Year'), '라가불린16', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Lagavulin 16 Year'), '라가불린', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Bowmore 12 Year'), '보모어12', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 잭다니엘
INSERT INTO alcohol_alias (alcohol_id, alias, created_at, updated_at) VALUES
((SELECT id FROM alcohol WHERE name = 'Jack Daniel''s Old No. 7'), '잭다니엘', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Jack Daniel''s Old No. 7'), '잭다니얼', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Jack Daniel''s Old No. 7'), 'JD', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Jack Daniel''s Old No. 7'), '잭 다니엘', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Jack Daniel''s Gentleman Jack'), '젠틀맨 잭', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Jack Daniel''s Single Barrel'), '잭다니엘 싱글배럴', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 기타 아메리칸
INSERT INTO alcohol_alias (alcohol_id, alias, created_at, updated_at) VALUES
((SELECT id FROM alcohol WHERE name = 'Maker''s Mark'), '메이커스마크', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Woodford Reserve'), '우드포드', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Buffalo Trace'), '버팔로트레이스', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Wild Turkey 101'), '와일드터키', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 아이리시
INSERT INTO alcohol_alias (alcohol_id, alias, created_at, updated_at) VALUES
((SELECT id FROM alcohol WHERE name = 'Jameson'), '제임슨 아이리시', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Jameson'), '제임슨 위스키', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Tullamore D.E.W.'), '털러모어듀', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 재패니즈
INSERT INTO alcohol_alias (alcohol_id, alias, created_at, updated_at) VALUES
((SELECT id FROM alcohol WHERE name = 'Nikka From the Barrel'), '닛카프롬', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Nikka From the Barrel'), '프롬 더 배럴', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Hibiki Harmony'), '히비키', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Hibiki Harmony'), '響 하모니', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Yamazaki 12 Year'), '야마자키12', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Yamazaki 12 Year'), '야마자키', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Hakushu 12 Year'), '하쿠슈12', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Hakushu 12 Year'), '하쿠슈', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Suntory Toki'), '토키', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Suntory Kakubin'), '가쿠빈', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 코냑/브랜디
INSERT INTO alcohol_alias (alcohol_id, alias, created_at, updated_at) VALUES
((SELECT id FROM alcohol WHERE name = 'Hennessy VS'), '헤네시 브이에스', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Hennessy VSOP'), '헤네시 브이에스오피', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Hennessy XO'), '헤네시 엑스오', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Remy Martin VSOP'), '레미마르탱 VSOP', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 와인 별칭
INSERT INTO alcohol_alias (alcohol_id, alias, created_at, updated_at) VALUES
((SELECT id FROM alcohol WHERE name = 'Casillero del Diablo Cabernet Sauvignon'), '카시예로', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Casillero del Diablo Cabernet Sauvignon'), '까시예로 디아블로', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = '1865 Cabernet Sauvignon'), '1865 와인', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Cloudy Bay Sauvignon Blanc'), '클라우디베이', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Moet & Chandon Brut Imperial'), '모엣 샹동', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Moet & Chandon Brut Imperial'), '모에샹동', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Veuve Clicquot Yellow Label Brut'), '브브 클리코', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Veuve Clicquot Yellow Label Brut'), '뵈브 클리코', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 소주 별칭
INSERT INTO alcohol_alias (alcohol_id, alias, created_at, updated_at) VALUES
((SELECT id FROM alcohol WHERE name = 'Chamisul Fresh'), '참이슬', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Jinro Is Back'), '진로이즈백', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Hwayo 41'), '화요', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 맥주 별칭
INSERT INTO alcohol_alias (alcohol_id, alias, created_at, updated_at) VALUES
((SELECT id FROM alcohol WHERE name = 'Heineken Lager'), '하이네켄', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Guinness Draught'), '기네스', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Hoegaarden White'), '호가든', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Chimay Blue'), '쉬마이', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Weihenstephaner Hefeweissbier'), '바이엔슈테파너', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 닷사이 별칭
INSERT INTO alcohol_alias (alcohol_id, alias, created_at, updated_at) VALUES
((SELECT id FROM alcohol WHERE name = 'Dassai 23 Junmai Daiginjo'), '닷사이 23', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Dassai 23 Junmai Daiginjo'), '獺祭 23', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Dassai 39 Junmai Daiginjo'), '닷사이 39', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
((SELECT id FROM alcohol WHERE name = 'Dassai 45 Junmai Daiginjo'), '닷사이 45', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
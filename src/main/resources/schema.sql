CREATE TABLE IF NOT EXISTS example_prompt (
                                              prompt_id BIGINT AUTO_INCREMENT PRIMARY KEY,  -- 기본 키, 자동 증가
                                              prompt VARCHAR(255) NOT NULL                  -- 프롬프트 내용을 저장할 필드, NULL을 허용하지 않음
);
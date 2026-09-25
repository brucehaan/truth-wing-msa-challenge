### M4 - 부분 환불 잔여 규칙 제거
- 바꾼 줄 : JournalFactory.refund() 의 삼항 연산자를 commissionOff(amount).min(remaining) 하나로
- 빨개진 테스트 : 부분환불 잔여 규칙 (기대 33원, 실제 31원)
- 배운 것 : 건별 절사의 합은 총액 절사와 다르다. 마지막 건이 잔여를 흡수해야 한다.
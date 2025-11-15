// point_transactions.js

/**
 * 서버 API(/api/point-transactions)를 호출하여 포인트 거래 내역을 가져옵니다.
 * 이 함수의 유일한 역할은 데이터를 가져와서 UI에 맞는 형태로 가공한 후,
 * 그 결과를 '반환(return)'하는 것입니다.
 * * @returns {Promise<Array>} 성공 시, 변환된 데이터 배열을 담은 Promise를 반환합니다.
 * @throws {Error} 네트워크 오류나 데이터 처리 중 오류 발생 시 에러를 던집니다.
 * 이 에러는 이 함수를 호출한 쪽(point_history.html)에서 처리하게 됩니다.
 */
window.fetchPointTransactionData = async function () {
  // try...catch 블록은 이 함수를 호출하는 point_history.html에 있으므로 여기서는 제거합니다.
  // 이 함수는 성공하면 데이터를 반환하고, 실패하면 에러를 던지는 역할에만 집중합니다.
  const res = await fetch("/api/point-transactions");

  // 네트워크 응답이 실패한 경우, 명확한 에러 메시지와 함께 에러를 발생시킵니다.
  if (!res.ok) {
    throw new Error(`서버 응답 오류: ${res.status} ${res.statusText}`);
  }

  // 응답 본문을 JSON으로 파싱합니다.
  const data = await res.json();

  // 서버에서 받은 데이터가 배열 형태가 맞는지 확인하여 안정성을 높입니다.
  if (!Array.isArray(data)) {
    throw new Error("서버로부터 받은 데이터가 올바른 배열 형식이 아닙니다.");
  }

  // UI에 표시하기 좋은 데이터 구조로 변환(map)합니다.
  const mappedData = data.map((row) => {
    let date = "";
    let time = "";

    // created_at 필드가 유효할 때만 날짜와 시간을 변환합니다.
    if (row.created_at) {
      const dateObj = new Date(row.created_at);
      // 유효하지 않은 날짜(Invalid Date) 객체인지 확인하여 오류를 방지합니다.
      if (!isNaN(dateObj.getTime())) {
        date = dateObj.toISOString().slice(0, 10);
        time = dateObj.toTimeString().slice(0, 8);
      }
    }

    return {
      date,
      time,
      name: row.name || "정보 없음", // 이름이 없는 경우 기본값을 사용합니다.
      id: row.user_id,
      amount: row.amount,
      status: "완료", // status는 '완료'로 고정합니다.
      type: row.transaction_type,
      typeText: row.transaction_type === "charge" ? "충전" : "환불",
    };
  });

  // [가장 중요한 부분]
  // 함수가 처리된 데이터(mappedData)를 '반환'합니다.
  // 이 반환된 값을 point_history.html 스크립트가 받아서 화면을 그리게 되므로,
  // 'undefined' 오류가 해결됩니다.
  return mappedData;
};

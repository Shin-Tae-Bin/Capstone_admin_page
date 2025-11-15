// payment.js

/**
 * 서버에서 받은 원본 식권 결제 데이터를 UI에 표시하기 좋은 형태로 변환합니다.
 * @param {Array} data - 서버 API로부터 받은 원본 데이터 배열
 * @returns {Array} UI에 맞게 변환된 데이터 배열
 */
window.setCurrentMealTicketData = function (data) {
  // 전역 변수에 변환된 데이터를 저장합니다.
  window.currentMealTicketData = data.map((row) => {
    let date = "",
      time = "";

    // 결제 시간이 존재하면 'YYYY-MM-DD' 및 'HH:MM:SS' 형식으로 분리합니다.
    if (row.paymentTime) {
      const dateObj = new Date(row.paymentTime);
      if (!isNaN(dateObj.getTime())) {
        // 유효한 날짜인지 확인
        date = dateObj.toISOString().slice(0, 10);
        time = dateObj.toTimeString().slice(0, 8);
      }
    }

    // 메뉴 이름에서 식권 개수를 추출합니다. (예: "식권 2개" -> "2개")
    let ticketCount = "1개"; // 기본값
    if (row.menuName) {
      const match = row.menuName.match(/(\d+)개/);
      if (match && match[1]) {
        ticketCount = match[1] + "개";
      }
    }

    return {
      date,
      time,
      ticketCount,
      userId: row.userId || "",
      amount: row.amount,
      // 결제 상태를 한글로 변환합니다. 'success'가 아니면 '완료'로 통일합니다.
      status:
        row.status === "success"
          ? "완료"
          : row.status === "pending"
          ? "대기"
          : "완료",
      paymentMethod: row.paymentMethod, // API에서 받은 값(영어) 그대로 사용
      cardNumber: row.cardNumber || "",
      restaurant: row.restaurant || "-", // 식당 정보가 없으면 '-'로 표시
    };
  });
  return window.currentMealTicketData;
};

/**
 * 서버 API를 호출하여 식권 결제 내역 데이터를 가져옵니다.
 * @param {string} type - 결제 수단 필터 (예: 'all', 'card', 'point')
 * @returns {Promise<Array>} 성공 시, 변환된 데이터 배열을 담은 Promise. 실패 시 빈 배열.
 */
window.fetchMealTicketPayments = async function (type = "all") {
  try {
    // API 엔드포인트에 쿼리 파라미터를 추가하여 데이터를 요청합니다.
    const res = await fetch(`/api/meal-ticket-payments?type=${type}`);
    if (!res.ok) {
      throw new Error(`서버 응답 오류: ${res.status}`);
    }
    const data = await res.json();

    // 받아온 데이터를 UI에 맞게 변환합니다.
    const transformedData = window.setCurrentMealTicketData(data);

    // 최종적으로 변환된 데이터를 반환합니다.
    return transformedData;
  } catch (e) {
    console.error("식권 결제 내역을 불러오는 중 오류 발생:", e);
    const contentContainer = document.getElementById("admin-content-container");
    if (contentContainer) {
      contentContainer.innerHTML = `<p class="text-red-500 p-4">오류: ${e.message}</p>`;
    }
    return []; // 에러 발생 시 빈 배열을 반환하여 UI 오류를 방지합니다.
  }
};

/**
 * 식권 결제 내역 탭(전체, 카드, 포인트)을 클릭했을 때 실행되는 이벤트 핸들러입니다.
 * 데이터를 새로 불러오고, 화면 렌더링을 요청합니다.
 * @param {string} type - 클릭된 탭의 종류 (예: 'all', 'card', 'point')
 */
window.onMealTabClick = async function (type) {
  // 현재 선택된 월(month) 정보를 가져옵니다. 없으면 현재 월을 기본값으로 사용합니다.
  const initialMonth =
    window.currentMealTicketMonth || new Date().toISOString().substring(0, 7);

  // 선택된 탭 타입으로 데이터를 비동기적으로 불러옵니다.
  const data = await window.fetchMealTicketPayments(type);

  // 렌더링 함수가 존재하는지 확인한 후,
  // 불러온 데이터를 인자로 전달하여 화면 갱신을 요청합니다.
  if (window.renderMealTicketPurchaseHistoryWithData) {
    window.renderMealTicketPurchaseHistoryWithData(
      type, // 현재 탭 타입
      "all", // 식당 필터 (초기값 'all')
      initialMonth, // 월 필터
      1, // 현재 페이지 (초기값 1)
      data // 방금 불러온 실시간 데이터
    );
  }
};

// admin.js

// 1. 전역 변수 선언
var dailySalesLineChart = null;
var paymentMethodsPieChartInstance = null;
var restaurantDistributionPieChartInstance = null;
var currentPointTransactionData = [];
var allMealTicketPurchases = [];
var currentViewUrl = "";
var tokenTimerId = null;

// 2. 페이지네이션 생성 함수
function createPaginationControls(totalPages, currentPage, callbackName, ...prefixArgs) {
  if (totalPages <= 1) return "";
  let paginationHTML = '<div class="mt-6 flex justify-center items-center">';

  const prefixArgsString = prefixArgs
    .map((arg) => {
      if (arg === null || arg === undefined) return "null";
      return typeof arg === "string" ? `'${arg.replace(/'/g, "\\'")}'` : arg;
    })
    .join(", ");

  const commaIfNeeded = prefixArgs.length > 0 && prefixArgs.some((arg) => arg !== undefined) ? ", " : "";
  const stringifiedPrefixArgs = prefixArgsString + (prefixArgs.length > 0 && commaIfNeeded ? commaIfNeeded : "");

  // 이전 버튼
  paginationHTML += `<button class="pagination-button" onclick="${callbackName}(${stringifiedPrefixArgs}${
    currentPage - 1
  })" ${currentPage === 1 ? "disabled" : ""}>이전</button>`;

  // 페이지 번호 버튼
  const maxPagesToShow = 10;
  let startPage = 1;
  let endPage = totalPages;

  if (totalPages > maxPagesToShow) {
    startPage = Math.max(1, currentPage - Math.floor(maxPagesToShow / 2));
    endPage = Math.min(totalPages, startPage + maxPagesToShow - 1);
    if (endPage - startPage + 1 < maxPagesToShow) {
      startPage = Math.max(1, endPage - maxPagesToShow + 1);
    }
  }

  for (let i = startPage; i <= endPage; i++) {
    paginationHTML += `<button class="pagination-button ${
      i === currentPage ? "active" : ""
    }" onclick="${callbackName}(${stringifiedPrefixArgs}${i})">${i}</button>`;
  }

  // 다음 버튼
  paginationHTML += `<button class="pagination-button" onclick="${callbackName}(${stringifiedPrefixArgs}${
    currentPage + 1
  })" ${currentPage === totalPages ? "disabled" : ""}>다음</button>`;
  
  paginationHTML += "</div>";
  return paginationHTML;
}

// 3. 토큰 체크 및 유효성 검증
async function checkTokenValidity() {
  const token = localStorage.getItem('token');
  if (!token) {
    window.location.href = '/login';
    return false;
  }

  try {
    const response = await fetch('/api/admin/refresh-token', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });

    if (!response.ok) {
      localStorage.removeItem('token');
      window.location.href = '/login';
      return false;
    }

    const data = await response.json();
    localStorage.setItem('token', data.token);
    return true;
  } catch (error) {
    console.error('Token validation error:', error);
    localStorage.removeItem('token');
    window.location.href = '/login';
    return false;
  }
}

// 4. 컴포넌트 로더
async function loadComponent(url, containerId) {
  try {
    const response = await fetch(url);
    if (!response.ok) throw new Error(`Failed to load ${url}`);
    const text = await response.text();
    const container = document.getElementById(containerId);
    if (container) {
      const parser = new DOMParser();
      const doc = parser.parseFromString(text, "text/html");
      
      const tempDiv = document.createElement('div');
      tempDiv.innerHTML = doc.body.innerHTML;
      const scriptElements = tempDiv.getElementsByTagName('script');
      Array.from(scriptElements).forEach(script => script.remove());
      container.innerHTML = tempDiv.innerHTML;

      const docScripts = doc.getElementsByTagName("script");
      for (let script of docScripts) {
        const newScript = document.createElement("script");
        let scriptContent = script.textContent;
        scriptContent = scriptContent.replace(/\bconst\b/g, "var").replace(/\blet\b/g, "var");
        newScript.textContent = scriptContent;
        document.body.appendChild(newScript);
      }
    }
  } catch (error) {
    console.error(`Error loading component ${url}:`, error);
  }
}

// 5. 뷰 네비게이션
async function navigateTo(url, navElement) {
  if (currentViewUrl === url) return;

  if (dailySalesLineChart) { dailySalesLineChart.destroy(); dailySalesLineChart = null; }
  if (paymentMethodsPieChartInstance) { paymentMethodsPieChartInstance.destroy(); paymentMethodsPieChartInstance = null; }
  if (restaurantDistributionPieChartInstance) { restaurantDistributionPieChartInstance.destroy(); restaurantDistributionPieChartInstance = null; }

  document.querySelectorAll("#admin-sidebar-container .sidebar-item").forEach(item => item.classList.remove("active"));
  if (navElement) navElement.classList.add("active");

  await loadComponent(url, "admin-content-container");
  currentViewUrl = url;
}

// 6. 토큰 정보 파싱
function parseJwt(token) {
  try {
    return JSON.parse(atob(token.split('.')[1]));
  } catch (e) {
    return null;
  }
}

// 7. 남은 시간 타이머 표시 (수정됨)
function showTokenRemainingTime(token) {
  const payload = parseJwt(token);
  if (!payload || !payload.exp) return;

  const timerElement = document.getElementById('token-timer');
  if (!timerElement) return; 

  if (tokenTimerId) clearTimeout(tokenTimerId);

  function update() {
    const now = Math.floor(Date.now() / 1000);
    const diff = payload.exp - now;
    const el = document.getElementById('token-timer');
    
    if (!el) return;

    if (diff <= 0) {
      el.textContent = '만료됨';
      return;
    }
    const min = Math.floor(diff / 60);
    const sec = diff % 60;
    
  
    el.textContent = `${min}분 ${sec}초`;
    
    tokenTimerId = setTimeout(update, 1000);
  }
  update();
}

// 8. 로그아웃 함수
function logout() {
    if (tokenTimerId) clearTimeout(tokenTimerId);
    localStorage.removeItem('token');
    window.location.href = '/login';
}


document.addEventListener("DOMContentLoaded", async () => {
  const isValid = await checkTokenValidity();
  if (!isValid) return; 

  await Promise.all([
    loadComponent("header.html", "admin-header-container"),
    loadComponent("sidebar.html", "admin-sidebar-container"),
  ]);

  const refreshBtn = document.getElementById('refreshButton');
  const logoutBtn = document.getElementById('logoutButton');

  if (refreshBtn) {
      refreshBtn.addEventListener('click', async () => {
          await checkTokenValidity(); 
          const token = localStorage.getItem('token');
          if(token) showTokenRemainingTime(token); 
      });
  }

  if (logoutBtn) {
      logoutBtn.addEventListener('click', logout);
  }

  const token = localStorage.getItem('token');
  if (token) {
    showTokenRemainingTime(token);
  }

  try {
    if (typeof window.fetchMealTicketPayments === 'function') {
        allMealTicketPurchases = await window.fetchMealTicketPayments();
    }
  } catch (error) {
    console.error("초기 데이터 로딩 실패:", error);
  }

  const navPoint = document.getElementById("nav-point-history");
  const navMeal = document.getElementById("nav-meal-ticket-history");

  if (navPoint && navMeal) {
    navPoint.addEventListener("click", (e) => {
      e.preventDefault();
      navigateTo("point_history.html", navPoint);
    });

    navMeal.addEventListener("click", (e) => {
      e.preventDefault();
      navigateTo("meal_ticket_history.html", navMeal);
    });

    await navigateTo("point_history.html", navPoint);
  }
});
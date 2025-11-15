document.addEventListener('DOMContentLoaded', function() {
    const loginForm = document.getElementById('loginForm');

    if (loginForm) {
        loginForm.addEventListener('submit', async function(e) {
            e.preventDefault();
            
            const submitButton = this.querySelector('button[type="submit"]');
            const originalText = submitButton.textContent;
            
            // 로딩 상태 표시
            submitButton.textContent = '로그인 중...';
            submitButton.disabled = true;
            
            try {
                const username = document.getElementById('username').value;
                const password = document.getElementById('password').value;
                
                // reCAPTCHA 토큰 생성
               
                const token = await new Promise((resolve, reject) => {
                    if (typeof grecaptcha !== 'undefined') {
                        grecaptcha.ready(function() {
                            grecaptcha.execute('6LdvwmMrAAAAAKdeVxifl0DblbAGGTXMtLA4ypfQ', {action: 'login'})
                                .then(resolve)
                                .catch((err) => {
                                    console.error("reCAPTCHA Error:", err);
                                    resolve(null); 
                                });
                        });
                    } else {
                        console.warn("reCAPTCHA not loaded");
                        resolve(null);
                    }
                });
                
                // 2. 서버로 로그인 요청
                const response = await fetch('/api/admin/login', {
                    method: 'POST',
                    headers: {'Content-Type': 'application/json'},
                   
                    body: JSON.stringify({ username, password, recaptchaToken: token })
                });
                
                const data = await response.json();
                
                if (response.ok && data.token) {
                  
                    localStorage.setItem('token', data.token);
                    window.location.href = '/admin'; 
                } else {
                    alert(data.message || '로그인에 실패했습니다.');
                }
            } catch (error) {
                console.error('Login error:', error);
                alert('서버 연결에 실패했습니다.');
            } finally {
                
                submitButton.textContent = originalText;
                submitButton.disabled = false;
            }
        });
    }
});
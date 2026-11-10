// Danh sach don hang (CN-02-01): go toi dau bang cap nhat toi do, khong tai lai trang.
//
// Khong loc tren trinh duyet vi moi trang chi co 15 don: loc tai cho thi don o trang 2, 3...
// se khong bao gio tim thay. Thay vao do goi lai chinh /orders voi tu khoa moi, lay khoi
// #order-results (bang + phan trang) trong trang tra ve roi thay vao trang dang xem.
(function () {
    const form = document.getElementById('order-filter-form');
    const keywordInput = document.getElementById('keyword');
    if (!form || !keywordInput) {
        return;
    }

    // Cho nguoi dung ngung go mot chut moi goi server: go "an phat" la 7 phim,
    // khong can 7 request
    const DEBOUNCE_MS = 300;
    let debounceTimer = null;
    let inFlight = null;

    function buildUrl() {
        const params = new URLSearchParams();
        new FormData(form).forEach(function (value, name) {
            const text = String(value).trim();
            if (text !== '') {
                params.set(name, text);
            }
        });
        // Khong gui page: doi tu khoa thi ket qua khac han, phai quay ve trang dau
        const query = params.toString();
        return form.action + (query ? '?' + query : '');
    }

    function refreshResults() {
        const url = buildUrl();

        // Huy request cu: go nhanh thi request cham hon co the ve sau va de ket qua
        // cu len ket qua moi
        if (inFlight) {
            inFlight.abort();
        }
        inFlight = new AbortController();

        fetch(url, { signal: inFlight.signal, credentials: 'same-origin' })
            .then(function (response) {
                return response.text().then(function (html) {
                    return { response: response, html: html };
                });
            })
            .then(function (result) {
                const doc = new DOMParser().parseFromString(result.html, 'text/html');
                const freshResults = doc.getElementById('order-results');
                const currentResults = document.getElementById('order-results');

                // Het phien dang nhap thi server tra ve trang login, khong co khoi ket qua.
                // Chuyen han sang URL do de nguoi dung thay trang login that
                if (!result.response.ok || !freshResults || !currentResults) {
                    window.location.href = url;
                    return;
                }

                currentResults.innerHTML = freshResults.innerHTML;
                // Giu tu khoa tren thanh dia chi: F5 hay bam vao mot don roi quay lai
                // van thay dung danh sach dang tim
                window.history.replaceState(null, '', url);
            })
            .catch(function (error) {
                if (error.name !== 'AbortError') {
                    window.location.href = url;
                }
            });
    }

    function scheduleRefresh() {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(refreshResults, DEBOUNCE_MS);
    }

    keywordInput.addEventListener('input', function (event) {
        // Bo go tieng Viet cua trinh duyet dang ghep dau thi chua tim, doi ghep xong
        if (event.isComposing) {
            return;
        }
        scheduleRefresh();
    });
    keywordInput.addEventListener('compositionend', scheduleRefresh);
})();

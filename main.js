/* main.js */
const API_KEY = "0e82366e8146f67cd2fac4981a8cb2507e6b56d8b1e7437964f6c55405812ba3"; // 공공데이터포털 서비스키
const BCRYPT_ROUNDS = 10;
const KAKAO_MOBILITY_REST_KEY = "23cc857488915715b8e944bd006fc8a9";
const MAX_ROUTE_WAYPOINTS = 8;
const ROUTE_SEGMENT_COLORS = ['#2563eb', '#f97316', '#16a34a', '#db2777', '#eab308', '#0ea5e9', '#8b5cf6', '#ef4444'];
const HOME_MARKER_SVG = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64"><path fill="#2563eb" d="M32 4 4 27h7v29h17V39h8v17h17V27h7z"/><rect x="24" y="34" width="16" height="22" rx="2" fill="#ffffff"/></svg>`;

const AppState = {
    user: JSON.parse(localStorage.getItem('et_user')),        // F107 회원정보
    plans: JSON.parse(localStorage.getItem('et_plans') || '[]'),       // F104 여행계획
    hotplaces: JSON.parse(localStorage.getItem('et_hotplaces') || '[]'), // F105 핫플레이스
    boards: JSON.parse(localStorage.getItem('et_boards') || '[]'),     // F110 게시판
    notices: JSON.parse(localStorage.getItem('et_notices') || '[]'),   // F109 공지사항
    lodging: JSON.parse(localStorage.getItem('et_lodging') || 'null'),
    currPlanItems: [],
    map: null,
    markers: [],
    infowindows: [],
    dragSrcIdx: null,
    _planPolyline: null,
    _routePolylines: [],
    _homeMarkerImage: null,
    _routeCache: {},
    routeResult: null,
    modalPlanId: null,
    modalPlanItems: [],
    modalDragSrcIdx: null,
    modalPlanLodging: null
};

// =====================================================
// 유틸리티
// =====================================================

/** XSS 방지용 HTML 이스케이프 */
function escapeHtml(str) {
    if (str == null) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}

/** 하단 토스트 알림 */
function showToast(msg) {
    const el = document.getElementById('toast');
    el.textContent = msg;
    el.style.display = 'block';
    clearTimeout(el._timer);
    el._timer = setTimeout(() => { el.style.display = 'none'; }, 3000);
}

function _getUsers() {
    return JSON.parse(localStorage.getItem('et_users') || '[]');
}

function _saveUsers(users) {
    localStorage.setItem('et_users', JSON.stringify(users));
}

function _setLodging(lodging) {
    AppState.lodging = lodging;
    localStorage.setItem('et_lodging', JSON.stringify(lodging));
    renderLodgingInfo();
}

function _isValidLodging(lodging) {
    return !!lodging
        && Number.isFinite(Number(lodging.lat))
        && Number.isFinite(Number(lodging.lng))
        && !!(lodging.placeName || lodging.address);
}

function _getPlanLodging(plan) {
    if (_isValidLodging(plan?.lodging)) return plan.lodging;
    if (_isValidLodging(AppState.lodging)) return AppState.lodging;
    return null;
}

function formatMinutes(minutes) {
    if (!Number.isFinite(minutes)) return '-';
    const rounded = Math.round(minutes);
    const h = Math.floor(rounded / 60);
    const m = rounded % 60;
    return h > 0 ? `${h}시간 ${m}분` : `${m}분`;
}

function _getBcrypt() {
    return window.bcrypt || (window.dcodeIO && window.dcodeIO.bcrypt);
}

function _verifyPassword(user, plainPw, bcrypt) {
    if (!user || !plainPw || !bcrypt) return false;
    try {
        if (user.pwHash) return bcrypt.compareSync(plainPw, user.pwHash);
    } catch (e) {
        return false;
    }
    return !!user.pw && user.pw === plainPw;
}

function _promoteLegacyPassword(user, plainPw, bcrypt) {
    const next = { ...user };
    let changed = false;

    if (!next.pwHash && next.pw && next.pw === plainPw) {
        next.pwHash = bcrypt.hashSync(plainPw, BCRYPT_ROUNDS);
        next.createdAt = next.createdAt || new Date().toISOString();
        changed = true;
    }
    if (Object.prototype.hasOwnProperty.call(next, 'pw')) {
        delete next.pw;
        changed = true;
    }
    if (changed) next.updatedAt = new Date().toISOString();

    return { user: next, changed };
}

// =====================================================
// 1. 페이지 내비게이션 (F101~F110)
// =====================================================
function navigate(page, { preserveAttractionState = false } = {}) {
    document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
    document.getElementById(`page-${page}`).classList.add('active');
    if (page !== 'attraction') {
        clearMapState({ resetRouteResult: true });
        clearRouteResultCard();
    }
    if (page === 'attraction') {
        if (!preserveAttractionState) {
            resetAttractionView();
        }
        setAttractionRouteMode(preserveAttractionState ? AppState.routeResult : null);
        setTimeout(() => {
            if (!AppState.map) {
                initMap();
            } else {
                // 창 크기에 맞게 지도 크기 재계산 (F101 관련 가용성 확보)
                AppState.map.relayout();
            }
        }, 100);
    }
    if (page === 'board') renderBoardList();
    if (page === 'hotplace') renderHotplaces();
    if (page === 'notice') renderNoticeList();
    if (page === 'plan') { renderPlanItems(); renderSavedPlans(); renderLodgingInfo(); }
}

function resetAttractionView() {
    clearMapState({ resetRouteResult: true });
    clearRouteResultCard();

    AppState._lastItems = [];
    const list = document.getElementById('resultList');
    if (list) {
        list.innerHTML = `<div class="text-center text-muted p-4">조회할 관광정보를 검색해 주세요.</div>`;
    }

    const areaEl = document.getElementById('searchArea');
    if (areaEl) areaEl.value = '';

    const allTypeBtn = document.querySelector('#typeGroup .btn[data-type=""]');
    if (allTypeBtn) {
        document.querySelectorAll('#typeGroup .btn').forEach(b => {
            b.classList.remove('btn-indigo', 'active');
            b.classList.add('btn-light');
        });
        allTypeBtn.classList.remove('btn-light');
        allTypeBtn.classList.add('btn-indigo', 'active');
    }

    setAttractionRouteMode(null);

    if (AppState.map) {
        AppState.map.setLevel(8);
        AppState.map.setCenter(new kakao.maps.LatLng(37.5665, 126.9780));
    }
}

function navigateAttractionTab() {
    navigate('attraction', { preserveAttractionState: false });
}

// =====================================================
// 2. 카카오맵 초기화 및 마커 제어 (F101, F102, F103)
// =====================================================
function initMap() {
    // 이미 초기화된 경우 resize만 트리거
    if (AppState.map) {
        kakao.maps.event.trigger(AppState.map, 'resize');
        return;
    }
    const container = document.getElementById('map');
    const options = { center: new kakao.maps.LatLng(37.5665, 126.9780), level: 8 };
    AppState.map = new kakao.maps.Map(container, options);
}

function setAttractionRouteMode(routeResult = null) {
    const titleEl = document.getElementById('attractionTitle');
    const backBtnEl = document.getElementById('attractionBackBtn');
    const filterBoxEl = document.getElementById('attractionFilterBox');

    if (routeResult) {
        if (titleEl) titleEl.textContent = `${routeResult.planTitle} 경로`;
        if (backBtnEl) backBtnEl.classList.remove('d-none');
        if (filterBoxEl) filterBoxEl.classList.add('d-none');
        return;
    }

    if (titleEl) titleEl.textContent = '관광정보 조회';
    if (backBtnEl) backBtnEl.classList.add('d-none');
    if (filterBoxEl) filterBoxEl.classList.remove('d-none');
}

function exitRouteNavigationView() {
    clearMapState({ resetRouteResult: true });
    clearRouteResultCard();
    setAttractionRouteMode(null);

    if (AppState._lastItems?.length) {
        renderResults(AppState._lastItems);
        setMarkers(AppState._lastItems);
    } else {
        const list = document.getElementById('resultList');
        if (list) list.innerHTML = `<div class="text-center text-muted p-4">조회할 관광정보를 검색해 주세요.</div>`;
    }
    showToast('일반 관광정보 조회 화면으로 돌아왔습니다.');
}

function _getHomeMarkerImage() {
    if (AppState._homeMarkerImage) return AppState._homeMarkerImage;
    const imageSrc = `data:image/svg+xml;charset=UTF-8,${encodeURIComponent(HOME_MARKER_SVG)}`;
    const imageSize = new kakao.maps.Size(36, 40);
    const imageOption = { offset: new kakao.maps.Point(18, 38) };
    AppState._homeMarkerImage = new kakao.maps.MarkerImage(imageSrc, imageSize, imageOption);
    return AppState._homeMarkerImage;
}

function clearPlanPolyline() {
    if (AppState._planPolyline) {
        AppState._planPolyline.setMap(null);
        AppState._planPolyline = null;
    }

    if (AppState._routePolylines.length) {
        AppState._routePolylines.forEach(polyline => polyline.setMap(null));
        AppState._routePolylines = [];
    }
}

function renderMapRouteInfo(routeInfo = null) {
    const panel = document.getElementById('mapRouteInfo');
    if (!panel) return;

    if (!routeInfo) {
        panel.classList.add('d-none');
        panel.innerHTML = '';
        return;
    }

    panel.innerHTML = `
        ⏳ 예상 소요 시간: ${Math.round(routeInfo.duration / 60)}분<br>
        📏 예상 이동 거리: ${(routeInfo.distance / 1000).toFixed(1)}km
    `;
    panel.classList.remove('d-none');
}

function setMapLoading(isLoading, message = '로딩중입니다...') {
    const overlay = document.getElementById('mapLoadingOverlay');
    if (!overlay) return;
    const labelEl = overlay.querySelector('.map-loading-card');
    if (labelEl) {
        labelEl.innerHTML = `
            <span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
            ${escapeHtml(message)}
        `;
    }
    overlay.classList.toggle('d-none', !isLoading);
}

function setResultListLoading(isLoading, message = '로딩중입니다...') {
    const list = document.getElementById('resultList');
    if (!list) return;
    if (!isLoading) return;

    list.innerHTML = `
        <div class="result-loading">
            <span class="spinner-border spinner-border-sm me-2" role="status" aria-hidden="true"></span>
            ${escapeHtml(message)}
        </div>
    `;
}

function _getRouteSegmentColor(idx) {
    return ROUTE_SEGMENT_COLORS[idx % ROUTE_SEGMENT_COLORS.length];
}

function clearRouteSegmentFocus() {
    AppState._routePolylines.forEach((polyline, idx) => {
        polyline.setOptions({
            strokeWeight: 5,
            strokeOpacity: 0.9,
            strokeColor: _getRouteSegmentColor(idx),
            zIndex: 2
        });
    });
}

function focusRouteSegment(activeIdx) {
    AppState._routePolylines.forEach((polyline, idx) => {
        const isActive = idx === activeIdx;
        polyline.setOptions({
            strokeWeight: isActive ? 7 : 4,
            strokeOpacity: isActive ? 1 : 0.2,
            zIndex: isActive ? 5 : 1
        });
    });
}

function renderRouteLegList(routeResult) {
    const list = document.getElementById('resultList');
    if (!list) return;

    const cardsHtml = routeResult.legs.map((leg, idx) => {
        const color = _getRouteSegmentColor(idx);
        return `
        <div class="route-leg-card border rounded-4 mb-2 bg-white shadow-sm p-3"
             data-segment-idx="${idx}" style="border-left: 6px solid ${color} !important;">
            <div class="d-flex justify-content-between align-items-center mb-1">
                <strong>${idx + 1}번 구간</strong>
                <span class="badge text-bg-light" style="color:${color}; border:1px solid ${color};">${idx + 1} → ${idx + 2}</span>
            </div>
            <div class="small mb-1">${escapeHtml(leg.from)} → ${escapeHtml(leg.to)}</div>
            <div class="small text-muted">⏳ ${Math.round(leg.durationSec / 60)}분 · 📏 ${(leg.distanceMeters / 1000).toFixed(1)}km</div>
        </div>`;
    }).join('');

    const toolbarLabel = routeResult.routeModeLabel || '추천 순서';
    const saveButtonHtml = routeResult.allowOrderSave
        ? '<button class="btn btn-sm btn-indigo" onclick="saveRecommendedRouteOrder()">이 순서 저장</button>'
        : '';

    list.innerHTML = `
        <div class="d-flex justify-content-between align-items-center mb-2 route-leg-toolbar">
            <span class="small fw-semibold text-muted">${escapeHtml(routeResult.planTitle)} ${escapeHtml(toolbarLabel)}</span>
            ${saveButtonHtml}
        </div>
        ${cardsHtml}
    `;

    list.querySelectorAll('.route-leg-card').forEach(card => {
        const idx = Number(card.dataset.segmentIdx);
        card.addEventListener('mouseenter', () => focusRouteSegment(idx));
        card.addEventListener('mouseleave', () => clearRouteSegmentFocus());
        card.addEventListener('focusin', () => focusRouteSegment(idx));
        card.addEventListener('focusout', () => clearRouteSegmentFocus());
    });
}

function _toStopKey(item) {
    return `${Number(item.lat).toFixed(6)}|${Number(item.lng).toFixed(6)}|${String(item.title || '').trim()}`;
}

function saveRecommendedRouteOrder() {
    const routeResult = AppState.routeResult;
    if (!routeResult) {
        showToast('저장할 추천 경로가 없습니다.');
        return;
    }
    if (!routeResult.allowOrderSave) {
        showToast('현재 경로는 저장 대상이 아닙니다.');
        return;
    }

    const planIdx = AppState.plans.findIndex(p => p.id === routeResult.planId);
    if (planIdx === -1) {
        showToast('원본 여행 계획을 찾을 수 없습니다.');
        return;
    }

    const targetPlan = AppState.plans[planIdx];
    const stopsOnly = routeResult.orderedStops.filter(stop => !stop.isLodging);
    const remaining = [...targetPlan.items];
    const reordered = [];

    stopsOnly.forEach(stop => {
        const key = _toStopKey(stop);
        let foundIdx = remaining.findIndex(item => _toStopKey(item) === key);
        if (foundIdx === -1) {
            foundIdx = remaining.findIndex(item => (
                Number(item.lat).toFixed(6) === Number(stop.lat).toFixed(6)
                && Number(item.lng).toFixed(6) === Number(stop.lng).toFixed(6)
            ));
        }
        if (foundIdx === -1) return;
        reordered.push(remaining.splice(foundIdx, 1)[0]);
    });

    targetPlan.items = [...reordered, ...remaining];
    AppState.plans[planIdx] = targetPlan;
    localStorage.setItem('et_plans', JSON.stringify(AppState.plans));
    renderSavedPlans();
    showToast('추천 경로 순서를 여행 계획에 저장했습니다.');
}

/** 지도 위에 마커와 정보창 표시 */
function clearMapState({ resetRouteResult = false } = {}) {
    AppState.markers.forEach(marker => marker.setMap(null));
    AppState.markers = [];

    AppState.infowindows.forEach(iw => iw.close());
    AppState.infowindows = [];

    clearPlanPolyline();
    renderMapRouteInfo(null);

    if (resetRouteResult) {
        AppState.routeResult = null;
    }
}

function setMarkers(items) {
    clearMapState();

    if (!items || items.length === 0) return;

    const bounds = new kakao.maps.LatLngBounds();

    items.forEach(item => {
        const pos = new kakao.maps.LatLng(item.mapy, item.mapx);
        const marker = new kakao.maps.Marker({ 
            map: AppState.map, 
            position: pos,
            clickable: true 
        });

        // 현대적인 심플 디자인을 적용한 정보창 HTML [cite: 23]
        const iwContent = `
            <div style="padding:12px; min-width:180px; max-width:240px; border:none; white-space:normal; word-break:break-word; overflow-wrap:anywhere;">
                <div style="font-weight:700; font-size:14px; margin-bottom:4px; color:#4f46e5; white-space:normal; word-break:break-word; overflow-wrap:anywhere;">${escapeHtml(item.title)}</div>
                <div style="font-size:12px; color:#666; line-height:1.4; white-space:normal; word-break:break-word; overflow-wrap:anywhere;">${escapeHtml(item.addr1)}</div>
            </div>`;
            
        const infowindow = new kakao.maps.InfoWindow({ 
            content: iwContent, 
            removable: true 
        });

        kakao.maps.event.addListener(marker, 'click', () => {
            AppState.infowindows.forEach(iw => iw.close());
            infowindow.open(AppState.map, marker);
            AppState.map.panTo(pos); // 핀 클릭 시 중심으로 이동
        });

        AppState.markers.push(marker);
        AppState.infowindows.push(infowindow);
        bounds.extend(pos);
    });

    AppState.map.setBounds(bounds);
}

/** 지도 특정 좌표로 이동 */
function panTo(lat, lng) {
    if (!AppState.map) return;
    AppState.map.panTo(new kakao.maps.LatLng(lat, lng));
}

// =====================================================
// 3. 관광정보 조회 (F101, F102, F103) — KorService2
// =====================================================

/** 공통 API URL 빌더 */
function _buildApiUrl(endpoint, extraParams = '') {
    return `https://apis.data.go.kr/B551011/KorService2/${endpoint}?serviceKey=${API_KEY}&MobileOS=ETC&MobileApp=EnjoyTrip&_type=json&numOfRows=20&pageNo=1${extraParams}`;
}

/** 응답 item 배열 정규화 (단건일 때 object로 반환되므로 배열로 통일) */
function _normalizeItems(raw) {
    if (!raw) return [];
    return Array.isArray(raw) ? raw : [raw];
}

/** 지역기반 관광정보 조회 (areaBasedList2) */
async function fetchTourismData() {
    setAttractionRouteMode(null);
    const lDongRegnCd = document.getElementById('searchArea').value;
    const typeId      = document.querySelector('#typeGroup .active')?.dataset.type ?? '';
    const btn         = document.querySelector('[onclick="fetchTourismData()"]');

    let extra = '&arrange=A';
    if (lDongRegnCd) extra += `&lDongRegnCd=${lDongRegnCd}`;
    if (typeId)      extra += `&contentTypeId=${typeId}`;

    if (btn) btn.disabled = true;
    setResultListLoading(true, '관광정보 조회중입니다...');
    setMapLoading(true, '관광정보 조회중입니다...');
    try {
        const res  = await fetch(_buildApiUrl('areaBasedList2', extra));
        const json = await res.json();
        const items = _normalizeItems(json?.response?.body?.items?.item);
        AppState._lastItems = items;
        renderResults(items);
        setMarkers(items);
    } catch (e) {
        showToast('데이터를 가져오는 중 오류가 발생했습니다.');
        const list = document.getElementById('resultList');
        if (list) list.innerHTML = `<div class="text-center text-muted p-4">조회 중 오류가 발생했습니다.</div>`;
    } finally {
        if (btn) btn.disabled = false;
        setMapLoading(false);
    }
}

/** 키워드 검색 조회 (searchKeyword2) */
async function searchByKeyword(keyword) {
    if (!keyword.trim()) { showToast('검색어를 입력하세요.'); return; }

    navigate('attraction');
    setAttractionRouteMode(null);
    await new Promise(r => setTimeout(r, 150));

    const typeId = document.querySelector('#typeGroup .active')?.dataset.type ?? '';
    let extra = `&keyword=${encodeURIComponent(keyword)}&arrange=A`;
    if (typeId) extra += `&contentTypeId=${typeId}`;

    setResultListLoading(true, '키워드 검색중입니다...');
    setMapLoading(true, '키워드 검색중입니다...');
    try {
        const res  = await fetch(_buildApiUrl('searchKeyword2', extra));
        const json = await res.json();
        const items = _normalizeItems(json?.response?.body?.items?.item);
        AppState._lastItems = items;
        renderResults(items);
        setMarkers(items);
    } catch (e) {
        showToast('검색 중 오류가 발생했습니다.');
        const list = document.getElementById('resultList');
        if (list) list.innerHTML = `<div class="text-center text-muted p-4">검색 중 오류가 발생했습니다.</div>`;
    } finally {
        setMapLoading(false);
    }
}

/** index 기반 계획 추가 헬퍼 (onclick 인라인에서 XSS 없이 사용) */
function addToPlanByIdx(idx) {
    const item = AppState._lastItems?.[idx];
    if (!item) return;
    addToPlan(item.contentid, item.title, item.mapy, item.mapx);
}

function renderResults(items) {
    const list = document.getElementById('resultList');
    if (!items.length) {
        list.innerHTML = `<div class="text-center text-muted p-4">검색 결과가 없습니다.</div>`;
        return;
    }
    list.innerHTML = items.map((item, idx) => `
        <div class="spot-card border rounded-4 mb-2 bg-white shadow-sm overflow-hidden"
             onclick="panTo(${item.mapy}, ${item.mapx})">
            ${item.firstimage
                ? `<img src="${item.firstimage}" class="w-100" style="height:120px;object-fit:cover;" alt="${escapeHtml(item.title)}" loading="lazy">`
                : '<div class="bg-light d-flex align-items-center justify-content-center" style="height:60px;"><i class="bi bi-image text-muted fs-3"></i></div>'}
            <div class="p-3">
                <h6 class="fw-bold mb-1">${escapeHtml(item.title)}</h6>
                <p class="small text-muted mb-2">${escapeHtml(item.addr1) || '주소 정보 없음'}</p>
                <button class="btn btn-sm btn-indigo w-100"
                        onclick="event.stopPropagation(); addToPlanByIdx(${idx})">
                    계획에 추가
                </button>
            </div>
        </div>
    `).join('');
}

// =====================================================
// 4. 여행 계획 설정 (F104)
// =====================================================
function addToPlan(id, title, lat, lng) {
    if (AppState.currPlanItems.some(i => i.id === String(id))) {
        showToast(`"${title}"은(는) 이미 추가되어 있습니다.`);
        return;
    }
    AppState.currPlanItems.push({ id: String(id), title, lat, lng });
    renderPlanItems();
    showToast(`"${title}"이(가) 여행 목록에 추가되었습니다.`);
}

function removeFromPlan(idx) {
    AppState.currPlanItems.splice(idx, 1);
    renderPlanItems();
}

function renderPlanItems() {
    const el = document.getElementById('planSortable');
    if (AppState.currPlanItems.length === 0) {
        el.innerHTML = `<li class="empty-msg text-center p-5 border border-dashed rounded-4 text-muted">관광지 조회에서 장소를 추가하세요.</li>`;
        return;
    }
    el.innerHTML = AppState.currPlanItems.map((item, idx) => `
        <li class="plan-item p-3 border rounded-4 bg-white shadow-sm d-flex justify-content-between align-items-center"
            draggable="true"
            ondragstart="handleDragStart(${idx})"
            ondragover="handleDragOver(event)"
            ondrop="handleDrop(event, ${idx})">
            <span><span class="badge bg-indigo me-2">${idx + 1}</span>${escapeHtml(item.title)}</span>
            <button class="btn btn-sm text-danger" onclick="removeFromPlan(${idx})">
                <i class="bi bi-trash"></i>
            </button>
        </li>
    `).join('');
}

/** 여행 계획 저장 (F104) */
function savePlan() {
    if (!AppState.user) {
        showToast('로그인 후 이용 가능합니다.');
        showAuthModal('login');
        return;
    }
    const title = document.getElementById('planTitle').value.trim();
    const memo  = document.getElementById('planMemo').value.trim();
    if (!title) { showToast('일정 제목을 입력하세요.'); return; }
    if (AppState.currPlanItems.length === 0) { showToast('여행지를 하나 이상 추가하세요.'); return; }

    const plan = {
        id: Date.now(),
        title,
        memo,
        items: [...AppState.currPlanItems],
        lodging: AppState.lodging ? { ...AppState.lodging } : null,
        author: AppState.user.id,
        date: new Date().toLocaleDateString()
    };
    AppState.plans.push(plan);
    localStorage.setItem('et_plans', JSON.stringify(AppState.plans));

    AppState.currPlanItems = [];
    document.getElementById('planTitle').value = '';
    document.getElementById('planMemo').value = '';
    renderPlanItems();
    renderSavedPlans();
    showToast('여행 계획이 저장되었습니다.');
}

// --- 드래그 앤 드롭 (F104 순서 변경) ---
function handleDragStart(idx) {
    AppState.dragSrcIdx = idx;
}

function handleDragOver(e) {
    e.preventDefault();
}

function handleDrop(e, idx) {
    e.preventDefault();
    const src = AppState.dragSrcIdx;
    if (src === null || src === idx) return;
    const items = [...AppState.currPlanItems];
    const [moved] = items.splice(src, 1);
    items.splice(idx, 0, moved);
    AppState.currPlanItems = items;
    AppState.dragSrcIdx = null;
    renderPlanItems();
}

// --- 저장된 여행 계획 목록 (F104) ---
function renderSavedPlans() {
    const el = document.getElementById('savedPlanList');
    if (!el) return;
    const myPlans = AppState.user
        ? AppState.plans.filter(p => p.author === AppState.user.id)
        : [];
    if (!myPlans.length) {
        el.innerHTML = `<p class="text-muted">저장된 여행 계획이 없습니다.</p>`;
        return;
    }
    el.innerHTML = myPlans.map(plan => `
        <div class="card border-0 shadow-sm rounded-4 p-4 mb-3 saved-plan-card" onclick="openPlanDetailModal(${plan.id})">
            <div class="d-flex justify-content-between align-items-start flex-wrap gap-2">
                <div>
                    <h5 class="fw-bold mb-1">${escapeHtml(plan.title)}</h5>
                    <p class="text-muted small mb-2">${plan.date} · ${plan.items.length}개 장소</p>
                    <p class="small mb-2">
                        <span class="fw-semibold">숙소:</span>
                        ${escapeHtml(_getPlanLodging(plan)?.placeName || _getPlanLodging(plan)?.address || '미설정')}
                    </p>
                    ${plan.memo ? `<p class="small mb-2">${escapeHtml(plan.memo)}</p>` : ''}
                    <div class="d-flex flex-wrap gap-1 mt-1">
                        ${plan.items.map((item, i) => `
                            <span class="badge bg-light text-dark border">
                                <span class="text-indigo fw-bold">${i + 1}</span> ${escapeHtml(item.title)}
                            </span>`).join('')}
                    </div>
                </div>
                <div class="d-flex gap-2 flex-shrink-0">
                    <button class="btn btn-sm btn-indigo" onclick="event.stopPropagation(); showPlanOnMap(${plan.id})">
                        <i class="bi bi-map me-1"></i>지도 보기
                    </button>
                    <button class="btn btn-sm btn-outline-primary" onclick="event.stopPropagation(); recommendBestRoute(${plan.id}, this)">
                        <i class="bi bi-sign-turn-right me-1"></i>최단 경로 추천
                    </button>
                    <button class="btn btn-sm btn-outline-danger" onclick="event.stopPropagation(); deletePlan(${plan.id})">삭제</button>
                </div>
            </div>
        </div>
    `).join('');
}

function openPlanDetailModal(planId) {
    const plan = AppState.plans.find(p => p.id === planId);
    const titleEl = document.getElementById('planDetailTitle');
    const modalEl = document.getElementById('planDetailModal');
    if (!plan || !titleEl || !modalEl) return;

    AppState.modalPlanId = plan.id;
    AppState.modalPlanItems = plan.items.map(item => ({ ...item }));
    AppState.modalDragSrcIdx = null;
    const modalLodging = _getPlanLodging(plan);
    AppState.modalPlanLodging = modalLodging ? { ...modalLodging } : null;

    titleEl.textContent = plan.title;
    renderPlanDetailModalBody(plan);

    bootstrap.Modal.getOrCreateInstance(modalEl).show();
}

function renderPlanDetailModalBody(plan) {
    const bodyEl = document.getElementById('planDetailBody');
    if (!bodyEl) return;

    const lodgingTitle = AppState.modalPlanLodging?.placeName || '숙소 좌표 확정됨';
    const lodgingAddress = AppState.modalPlanLodging?.address || '';
    const lodgingMeta = AppState.modalPlanLodging
        ? `위도 ${Number(AppState.modalPlanLodging.lat).toFixed(6)}, 경도 ${Number(AppState.modalPlanLodging.lng).toFixed(6)}`
        : '숙소 미설정';

    bodyEl.innerHTML = `
        <div class="mb-2 text-muted small">${plan.date} · ${AppState.modalPlanItems.length}개 장소</div>
        ${plan.memo ? `<div class="p-3 rounded-3 bg-light border small mb-3">${escapeHtml(plan.memo)}</div>` : '<div class="text-muted small mb-3">메모 없음</div>'}
        <div class="border rounded-4 p-3 mb-3 bg-light-subtle">
            <div class="small fw-semibold mb-2">숙소 정보</div>
            <div class="d-flex flex-column flex-md-row gap-2 mb-2">
                <input type="text" id="modalLodgingAddress" class="form-control" placeholder="숙소 주소를 입력하세요" value="${escapeHtml(lodgingAddress)}">
                <button class="btn btn-outline-secondary" onclick="updateModalPlanLodging()">숙소 좌표 확인</button>
            </div>
            <div class="small text-muted" id="modalLodgingStatus">
                <span class="fw-semibold text-dark">${escapeHtml(lodgingTitle)}</span><br>
                ${escapeHtml(lodgingAddress || '주소 정보 없음')}<br>
                ${escapeHtml(lodgingMeta)}
            </div>
        </div>
        <div class="small fw-semibold mb-2">드래그해서 순서 변경</div>
        <ul class="list-unstyled d-flex flex-column gap-2 mb-3" id="modalPlanSortable">
            ${AppState.modalPlanItems.map((item, idx) => `
                <li class="modal-plan-item p-3 border rounded-4 bg-white shadow-sm d-flex align-items-center"
                    draggable="true"
                    ondragstart="handleModalPlanDragStart(${idx})"
                    ondragover="handleModalPlanDragOver(event)"
                    ondrop="handleModalPlanDrop(event, ${idx})">
                    <span class="badge bg-indigo me-2 modal-plan-index">${idx + 1}</span>
                    <span>${escapeHtml(item.title)}</span>
                </li>
            `).join('')}
        </ul>
        <div class="d-flex justify-content-end">
            <button class="btn btn-indigo" onclick="saveModalPlanOrder()">순서 저장</button>
        </div>
    `;
}

function handleModalPlanDragStart(idx) {
    AppState.modalDragSrcIdx = idx;
}

function handleModalPlanDragOver(e) {
    e.preventDefault();
}

function handleModalPlanDrop(e, idx) {
    e.preventDefault();
    const src = AppState.modalDragSrcIdx;
    if (src === null || src === idx) return;

    const items = [...AppState.modalPlanItems];
    const [moved] = items.splice(src, 1);
    items.splice(idx, 0, moved);
    AppState.modalPlanItems = items;
    AppState.modalDragSrcIdx = null;

    const plan = AppState.plans.find(p => p.id === AppState.modalPlanId);
    if (plan) renderPlanDetailModalBody(plan);
}

async function _resolveLodgingFromAddress(address) {
    if (!(window.kakao && kakao.maps && kakao.maps.services)) {
        throw new Error('카카오 주소 검색 라이브러리를 불러오지 못했습니다.');
    }

    const geocoder = new kakao.maps.services.Geocoder();
    const places = new kakao.maps.services.Places();

    const lodging = await new Promise((resolve, reject) => {
        geocoder.addressSearch(address, (result, status) => {
            if (status !== kakao.maps.services.Status.OK || !result.length) {
                reject(new Error('주소를 찾을 수 없습니다.'));
                return;
            }
            resolve({
                address: result[0].address_name || address,
                lat: Number(result[0].y),
                lng: Number(result[0].x),
                placeName: null
            });
        });
    });

    await new Promise(resolve => {
        places.keywordSearch(address, (result, status) => {
            if (status === kakao.maps.services.Status.OK && result?.length) {
                lodging.placeName = result[0].place_name || null;
            }
            resolve();
        });
    });

    return lodging;
}

async function updateModalPlanLodging() {
    const inputEl = document.getElementById('modalLodgingAddress');
    if (!inputEl) return;

    const address = inputEl.value.trim();
    if (!address) {
        showToast('숙소 주소를 입력하세요.');
        return;
    }

    try {
        const lodging = await _resolveLodgingFromAddress(address);
        AppState.modalPlanLodging = lodging;
        const plan = AppState.plans.find(p => p.id === AppState.modalPlanId);
        if (plan) renderPlanDetailModalBody(plan);
        showToast('모달에서 숙소 정보를 업데이트했습니다.');
    } catch (e) {
        showToast(e.message);
    }
}

function saveModalPlanOrder() {
    if (!AppState.modalPlanId) return;
    const idx = AppState.plans.findIndex(p => p.id === AppState.modalPlanId);
    if (idx === -1) return;

    AppState.plans[idx].items = AppState.modalPlanItems.map(item => ({ ...item }));
    AppState.plans[idx].lodging = AppState.modalPlanLodging ? { ...AppState.modalPlanLodging } : null;
    localStorage.setItem('et_plans', JSON.stringify(AppState.plans));
    renderSavedPlans();
    showToast('여행 계획 순서를 저장했습니다.');

    const modalEl = document.getElementById('planDetailModal');
    if (modalEl) bootstrap.Modal.getOrCreateInstance(modalEl).hide();
}

function renderLodgingInfo() {
    const inputEl = document.getElementById('lodgingAddress');
    const infoEl = document.getElementById('lodgingInfo');
    if (!inputEl || !infoEl) return;

    if (AppState.lodging) {
        inputEl.value = AppState.lodging.address || '';
        infoEl.classList.remove('is-pending');
        infoEl.classList.add('is-confirmed');
        const lodgingTitle = AppState.lodging.placeName || '숙소 좌표 확정됨';
        infoEl.innerHTML = `
            <span class="fw-bold text-dark">${escapeHtml(lodgingTitle)}</span><br>
            ${escapeHtml(AppState.lodging.address)}<br>
            위도 ${AppState.lodging.lat.toFixed(6)}, 경도 ${AppState.lodging.lng.toFixed(6)}
        `;
    } else {
        infoEl.classList.remove('is-confirmed');
        infoEl.classList.add('is-pending');
        infoEl.textContent = '숙소를 입력하면 저장 계획에서 최단 소요시간 경로를 추천합니다.';
    }
}

function clearRouteResultCard() {
    const card = document.getElementById('routeResultCard');
    if (!card) return;
    card.classList.add('d-none');
    card.innerHTML = '';
    renderMapRouteInfo(null);
}

function renderRouteResultCard(routeResult) {
    const card = document.getElementById('routeResultCard');
    if (!card) return;

    const rows = routeResult.legs.map((leg, idx) => `
        <tr>
            <td>${idx + 1}</td>
            <td>${escapeHtml(leg.from)} → ${escapeHtml(leg.to)}</td>
            <td>${formatMinutes(leg.minutes)}</td>
        </tr>
    `).join('');

    card.innerHTML = `
        <div class="d-flex justify-content-between align-items-center mb-3">
            <h5 class="fw-bold mb-0">최단 소요시간 경로 결과</h5>
            <span class="badge text-bg-primary">${formatMinutes(routeResult.totalMinutes)}</span>
        </div>
        <p class="text-muted small mb-3">
            ${escapeHtml(routeResult.planTitle)} · 숙소 왕복 · 총 ${routeResult.orderedStops.length - 2}개 경유지
        </p>
        <div class="small mb-3">
            <span class="fw-bold">추천 순서:</span>
            ${routeResult.orderedStops.map(s => escapeHtml(s.title)).join(' → ')}
        </div>
        <div class="table-responsive">
            <table class="table table-sm align-middle">
                <thead><tr><th>#</th><th>구간</th><th>소요시간</th></tr></thead>
                <tbody>${rows}</tbody>
            </table>
        </div>
    `;
    card.classList.remove('d-none');
}

/** 저장된 계획 경로를 사용자 지정 순서대로 API 기반 경로로 표시 (F104) */
async function showPlanOnMap(planId) {
    const plan = AppState.plans.find(p => p.id === planId);
    if (!plan || !plan.items.length) return;
    const lodging = _getPlanLodging(plan);
    if (!lodging || !Number.isFinite(lodging.lat) || !Number.isFinite(lodging.lng)) {
        showToast('숙소 좌표를 먼저 확인해 주세요.');
        return;
    }

    navigate('attraction');
    await new Promise(resolve => setTimeout(resolve, 150));
    setResultListLoading(true, '저장된 경로를 불러오는 중입니다...');
    setMapLoading(true, '저장된 경로를 불러오는 중입니다...');

    // 기존 마커·폴리라인 제거
    clearMapState();

    const planStops = plan.items
        .filter(item => Number.isFinite(Number(item.lat)) && Number.isFinite(Number(item.lng)))
        .map(item => ({ title: item.title, lat: Number(item.lat), lng: Number(item.lng) }));

    const lodgingNode = {
        title: `숙소 (${lodging.placeName || lodging.address})`,
        lat: Number(lodging.lat),
        lng: Number(lodging.lng),
        isLodging: true
    };

    const orderedStops = [lodgingNode, ...planStops, lodgingNode];

    if (planStops.length === 0) {
        showToast('지도에 표시할 좌표 정보가 없습니다.');
        return;
    }

    const legs = [];
    for (let i = 0; i < orderedStops.length - 1; i += 1) {
        const from = orderedStops[i];
        const to = orderedStops[i + 1];

        try {
            const edge = await _fetchTravelEdge(from, to);
            legs.push({
                from: from.title,
                to: to.title,
                minutes: edge.minutes,
                durationSec: edge.durationSec,
                distanceMeters: edge.distanceMeters,
                points: edge.points
            });
        } catch (e) {
            legs.push({
                from: from.title,
                to: to.title,
                minutes: 0,
                durationSec: 0,
                distanceMeters: 0,
                points: [
                    { lat: from.lat, lng: from.lng },
                    { lat: to.lat, lng: to.lng }
                ]
            });
        }
    }

    const routeResult = {
        planId: plan.id,
        planTitle: plan.title,
        routeModeLabel: '저장된 순서 경로',
        allowOrderSave: false,
        orderedStops,
        totalMinutes: legs.reduce((acc, leg) => acc + leg.minutes, 0),
        totalDurationSec: legs.reduce((acc, leg) => acc + leg.durationSec, 0),
        totalDistanceMeters: legs.reduce((acc, leg) => acc + leg.distanceMeters, 0),
        legs,
        pathPoints: []
    };

    AppState.routeResult = routeResult;
    _renderRecommendedRouteOnMap(routeResult);
}

async function resolveLodgingAddress() {
    const addressEl = document.getElementById('lodgingAddress');
    if (!addressEl) return;
    const address = addressEl.value.trim();
    if (!address) {
        showToast('숙소 주소를 입력하세요.');
        return;
    }
    const lodging = await _resolveLodgingFromAddress(address).catch(err => {
        showToast(err.message);
        return null;
    });

    if (!lodging) return;

    _setLodging(lodging);
    clearRouteResultCard();
    showToast('숙소 좌표가 저장되었습니다.');
}

function _routeKey(from, to) {
    return `${from.lng.toFixed(6)},${from.lat.toFixed(6)}->${to.lng.toFixed(6)},${to.lat.toFixed(6)}`;
}

async function _fetchTravelEdge(from, to) {
    const key = _routeKey(from, to);
    if (AppState._routeCache[key]) return AppState._routeCache[key];

    if (!KAKAO_MOBILITY_REST_KEY || KAKAO_MOBILITY_REST_KEY === 'YOUR_KAKAO_REST_API_KEY') {
        throw new Error('KAKAO_MOBILITY_REST_KEY 값을 main.js에 설정해 주세요.');
    }

    const url = `https://apis-navi.kakaomobility.com/v1/directions?origin=${from.lng},${from.lat}&destination=${to.lng},${to.lat}&priority=RECOMMEND&car_fuel=GASOLINE&car_hipass=false`;
    const res = await fetch(url, {
        headers: { Authorization: `KakaoAK ${KAKAO_MOBILITY_REST_KEY}` }
    });
    if (!res.ok) {
        throw new Error(`경로 API 호출 실패 (${res.status})`);
    }
    const json = await res.json();
    const route = json?.routes?.[0];
    const summary = route?.summary;
    if (!summary) {
        throw new Error('경로 정보를 찾지 못했습니다.');
    }

    const points = [];
    const roads = route?.sections?.[0]?.roads || [];
    roads.forEach(road => {
        for (let i = 0; i < road.vertexes.length; i += 2) {
            points.push({ lng: Number(road.vertexes[i]), lat: Number(road.vertexes[i + 1]) });
        }
    });
    if (!points.length) {
        points.push({ lat: from.lat, lng: from.lng }, { lat: to.lat, lng: to.lng });
    }

    const edge = {
        minutes: Number(summary.duration) / 60,
        durationSec: Number(summary.duration),
        distanceMeters: Number(summary.distance),
        points
    };
    AppState._routeCache[key] = edge;
    return edge;
}

function _dijkstra(adjacency, source) {
    const n = adjacency.length;
    const dist = Array(n).fill(Infinity);
    const prev = Array(n).fill(-1);
    const visited = Array(n).fill(false);
    dist[source] = 0;

    for (let c = 0; c < n; c += 1) {
        let u = -1;
        let min = Infinity;
        for (let i = 0; i < n; i += 1) {
            if (!visited[i] && dist[i] < min) {
                min = dist[i];
                u = i;
            }
        }
        if (u === -1) break;
        visited[u] = true;

        adjacency[u].forEach(edge => {
            const alt = dist[u] + edge.weight;
            if (alt < dist[edge.to]) {
                dist[edge.to] = alt;
                prev[edge.to] = u;
            }
        });
    }
    return { dist, prev };
}

function _buildPermutations(arr) {
    const out = [];
    const used = Array(arr.length).fill(false);
    const current = [];

    function dfs() {
        if (current.length === arr.length) {
            out.push([...current]);
            return;
        }
        for (let i = 0; i < arr.length; i += 1) {
            if (used[i]) continue;
            used[i] = true;
            current.push(arr[i]);
            dfs();
            current.pop();
            used[i] = false;
        }
    }
    dfs();
    return out;
}

async function _buildGraph(nodes) {
    const n = nodes.length;
    const adjacency = Array.from({ length: n }, () => []);
    const directEdges = {};

    const tasks = [];
    for (let i = 0; i < n; i += 1) {
        for (let j = 0; j < n; j += 1) {
            if (i === j) continue;
            tasks.push((async () => {
                const edge = await _fetchTravelEdge(nodes[i], nodes[j]);
                adjacency[i].push({ to: j, weight: edge.minutes });
                directEdges[`${i}->${j}`] = edge;
            })());
        }
    }
    await Promise.all(tasks);

    const distMatrix = [];
    for (let i = 0; i < n; i += 1) {
        const { dist } = _dijkstra(adjacency, i);
        distMatrix.push(dist);
    }
    return { distMatrix, directEdges };
}

function _renderRecommendedRouteOnMap(routeResult) {
    navigate('attraction', { preserveAttractionState: true });
    setTimeout(() => {
        clearMapState();
        setAttractionRouteMode(routeResult);

        const bounds = new kakao.maps.LatLngBounds();
        routeResult.orderedStops.forEach((stop, idx) => {
            const pos = new kakao.maps.LatLng(stop.lat, stop.lng);
            bounds.extend(pos);
            const markerOptions = { map: AppState.map, position: pos };
            if (stop.isLodging) {
                markerOptions.image = _getHomeMarkerImage();
            }
            const marker = new kakao.maps.Marker(markerOptions);
            const label = idx === 0 ? '출발' : (idx === routeResult.orderedStops.length - 1 ? '도착' : `${idx}`);
            const iw = new kakao.maps.InfoWindow({
                content: `<div style="padding:8px;min-width:150px;max-width:230px;white-space:normal;word-break:break-word;overflow-wrap:anywhere;">
                    <span class="badge" style="background:#4f46e5;">${label}</span>
                    <strong style="margin-left:4px;white-space:normal;word-break:break-word;overflow-wrap:anywhere;">${escapeHtml(stop.title)}</strong>
                </div>`,
                removable: true
            });
            kakao.maps.event.addListener(marker, 'click', () => {
                AppState.infowindows.forEach(w => w.close());
                iw.open(AppState.map, marker);
            });
            AppState.markers.push(marker);
            AppState.infowindows.push(iw);
        });

        clearPlanPolyline();
        routeResult.legs.forEach((leg, idx) => {
            const points = leg.points.length
                ? leg.points.map(p => new kakao.maps.LatLng(p.lat, p.lng))
                : [
                    new kakao.maps.LatLng(routeResult.orderedStops[idx].lat, routeResult.orderedStops[idx].lng),
                    new kakao.maps.LatLng(routeResult.orderedStops[idx + 1].lat, routeResult.orderedStops[idx + 1].lng)
                ];

            const polyline = new kakao.maps.Polyline({
                map: AppState.map,
                path: points,
                strokeWeight: 5,
                strokeColor: _getRouteSegmentColor(idx),
                strokeOpacity: 0.9,
                strokeStyle: 'solid',
                zIndex: 2
            });
            AppState._routePolylines.push(polyline);
        });

        clearRouteSegmentFocus();
        AppState.map.setBounds(bounds);
        renderMapRouteInfo({
            duration: routeResult.totalDurationSec,
            distance: routeResult.totalDistanceMeters
        });
        renderRouteLegList(routeResult);
        setMapLoading(false);
    }, 150);
}

function _setRecommendButtonLoading(btnEl, isLoading) {
    if (!btnEl) return;
    if (!btnEl.dataset.defaultHtml) {
        btnEl.dataset.defaultHtml = btnEl.innerHTML;
    }
    btnEl.disabled = !!isLoading;
    btnEl.innerHTML = isLoading
        ? '<span class="spinner-border spinner-border-sm me-1" role="status" aria-hidden="true"></span>계산 중...'
        : btnEl.dataset.defaultHtml;
}

async function recommendBestRoute(planId, btnEl = null) {
    _setRecommendButtonLoading(btnEl, true);
    const plan = AppState.plans.find(p => p.id === planId);
    if (!plan || !plan.items.length) {
        showToast('추천할 여행 계획이 없습니다.');
        _setRecommendButtonLoading(btnEl, false);
        return;
    }
    const planLodging = _getPlanLodging(plan);
    if (!planLodging) {
        showToast('먼저 숙소 주소를 입력하고 좌표를 확인해 주세요.');
        _setRecommendButtonLoading(btnEl, false);
        return;
    }
    if (plan.items.length > MAX_ROUTE_WAYPOINTS) {
        showToast(`경유지는 최대 ${MAX_ROUTE_WAYPOINTS}개까지 추천할 수 있습니다.`);
        _setRecommendButtonLoading(btnEl, false);
        return;
    }

    const nodes = [
        {
            title: `숙소 (${planLodging.placeName || planLodging.address})`,
            lat: planLodging.lat,
            lng: planLodging.lng,
            isLodging: true
        },
        ...plan.items.map(item => ({ title: item.title, lat: Number(item.lat), lng: Number(item.lng) }))
    ];
    if (nodes.some(n => !Number.isFinite(n.lat) || !Number.isFinite(n.lng))) {
        showToast('경로 계산에 필요한 좌표 정보가 부족합니다.');
        _setRecommendButtonLoading(btnEl, false);
        return;
    }

    setResultListLoading(true, '최단 경로 계산중입니다...');
    setMapLoading(true, '최단 경로 계산중입니다...');
    try {
        const { distMatrix, directEdges } = await _buildGraph(nodes);
        const waypointIdx = Array.from({ length: nodes.length - 1 }, (_, i) => i + 1);
        const perms = _buildPermutations(waypointIdx);
        let best = null;

        perms.forEach(perm => {
            let total = 0;
            let prev = 0;
            for (const idx of perm) {
                total += distMatrix[prev][idx];
                prev = idx;
            }
            total += distMatrix[prev][0];

            if (!Number.isFinite(total)) return;
            if (!best || total < best.total) {
                best = { perm, total };
            }
        });

        if (!best) {
            showToast('경로를 계산할 수 없습니다.');
            return;
        }

        const sequence = [0, ...best.perm, 0];
        const legs = [];
        const pathPoints = [];
        for (let i = 0; i < sequence.length - 1; i += 1) {
            const from = sequence[i];
            const to = sequence[i + 1];
            const edge = directEdges[`${from}->${to}`];
            if (!edge) throw new Error(`${nodes[from].title} → ${nodes[to].title} 구간 정보가 없습니다.`);
            legs.push({
                from: nodes[from].title,
                to: nodes[to].title,
                minutes: edge.minutes,
                durationSec: edge.durationSec,
                distanceMeters: edge.distanceMeters,
                points: edge.points
            });
            edge.points.forEach((point, pointIdx) => {
                if (pointIdx === 0 && pathPoints.length > 0) return;
                pathPoints.push(point);
            });
        }

        const routeResult = {
            planId: plan.id,
            planTitle: plan.title,
            routeModeLabel: '추천 순서',
            allowOrderSave: true,
            orderedStops: sequence.map(idx => nodes[idx]),
            totalMinutes: legs.reduce((acc, leg) => acc + leg.minutes, 0),
            totalDurationSec: legs.reduce((acc, leg) => acc + leg.durationSec, 0),
            totalDistanceMeters: legs.reduce((acc, leg) => acc + leg.distanceMeters, 0),
            legs,
            pathPoints
        };

        AppState.routeResult = routeResult;
        renderRouteResultCard(routeResult);
        _renderRecommendedRouteOnMap(routeResult);
        showToast('최단 소요시간 경로를 추천했습니다.');
    } catch (e) {
        setMapLoading(false);
        showToast(`경로 계산 실패: ${e.message}`);
    } finally {
        _setRecommendButtonLoading(btnEl, false);
        if (!AppState.routeResult || AppState.routeResult.planId !== planId) {
            setMapLoading(false);
        }
    }
}

/** 저장된 계획 삭제 */
function deletePlan(planId) {
    if (!confirm('여행 계획을 삭제하시겠습니까?')) return;
    AppState.plans = AppState.plans.filter(p => p.id !== planId);
    localStorage.setItem('et_plans', JSON.stringify(AppState.plans));
    renderSavedPlans();
    clearRouteResultCard();
    showToast('여행 계획이 삭제되었습니다.');
}

// =====================================================
// 5. 핫플레이스 (F105)
// =====================================================
let _hpImgDataUrl = null;

function handleFileDropHP(event) {
    event.preventDefault();
    event.stopPropagation();
    document.getElementById('uploadZone').classList.remove('dragover');
    
    const files = event.dataTransfer?.files || event.target.files;
    if (files?.length > 0) {
        const file = files[0];
        const input = document.getElementById('hpFile');
        input.files = files;
        previewImg({ target: input });
    }
}

function previewImg(event) {
    const file = event.target.files[0];
    if (!file) return;
    
    // 이미지 파일만 허용
    if (!file.type.startsWith('image/')) {
        showToast('이미지 파일만 업로드할 수 있습니다.');
        return;
    }
    
    // 파일 크기 체크 (10MB 이상이면 경고)
    if (file.size > 10 * 1024 * 1024) {
        showToast('파일 크기는 10MB 이하여야 합니다.');
        return;
    }
    
    const reader = new FileReader();
    reader.onload = e => {
        _hpImgDataUrl = e.target.result;
        const preview = document.getElementById('hpPreview');
        const statusEl = document.getElementById('hpFileStatus');
        
        preview.innerHTML = `<img src="${_hpImgDataUrl}" class="img-fluid rounded-4" alt="미리보기" style="max-height:240px; object-fit:cover; width:100%; border: 1px solid #e2e8f0;">`;
        preview.classList.remove('d-none');
        
        statusEl.innerHTML = `✓ ${escapeHtml(file.name)} (${(file.size / 1024).toFixed(1)}KB) 등록됨`;
        statusEl.classList.remove('d-none');
    };
    reader.onerror = () => {
        showToast('파일 읽기에 실패했습니다.');
    };
    reader.readAsDataURL(file);
}

function registerHotplace() {
    if (!AppState.user) {
        showToast('로그인 후 이용 가능합니다.');
        showAuthModal('login');
        return;
    }
    const name = document.getElementById('hpName').value.trim();
    const desc = document.getElementById('hpDesc').value.trim();
    if (!name) { showToast('장소명을 입력하세요.'); return; }
    if (!desc) { showToast('상세 설명을 입력하세요.'); return; }

    const hotplace = {
        id: Date.now(),
        name,
        desc,
        img: _hpImgDataUrl,
        author: AppState.user.id,
        date: new Date().toLocaleDateString()
    };
    AppState.hotplaces.push(hotplace);
    localStorage.setItem('et_hotplaces', JSON.stringify(AppState.hotplaces));

    // 폼 초기화
    _hpImgDataUrl = null;
    document.getElementById('hpName').value = '';
    document.getElementById('hpDesc').value = '';
    document.getElementById('hpFile').value = '';
    document.getElementById('hpPreview').innerHTML = '';
    document.getElementById('hpPreview').classList.add('d-none');

    renderHotplaces();
    showToast('핫플레이스가 등록되었습니다.');
}

function renderHotplaces() {
    const container = document.getElementById('hotplaceList');
    if (!AppState.hotplaces.length) {
        container.innerHTML = `<div class="col-12 text-center text-muted p-5">등록된 핫플레이스가 없습니다.</div>`;
        return;
    }
    container.innerHTML = AppState.hotplaces.map(hp => `
        <div class="col">
            <div class="card border-0 shadow-sm rounded-4 h-100">
                ${hp.img ? `<img src="${hp.img}" class="card-img-top rounded-top-4" style="height:180px; object-fit:cover;" alt="${escapeHtml(hp.name)}">` : ''}
                <div class="card-body">
                    <h6 class="fw-bold mb-1">${escapeHtml(hp.name)}</h6>
                    <p class="small text-muted mb-2">${escapeHtml(hp.desc)}</p>
                    <div class="d-flex justify-content-between align-items-center mt-auto pt-2">
                        <small class="text-muted">${escapeHtml(hp.author)} · ${hp.date}</small>
                        ${AppState.user && AppState.user.id === hp.author ? `<button class="btn btn-outline-danger btn-sm" onclick="deleteHotplace(${hp.id})"><i class="bi bi-trash"></i> 삭제</button>` : ''}
                    </div>
                </div>
            </div>
        </div>
    `).join('');
}

function deleteHotplace(id) {
    if (!confirm('핫플레이스를 삭제하시겠습니까?')) return;
    AppState.hotplaces = AppState.hotplaces.filter(h => h.id !== id);
    localStorage.setItem('et_hotplaces', JSON.stringify(AppState.hotplaces));
    renderHotplaces();
    showToast('핫플레이스가 삭제되었습니다.');
}

// =====================================================
// 6. 게시판 CRUD (F110)
// =====================================================
function renderBoardList() {
    const tbody = document.getElementById('boardTbody');
    if (!tbody) return;
    if (!AppState.boards.length) {
        tbody.innerHTML = `<tr><td colspan="4" class="text-center text-muted py-4">등록된 게시물이 없습니다.</td></tr>`;
        return;
    }
    tbody.innerHTML = AppState.boards.map((post, idx) => `
        <tr onclick="viewPost(${post.id})" style="cursor:pointer">
            <td>${AppState.boards.length - idx}</td>
            <td class="fw-bold">${escapeHtml(post.title)}</td>
            <td>${escapeHtml(post.author)}</td>
            <td>${post.date}</td>
        </tr>
    `).join('');
}

function _restoreBoardListArea() {
    const area = document.getElementById('boardListArea');
    area.innerHTML = `
        <div class="d-flex justify-content-between mb-4">
            <h2 class="fw-bold">커뮤니티</h2>
            <button class="btn btn-dark" onclick="showBoardWrite()">글쓰기</button>
        </div>
        <table class="table table-hover border rounded-4 overflow-hidden shadow-sm">
            <thead class="table-light"><tr><th>번호</th><th>제목</th><th>작성자</th><th>날짜</th></tr></thead>
            <tbody id="boardTbody"></tbody>
        </table>`;
    renderBoardList();
}

/** 글쓰기 화면 표시 (F110) */
function showBoardWrite() {
    if (!AppState.user) {
        showToast('로그인 후 이용 가능합니다.');
        showAuthModal('login');
        return;
    }
    document.getElementById('boardListArea').innerHTML = `
        <div class="d-flex justify-content-between mb-4">
            <h2 class="fw-bold">글쓰기</h2>
            <button class="btn btn-outline-secondary" onclick="_restoreBoardListArea()">취소</button>
        </div>
        <div class="card border-0 shadow-sm rounded-4 p-4">
            <input type="text" id="boardWriteTitle" class="form-control mb-3" placeholder="제목">
            <textarea id="boardWriteContent" class="form-control mb-3" rows="8" placeholder="내용"></textarea>
            <button class="btn btn-indigo" onclick="submitPost()">등록</button>
        </div>`;
}

function submitPost() {
    const title   = document.getElementById('boardWriteTitle').value.trim();
    const content = document.getElementById('boardWriteContent').value.trim();
    if (!title)   { showToast('제목을 입력하세요.'); return; }
    if (!content) { showToast('내용을 입력하세요.'); return; }

    const post = {
        id: Date.now(),
        title,
        content,
        author: AppState.user.id,
        date: new Date().toLocaleDateString()
    };
    AppState.boards.unshift(post);
    localStorage.setItem('et_boards', JSON.stringify(AppState.boards));
    _restoreBoardListArea();
    showToast('게시글이 등록되었습니다.');
}

/** 게시글 상세 보기 (F110) */
function viewPost(id) {
    const post = AppState.boards.find(p => p.id === id);
    if (!post) return;
    const isAuthor = AppState.user?.id === post.author;
    document.getElementById('boardListArea').innerHTML = `
        <div class="d-flex justify-content-between mb-4">
            <h2 class="fw-bold">게시글</h2>
            <button class="btn btn-outline-secondary" onclick="_restoreBoardListArea()">목록으로</button>
        </div>
        <div class="card border-0 shadow-sm rounded-4 p-4">
            <h4 class="fw-bold mb-2">${escapeHtml(post.title)}</h4>
            <div class="d-flex gap-3 text-muted small mb-4">
                <span>작성자: ${escapeHtml(post.author)}</span>
                <span>날짜: ${post.date}</span>
            </div>
            <hr>
            <p class="mt-3" style="white-space:pre-wrap;">${escapeHtml(post.content)}</p>
            ${isAuthor ? `
            <div class="d-flex gap-2 justify-content-end mt-4">
                <button class="btn btn-outline-secondary btn-sm" onclick="showBoardEdit(${post.id})">수정</button>
                <button class="btn btn-outline-danger btn-sm" onclick="deletePost(${post.id})">삭제</button>
            </div>` : ''}
        </div>`;
}

/** 게시글 수정 화면 (F110) */
function showBoardEdit(id) {
    const post = AppState.boards.find(p => p.id === id);
    if (!post) return;
    document.getElementById('boardListArea').innerHTML = `
        <div class="d-flex justify-content-between mb-4">
            <h2 class="fw-bold">게시글 수정</h2>
            <button class="btn btn-outline-secondary" onclick="viewPost(${id})">취소</button>
        </div>
        <div class="card border-0 shadow-sm rounded-4 p-4">
            <input type="text" id="boardEditTitle" class="form-control mb-3" value="${escapeHtml(post.title)}">
            <textarea id="boardEditContent" class="form-control mb-3" rows="8">${escapeHtml(post.content)}</textarea>
            <button class="btn btn-indigo" onclick="updatePost(${id})">수정 완료</button>
        </div>`;
}

/** 게시글 수정 저장 (F110) */
function updatePost(id) {
    const title   = document.getElementById('boardEditTitle').value.trim();
    const content = document.getElementById('boardEditContent').value.trim();
    if (!title)   { showToast('제목을 입력하세요.'); return; }
    if (!content) { showToast('내용을 입력하세요.'); return; }

    const idx = AppState.boards.findIndex(p => p.id === id);
    if (idx === -1) return;
    AppState.boards[idx] = { ...AppState.boards[idx], title, content };
    localStorage.setItem('et_boards', JSON.stringify(AppState.boards));
    viewPost(id);
    showToast('게시글이 수정되었습니다.');
}

/** 게시글 삭제 (F110) */
function deletePost(id) {
    if (!confirm('게시글을 삭제하시겠습니까?')) return;
    AppState.boards = AppState.boards.filter(p => p.id !== id);
    localStorage.setItem('et_boards', JSON.stringify(AppState.boards));
    _restoreBoardListArea();
    showToast('게시글이 삭제되었습니다.');
}

// =====================================================
// 7-1. 공지사항 CRUD (F109)
// =====================================================
function renderNoticeList() {
    const tbody = document.getElementById('noticeTbody');
    if (!tbody) return;
    if (!AppState.notices.length) {
        tbody.innerHTML = `<tr><td colspan="4" class="text-center text-muted py-4">등록된 공지사항이 없습니다.</td></tr>`;
        return;
    }
    tbody.innerHTML = AppState.notices.map((notice, idx) => `
        <tr onclick="viewNotice(${notice.id})" style="cursor:pointer">
            <td>${AppState.notices.length - idx}</td>
            <td class="fw-bold">${escapeHtml(notice.title)}</td>
            <td>${escapeHtml(notice.author)}</td>
            <td>${notice.date}</td>
        </tr>
    `).join('');
}

function _restoreNoticeListArea() {
    const area = document.getElementById('noticeListArea');
    area.innerHTML = `
        <div class="d-flex justify-content-between mb-4">
            <h2 class="fw-bold">공지사항</h2>
            <button class="btn btn-dark" onclick="showNoticeWrite()">글쓰기</button>
        </div>
        <table class="table table-hover border rounded-4 overflow-hidden shadow-sm">
            <thead class="table-light"><tr><th>번호</th><th>제목</th><th>작성자</th><th>날짜</th></tr></thead>
            <tbody id="noticeTbody"></tbody>
        </table>`;
    renderNoticeList();
}

function showNoticeWrite() {
    if (!AppState.user) { showToast('로그인이 필요합니다.'); showAuthModal('login'); return; }
    document.getElementById('noticeListArea').innerHTML = `
        <div class="d-flex justify-content-between mb-4">
            <h2 class="fw-bold">공지사항 작성</h2>
            <button class="btn btn-outline-secondary" onclick="_restoreNoticeListArea()">목록으로</button>
        </div>
        <div class="card border-0 shadow-sm rounded-4 p-4">
            <div class="mb-3">
                <input type="text" id="noticeTitle" class="form-control rounded-3" placeholder="제목을 입력하세요">
            </div>
            <div class="mb-3">
                <textarea id="noticeContent" class="form-control rounded-3" rows="10" placeholder="내용을 입력하세요"></textarea>
            </div>
            <div class="d-flex justify-content-end gap-2">
                <button class="btn btn-outline-secondary" onclick="_restoreNoticeListArea()">취소</button>
                <button class="btn btn-dark" onclick="submitNotice()">등록</button>
            </div>
        </div>`;
}

function submitNotice() {
    const title = document.getElementById('noticeTitle').value.trim();
    const content = document.getElementById('noticeContent').value.trim();
    if (!title || !content) { showToast('제목과 내용을 모두 입력해 주세요.'); return; }
    const notice = {
        id: Date.now(),
        title,
        content,
        author: AppState.user.id,
        date: new Date().toLocaleDateString('ko-KR')
    };
    AppState.notices.unshift(notice);
    localStorage.setItem('et_notices', JSON.stringify(AppState.notices));
    _restoreNoticeListArea();
    showToast('공지사항이 등록되었습니다.');
}

function viewNotice(id) {
    const notice = AppState.notices.find(n => n.id === id);
    if (!notice) return;
    const isAuthor = AppState.user && AppState.user.id === notice.author;
    document.getElementById('noticeListArea').innerHTML = `
        <div class="d-flex justify-content-between mb-4">
            <h2 class="fw-bold">공지사항</h2>
            <button class="btn btn-outline-secondary" onclick="_restoreNoticeListArea()">목록으로</button>
        </div>
        <div class="card border-0 shadow-sm rounded-4 p-4">
            <h5 class="fw-bold mb-1">${escapeHtml(notice.title)}</h5>
            <div class="text-muted small mb-3">${escapeHtml(notice.author)} · ${notice.date}</div>
            <hr>
            <div class="py-2" style="white-space:pre-wrap">${escapeHtml(notice.content)}</div>
            ${isAuthor ? `
            <hr>
            <div class="d-flex justify-content-end gap-2">
                <button class="btn btn-outline-secondary btn-sm" onclick="showNoticeEdit(${notice.id})">수정</button>
                <button class="btn btn-outline-danger btn-sm" onclick="deleteNotice(${notice.id})">삭제</button>
            </div>` : ''}
        </div>`;
}

function showNoticeEdit(id) {
    const notice = AppState.notices.find(n => n.id === id);
    if (!notice) return;
    document.getElementById('noticeListArea').innerHTML = `
        <div class="d-flex justify-content-between mb-4">
            <h2 class="fw-bold">공지사항 수정</h2>
            <button class="btn btn-outline-secondary" onclick="viewNotice(${notice.id})">취소</button>
        </div>
        <div class="card border-0 shadow-sm rounded-4 p-4">
            <div class="mb-3">
                <input type="text" id="noticeEditTitle" class="form-control rounded-3" value="${escapeHtml(notice.title)}">
            </div>
            <div class="mb-3">
                <textarea id="noticeEditContent" class="form-control rounded-3" rows="10">${escapeHtml(notice.content)}</textarea>
            </div>
            <div class="d-flex justify-content-end gap-2">
                <button class="btn btn-outline-secondary" onclick="viewNotice(${notice.id})">취소</button>
                <button class="btn btn-dark" onclick="updateNotice(${notice.id})">저장</button>
            </div>
        </div>`;
}

function updateNotice(id) {
    const title = document.getElementById('noticeEditTitle').value.trim();
    const content = document.getElementById('noticeEditContent').value.trim();
    if (!title || !content) { showToast('제목과 내용을 모두 입력해 주세요.'); return; }
    const notice = AppState.notices.find(n => n.id === id);
    if (!notice) return;
    notice.title = title;
    notice.content = content;
    localStorage.setItem('et_notices', JSON.stringify(AppState.notices));
    viewNotice(id);
    showToast('공지사항이 수정되었습니다.');
}

function deleteNotice(id) {
    if (!confirm('공지사항을 삭제하시겠습니까?')) return;
    AppState.notices = AppState.notices.filter(n => n.id !== id);
    localStorage.setItem('et_notices', JSON.stringify(AppState.notices));
    _restoreNoticeListArea();
    showToast('공지사항이 삭제되었습니다.');
}

/** 로그인 상태에 따라 네비게이션 우측 영역 갱신 */
function updateAuthUI() {
    const area = document.getElementById('authArea');
    if (AppState.user) {
        area.innerHTML = `
            <button class="btn btn-link text-muted small fw-bold text-decoration-none p-0 me-1"
                    onclick="showMyPage()">${escapeHtml(AppState.user.name || AppState.user.id)}님</button>
            <button class="btn btn-outline-secondary btn-sm" onclick="logout()">로그아웃</button>`;
    } else {
        area.innerHTML = `
            <button class="btn btn-outline-indigo btn-sm" onclick="showAuthModal('login')">로그인</button>
            <button class="btn btn-indigo btn-sm" onclick="showAuthModal('register')">회원가입</button>`;
    }
}

/** 로그인 / 회원가입 / 비밀번호찾기 모달 표시 */
function showAuthModal(mode) {
    const modalEl = document.getElementById('authModal');
    const content = document.getElementById('authModalContent');

    if (mode === 'login') {
        content.innerHTML = `
            <div class="modal-header border-0 pb-0">
                <h5 class="modal-title fw-bold">로그인</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body px-4 pb-4">
                <input type="text" id="loginId" class="form-control mb-3" placeholder="아이디">
                <input type="password" id="loginPw" class="form-control mb-3" placeholder="비밀번호">
                <button class="btn btn-indigo w-100 mb-3" onclick="login()">로그인</button>
                <div class="d-flex justify-content-between">
                    <p class="text-muted small mb-0">계정이 없으신가요?
                        <a href="#" onclick="showAuthModal('register')" class="text-indigo">회원가입</a>
                    </p>
                    <a href="#" onclick="showAuthModal('findpw')" class="text-muted small">비밀번호 찾기</a>
                </div>
            </div>`;
    } else if (mode === 'register') {
        content.innerHTML = `
            <div class="modal-header border-0 pb-0">
                <h5 class="modal-title fw-bold">회원가입</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body px-4 pb-4">
                <input type="text" id="regId"   class="form-control mb-3" placeholder="아이디">
                <input type="text" id="regName" class="form-control mb-3" placeholder="이름">
                <input type="password" id="regPw"  class="form-control mb-3" placeholder="비밀번호">
                <input type="password" id="regPw2" class="form-control mb-3" placeholder="비밀번호 확인">
                <button class="btn btn-indigo w-100 mb-3" onclick="register()">가입하기</button>
                <p class="text-center text-muted small mb-0">이미 계정이 있으신가요?
                    <a href="#" onclick="showAuthModal('login')" class="text-indigo">로그인</a>
                </p>
            </div>`;
    } else if (mode === 'findpw') {
        content.innerHTML = `
            <div class="modal-header border-0 pb-0">
                <h5 class="modal-title fw-bold">비밀번호 찾기</h5>
                <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
            </div>
            <div class="modal-body px-4 pb-4">
                <p class="text-muted small mb-3">비밀번호는 보안 정책상 해시로만 저장되어 조회할 수 없습니다. 계정 확인 후, 로그인 뒤 마이페이지에서 새 비밀번호로 변경해 주세요.</p>
                <input type="text" id="findId"   class="form-control mb-3" placeholder="아이디">
                <input type="text" id="findName" class="form-control mb-3" placeholder="이름">
                <div id="findPwResult" class="alert alert-info d-none mb-3"></div>
                <button class="btn btn-indigo w-100 mb-3" onclick="findPassword()">계정 확인</button>
                <p class="text-center text-muted small mb-0">
                    <a href="#" onclick="showAuthModal('login')" class="text-indigo">로그인으로 돌아가기</a>
                </p>
            </div>`;
    }

    bootstrap.Modal.getOrCreateInstance(modalEl).show();
}

/** 비밀번호 찾기 (F108) */
function findPassword() {
    const id   = document.getElementById('findId').value.trim();
    const name = document.getElementById('findName').value.trim();
    const resultEl = document.getElementById('findPwResult');
    if (!id || !name) { showToast('아이디와 이름을 모두 입력하세요.'); return; }

    const users = _getUsers();
    const user  = users.find(u => u.id === id && u.name === name);
    if (!user) {
        resultEl.className = 'alert alert-danger mb-3';
        resultEl.textContent = '일치하는 회원 정보가 없습니다.';
        resultEl.classList.remove('d-none');
        return;
    }
    resultEl.className = 'alert alert-success mb-3';
    resultEl.textContent = '계정이 확인되었습니다. 비밀번호는 보안 정책상 조회할 수 없습니다. 로그인 후 마이페이지에서 새 비밀번호로 변경해 주세요.';
    resultEl.classList.remove('d-none');
}

/** 마이페이지 모달 (F107 조회/수정/탈퇴) */
function showMyPage() {
    const user = AppState.user;
    if (!user) return;
    const content = document.getElementById('authModalContent');
    content.innerHTML = `
        <div class="modal-header border-0 pb-0">
            <h5 class="modal-title fw-bold">마이페이지</h5>
            <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
        </div>
        <div class="modal-body px-4 pb-4">
            <div class="mb-3 p-3 bg-light rounded-3">
                <div class="text-muted small mb-1">아이디</div>
                <div class="fw-bold">${escapeHtml(user.id)}</div>
            </div>
            <label class="small fw-bold mb-1">이름 수정</label>
            <input type="text" id="editName" class="form-control mb-3" value="${escapeHtml(user.name || '')}">
            <label class="small fw-bold mb-1">현재 비밀번호 확인 <span class="text-danger">*</span></label>
            <input type="password" id="editPwCheck" class="form-control mb-3" placeholder="현재 비밀번호 입력">
            <label class="small fw-bold mb-1">새 비밀번호 <span class="text-muted fw-normal">(변경 시에만 입력)</span></label>
            <input type="password" id="editNewPw" class="form-control mb-4" placeholder="변경하지 않으면 빈칸">
            <button class="btn btn-indigo w-100 mb-2" onclick="updateUserInfo()">정보 수정</button>
            <button class="btn btn-outline-danger w-100" onclick="withdrawUser()">회원 탈퇴</button>
        </div>`;
    bootstrap.Modal.getOrCreateInstance(document.getElementById('authModal')).show();
}

/** 회원 정보 수정 (F107) */
async function updateUserInfo() {
    const name    = document.getElementById('editName').value.trim();
    const pwCheck = document.getElementById('editPwCheck').value;
    const newPw   = document.getElementById('editNewPw').value;
    const bcrypt = _getBcrypt();

    if (!name)            { showToast('이름을 입력하세요.'); return; }
    if (!pwCheck)         { showToast('현재 비밀번호를 입력하세요.'); return; }
    if (newPw && newPw.length < 4)   { showToast('새 비밀번호는 4자 이상 입력하세요.'); return; }
    if (!bcrypt)          { showToast('bcrypt 라이브러리를 불러오지 못했습니다.'); return; }

    const users = _getUsers();
    const idx   = users.findIndex(u => u.id === AppState.user.id);
    if (idx === -1) return;
    const user = users[idx];

    const isCurrentPwValid = _verifyPassword(user, pwCheck, bcrypt);
    if (!isCurrentPwValid) {
        showToast('현재 비밀번호가 올바르지 않습니다.');
        return;
    }

    let updated = {
        ...user,
        name,
        updatedAt: new Date().toISOString()
    };
    if (newPw) {
        updated.pwHash = bcrypt.hashSync(newPw, BCRYPT_ROUNDS);
    } else {
        const promoted = _promoteLegacyPassword(updated, pwCheck, bcrypt);
        updated = promoted.user;
    }
    delete updated.pw;
    users[idx] = updated;
    _saveUsers(users);

    AppState.user = updated;
    localStorage.setItem('et_user', JSON.stringify(AppState.user));
    bootstrap.Modal.getInstance(document.getElementById('authModal')).hide();
    updateAuthUI();
    showToast('회원 정보가 수정되었습니다.');
}

/** 회원 탈퇴 (F107) */
function withdrawUser() {
    if (!confirm('정말 탈퇴하시겠습니까? 복구할 수 없습니다.')) return;
    const users = _getUsers();
    _saveUsers(users.filter(u => u.id !== AppState.user.id));
    AppState.user = null;
    localStorage.removeItem('et_user');
    bootstrap.Modal.getInstance(document.getElementById('authModal')).hide();
    updateAuthUI();
    showToast('회원 탈퇴가 완료되었습니다.');
}

/** 로그인 (F107) */
async function login() {
    const id = document.getElementById('loginId').value.trim();
    const pw = document.getElementById('loginPw').value;
    const bcrypt = _getBcrypt();
    if (!id || !pw) { showToast('아이디와 비밀번호를 입력하세요.'); return; }
    if (!bcrypt)    { showToast('bcrypt 라이브러리를 불러오지 못했습니다.'); return; }

    const users = _getUsers();
    const idx = users.findIndex(u => u.id === id);
    if (idx === -1) { showToast('아이디 또는 비밀번호가 올바르지 않습니다.'); return; }

    const user = users[idx];
    const matched = _verifyPassword(user, pw, bcrypt);

    if (!matched) { showToast('아이디 또는 비밀번호가 올바르지 않습니다.'); return; }

    let sessionUser = user;
    const promoted = _promoteLegacyPassword(user, pw, bcrypt);
    if (promoted.changed) {
        users[idx] = promoted.user;
        _saveUsers(users);
        sessionUser = promoted.user;
    }

    AppState.user = sessionUser;
    localStorage.setItem('et_user', JSON.stringify(sessionUser));
    bootstrap.Modal.getInstance(document.getElementById('authModal')).hide();
    updateAuthUI();
    showToast(`${escapeHtml(sessionUser.name || sessionUser.id)}님, 환영합니다!`);
}

/** 회원가입 (F107) */
async function register() {
    const id   = document.getElementById('regId').value.trim();
    const name = document.getElementById('regName').value.trim();
    const pw   = document.getElementById('regPw').value;
    const pw2  = document.getElementById('regPw2').value;
    const bcrypt = _getBcrypt();

    if (!id || !name || !pw) { showToast('모든 항목을 입력하세요.'); return; }
    if (pw !== pw2)          { showToast('비밀번호가 일치하지 않습니다.'); return; }
    if (pw.length < 4)       { showToast('비밀번호는 4자 이상 입력하세요.'); return; }
    if (!bcrypt)             { showToast('bcrypt 라이브러리를 불러오지 못했습니다.'); return; }

    const users = _getUsers();
    if (users.some(u => u.id === id)) { showToast('이미 사용 중인 아이디입니다.'); return; }

    const now = new Date().toISOString();
    const newUser = {
        id,
        name,
        pwHash: bcrypt.hashSync(pw, BCRYPT_ROUNDS),
        createdAt: now,
        updatedAt: now
    };
    users.push(newUser);
    _saveUsers(users);

    AppState.user = newUser;
    localStorage.setItem('et_user', JSON.stringify(newUser));
    bootstrap.Modal.getInstance(document.getElementById('authModal')).hide();
    updateAuthUI();
    showToast(`${escapeHtml(name)}님, 회원가입을 환영합니다!`);
}

/** 로그아웃 (F108) */
function logout() {
    AppState.user = null;
    localStorage.removeItem('et_user');
    updateAuthUI();
    showToast('로그아웃되었습니다.');
}

// =====================================================
// 초기 로드
// =====================================================
window.onload = () => {
    setAttractionRouteMode(null);
    updateAuthUI();
    renderBoardList();
    renderHotplaces();
    renderNoticeList();
    renderSavedPlans();
    renderLodgingInfo();

    // 콘텐츠 유형 탭 토글
    document.querySelectorAll('#typeGroup .btn').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('#typeGroup .btn').forEach(b => {
                b.classList.remove('btn-indigo', 'active');
                b.classList.add('btn-light');
            });
            btn.classList.remove('btn-light');
            btn.classList.add('btn-indigo', 'active');
        });
    });

    // 새로고침/복원 직후 관광정보 화면은 항상 기본 상태로 시작한다.
    resetAttractionView();

    const planDetailModalEl = document.getElementById('planDetailModal');
    if (planDetailModalEl) {
        planDetailModalEl.addEventListener('hidden.bs.modal', () => {
            AppState.modalPlanId = null;
            AppState.modalPlanItems = [];
            AppState.modalDragSrcIdx = null;
            AppState.modalPlanLodging = null;
        });
    }
};

window.addEventListener('pageshow', () => {
    resetAttractionView();
});

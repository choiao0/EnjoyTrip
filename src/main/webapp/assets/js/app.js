function bootAttractionMap(items) {
    if (!window.kakao || !document.getElementById("map")) {
        return;
    }
    const map = new kakao.maps.Map(document.getElementById("map"), {
        center: new kakao.maps.LatLng(36.5, 127.8),
        level: 13,
    });
    if (!items || !items.length) {
        return;
    }
    const bounds = new kakao.maps.LatLngBounds();
    let hasValidPoint = false;
    items.forEach((item) => {
        if (!item.lat || !item.lng) {
            return;
        }
        const position = new kakao.maps.LatLng(item.lat, item.lng);
        bounds.extend(position);
        hasValidPoint = true;
        const marker = new kakao.maps.Marker({ map, position });
        const info = new kakao.maps.InfoWindow({
            content: `<div style="padding:8px 10px;font-size:12px;"><strong>${item.title}</strong><br>${item.address || ""}</div>`,
        });
        kakao.maps.event.addListener(marker, "click", () => info.open(map, marker));
    });
    if (hasValidPoint) {
        map.relayout();
        map.setBounds(bounds);
    }
}

function bootPlanMap(payload) {
    if (!window.kakao || !document.getElementById("planMap")) {
        return;
    }
    const map = new kakao.maps.Map(document.getElementById("planMap"), {
        center: new kakao.maps.LatLng(37.5665, 126.9780),
        level: 8,
    });
    if (!payload || !payload.stops || !payload.stops.length) {
        return;
    }
    const bounds = new kakao.maps.LatLngBounds();
    payload.stops.forEach((stop, index) => {
        const position = new kakao.maps.LatLng(stop.lat, stop.lng);
        bounds.extend(position);
        const marker = new kakao.maps.Marker({ map, position });
        const label = stop.lodging ? "숙소" : `${index}`;
        const info = new kakao.maps.InfoWindow({
            content: `<div style="padding:8px 10px;font-size:12px;"><strong>${label}. ${stop.title}</strong></div>`,
        });
        info.open(map, marker);
    });
    if (payload.segments && payload.segments.length) {
        payload.segments.forEach((segment, index) => {
            if (!segment || !segment.length) {
                return;
            }
            const path = segment.map((point) => new kakao.maps.LatLng(point.lat, point.lng));
            new kakao.maps.Polyline({
                map,
                path,
                strokeWeight: 5,
                strokeOpacity: 0.9,
                strokeStyle: "solid",
                strokeColor: ["#2563eb", "#f97316", "#16a34a", "#db2777", "#eab308", "#0ea5e9"][index % 6],
            });
        });
    }
    map.setBounds(bounds);
}

function resolveLodgingCoordinates() {
    const addressInput = document.getElementById("lodgingAddress");
    if (!addressInput || !window.kakao || !kakao.maps || !kakao.maps.services) {
        return;
    }
    const address = addressInput.value.trim();
    if (!address) {
        alert("숙소 주소를 입력하세요.");
        return;
    }
    const geocoder = new kakao.maps.services.Geocoder();
    geocoder.addressSearch(address, (result, status) => {
        if (status !== kakao.maps.services.Status.OK || !result.length) {
            alert("숙소 좌표를 찾지 못했습니다.");
            return;
        }
        const first = result[0];
        document.getElementById("lodgingLat").value = first.y;
        document.getElementById("lodgingLng").value = first.x;
        document.getElementById("lodgingPlaceName").value = address;
        const info = document.getElementById("lodgingInfo");
        info.classList.add("is-confirmed");
        info.innerHTML = `<span class="fw-bold">${address}</span><br>좌표 확인 완료: ${Number(first.y).toFixed(5)}, ${Number(first.x).toFixed(5)}`;
    });
}

// 구군 목록 AJAX 로딩 (시도 선택 시 호출)
function loadGuguns(sidoCode) {
    const gugunSelect = document.getElementById("gugunSelect");
    if (!gugunSelect) return;

    gugunSelect.innerHTML = '<option value="">전체</option>';
    if (!sidoCode) return;

    fetch(`/api/guguns?sidoCode=${sidoCode}`)
        .then((res) => res.json())
        .then((guguns) => {
            guguns.forEach((gugun) => {
                const option = document.createElement("option");
                option.value = gugun.gugunCode;
                option.textContent = gugun.gugunName;
                if (typeof SELECTED_GUGUN_CODE !== "undefined"
                        && String(gugun.gugunCode) === String(SELECTED_GUGUN_CODE)) {
                    option.selected = true;
                }
                gugunSelect.appendChild(option);
            });
        })
        .catch(() => {});
}

function bindUploadPreview() {
    const input = document.getElementById("hpFile");
    const zone = document.getElementById("uploadZone");
    const preview = document.getElementById("hpPreview");
    if (!input || !zone || !preview) {
        return;
    }
    const renderPreview = (file) => {
        if (!file) {
            return;
        }
        const reader = new FileReader();
        reader.onload = (event) => {
            preview.classList.remove("d-none");
            preview.innerHTML = `<img src="${event.target.result}" class="img-fluid rounded-4" alt="미리보기" style="max-height:240px; object-fit:cover; width:100%;">`;
        };
        reader.readAsDataURL(file);
    };
    input.addEventListener("change", () => renderPreview(input.files[0]));
    ["dragenter", "dragover"].forEach((name) => {
        zone.addEventListener(name, (event) => {
            event.preventDefault();
            zone.classList.add("dragover");
        });
    });
    ["dragleave", "drop"].forEach((name) => {
        zone.addEventListener(name, (event) => {
            event.preventDefault();
            zone.classList.remove("dragover");
        });
    });
    zone.addEventListener("drop", (event) => {
        if (event.dataTransfer.files.length) {
            input.files = event.dataTransfer.files;
            renderPreview(event.dataTransfer.files[0]);
        }
    });
}

document.addEventListener("DOMContentLoaded", () => {
    bindUploadPreview();
});

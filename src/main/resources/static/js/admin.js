/*
 * MySeoulection Admin — 등록·편집 드로어.
 *
 * 등록 폼은 화면 오른쪽에서 밀려 나오는 드로어로 연다. 목록 사이에 끼워 넣으면(예전 방식) 표가
 * 아래로 밀려나고, 모달에 넣으면 800px가 넘는 폼이 잘린다. 드로어는 화면 높이를 그대로 쓰면서
 * 목록을 왼쪽에 남겨 둔다.
 *
 * 서버가 검증 실패로 화면을 다시 그릴 때는 템플릿이 미리 .is-open을 붙여 보낸다 —
 * 그 경우 여기서는 스크림·포커스만 맞춰 주면 된다.
 */
(() => {
    const FOCUSABLE = 'a[href], button:not([disabled]), input:not([disabled]),'
        + ' select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';

    const scrim = document.querySelector('[data-drawer-scrim]');
    const shell = document.querySelector('.shell');
    let openDrawer = null;
    let lastTrigger = null;

    const togglesFor = (id) => document.querySelectorAll(`[data-panel-toggle="${id}"]`);

    function close({ restoreFocus = true } = {}) {
        if (!openDrawer) return;
        const drawer = openDrawer;
        openDrawer = null;
        drawer.classList.remove('is-open');
        scrim?.classList.remove('is-open');
        document.body.classList.remove('is-drawer-open');
        // 배경을 inert로 만들면 포커스도 클릭도 새지 않는다. 아래 Tab 가두기는 미지원 브라우저용 보완.
        shell?.removeAttribute('inert');
        togglesFor(drawer.id).forEach(button => button.setAttribute('aria-expanded', 'false'));
        if (restoreFocus) lastTrigger?.focus();
    }

    function open(drawer, trigger) {
        if (openDrawer && openDrawer !== drawer) close({ restoreFocus: false });
        openDrawer = drawer;
        lastTrigger = trigger ?? null;
        drawer.classList.add('is-open');
        scrim?.classList.add('is-open');
        document.body.classList.add('is-drawer-open');
        shell?.setAttribute('inert', '');
        togglesFor(drawer.id).forEach(button => button.setAttribute('aria-expanded', 'true'));
        (drawer.querySelector('input:not([type=hidden]):not([disabled]), textarea') || drawer).focus();
    }

    document.querySelectorAll('[data-panel-toggle]').forEach(button => {
        button.addEventListener('click', () => {
            const drawer = document.getElementById(button.dataset.panelToggle);
            if (!drawer) return;
            if (drawer.classList.contains('is-open')) close(); else open(drawer, button);
        });
    });

    scrim?.addEventListener('click', () => close());

    document.addEventListener('keydown', (event) => {
        if (!openDrawer) return;
        if (event.key === 'Escape') {
            close();
            return;
        }
        if (event.key !== 'Tab') return;
        const items = [...openDrawer.querySelectorAll(FOCUSABLE)];
        if (!items.length) return;
        const first = items[0];
        const last = items[items.length - 1];
        if (event.shiftKey && document.activeElement === first) {
            event.preventDefault();
            last.focus();
        } else if (!event.shiftKey && document.activeElement === last) {
            event.preventDefault();
            first.focus();
        }
    });

    // 서버가 열린 채로 내려보낸 드로어(검증 실패)의 나머지 상태를 맞춘다.
    const serverOpened = document.querySelector('.drawer.is-open');
    if (serverOpened) open(serverOpened, null);
})();


/* ─────────────────────────────────────────────────────────────────────────────
   외부 호출(안전나라 조회·LLM 이름 해석) 대기 표시.

   data-busy="문구" 가 붙은 폼이 제출되면 버튼을 잠그고 문구를 바꾼다. 잠그는 게 핵심이다 —
   응답이 늦으면 어드민이 한 번 더 누르고, 서버는 같은 외부 API 를 두 번 부른다.

   ⚠️ 버튼을 disabled 로 만들면 그 버튼의 name/value 가 폼 데이터에서 빠진다. 여기서는
   제출값으로 쓰는 버튼이 없어 문제가 없지만, 값을 실어 보내는 버튼에 붙일 때는
   hidden 으로 옮기고 잠글 것.
   ───────────────────────────────────────────────────────────────────────────── */
(() => {
    const showBar = () => {
        if (document.querySelector('.busy-bar')) return;
        const bar = document.createElement('div');
        bar.className = 'busy-bar';
        document.body.appendChild(bar);
    };

    document.querySelectorAll('form[data-busy]').forEach((form) => {
        form.addEventListener('submit', () => {
            const label = form.dataset.busy || '처리 중…';
            form.querySelectorAll('button[type="submit"], input[type="submit"]').forEach((button) => {
                button.classList.add('is-busy');
                button.innerHTML = '<span class="spinner"></span>' + label;
            });
            form.classList.add('is-busy');
            showBar();
        });
    });
})();

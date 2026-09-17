(() => {
    const panel = document.getElementById('screening-job');
    if (!panel) return;
    const message = document.getElementById('screening-job-message');
    const refresh = document.getElementById('screening-result-refresh');
    let previous = panel.dataset.initialStatus;
    let timer;
    async function poll() {
        try {
            const response = await fetch(panel.dataset.statusUrl, { headers: { Accept: 'application/json' } });
            if (!response.ok) throw new Error('status');
            const job = await response.json();
            const labels = {
                NONE: '아직 등록된 작업이 없습니다. 검수 대상이고 한글 이름이 있으면 자동 등록됩니다.',
                QUEUED: `조회 대기 중 · 앞에 ${job.ahead ?? 0}개 작업이 있습니다.`,
                RUNNING: '안전나라 조회 및 후보 추천 중… 다른 제품으로 이동해도 됩니다.',
                COMPLETED: '조회 완료 · 후보와 근거를 확인한 뒤 직접 저장해 주세요.',
                FAILED: `조회 실패 · ${job.message || '잠시 후 다시 조회해 주세요.'}`,
                CANCELLED: `조회 취소 · ${job.message || '제품 입력 또는 검수 상태가 변경되었습니다.'}`,
            };
            message.textContent = labels[job.status] || '조회 상태 확인 중';
            refresh.hidden = !(job.status === 'COMPLETED' && previous !== 'COMPLETED');
            // Never auto-reload an administrator's unsaved form.
            if (job.status !== 'COMPLETED') previous = job.status;
            if (['NONE', 'QUEUED', 'RUNNING'].includes(job.status)) timer = setTimeout(poll, 5000);
        } catch {
            message.textContent = '조회 상태를 확인하지 못했습니다. 다시 확인 중…';
            timer = setTimeout(poll, 10000);
        }
    }
    poll();
    document.addEventListener('screening-enqueued', () => {
        clearTimeout(timer);
        previous = 'QUEUED';
        poll();
    });
})();

(() => {
    const form = document.querySelector('form[data-screening-enqueue]');
    if (!form) return;
    let submitting = false;
    form.addEventListener('submit', async (event) => {
        event.preventDefault();
        if (submitting) return;
        submitting = true;
        const controller = new AbortController();
        const timeout = setTimeout(() => controller.abort(), 15000);
        let notice = document.getElementById('screening-enqueue-notice');
        if (!notice) {
            notice = document.createElement('div');
            notice.id = 'screening-enqueue-notice';
            notice.setAttribute('role', 'status');
            form.before(notice);
        }
        try {
            const response = await fetch(form.action, {
                method: 'POST', body: new URLSearchParams(new FormData(form)),
                headers: { Accept: 'application/json' }, signal: controller.signal,
            });
            if (!response.ok) throw new Error('request');
            const result = await response.json();
            notice.className = `alert ${result.accepted ? 'alert-success' : 'alert-error'}`;
            notice.textContent = result.message;
            document.dispatchEvent(new Event('screening-enqueued'));
        } catch {
            notice.className = 'alert alert-error';
            notice.textContent = '등록 결과를 확인하지 못했습니다. 조회 상태를 확인한 뒤 다시 시도해 주세요.';
        } finally {
            clearTimeout(timeout);
            submitting = false;
            form.classList.remove('is-busy');
            form.querySelectorAll('.is-busy').forEach(button => {
                button.classList.remove('is-busy');
                if (button.dataset.originalContent) button.innerHTML = button.dataset.originalContent;
            });
            document.querySelectorAll('.busy-bar').forEach(bar => bar.remove());
        }
    });
})();

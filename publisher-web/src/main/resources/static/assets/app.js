(() => {
    const body = document.body;
    document.querySelectorAll('[data-sidebar-open]').forEach(button => button.addEventListener('click', () => {
        body.classList.add('sidebar-open');
    }));
    document.querySelectorAll('[data-sidebar-close]').forEach(button => button.addEventListener('click', () => {
        body.classList.remove('sidebar-open');
    }));

    document.querySelectorAll('[data-sidebar-group]').forEach(group => {
        const toggle = group.querySelector('[data-sidebar-toggle]');
        const groupName = group.dataset.sidebarGroup;
        const storageKey = `content-publisher:sidebar:${groupName}`;
        try {
            const savedState = window.localStorage.getItem(storageKey);
            if (savedState !== null) group.classList.toggle('expanded', savedState === 'expanded');
        } catch (_error) { /* Storage can be disabled by browser policy. */ }
        const syncState = () => toggle?.setAttribute('aria-expanded', String(group.classList.contains('expanded')));
        syncState();
        toggle?.addEventListener('click', () => {
            group.classList.toggle('expanded');
            syncState();
            try {
                window.localStorage.setItem(storageKey,
                    group.classList.contains('expanded') ? 'expanded' : 'collapsed');
            } catch (_error) { /* The menu still works without persisted state. */ }
        });
    });

    document.querySelectorAll('form[data-confirm]').forEach(form => form.addEventListener('submit', event => {
        if (!window.confirm(form.dataset.confirm || '确认执行此操作？')) event.preventDefault();
    }));

    document.querySelectorAll('[data-platform-launch]').forEach(link => link.addEventListener('click', event => {
        event.preventDefault();
        const platformWindow = window.open(link.href, link.dataset.platformWindow || link.target || 'publisher-platform');
        if (platformWindow) {
            try { platformWindow.opener = null; } catch (_error) { /* Cross-origin windows may deny access. */ }
            platformWindow.focus();
        }
    }));

    const copy = async (value, button) => {
        try {
            await navigator.clipboard.writeText(value);
            const label = button.textContent;
            button.textContent = '已复制';
            button.classList.add('copied');
            window.setTimeout(() => {
                button.textContent = label;
                button.classList.remove('copied');
            }, 1600);
        } catch (_error) {
            button.textContent = '复制失败';
        }
    };
    document.querySelectorAll('[data-copy-target]').forEach(button => button.addEventListener('click', () => {
        const target = document.querySelector(button.dataset.copyTarget);
        if (target) copy(target.value || target.textContent || '', button);
    }));
    document.querySelectorAll('[data-copy-combined]').forEach(button => button.addEventListener('click', () => {
        const title = document.querySelector(button.dataset.copyTitle);
        const content = document.querySelector(button.dataset.copyContent);
        if (title && content) copy(`${title.value}\n\n${content.value}`, button);
    }));
    document.querySelectorAll('[data-copy-tags]').forEach(button => button.addEventListener('click', () => {
        const tags = [...document.querySelectorAll(button.dataset.copyTags)]
            .map(tag => tag.textContent.trim()).filter(Boolean);
        if (tags.length) copy(tags.join(' '), button);
    }));

    const channelSelect = document.querySelector('[data-channel-select]');
    const credentialFields = [...document.querySelectorAll('[data-credential-field]')];
    const syncCredentials = () => {
        const labels = channelSelect?.selectedOptions[0]?.dataset.credentialLabels?.split('|').filter(Boolean) || [];
        credentialFields.forEach((field, index) => {
            const visible = index < labels.length;
            field.hidden = !visible;
            const label = field.querySelector('label');
            const input = field.querySelector('input');
            if (label) label.textContent = labels[index] || '';
            if (input) input.required = visible;
        });
    };
    if (channelSelect) {
        channelSelect.addEventListener('change', syncCredentials);
        syncCredentials();
    }

    document.querySelectorAll('[data-count-source]').forEach(counter => {
        const source = document.querySelector(counter.dataset.countSource);
        const limit = Number(counter.dataset.countLimit || 0);
        const refresh = () => {
            const count = [...(source?.value || '')].length;
            counter.textContent = limit ? `${count} / ${limit}` : String(count);
            counter.classList.toggle('over-limit', limit > 0 && count > limit);
        };
        source?.addEventListener('input', refresh);
        refresh();
    });

    const publishedUrlInput = document.querySelector('#manual-external-url');
    const publishedUrlButton = document.querySelector('[data-validate-published-url]');
    const publishedUrlState = document.querySelector('[data-published-url-state]');
    if (publishedUrlInput && publishedUrlButton && publishedUrlState) {
        const initialHint = publishedUrlState.textContent;
        const showState = (message, state) => {
            publishedUrlState.textContent = message;
            publishedUrlState.classList.toggle('valid', state === 'valid');
            publishedUrlState.classList.toggle('invalid', state === 'invalid');
        };
        publishedUrlInput.addEventListener('input', () => {
            publishedUrlInput.setCustomValidity('');
            showState(initialHint, 'idle');
        });
        publishedUrlButton.addEventListener('click', async () => {
            const value = publishedUrlInput.value.trim();
            if (!value) {
                publishedUrlInput.setCustomValidity('请先填写发布后的文章链接');
                publishedUrlInput.reportValidity();
                return;
            }
            publishedUrlButton.disabled = true;
            showState('正在验证链接…', 'idle');
            try {
                const url = new URL(publishedUrlButton.dataset.validationUrl, window.location.origin);
                url.searchParams.set('channelType', publishedUrlButton.dataset.channelType);
                url.searchParams.set('url', value);
                const response = await fetch(url, {
                    credentials: 'same-origin', cache: 'no-store', headers: {'Accept': 'application/json'}
                });
                const result = await response.json().catch(() => ({}));
                if (!response.ok) throw new Error(result.message || '链接校验失败');
                publishedUrlInput.value = result.normalizedUrl || value;
                publishedUrlInput.setCustomValidity('');
                showState(`链接有效 · ${new URL(publishedUrlInput.value).hostname}`, 'valid');
            } catch (error) {
                const message = error.message || '链接校验失败';
                publishedUrlInput.setCustomValidity(message);
                showState(message, 'invalid');
                publishedUrlInput.reportValidity();
            } finally {
                publishedUrlButton.disabled = false;
            }
        });
    }

    const jobLive = document.querySelector('[data-job-live]');
    if (jobLive?.dataset.jobActive === 'true') {
        const jobStatusLabels = {
            PENDING: '等待执行', RUNNING: '执行中', RETRY_WAIT: '等待重试',
            SUCCEEDED: '执行成功', FAILED: '执行失败'
        };
        const progressCard = document.querySelector('.job-progress-card');
        const progressTrack = document.querySelector('.job-progress-track');
        const progressBar = document.querySelector('[data-job-progress-bar]');
        const progressPercent = document.querySelector('[data-job-progress-percent]');
        const progressLabel = document.querySelector('[data-job-progress-label]');
        const progressDetail = document.querySelector('[data-job-progress-detail]');
        const liveNote = document.querySelector('[data-job-live-note]');
        const status = document.querySelector('[data-job-status]');
        const attempt = document.querySelector('[data-job-attempt]');
        let displayedProgress = Number(progressTrack?.getAttribute('aria-valuenow') || 8);
        let syncing = false;
        const renderProgress = value => {
            displayedProgress = Math.max(0, Math.min(100, Number(value) || 0));
            if (progressBar) progressBar.style.width = `${displayedProgress}%`;
            if (progressPercent) progressPercent.textContent = `${Math.round(displayedProgress)}%`;
            progressTrack?.setAttribute('aria-valuenow', String(Math.round(displayedProgress)));
            document.querySelectorAll('[data-stage-threshold]').forEach(stage => {
                stage.classList.toggle('done', displayedProgress >= Number(stage.dataset.stageThreshold));
            });
        };
        const pollJob = async () => {
            if (syncing) return;
            syncing = true;
            try {
                const response = await fetch(jobLive.dataset.jobStatusUrl, {
                    credentials: 'same-origin', cache: 'no-store', headers: {'Accept': 'application/json'}
                });
                if (!response.ok) throw new Error(`任务状态请求失败: ${response.status}`);
                const job = await response.json();
                progressCard?.classList.remove('job-progress-sync-error');
                if (progressLabel) progressLabel.textContent = job.progressLabel;
                if (progressDetail) progressDetail.textContent = job.progressDetail;
                if (attempt) attempt.textContent = `${job.attempt}/${job.maxAttempts}`;
                if (status) {
                    status.className = `status-pill status-${job.status.toLowerCase()}`;
                    status.textContent = jobStatusLabels[job.status] || job.status;
                }
                renderProgress(job.progressPercent);
                if (liveNote) liveNote.textContent = `刚刚同步 · ${new Date().toLocaleTimeString()}`;
                if (['SUCCEEDED', 'FAILED'].includes(job.status)) {
                    progressCard?.classList.toggle('job-progress-failed', job.status === 'FAILED');
                    window.setTimeout(() => window.location.reload(), 500);
                }
            } catch (_error) {
                progressCard?.classList.add('job-progress-sync-error');
                if (liveNote) liveNote.textContent = '状态同步暂时中断，正在自动重试';
            } finally {
                syncing = false;
            }
        };
        window.setInterval(pollJob, 2000);
        pollJob();
    }

    const monitorScreen = document.querySelector('[data-monitor-screen]');
    if (monitorScreen) {
        const clock = monitorScreen.querySelector('[data-monitor-clock]');
        const countdown = monitorScreen.querySelector('[data-monitor-countdown]');
        const refreshState = monitorScreen.querySelector('[data-monitor-refresh-state]');
        const refreshSeconds = Number(monitorScreen.dataset.refreshSeconds || 60);
        let remaining = refreshSeconds;
        let refreshing = false;
        const refreshMonitor = async () => {
            if (refreshing) return;
            refreshing = true;
            monitorScreen.classList.add('monitor-refreshing');
            monitorScreen.classList.remove('monitor-refresh-failed', 'monitor-refresh-ok');
            if (refreshState) refreshState.textContent = '正在同步';
            try {
                const url = new URL(monitorScreen.dataset.monitorLiveUrl, window.location.origin);
                url.searchParams.set('range', monitorScreen.dataset.monitorRange || '24h');
                const response = await fetch(url, {
                    credentials: 'same-origin',
                    cache: 'no-store',
                    headers: {'Accept': 'text/html', 'X-Requested-With': 'XMLHttpRequest'}
                });
                if (!response.ok) throw new Error(`刷新请求失败: ${response.status}`);
                const documentFragment = new DOMParser().parseFromString(await response.text(), 'text/html');
                const nextRegion = documentFragment.querySelector('[data-monitor-live-region]');
                const currentRegion = monitorScreen.querySelector('[data-monitor-live-region]');
                if (!nextRegion || !currentRegion) throw new Error('刷新内容不完整');
                currentRegion.replaceWith(nextRegion);
                remaining = refreshSeconds;
                monitorScreen.classList.add('monitor-refresh-ok');
                if (refreshState) refreshState.textContent = '刚刚更新';
            } catch (_error) {
                remaining = Math.min(15, refreshSeconds);
                monitorScreen.classList.add('monitor-refresh-failed');
                if (refreshState) refreshState.textContent = '更新失败，准备重试';
            } finally {
                monitorScreen.classList.remove('monitor-refreshing');
                refreshing = false;
            }
        };
        const tick = () => {
            if (clock) clock.textContent = new Date().toISOString().replace('T', ' ').replace(/\.\d{3}Z$/, ' UTC');
            remaining -= 1;
            if (remaining <= 0) refreshMonitor();
            if (countdown) countdown.textContent = String(Math.max(remaining, 0));
        };
        window.setInterval(tick, 1000);
        tick();
        monitorScreen.querySelector('[data-monitor-refresh-now]')?.addEventListener('click', refreshMonitor);
        monitorScreen.querySelector('[data-monitor-fullscreen]')?.addEventListener('click', async () => {
            try {
                if (document.fullscreenElement) await document.exitFullscreen();
                else await document.documentElement.requestFullscreen();
            } catch (_error) {
                // Browsers may deny fullscreen when the page is embedded.
            }
        });
    }

    if (body.classList.contains('app-page') && !document.querySelector('[data-support-bot]')) {
        const supportBot = document.createElement('script');
        supportBot.src = '/support-bot/static/widget.js';
        supportBot.dataset.apiBase = '/support-bot';
        supportBot.dataset.supportBot = 'true';
        supportBot.async = true;
        document.body.appendChild(supportBot);
    }
})();

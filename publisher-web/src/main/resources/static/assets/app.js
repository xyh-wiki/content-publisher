(() => {
    const body = document.body;
    const sidebar = document.querySelector('.app-sidebar');
    const sidebarOpeners = [...document.querySelectorAll('[data-sidebar-open]')];
    const sidebarClosers = [...document.querySelectorAll('[data-sidebar-close]')];
    const sidebarMedia = window.matchMedia('(max-width: 820px)');
    let sidebarOpener = null;
    let sidebarBackdrop = null;

    if (sidebar) {
        sidebarBackdrop = document.createElement('button');
        sidebarBackdrop.type = 'button';
        sidebarBackdrop.tabIndex = -1;
        sidebarBackdrop.className = 'sidebar-backdrop';
        sidebarBackdrop.setAttribute('aria-label', '关闭导航');
        sidebar.insertAdjacentElement('afterend', sidebarBackdrop);
    }

    const syncSidebarAccessibility = () => {
        if (!sidebar) return;
        const mobileOpen = sidebarMedia.matches && body.classList.contains('sidebar-open');
        sidebarOpeners.forEach(button => button.setAttribute('aria-expanded', String(mobileOpen)));
        if (sidebarMedia.matches) {
            sidebar.setAttribute('aria-hidden', String(!mobileOpen));
            if ('inert' in sidebar) sidebar.inert = !mobileOpen;
        } else {
            sidebar.removeAttribute('aria-hidden');
            if ('inert' in sidebar) sidebar.inert = false;
        }
    };
    const closeSidebar = (restoreFocus = false) => {
        body.classList.remove('sidebar-open');
        syncSidebarAccessibility();
        if (restoreFocus) sidebarOpener?.focus();
    };
    const openSidebar = button => {
        sidebarOpener = button;
        body.classList.add('sidebar-open');
        syncSidebarAccessibility();
        sidebarClosers[0]?.focus();
    };

    sidebarOpeners.forEach(button => button.addEventListener('click', () => openSidebar(button)));
    sidebarClosers.forEach(button => button.addEventListener('click', () => closeSidebar(true)));
    sidebarBackdrop?.addEventListener('click', () => closeSidebar(true));
    sidebar?.querySelectorAll('a[href]').forEach(link => link.addEventListener('click', () => {
        if (sidebarMedia.matches) closeSidebar(false);
    }));
    document.addEventListener('keydown', event => {
        if (!sidebarMedia.matches || !body.classList.contains('sidebar-open') || !sidebar) return;
        if (event.key === 'Escape') {
            event.preventDefault();
            closeSidebar(true);
            return;
        }
        if (event.key !== 'Tab') return;
        const focusable = [...sidebar.querySelectorAll('a[href], button:not([disabled]), input:not([disabled])')]
            .filter(element => element.offsetParent !== null);
        if (!focusable.length) return;
        const first = focusable[0];
        const last = focusable[focusable.length - 1];
        if (event.shiftKey && document.activeElement === first) {
            event.preventDefault();
            last.focus();
        } else if (!event.shiftKey && document.activeElement === last) {
            event.preventDefault();
            first.focus();
        }
    });
    const handleSidebarViewport = () => {
        if (!sidebarMedia.matches) body.classList.remove('sidebar-open');
        syncSidebarAccessibility();
    };
    if (sidebarMedia.addEventListener) sidebarMedia.addEventListener('change', handleSidebarViewport);
    else sidebarMedia.addListener(handleSidebarViewport);
    syncSidebarAccessibility();

    const sidebarGroups = [...document.querySelectorAll('[data-sidebar-group]')];
    const activeSidebarGroup = sidebarGroups.find(group => group.dataset.sidebarActive === 'true');
    const readSidebarGroupState = group => {
        try { return window.localStorage.getItem(`content-publisher:sidebar:${group.dataset.sidebarGroup}`); }
        catch (_error) { return null; }
    };
    const setSidebarGroupState = (group, expanded, persist = false) => {
        group.classList.toggle('expanded', expanded);
        group.querySelector('[data-sidebar-toggle]')?.setAttribute('aria-expanded', String(expanded));
        group.querySelector('.sidebar-subnav')?.setAttribute('aria-hidden', String(!expanded));
        if (!persist) return;
        try {
            window.localStorage.setItem(`content-publisher:sidebar:${group.dataset.sidebarGroup}`,
                expanded ? 'expanded' : 'collapsed');
        } catch (_error) { /* The menu still works without persisted state. */ }
    };
    const savedSidebarGroup = activeSidebarGroup ? null
        : sidebarGroups.find(group => readSidebarGroupState(group) === 'expanded');
    sidebarGroups.forEach(group => {
        setSidebarGroupState(group, group === activeSidebarGroup || group === savedSidebarGroup);
        group.querySelector('[data-sidebar-toggle]')?.addEventListener('click', () => {
            const shouldExpand = !group.classList.contains('expanded');
            if (shouldExpand) {
                sidebarGroups.filter(candidate => candidate !== group)
                    .forEach(candidate => setSidebarGroupState(candidate, false, true));
            }
            setSidebarGroupState(group, shouldExpand, true);
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
        const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
        const chartNumberFormat = new Intl.NumberFormat('zh-CN');
        let remaining = refreshSeconds;
        let refreshing = false;
        const readChartValue = element => {
            const value = Number(element.dataset.chartValue || 0);
            return Number.isFinite(value) ? Math.max(0, value) : 0;
        };
        const renderChartNumber = element => {
            const target = readChartValue(element);
            const suffix = element.dataset.chartSuffix || '';
            const format = value => `${chartNumberFormat.format(Math.round(value))}${suffix}`;
            if (reducedMotion.matches) {
                element.textContent = format(target);
                return;
            }
            const startedAt = performance.now();
            const duration = 720;
            element.textContent = format(0);
            const step = now => {
                const progress = Math.min(1, (now - startedAt) / duration);
                const eased = 1 - Math.pow(1 - progress, 3);
                element.textContent = format(target * eased);
                if (progress < 1) window.requestAnimationFrame(step);
            };
            window.requestAnimationFrame(step);
        };
        const renderMonitorCharts = region => {
            if (!region) return;
            region.querySelectorAll('[data-monitor-gauge]').forEach(gauge => {
                const value = Math.min(100, readChartValue(gauge));
                const gaugeValue = gauge.querySelector('.monitor-gauge-value');
                if (!gaugeValue) return;
                gaugeValue.style.strokeDasharray = reducedMotion.matches ? `${value} 100` : '0 100';
                if (!reducedMotion.matches) window.requestAnimationFrame(() => {
                    window.requestAnimationFrame(() => { gaugeValue.style.strokeDasharray = `${value} 100`; });
                });
            });
            const columns = [...region.querySelectorAll('[data-monitor-column]')];
            const columnMax = Math.max(1, ...columns.map(readChartValue));
            columns.forEach(column => {
                const value = readChartValue(column);
                const columnFill = column.querySelector('.monitor-column-track i');
                if (!columnFill) return;
                const height = value === 0 ? 0 : Math.max(4, Math.min(100, value / columnMax * 100));
                columnFill.style.height = reducedMotion.matches ? `${height}%` : '0%';
                if (!reducedMotion.matches) window.requestAnimationFrame(() => {
                    window.requestAnimationFrame(() => { columnFill.style.height = `${height}%`; });
                });
            });
            region.querySelectorAll('[data-chart-number]').forEach(renderChartNumber);
        };
        const readMonitorTabSelection = region => {
            const selection = new Map();
            if (!region) return selection;
            region.querySelectorAll('[data-monitor-tabs]').forEach(group => {
                const groupName = group.dataset.monitorTabGroup;
                const selectedTab = group.querySelector('[data-monitor-tab][aria-selected="true"]');
                if (groupName && selectedTab) selection.set(groupName, selectedTab.dataset.monitorTab);
            });
            return selection;
        };
        const initializeMonitorTabs = (region, preferredSelection = new Map()) => {
            if (!region) return;
            region.querySelectorAll('[data-monitor-tabs]').forEach(group => {
                const tabs = [...group.querySelectorAll('[data-monitor-tab]')];
                const panels = [...group.querySelectorAll('[data-monitor-tab-panel]')];
                if (!tabs.length || !panels.length) return;
                const requestedTab = preferredSelection.get(group.dataset.monitorTabGroup);
                const initialTab = tabs.find(tab => tab.dataset.monitorTab === requestedTab)
                    || tabs.find(tab => tab.getAttribute('aria-selected') === 'true')
                    || tabs[0];
                const activateTab = (tab, moveFocus = false) => {
                    tabs.forEach(item => {
                        const selected = item === tab;
                        item.setAttribute('aria-selected', String(selected));
                        item.tabIndex = selected ? 0 : -1;
                    });
                    panels.forEach(panel => {
                        panel.hidden = panel.dataset.monitorTabPanel !== tab.dataset.monitorTab;
                    });
                    if (moveFocus) tab.focus();
                };
                activateTab(initialTab);
                tabs.forEach((tab, index) => {
                    tab.addEventListener('click', () => activateTab(tab));
                    tab.addEventListener('keydown', event => {
                        let nextIndex;
                        if (event.key === 'ArrowRight' || event.key === 'ArrowDown') nextIndex = (index + 1) % tabs.length;
                        else if (event.key === 'ArrowLeft' || event.key === 'ArrowUp') nextIndex = (index - 1 + tabs.length) % tabs.length;
                        else if (event.key === 'Home') nextIndex = 0;
                        else if (event.key === 'End') nextIndex = tabs.length - 1;
                        else return;
                        event.preventDefault();
                        activateTab(tabs[nextIndex], true);
                    });
                });
            });
        };
        const renderMonitorRegion = (region, preferredSelection) => {
            initializeMonitorTabs(region, preferredSelection);
            renderMonitorCharts(region);
        };
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
                const selectedTabs = readMonitorTabSelection(currentRegion);
                currentRegion.replaceWith(nextRegion);
                renderMonitorRegion(nextRegion, selectedTabs);
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
        renderMonitorRegion(monitorScreen.querySelector('[data-monitor-live-region]'));
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

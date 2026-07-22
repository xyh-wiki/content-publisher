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

    const bindConfirmForms = (root = document) => {
        root.querySelectorAll('form[data-confirm]').forEach(form => {
            if (form.dataset.confirmBound === 'true') return;
            form.dataset.confirmBound = 'true';
            form.addEventListener('submit', event => {
                if (!window.confirm(form.dataset.confirm || '确认执行此操作？')) event.preventDefault();
            });
        });
    };
    bindConfirmForms();

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
    const channelFormNote = document.querySelector('[data-channel-form-note]');
    const channelNote = channelFormNote?.querySelector('[data-channel-note]');
    const channelGuide = channelFormNote?.querySelector('[data-channel-guide]');
    const syncCredentials = () => {
        const option = channelSelect?.selectedOptions[0];
        const labels = option?.dataset.credentialLabels?.split('|').filter(Boolean) || [];
        credentialFields.forEach((field, index) => {
            const visible = index < labels.length;
            field.hidden = !visible;
            const label = field.querySelector('label');
            const input = field.querySelector('input');
            if (label) label.textContent = labels[index] || '';
            if (input) input.required = visible;
        });
        const note = option?.dataset.channelNote || '';
        const guideUrl = option?.dataset.guideUrl || '';
        if (channelFormNote) channelFormNote.hidden = !note && !guideUrl;
        if (channelNote) channelNote.textContent = note;
        if (channelGuide) {
            channelGuide.hidden = !guideUrl;
            if (guideUrl) channelGuide.href = guideUrl;
        }
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

    document.querySelectorAll('[data-dirty-form]').forEach(form => {
        let dirty = false;
        form.addEventListener('input', () => { dirty = true; });
        form.addEventListener('change', () => { dirty = true; });
        form.addEventListener('submit', () => { dirty = false; });
        window.addEventListener('beforeunload', event => {
            if (!dirty) return;
            event.preventDefault();
            event.returnValue = '';
        });
    });

    const generationPresets = {
        'project-quickstart': {
            tone: '专业、清晰、面向首次使用者', minCharacters: '800', maxCharacters: '1800', maxKeywords: '10',
            requiredSections: '项目概述\n适用场景\n安装与配置\n快速开始\n常见问题'
        },
        'project-architecture': {
            tone: '专业、客观、突出架构取舍', minCharacters: '1200', maxCharacters: '2600', maxKeywords: '14',
            requiredSections: '项目概述\n架构与模块\n核心流程\n关键技术取舍\n扩展方式\n总结'
        },
        'project-practices': {
            tone: '务实、克制、面向生产环境', minCharacters: '1000', maxCharacters: '2400', maxKeywords: '12',
            requiredSections: '适用场景\n生产配置\n安全与权限\n性能与可观测性\n常见故障\n上线检查清单'
        },
        'topic-tutorial': {
            articleType: 'TUTORIAL', knowledgeLevel: 'MIXED', tone: '专业、清晰、循序渐进',
            minCharacters: '1000', maxCharacters: '2400', maxKeywords: '12',
            requiredSections: '学习目标\n前置知识\n分步教程\n完整示例\n常见问题\n总结'
        },
        'topic-practices': {
            articleType: 'BEST_PRACTICES', knowledgeLevel: 'INTERMEDIATE', tone: '务实、客观、突出取舍',
            minCharacters: '1000', maxCharacters: '2400', maxKeywords: '14',
            requiredSections: '问题背景\n推荐做法\n反例与风险\n实施步骤\n检查清单\n总结'
        },
        'topic-troubleshooting': {
            articleType: 'TROUBLESHOOTING', knowledgeLevel: 'INTERMEDIATE', tone: '直接、准确、便于排查',
            minCharacters: '900', maxCharacters: '2200', maxKeywords: '12',
            requiredSections: '问题现象\n影响范围\n排查步骤\n常见原因\n修复方法\n预防措施'
        },
        'website-overview': {
            recommendationAngle: '说明网站定位、核心功能、适用人群、使用方式、优势与局限',
            tone: '客观、克制、信息密度高', minCharacters: '700', maxCharacters: '1800', maxKeywords: '12',
            requiredSections: '网站定位\n核心功能\n适用人群\n使用方式\n优势与局限\n总结'
        },
        'website-selection': {
            recommendationAngle: '从使用门槛、核心能力、费用边界、数据与安全、适用场景进行选型评估',
            tone: '中立、具体、突出决策依据', minCharacters: '900', maxCharacters: '2200', maxKeywords: '14',
            requiredSections: '产品定位\n核心能力\n使用门槛\n费用与限制\n数据与安全\n适合与不适合的人群\n选型结论'
        },
        'website-guide': {
            recommendationAngle: '围绕首次使用流程，说明准备工作、核心操作、常见问题和使用限制',
            tone: '清晰、直接、面向首次使用者', minCharacters: '800', maxCharacters: '2000', maxKeywords: '10',
            requiredSections: '使用前准备\n注册与配置\n核心操作\n常见问题\n使用限制\n总结'
        }
    };
    document.querySelectorAll('[data-generation-preset]').forEach(select => {
        const form = select.closest('[data-generation-form]');
        const state = form?.querySelector('[data-preset-state]');
        select.addEventListener('change', () => {
            const preset = generationPresets[select.value];
            if (!form || !preset) {
                if (state) state.textContent = '保留当前设置。';
                return;
            }
            Object.entries(preset).forEach(([name, value]) => {
                const field = form.elements.namedItem(name);
                if (!field) return;
                field.value = value;
                field.dispatchEvent(new Event('input', {bubbles: true}));
                field.dispatchEvent(new Event('change', {bubbles: true}));
            });
            if (state) state.textContent = `${select.selectedOptions[0].textContent}预设已应用，可继续调整。`;
        });
    });

    const editorWorkspace = document.querySelector('[data-editor-workspace]');
    if (editorWorkspace) {
        const tabs = [...editorWorkspace.querySelectorAll('[data-editor-tab]')];
        const panels = [...editorWorkspace.querySelectorAll('[data-editor-panel]')];
        let activeLanguage = 'zh';
        const activateEditorTab = (tab, moveFocus = false) => {
            const name = tab.dataset.editorTab;
            if (name === 'zh' || name === 'en') activeLanguage = name;
            tabs.forEach(item => {
                const selected = item === tab;
                item.setAttribute('aria-selected', String(selected));
                item.tabIndex = selected ? 0 : -1;
            });
            panels.forEach(panel => { panel.hidden = panel.dataset.editorPanel !== name; });
            if (moveFocus) tab.focus();
        };
        tabs.forEach((tab, index) => {
            tab.addEventListener('click', () => activateEditorTab(tab));
            tab.addEventListener('keydown', event => {
                if (!['ArrowLeft', 'ArrowRight', 'Home', 'End'].includes(event.key)) return;
                event.preventDefault();
                let next = index;
                if (event.key === 'ArrowLeft') next = (index - 1 + tabs.length) % tabs.length;
                if (event.key === 'ArrowRight') next = (index + 1) % tabs.length;
                if (event.key === 'Home') next = 0;
                if (event.key === 'End') next = tabs.length - 1;
                activateEditorTab(tabs[next], true);
            });
        });
        activateEditorTab(tabs.find(tab => tab.getAttribute('aria-selected') === 'true') || tabs[0]);

        const previewPanel = editorWorkspace.querySelector('[data-editor-panel="preview"]');
        const previewTarget = previewPanel?.querySelector('[data-markdown-preview]');
        const previewState = previewPanel?.querySelector('[data-preview-state]');
        const renderPreview = async language => {
            const source = editorWorkspace.querySelector(`[data-preview-source="${language || activeLanguage}"]`);
            if (!source || !previewPanel || !previewTarget) return;
            const previewTab = tabs.find(tab => tab.dataset.editorTab === 'preview');
            if (previewTab) activateEditorTab(previewTab);
            previewState?.classList.remove('error');
            if (previewState) previewState.textContent = '正在生成预览…';
            const csrf = editorWorkspace.querySelector('input[name="_csrf"]')?.value;
            const headers = { 'Content-Type': 'application/json' };
            if (csrf) headers['X-CSRF-TOKEN'] = csrf;
            try {
                const response = await fetch(previewPanel.dataset.previewUrl, {
                    method: 'POST', headers, credentials: 'same-origin',
                    body: JSON.stringify({ markdown: source.value || '' })
                });
                if (!response.ok) throw new Error(`HTTP ${response.status}`);
                const payload = await response.json();
                previewTarget.innerHTML = payload.html || '<p>暂无内容。</p>';
                if (previewState) previewState.textContent = language === 'en' ? '英文预览已更新' : '中文预览已更新';
            } catch (error) {
                previewTarget.textContent = '预览生成失败。';
                previewState?.classList.add('error');
                if (previewState) previewState.textContent = '请检查登录状态或稍后重试。';
            }
        };
        editorWorkspace.querySelectorAll('[data-render-preview]').forEach(button =>
            button.addEventListener('click', () => renderPreview(button.dataset.renderPreview)));
    }

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
            SUCCEEDED: '执行成功', FAILED: '执行失败', CANCELLED: '已取消'
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
                if (['SUCCEEDED', 'FAILED', 'CANCELLED'].includes(job.status)) {
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

    let publicationBatches = document.querySelector('[data-publication-batches]');
    if (publicationBatches?.dataset.batchesActive === 'true') {
        let refreshingBatches = false;
        let batchRefreshTimer = null;
        const refreshPublicationBatches = async () => {
            if (refreshingBatches || document.hidden || publicationBatches.contains(document.activeElement)) return;
            refreshingBatches = true;
            const state = publicationBatches.querySelector('[data-batch-refresh-state]');
            if (state) state.textContent = '正在同步…';
            try {
                const response = await fetch(window.location.href, {
                    credentials: 'same-origin', cache: 'no-store', headers: {'Accept': 'text/html'}
                });
                if (!response.ok) throw new Error(`批次状态请求失败: ${response.status}`);
                const nextDocument = new DOMParser().parseFromString(await response.text(), 'text/html');
                const nextBatches = nextDocument.querySelector('[data-publication-batches]');
                if (!nextBatches) throw new Error('批次区域不存在');
                publicationBatches.replaceWith(nextBatches);
                publicationBatches = nextBatches;
                bindConfirmForms(publicationBatches);
                const currentSummary = document.querySelector('.publishing-summary');
                const nextSummary = nextDocument.querySelector('.publishing-summary');
                if (currentSummary && nextSummary) currentSummary.innerHTML = nextSummary.innerHTML;
                const currentTabs = document.querySelector('.workspace-tabs');
                const nextTabs = nextDocument.querySelector('.workspace-tabs');
                if (currentTabs && nextTabs) currentTabs.innerHTML = nextTabs.innerHTML;
                const refreshedState = publicationBatches.querySelector('[data-batch-refresh-state]');
                const remainsActive = publicationBatches.dataset.batchesActive === 'true';
                if (refreshedState) refreshedState.textContent = remainsActive
                    ? `刚刚同步 · ${new Date().toLocaleTimeString()}` : '批次已完成';
                if (!remainsActive && batchRefreshTimer) window.clearInterval(batchRefreshTimer);
            } catch (_error) {
                const failedState = publicationBatches.querySelector('[data-batch-refresh-state]');
                if (failedState) failedState.textContent = '同步失败，稍后自动重试';
            } finally {
                refreshingBatches = false;
            }
        };
        batchRefreshTimer = window.setInterval(refreshPublicationBatches, 5000);
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

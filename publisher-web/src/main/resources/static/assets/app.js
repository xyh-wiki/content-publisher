(() => {
    const body = document.body;
    document.querySelectorAll('[data-sidebar-open]').forEach(button => button.addEventListener('click', () => {
        body.classList.add('sidebar-open');
    }));
    document.querySelectorAll('[data-sidebar-close]').forEach(button => button.addEventListener('click', () => {
        body.classList.remove('sidebar-open');
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

    const credentialConfig = {
        DEV: ['API Key'], WORDPRESS: ['用户名', 'Application Password'],
        DISCOURSE: ['API Key', 'API Username'],
        GITHUB_DISCUSSIONS: ['Access Token', 'Repository ID', 'Category ID'],
        X: ['Access Token'], REDDIT: ['Access Token', 'Subreddit'],
        HASHNODE: ['Access Token', 'Publication ID'], MEDIUM: ['Integration Token', 'Author ID'],
        MASTODON: ['Access Token'], GHOST: ['Admin API Key']
    };
    const channelSelect = document.querySelector('[data-channel-select]');
    const credentialFields = [...document.querySelectorAll('[data-credential-field]')];
    const syncCredentials = () => {
        const labels = credentialConfig[channelSelect?.value] || [];
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
})();

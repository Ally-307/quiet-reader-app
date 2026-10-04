const books = [
  {
    id: "tide",
    title: "潮汐手记",
    author: "林屿",
    cover: "./assets/cover-tide.png",
    progress: 68,
    chapter: "第三章  雨后的站台"
  },
  {
    id: "camellia",
    title: "山茶与风",
    author: "周庭",
    cover: "./assets/cover-camellia.png",
    progress: 42,
    chapter: "第七章  向南的窗"
  },
  {
    id: "rain",
    title: "雨停之前",
    author: "许澄",
    cover: "./assets/cover-rain.png",
    progress: 19,
    chapter: "第二章  蓝色雨幕"
  },
  {
    id: "latitude",
    title: "北纬三十五度",
    author: "陈原",
    cover: "./assets/cover-latitude.png",
    progress: 76,
    chapter: "第十一章  旧城方向"
  },
  {
    id: "town",
    title: "小城散步",
    author: "叶行",
    cover: "./assets/cover-town.png",
    progress: 8,
    chapter: "第一章  清晨面包店"
  },
  {
    id: "stars",
    title: "纸上星河",
    author: "沈照",
    cover: "./assets/cover-stars.png",
    progress: 0,
    chapter: "尚未开始"
  }
];

const sources = [
  { id: "local-rules", name: "我的规则集", group: "自定义 · 18 条规则", enabled: true },
  { id: "archive", name: "公开文库规则", group: "自定义 · 6 条规则", enabled: true },
  { id: "testing", name: "调试中的书源", group: "测试分组 · 4 条规则", enabled: false }
];

const state = {
  screen: "shelf",
  previousScreen: "shelf",
  sheet: null,
  sheetClosing: false,
  readerControls: true,
  readerTheme: "paper",
  readerFont: "serif",
  fontSize: 18,
  lineHeight: 1.85,
  readerMargin: 24,
  ttsPlaying: false,
  ttsSpeed: "1.0",
  discoverTab: "sources",
  statsRange: "week",
  sourceTab: "test",
  sourceEnabled: Object.fromEntries(sources.map((source) => [source.id, source.enabled])),
  testStatus: "idle",
  syncOptions: {
    metadata: true,
    notes: true,
    sources: true,
    files: false
  }
};

const appRoot = document.querySelector("#app-root");
const phoneScreen = document.querySelector("#phone-screen");
const toastElement = document.querySelector("#toast");
let toastTimer;
let sheetCloseTimer;

function icon(name, label = "") {
  const accessible = label ? `aria-label="${label}"` : "aria-hidden=\"true\"";
  return `<i data-lucide="${name}" ${accessible}></i>`;
}

function refreshIcons() {
  if (window.lucide) {
    window.lucide.createIcons({ attrs: { "stroke-width": 1.7 } });
  }
}

function showToast(message) {
  window.clearTimeout(toastTimer);
  toastElement.textContent = message;
  toastElement.classList.add("show");
  toastTimer = window.setTimeout(() => toastElement.classList.remove("show"), 1800);
}

function sheetMotionClass() {
  return state.sheetClosing ? " is-closing" : "";
}

function motionEnabled() {
  const reducedMotion = window.matchMedia?.("(prefers-reduced-motion: reduce)")?.matches;
  return !reducedMotion || document.documentElement.classList.contains("motion-preview");
}

function openSheet(sheet) {
  window.clearTimeout(sheetCloseTimer);
  state.sheetClosing = false;
  state.sheet = sheet;
  render();
}

function closeSheet() {
  if (!state.sheet || state.sheetClosing) return;

  state.sheetClosing = true;
  render();

  window.clearTimeout(sheetCloseTimer);
  sheetCloseTimer = window.setTimeout(() => {
    state.sheet = null;
    state.sheetClosing = false;
    render();
  }, motionEnabled() ? 180 : 0);
}

function renderBottomNav(active) {
  const items = [
    ["shelf", "library-big", "书架"],
    ["discover", "compass", "发现"],
    ["stats", "chart-column", "统计"],
    ["profile", "user-round", "我的"]
  ];

  return `
    <nav class="bottom-nav" aria-label="主导航">
      ${items.map(([screen, iconName, label]) => `
        <button
          class="nav-button ${active === screen ? "active" : ""}"
          type="button"
          data-action="navigate"
          data-target="${screen}"
          ${active === screen ? 'aria-current="page"' : ""}
        >
          ${icon(iconName)}
          <span>${label}</span>
        </button>
      `).join("")}
    </nav>
  `;
}

function renderMiniPlayer() {
  if (!state.ttsPlaying) return "";

  return `
    <div class="mini-player" aria-label="正在听书">
      <span class="mini-player-marker" aria-hidden="true"></span>
      <button class="row-copy row-copy-button" type="button" data-action="open-reader">
        <strong>潮汐手记</strong>
        <span>第三章 · 雨后的站台</span>
      </button>
      <button class="icon-button small" type="button" data-action="toggle-tts" aria-label="暂停听书" title="暂停听书">
        ${icon("pause")}
      </button>
      <button class="icon-button small" type="button" data-action="close-tts" aria-label="关闭听书" title="关闭听书">
        ${icon("x")}
      </button>
    </div>
  `;
}

function renderAppFrame({ active, title, eyebrow, actions = "", content }) {
  return `
    <section class="app-view" data-screen-label="${active}">
      <header class="app-header">
        <div>
          ${eyebrow ? `<p class="eyebrow">${eyebrow}</p>` : ""}
          <h1>${title}</h1>
        </div>
        <div class="header-actions">${actions}</div>
      </header>
      <main class="screen-scroll">${content}</main>
      <div>
        ${renderMiniPlayer()}
        ${renderBottomNav(active)}
      </div>
      ${renderSheet()}
    </section>
  `;
}

function renderShelf() {
  const actions = `
    <button class="icon-button" type="button" data-action="shelf-search" aria-label="搜索书架" title="搜索书架">${icon("search")}</button>
    <button class="icon-button" type="button" data-action="open-book-import" aria-label="导入本地书籍" title="导入本地书籍">${icon("plus")}</button>
  `;

  const content = `
    <button class="continue-strip" type="button" data-action="open-reader">
      <img class="continue-cover" src="${books[0].cover}" alt="《${books[0].title}》封面" />
      <span class="continue-copy">
        <span class="continue-kicker">继续阅读 · ${books[0].progress}%</span>
        <span class="continue-title">${books[0].title}</span>
        <span class="continue-chapter">${books[0].chapter}</span>
        <span class="progress-track" aria-label="阅读进度 ${books[0].progress}%"><span style="--progress:${books[0].progress}%"></span></span>
      </span>
      ${icon("chevron-right")}
    </button>

    <div class="section-heading">
      <h2>全部书籍</h2>
      <span class="section-meta">${books.length} 本</span>
    </div>

    <div class="book-grid">
      ${books.map((book) => `
        <button class="book-item" type="button" data-action="open-reader" aria-label="阅读《${book.title}》">
          <span class="book-cover-wrap">
            <img class="book-cover" src="${book.cover}" alt="《${book.title}》封面" />
            ${book.progress > 0 ? `<span class="book-progress">${book.progress}%</span>` : ""}
          </span>
          <span class="book-title">${book.title}</span>
          <span class="book-author">${book.author}</span>
        </button>
      `).join("")}
    </div>
  `;

  return renderAppFrame({ active: "shelf", title: "书架", eyebrow: "09 / 2026", actions, content });
}

function renderDiscover() {
  const actions = `
    <button class="icon-button" type="button" data-action="open-source-import" aria-label="导入书源" title="导入书源">${icon("plus")}</button>
    <button class="icon-button" type="button" data-action="source-menu" aria-label="更多书源操作" title="更多书源操作">${icon("more-horizontal")}</button>
  `;

  const sourceContent = `
    <div class="source-toolbar">
      <span class="subtle-text">已启用 ${Object.values(state.sourceEnabled).filter(Boolean).length} / ${sources.length}</span>
      <button class="text-command" type="button" data-action="open-source-import">${icon("file-json")}导入 JSON</button>
    </div>
    <div class="source-list">
      ${sources.map((source, index) => `
        <div class="source-row">
          <span class="source-index">${String(index + 1).padStart(2, "0")}</span>
          <button class="row-copy row-copy-button" type="button" data-action="open-source-detail">
            <strong>${source.name}</strong>
            <span>${source.group}</span>
          </button>
          <button
            class="switch ${state.sourceEnabled[source.id] ? "on" : ""}"
            type="button"
            role="switch"
            aria-checked="${state.sourceEnabled[source.id]}"
            aria-label="${state.sourceEnabled[source.id] ? "停用" : "启用"}${source.name}"
            data-action="toggle-source"
            data-source-id="${source.id}"
          ></button>
        </div>
      `).join("")}
    </div>
    <div class="local-first-note">
      ${icon("shield-check")}
      <span>书源仅由你导入并保存在本机；应用不提供来源不明的预装书源。</span>
    </div>
  `;

  const searchContent = `
    <div class="search-empty">
      ${icon("book-open")}
      <strong>跨书源搜索</strong>
      <span>输入书名或作者后，将并行搜索全部已启用书源。</span>
    </div>
  `;

  const content = `
    <label class="search-field">
      ${icon("search")}
      <input type="search" placeholder="搜索书名或作者" aria-label="搜索书名或作者" />
      <button class="icon-button small" type="button" data-action="voice-search" aria-label="语音输入" title="语音输入">${icon("mic")}</button>
    </label>
    <div class="segmented-control" role="tablist" aria-label="发现视图">
      <button class="segment-button ${state.discoverTab === "search" ? "active" : ""}" type="button" role="tab" data-action="discover-tab" data-target="search">找书</button>
      <button class="segment-button ${state.discoverTab === "sources" ? "active" : ""}" type="button" role="tab" data-action="discover-tab" data-target="sources">书源</button>
    </div>
    ${state.discoverTab === "sources" ? sourceContent : searchContent}
  `;

  return renderAppFrame({ active: "discover", title: "发现", actions, content });
}

function renderStats() {
  const rangeData = {
    week: { total: "6:42", unit: "小时", bars: [34, 62, 28, 82, 54, 94, 70], labels: ["一", "二", "三", "四", "五", "六", "日"] },
    month: { total: "24:18", unit: "小时", bars: [52, 74, 42, 88], labels: ["第1周", "第2周", "第3周", "第4周"] },
    year: { total: "186", unit: "小时", bars: [28, 42, 67, 48, 72, 85, 63], labels: ["3月", "4月", "5月", "6月", "7月", "8月", "9月"] }
  }[state.statsRange];

  const content = `
    <div class="segmented-control" role="tablist" aria-label="统计周期">
      ${[["week", "本周"], ["month", "本月"], ["year", "今年"]].map(([value, label]) => `
        <button class="segment-button ${state.statsRange === value ? "active" : ""}" type="button" data-action="stats-range" data-target="${value}">${label}</button>
      `).join("")}
    </div>

    <section class="stats-hero">
      <p class="stats-label">阅读时长</p>
      <div class="stats-number"><strong>${rangeData.total}</strong><span>${rangeData.unit}</span></div>
      <div class="bar-chart" aria-label="阅读时长柱状图">
        ${rangeData.bars.map((height, index) => `
          <div class="bar-column ${index === rangeData.bars.length - 1 ? "current" : ""}">
            <span style="--bar-height:${height}%"></span>
            <span>${rangeData.labels[index]}</span>
          </div>
        `).join("")}
      </div>
    </section>

    <div class="stats-summary">
      <div class="summary-cell"><strong>12</strong><span>连续阅读天数</span></div>
      <div class="summary-cell"><strong>4</strong><span>本月读完</span></div>
      <div class="summary-cell"><strong>18.6万</strong><span>估算阅读字数</span></div>
    </div>

    <div class="section-heading">
      <h2>阅读排行</h2>
      <span class="section-meta">按时长</span>
    </div>
    <div class="ranking-list">
      ${books.slice(0, 4).map((book, index) => `
        <div class="ranking-row">
          <span class="rank-number">${String(index + 1).padStart(2, "0")}</span>
          <img class="rank-cover" src="${book.cover}" alt="" />
          <span class="row-copy"><strong>${book.title}</strong><span>${book.progress}% 已读</span></span>
          <span class="rank-time">${["2:14", "1:36", "1:05", "0:47"][index]}</span>
        </div>
      `).join("")}
    </div>
  `;

  return renderAppFrame({ active: "stats", title: "阅读统计", eyebrow: "只属于你的阅读年鉴", content });
}

function settingsRow(iconName, title, subtitle, value, action) {
  return `
    <button class="settings-row" type="button" data-action="${action}">
      ${icon(iconName)}
      <span class="row-copy"><strong>${title}</strong><span>${subtitle}</span></span>
      <span class="settings-value">${value}</span>
      ${icon("chevron-right")}
    </button>
  `;
}

function renderProfile() {
  const content = `
    <div class="profile-intro">
      <span class="profile-mark">${icon("book-open")}</span>
      <span>
        <strong>本地阅读空间</strong>
        <span>无需账号也能完整使用</span>
      </span>
    </div>

    <section class="settings-section">
      <h2>数据</h2>
      <div class="settings-list">
        ${settingsRow("cloud", "多设备同步", "WebDAV · 端到端由你掌控", "未连接", "open-sync")}
        ${settingsRow("database-backup", "备份与恢复", "书架、书源、笔记与设置", "", "not-ready")}
      </div>
    </section>

    <section class="settings-section">
      <h2>阅读</h2>
      <div class="settings-list">
        ${settingsRow("type", "默认排版", "字体、行距、页边距", "宋体 18", "open-reader-type")}
        ${settingsRow("volume-2", "听书设置", "系统 TTS 与媒体控制", "1.0×", "open-reader-tts")}
        ${settingsRow("palette", "主题管理", "日间、夜间与自建主题", "4", "not-ready")}
      </div>
    </section>

    <div class="local-first-note">
      ${icon("lock-keyhole")}
      <span>阅读记录和凭据优先保存在设备中；同步时仅上传你选择的数据。</span>
    </div>
  `;

  return renderAppFrame({ active: "profile", title: "我的", content });
}

function renderDetailHeader(title, backTarget = "discover") {
  return `
    <header class="detail-header">
      <button class="icon-button" type="button" data-action="back" data-target="${backTarget}" aria-label="返回" title="返回">${icon("arrow-left")}</button>
      <h1>${title}</h1>
    </header>
  `;
}

function testState(index) {
  if (state.testStatus === "success") return ["success", icon("check"), ["184 ms", "263 ms", "198 ms", "241 ms"][index]];
  if (state.testStatus === "running") {
    if (index === 0) return ["success", icon("check"), "184 ms"];
    if (index === 1) return ["running", icon("loader-circle"), "测试中"];
  }
  return ["", String(index + 1), "--"];
}

function renderSourceDetail() {
  const tabBar = `
    <div class="source-tabs-wrap">
      <div class="segmented-control" role="tablist" aria-label="书源调试视图">
        ${[["rules", "规则"], ["test", "测试"], ["logs", "日志"]].map(([value, label]) => `
          <button class="segment-button ${state.sourceTab === value ? "active" : ""}" type="button" data-action="source-tab" data-target="${value}">${label}</button>
        `).join("")}
      </div>
    </div>
  `;

  const panels = {
    rules: `
      <div class="source-panel rule-grid">
        <div class="rule-field"><label>搜索地址</label><code>/search?keyword={{key}}</code></div>
        <div class="rule-field"><label>书名规则</label><code>.result-item@h3@text</code></div>
        <div class="rule-field"><label>目录规则</label><code>.chapter-list@a</code></div>
        <div class="rule-field"><label>正文规则</label><code>#content@html</code></div>
      </div>
    `,
    test: `
      <div class="source-panel">
        <div class="test-list">
          ${["搜索规则", "详情规则", "目录规则", "正文规则"].map((label, index) => {
            const [className, marker, latency] = testState(index);
            return `
              <div class="test-row">
                <span class="test-state ${className}">${marker}</span>
                <span class="row-copy"><strong>${label}</strong><span>${["关键词：远山", "书籍：潮声之后", "共解析 24 章", "正文净化与分页"][index]}</span></span>
                <span class="test-latency">${latency}</span>
              </div>
            `;
          }).join("")}
        </div>
        <button class="primary-button full-width" type="button" data-action="run-source-test" ${state.testStatus === "running" ? "disabled" : ""}>
          ${icon(state.testStatus === "running" ? "loader-circle" : "flask-conical")}
          ${state.testStatus === "running" ? "正在测试" : "运行完整测试"}
        </button>
      </div>
    `,
    logs: `
      <div class="source-panel">
        <pre class="log-block">[20:31:04] GET /search?keyword=远山
[20:31:04] 200 OK · 38.2 KB
[20:31:04] selector .result-item → 12 nodes
[20:31:05] detail rule → title, author, cover
[20:31:05] chapter list → 24 items
[20:31:05] content sanitizer → 6 blocks removed
[20:31:05] result: PASS</pre>
      </div>
    `
  };

  return `
    <section class="detail-view" data-screen-label="source-detail">
      ${renderDetailHeader("书源调试")}
      <main class="detail-scroll">
        <div class="source-detail-head">
          <h2>我的规则集</h2>
          <p>Legado JSON · 自定义分组</p>
          <span class="status-inline"><span class="status-dot"></span>已启用</span>
        </div>
        ${tabBar}
        ${panels[state.sourceTab]}
      </main>
    </section>
  `;
}

function renderSync() {
  const optionRows = [
    ["metadata", "书架与阅读进度", "书籍信息、分组和最后阅读位置"],
    ["notes", "书签与笔记", "记录级合并，保留冲突副本"],
    ["sources", "书源与主题", "加密上传配置和自定义排版"],
    ["files", "本地书籍文件", "默认关闭，可能占用较多空间"]
  ];

  return `
    <section class="detail-view" data-screen-label="sync">
      ${renderDetailHeader("多设备同步", "profile")}
      <main class="detail-scroll">
        <div class="sync-intro">
          <div class="sync-intro-mark">${icon("shield-check")}本地优先</div>
          <p>不连接同步服务也能完整阅读。WebDAV 凭据由 Android Keystore 在本机保护。</p>
        </div>
        <div class="sync-form">
          <div class="form-grid">
            <div class="form-field"><label for="webdav-url">WebDAV 地址</label><input id="webdav-url" type="url" placeholder="https://example.com/dav/" autocomplete="url" /></div>
            <div class="form-field"><label for="webdav-user">用户名</label><input id="webdav-user" type="text" placeholder="用户名" autocomplete="username" /></div>
            <div class="form-field"><label for="webdav-password">应用密码</label><input id="webdav-password" type="password" placeholder="应用密码" autocomplete="current-password" /></div>
          </div>

          <div class="sync-options">
            ${optionRows.map(([id, title, subtitle]) => `
              <div class="sync-option">
                <span><strong>${title}</strong><span>${subtitle}</span></span>
                <button
                  class="switch ${state.syncOptions[id] ? "on" : ""}"
                  type="button"
                  role="switch"
                  aria-checked="${state.syncOptions[id]}"
                  aria-label="切换${title}同步"
                  data-action="toggle-sync-option"
                  data-option-id="${id}"
                ></button>
              </div>
            `).join("")}
          </div>

          <button class="primary-button full-width" type="button" data-action="test-sync">${icon("refresh-cw")}测试连接</button>
        </div>
      </main>
    </section>
  `;
}

function readerFontValue() {
  if (state.readerFont === "sans") return "var(--font-ui)";
  if (state.readerFont === "local") return 'KaiTi, "STKaiti", var(--font-reader)';
  return "var(--font-reader)";
}

function renderReader() {
  const controls = state.readerControls ? `
    <header class="reader-overlay-top">
      <button class="icon-button" type="button" data-action="reader-back" aria-label="返回书架" title="返回书架">${icon("arrow-left")}</button>
      <span class="reader-overlay-title">潮汐手记 · 第三章</span>
      <button class="icon-button" type="button" data-action="reader-menu" aria-label="更多阅读选项" title="更多阅读选项">${icon("more-horizontal")}</button>
    </header>
    <footer class="reader-overlay-bottom">
      <div class="reader-progress-control"><span>68%</span><input type="range" min="0" max="100" value="68" aria-label="阅读进度" /><span>24 / 36</span></div>
      <div class="reader-tools">
        <button class="reader-tool" type="button" data-action="reader-catalog">${icon("list")}<span>目录</span></button>
        <button class="reader-tool ${state.ttsPlaying ? "active" : ""}" type="button" data-action="open-tts">${icon("headphones")}<span>听书</span></button>
        <button class="reader-tool" type="button" data-action="reader-theme-quick">${icon("sun")}<span>明暗</span></button>
        <button class="reader-tool" type="button" data-action="open-type">${icon("type")}<span>排版</span></button>
      </div>
    </footer>
  ` : "";

  return `
    <section
      class="reader-view theme-${state.readerTheme}"
      data-screen-label="reader"
      style="--reader-size:${state.fontSize}px;--reader-line-height:${state.lineHeight};--reader-margin:${state.readerMargin}px;--active-reader-font:${readerFontValue()}"
    >
      <main class="reader-scroll" data-action="toggle-reader-controls">
        <article class="reader-content">
          <p class="reader-section-label">03 / 雨后的站台</p>
          <h1>雨后的站台</h1>
          <p>雨停以后，站台边缘积着一条窄窄的水。远处的山被云藏住一半，只剩下颜色很淡的轮廓，像纸上没有擦净的铅笔线。</p>
          <p>我把车票夹进书里，沿着屋檐慢慢往前走。风从铁轨另一端吹来，带着潮湿的青草气味，也吹动长椅上那张无人认领的旧报纸。</p>
          <p>广播响过两次，仍没有人催促。小站仿佛把时间放慢了，让每一滴落下来的水都能在地面停留片刻，再顺着砖缝安静地离开。</p>
          <p>列车出现时没有想象中喧闹。它从雾里驶来，窗子一格一格亮着，像沿河漂来的灯。有人起身，有人挥手，也有人只是把手放进口袋。</p>
          <p>我忽然明白，旅行并不总是为了抵达某个地方。有时候，它只是让人暂时离开熟悉的房间，在陌生的站台上重新听见自己的呼吸。</p>
          <div class="reader-footer-progress"><span>潮汐手记</span><span>68%</span></div>
        </article>
      </main>
      ${controls}
      ${renderSheet()}
    </section>
  `;
}

function renderTypeSheet() {
  const themes = [
    ["paper", "纸白", "#f4f0e7", "#272724"],
    ["white", "净白", "#fbfaf6", "#232321"],
    ["green", "青灰", "#dce2d5", "#263028"],
    ["night", "夜色", "#20201e", "#e7e2d8"]
  ];
  const motionClass = sheetMotionClass();

  return `
    <div class="sheet-scrim${motionClass}" data-action="close-sheet"></div>
    <section class="bottom-sheet${motionClass}" aria-label="排版设置">
      <div class="sheet-handle"></div>
      <header class="sheet-header"><h2>排版</h2><button class="icon-button small" type="button" data-action="close-sheet" aria-label="关闭" title="关闭">${icon("x")}</button></header>
      <div class="sheet-content">
        <div class="sheet-section">
          <div class="slider-row"><label for="font-size-range">字号</label><input id="font-size-range" type="range" min="14" max="28" step="1" value="${state.fontSize}" /><span class="slider-value" id="font-size-value">${state.fontSize}</span></div>
          <div class="slider-row"><label for="line-height-range">行距</label><input id="line-height-range" type="range" min="1.4" max="2.2" step="0.05" value="${state.lineHeight}" /><span class="slider-value" id="line-height-value">${state.lineHeight.toFixed(2)}</span></div>
          <div class="slider-row"><label for="margin-range">页边距</label><input id="margin-range" type="range" min="12" max="48" step="2" value="${state.readerMargin}" /><span class="slider-value" id="margin-value">${state.readerMargin}</span></div>
        </div>
        <div class="sheet-section">
          <div class="theme-swatches">
            ${themes.map(([value, label, background, foreground]) => `
              <button class="theme-swatch ${state.readerTheme === value ? "active" : ""}" type="button" data-action="reader-theme" data-target="${value}">
                <span class="swatch-color" style="background:${background};color:${foreground}"></span><span>${label}</span>
              </button>
            `).join("")}
          </div>
        </div>
        <div class="sheet-section">
          <div class="font-options" role="group" aria-label="正文字体">
            ${[["serif", "系统宋体"], ["sans", "系统黑体"], ["local", "导入字体"]].map(([value, label]) => `
              <button class="font-option ${state.readerFont === value ? "active" : ""}" type="button" data-action="reader-font" data-target="${value}">${label}</button>
            `).join("")}
          </div>
        </div>
      </div>
    </section>
  `;
}

function renderTtsSheet() {
  const motionClass = sheetMotionClass();

  return `
    <div class="sheet-scrim${motionClass}" data-action="close-sheet"></div>
    <section class="bottom-sheet${motionClass}" aria-label="听书控制">
      <div class="sheet-handle"></div>
      <header class="sheet-header"><h2>听书</h2><button class="icon-button small" type="button" data-action="close-sheet" aria-label="关闭" title="关闭">${icon("x")}</button></header>
      <div class="sheet-content">
        <div class="tts-book">
          <img src="${books[0].cover}" alt="" />
          <span><strong>潮汐手记</strong><span>第三章 · 雨后的站台</span></span>
        </div>
        <div class="tts-controls">
          <button class="tts-secondary" type="button" data-action="tts-back" aria-label="后退一段" title="后退一段">${icon("rewind")}</button>
          <button class="tts-play" type="button" data-action="toggle-tts" aria-label="${state.ttsPlaying ? "暂停" : "播放"}" title="${state.ttsPlaying ? "暂停" : "播放"}">${icon(state.ttsPlaying ? "pause" : "play")}</button>
          <button class="tts-secondary" type="button" data-action="tts-forward" aria-label="前进一段" title="前进一段">${icon("fast-forward")}</button>
        </div>
        <div class="sheet-section">
          <div class="speed-options" role="group" aria-label="听书速度">
            ${["0.8", "1.0", "1.4"].map((speed) => `<button class="speed-option ${state.ttsSpeed === speed ? "active" : ""}" type="button" data-action="tts-speed" data-target="${speed}">${speed}×</button>`).join("")}
          </div>
        </div>
        <div class="sheet-section">
          <button class="settings-row" type="button" data-action="tts-timer">
            ${icon("timer")}<span class="row-copy"><strong>定时停止</strong><span>播放完当前章节</span></span><span class="settings-value">章节末</span>${icon("chevron-right")}
          </button>
          <button class="settings-row" type="button" data-action="tts-voice">
            ${icon("volume-2")}<span class="row-copy"><strong>系统语音</strong><span>跟随 Android TTS 引擎</span></span><span class="settings-value">中文</span>${icon("chevron-right")}
          </button>
        </div>
      </div>
    </section>
  `;
}

function renderImportSheet(kind) {
  const isSource = kind === "source-import";
  const motionClass = sheetMotionClass();
  const options = isSource
    ? [["file-json", "选择 JSON 文件", "支持单个或批量 Legado 书源"], ["clipboard", "从剪贴板导入", "粘贴 JSON 文本或分享链接"], ["scan-line", "扫描二维码", "读取书源分享二维码"]]
    : [["book-open", "选择 EPUB", "保留目录、封面和排版结构"], ["file-text", "选择 TXT", "自动识别编码与章节"], ["file", "选择 PDF", "固定版式阅读"]];

  return `
    <div class="sheet-scrim${motionClass}" data-action="close-sheet"></div>
    <section class="bottom-sheet${motionClass}" aria-label="${isSource ? "导入书源" : "导入本地书籍"}">
      <div class="sheet-handle"></div>
      <header class="sheet-header"><h2>${isSource ? "导入书源" : "导入本地书籍"}</h2><button class="icon-button small" type="button" data-action="close-sheet" aria-label="关闭" title="关闭">${icon("x")}</button></header>
      <div class="sheet-content">
        <div class="import-options">
          ${options.map(([iconName, title, subtitle]) => `
            <button class="import-option" type="button" data-action="prototype-import" data-kind="${isSource ? "书源" : "书籍"}">
              ${icon(iconName)}<span class="row-copy"><strong>${title}</strong><span>${subtitle}</span></span>${icon("chevron-right")}
            </button>
          `).join("")}
        </div>
      </div>
    </section>
  `;
}

function renderSheet() {
  if (state.sheet === "type") return renderTypeSheet();
  if (state.sheet === "tts") return renderTtsSheet();
  if (state.sheet === "source-import" || state.sheet === "book-import") return renderImportSheet(state.sheet);
  return "";
}

function render({ screenMotion = "", controlsMotion = false } = {}) {
  const screens = {
    shelf: renderShelf,
    discover: renderDiscover,
    stats: renderStats,
    profile: renderProfile,
    "source-detail": renderSourceDetail,
    sync: renderSync,
    reader: renderReader
  };

  appRoot.innerHTML = screens[state.screen]();
  const screenElement = appRoot.firstElementChild;
  if (screenMotion && screenElement) {
    screenElement.classList.add("screen-enter", `screen-enter-${screenMotion}`);
  }
  if (controlsMotion) {
    appRoot.querySelectorAll(".reader-overlay-top, .reader-overlay-bottom").forEach((element) => {
      element.classList.add("reader-controls-enter");
    });
  }
  refreshIcons();
}

function navigate(screen, { sheet = null } = {}) {
  window.clearTimeout(sheetCloseTimer);
  state.previousScreen = state.screen;
  state.screen = screen;
  state.sheet = sheet;
  state.sheetClosing = false;
  state.readerControls = true;
  render({ screenMotion: screen === "reader" ? "reader" : "page" });
}

appRoot.addEventListener("click", (event) => {
  const target = event.target.closest("[data-action]");
  if (!target) return;

  const action = target.dataset.action;
  const value = target.dataset.target;

  if (action === "navigate") navigate(value);
  if (action === "open-reader") navigate("reader");
  if (action === "reader-back") navigate("shelf");
  if (action === "back") navigate(value || state.previousScreen || "shelf");

  if (action === "toggle-reader-controls" && !state.sheet) {
    state.readerControls = !state.readerControls;
    render({ controlsMotion: state.readerControls });
  }

  if (action === "open-type") {
    state.readerControls = true;
    openSheet("type");
  }

  if (action === "open-tts") {
    state.readerControls = true;
    openSheet("tts");
  }

  if (action === "close-sheet") {
    closeSheet();
  }

  if (action === "open-book-import") {
    openSheet("book-import");
  }

  if (action === "open-source-import") {
    openSheet("source-import");
  }

  if (action === "prototype-import") {
    closeSheet();
    showToast(`${target.dataset.kind}选择器将在 Android 版本中调用系统文件面板`);
  }

  if (action === "discover-tab") {
    state.discoverTab = value;
    render();
  }

  if (action === "stats-range") {
    state.statsRange = value;
    render();
  }

  if (action === "toggle-source") {
    const id = target.dataset.sourceId;
    state.sourceEnabled[id] = !state.sourceEnabled[id];
    render();
  }

  if (action === "open-source-detail") navigate("source-detail");

  if (action === "source-tab") {
    state.sourceTab = value;
    render();
  }

  if (action === "run-source-test") {
    state.testStatus = "running";
    render();
    window.setTimeout(() => {
      state.testStatus = "success";
      render();
      showToast("四项规则测试通过");
    }, 1000);
  }

  if (action === "open-sync") navigate("sync");

  if (action === "toggle-sync-option") {
    const id = target.dataset.optionId;
    state.syncOptions[id] = !state.syncOptions[id];
    render();
  }

  if (action === "test-sync") {
    const url = document.querySelector("#webdav-url")?.value.trim();
    const user = document.querySelector("#webdav-user")?.value.trim();
    if (!url || !user) showToast("请先填写 WebDAV 地址和用户名");
    else showToast("格式检查通过，正式版将发起连接测试");
  }

  if (action === "reader-theme") {
    state.readerTheme = value;
    render();
  }

  if (action === "reader-theme-quick") {
    state.readerTheme = state.readerTheme === "night" ? "paper" : "night";
    render();
  }

  if (action === "reader-font") {
    state.readerFont = value;
    render();
    if (value === "local") showToast("Android 版将打开字体文件选择器");
  }

  if (action === "toggle-tts") {
    state.ttsPlaying = !state.ttsPlaying;
    render();
    showToast(state.ttsPlaying ? "开始听书" : "已暂停");
  }

  if (action === "close-tts") {
    state.ttsPlaying = false;
    render();
  }

  if (action === "tts-speed") {
    state.ttsSpeed = value;
    render();
  }

  if (["shelf-search", "source-menu", "voice-search", "reader-menu", "reader-catalog", "tts-back", "tts-forward", "tts-timer", "tts-voice", "not-ready"].includes(action)) {
    showToast("该入口将在下一阶段接入完整流程");
  }

  if (action === "open-reader-type") {
    navigate("reader", { sheet: "type" });
  }

  if (action === "open-reader-tts") {
    navigate("reader", { sheet: "tts" });
  }
});

appRoot.addEventListener("input", (event) => {
  const readerView = document.querySelector(".reader-view");

  if (event.target.id === "font-size-range") {
    state.fontSize = Number(event.target.value);
    readerView?.style.setProperty("--reader-size", `${state.fontSize}px`);
    document.querySelector("#font-size-value").textContent = String(state.fontSize);
  }

  if (event.target.id === "line-height-range") {
    state.lineHeight = Number(event.target.value);
    readerView?.style.setProperty("--reader-line-height", String(state.lineHeight));
    document.querySelector("#line-height-value").textContent = state.lineHeight.toFixed(2);
  }

  if (event.target.id === "margin-range") {
    state.readerMargin = Number(event.target.value);
    readerView?.style.setProperty("--reader-margin", `${state.readerMargin}px`);
    document.querySelector("#margin-value").textContent = String(state.readerMargin);
  }
});

const tweaksTrigger = document.querySelector("#tweaks-trigger");
const tweaksPanel = document.querySelector("#tweaks-panel");
const tweaksClose = document.querySelector("#tweaks-close");
const densitySelect = document.querySelector("#density-select");
const textureToggle = document.querySelector("#texture-toggle");
const motionToggle = document.querySelector("#motion-toggle");
const motionPreviewFromUrl = new URLSearchParams(window.location.search).get("motion") === "on";

motionToggle.checked = motionPreviewFromUrl;

function applyPrototypeTweaks() {
  phoneScreen.classList.remove("density-airy", "density-balanced", "density-compact");
  phoneScreen.classList.add(`density-${densitySelect.value}`);
  phoneScreen.classList.toggle("has-texture", textureToggle.checked);
  document.documentElement.classList.toggle("motion-preview", motionToggle.checked);
}

tweaksTrigger.addEventListener("click", () => {
  tweaksPanel.hidden = false;
  refreshIcons();
});

tweaksClose.addEventListener("click", () => {
  tweaksPanel.hidden = true;
});

densitySelect.addEventListener("change", applyPrototypeTweaks);
textureToggle.addEventListener("change", applyPrototypeTweaks);
motionToggle.addEventListener("change", () => {
  applyPrototypeTweaks();
  const url = new URL(window.location.href);
  if (motionToggle.checked) url.searchParams.set("motion", "on");
  else url.searchParams.delete("motion");
  window.history.replaceState({}, "", url);
});

applyPrototypeTweaks();
render();

# 「归藏」完整设计规格说明书

---

## 一、色彩体系

### 深色模式（默认 `:root`）

| CSS变量 | 色值 | 用途 |
|---------|------|------|
| `--bg-canvas` | `#1E2022` | 画布大背景 |
| `--bg-surface` | `#272A2E` | 卡片、弹窗背景 |
| `--bg-surface-hover` | `#2F3236` | 悬浮态 |
| `--primary-dark` | `#E8E8E8` | 手绘轮廓线、纯白文字（深色下） |
| `--muted` | `#9E9E9E` | 次级文字（暖灰） |
| `--text-main` | `#E8E8E8` | 主文字色 |
| `--text-light` | `#FFFFFF` | 纯净白（卡片标题、大字数值） |
| `--sidebar-bg` | `#181A1C` | 侧边栏底 |
| `--body-bg` | `#1E2022` | body背景 |
| `--input-bg` | `#2C2F33` | 输入框底 |
| `--border-weak` | `rgba(255,255,255,0.06)` | 弱边框 |
| `--border-mid` | `rgba(255,255,255,0.12)` | 中边框 |
| `--card-bg` | `#272A2E` | 卡片背景（同 `--bg-surface`） |
| `--tag-bg` | `rgba(255,255,255,0.08)` | 标签背景 |
| `--drawer-bg` | `#222528` | 抽屉面板底 |
| `--progress-bg` | `rgba(255,255,255,0.05)` | 进度条底色 |

### 浅色模式（`body.light`）

| CSS变量 | 色值 | 用途 |
|---------|------|------|
| `--bg-canvas` | `#F6F5F0` | 燕麦奶白画布 |
| `--bg-surface` | `#FFFFFF` | 纯白卡片 |
| `--primary-dark` | `#222222` | 深炭黑轮廓线 |
| `--muted` | `#8A8278` | 暖灰铅笔 |
| `--text-main` | `#222222` | 主文字 |
| `--text-light` | `#111111` | 核心高亮文字 |
| `--sidebar-bg` | `#EEECE5` | |
| `--body-bg` | `#F6F5F0` | |
| `--input-bg` | `#F9F7F2` | |
| `--border-weak` | `rgba(34,34,34,0.08)` | |
| `--border-mid` | `rgba(34,34,34,0.15)` | |
| `--drawer-bg` | `#FAF9F5` | |
| `--progress-bg` | `rgba(34,34,34,0.06)` | |
| `--card-bg` | `#FFFFFF` | |
| `--tag-bg` | `rgba(229,186,143,0.2)` | |

### 跨模式共用色（深浅模式都不变）

| CSS变量 | 色值 | 用途 |
|---------|------|------|
| `--primary` | `#CBAF88` | 暖金奶茶色（主强调、¥符号、活动标签） |
| `--success` | `#8FAF96` | 抹茶绿 |
| `--warning` | `#D9A87C` | 焦糖琥珀 |
| `--danger` | `#D98880` | 草莓粉 |
| `--accent-green` | `#55876F` | 铜绿（细水长流卡片主题色、永久有效卡片） |
| `--accent-blue` | `#6B8A9E` | 盐系蓝灰（周期续费卡片主题色） |
| `--accent-amber` | `#B38650` | 焦糖琥珀（储备幸福卡片、储值卡主题色） |
| `--accent-rose` | `#A65252` | 枯玫瑰（收藏状态卡片、按量计费卡片主题色） |
| `--crayon-green` | `#9BB8A7` | 蜡笔抹茶绿（细水长流卡片的 `::before` 伪元素底色） |
| `--crayon-yellow` | `#EAD2AC` | 蜡笔麦穗黄（储备幸福卡片的 `::before` 伪元素底色） |
| `--crayon-pink` | `#E6B1B1` | 蜡笔肉粉（收藏状态卡片的 `::before` 伪元素底色） |
| `--crayon-blue` | `#A3BCD6` | 蜡笔天空蓝（预留） |

---

## 二、全局尺寸与布局

### 根容器 `window-container`
- 宽高：撑满整个 JCEF 浏览器视口（`width: 100%; height: 100%`）
- 布局方式：`display: flex; flex-direction: row`，水平排列侧边栏和主画布
- 定位：`position: relative`（作为内部绝对定位元素的锚点）
- 边框：无
- 溢出：`overflow: hidden`（不可滚动，不可见滚动条）

### 侧边栏 `sidebar`
- 宽度：`80px`，固定不缩放（`flex-shrink: 0`）
- 高度：与 window-container 等高
- 底色：`var(--sidebar-bg)`
- 右边框：`1.5px solid var(--primary-dark)`（手绘炭笔框，深浅模式统一）
- 内部布局：`display: flex; flex-direction: column; align-items: center`
- 上内边距：`30px`，下内边距：`0`
- 元素间距：`gap: 35px`
- 层级：`z-index: 20`

### 侧边栏内的导航项 `nav-item`
- 尺寸：`48px × 48px`
- 圆角：`8px`
- 边框：`1.5px solid transparent`（默认透明，悬浮/选中时变色）
- 文字大小：`18px`，颜色 `var(--muted)`
- 悬浮态：文字变 `var(--text-light)`，底变 `var(--bg-surface)`，边框变 `var(--primary-dark)`
- 选中态：文字变 `var(--primary-dark)`，底变 `var(--primary)`，边框变 `var(--primary-dark)`

### 叶子图标 `brand-logo`
- 尺寸：`font-size: 24px`，下方间距 `margin-bottom: 20px`
- 颜色：`var(--primary-dark)`
- 悬浮：`transform: scale(1.1)`
- 全局去荧光规则覆盖：`text-shadow: none !important`

### 主画布 `main-canvas`
- 宽度：`flex-grow: 1`（占满侧边栏剩余空间）
- 【修改】:高度自适应，最低是当前窗口的高度
- 内边距：`padding: 40px`（上下左右各40px）
- 溢出：`overflow-y: auto`（可垂直滚动），滚动条隐藏
- 层级：`z-index` 默认（低于 sidebar 和 drawer）

### 用户抽屉 `user-drawer`
- 宽度：`260px`
- 高度：`100%`（与 window-container 等高）
- 位置：`position: absolute; top: 0`
- 关闭态：`left: -300px`（藏在左侧屏幕外）
- 打开态：`left: 80px`（贴紧侧边栏右侧）
- 过渡：`left 0.3s cubic-bezier(0.25, 0.8, 0.25, 1)`
- 底色：`var(--drawer-bg)`
- 右边框：`1px solid var(--border-mid)`
- 层级：`z-index: 15`
- 内边距：`padding: 40px 24px`
- 内部布局：`display: flex; flex-direction: column; gap: 24px`

### 用户抽屉内头像 `user-avatar`
- 尺寸：`64px × 64px`，圆形（`border-radius: 50%`）
- 背景：`linear-gradient(135deg, var(--primary), var(--success))`（奶茶→抹茶绿渐变）
- 文字大小：`24px`，颜色 `var(--text-light)`

### 用户抽屉内菜单按钮 `user-menu-btn`
- 宽度：撑满抽屉（`width: 100%`）
- 内边距：`12px`
- 圆角：`8px`
- 底：`var(--input-bg)`，边框：`1px solid rgba(212,199,176,0.05)`
- 文字：`13px`，颜色 `var(--text-main)`
- 悬浮：底变 `var(--bg-surface-hover)`，文字变 `var(--text-light)`
- 菜单项（从上到下）：修改密码 → 导出备份数据（当前仅弹Toast模拟） → 切换账户（设置 `phase='auth'`，无实际logout API） → 退出登录（同切换账户） → 关于「归藏」

### 页面标题栏 `view-header`
- 位置：`position: sticky; top: -40px; z-index: 10`
- 布局：`display: flex; justify-content: space-between; align-items: center`
- 外间距：`margin: -40px -40px 30px -40px`（负margin抵消父padding，左右撑满画布宽）
- 内边距：`padding: 28px 40px 14px 40px`（上28px收紧，右40px延伸，下14px）
- 底色：`var(--body-bg)`（覆盖滚动上来的卡片）
- 不收缩：`flex-shrink: 0`
- 【修改】宽度自适应，高度不变

### 标题栏内文字
- 标题 h2：`font-size: 24px; font-weight: 600; color: var(--text-light); margin: 0 0 4px 0`
- 副标题 p：`font-size: 13px; color: var(--muted); margin: 0`

### 标题栏右侧按钮右缩进
- `.view-header .btn-primary` 和 `.view-header .quick-actions` 额外 `margin-right: 8px`

### 主按钮 `btn-primary`
- 内边距：`padding: 10px 18px`
- 圆角：`8px`（手绘覆写为 `8px !important`）
- 底：`var(--bg-surface)`
- 边框：`1.5px solid var(--primary-dark)`（手绘炭笔框）
- 文字：`font-size: 13px; font-weight: 700; color: var(--primary-dark)`
- 阴影：`box-shadow: 2px 2px 0px rgba(34,34,34,0.1)`（硬纸叠阴影）
- 悬浮：`background: color-mix(in srgb, var(--primary) 30%, transparent)`（30%奶茶淡染），`transform: translate(2px, 2px)`（按下位移），阴影收缩

### 卡片小按钮 `btn-action-sm`
- 内边距：`padding: 6px 12px`
- 圆角：`8px`
- 底：`var(--card-bg)`，边框：`1.5px solid var(--primary-dark)`
- 文字：`font-size: 12px; color: var(--primary-dark)`，`font-weight: 700`
- 阴影同主按钮
- 悬浮：30%奶茶淡染 + 按下位移

### 次级小按钮 `btn-secondary-sm`
- 同 `btn-action-sm` 尺寸，但颜色 `var(--muted)`，边框色更淡
- 悬浮：文字变亮，边框变 `var(--muted)`

### 快捷操作按钮 `quick-action-btn`
- 尺寸：`34px × 28px`
- 圆角：`8px`
- 边框：`1px solid rgba(212,199,176,0.08)`，底：`var(--bg-surface)`
- 文字：`13px`，颜色 `var(--muted)`
- 悬浮：边框变 `var(--primary)`，文字变 `var(--text-light)`

---

## 三、欢迎页

- 定位：`position: fixed; top: 0; left: 0`
- 尺寸：`width: 100vw; height: 100vh`（铺满整个视口）
- 底色：`var(--bg-canvas)`
- 层级：`z-index: 9999`
- 布局：`display: flex; justify-content: center; align-items: center`
- 显示/隐藏：由 Vue `v-if="phase==='welcome'"` 控制渲染/销毁，点击可跳过（3.2秒后自动跳转）
- CSS `.fade-out` 类定义存在但未在模板中使用（Vue 直接切换 phase 控制元素消失）

### 欢迎页标题
- 字体：`"Noto Serif SC", "思源宋体", serif`
- 尺寸：`48px; font-weight: 700`
- 颜色：`var(--text-main)`
- 动画：`heyteaFadeIn 2s ease forwards`
  - 起始：`opacity: 0; letter-spacing: 18px; transform: scale(1.03)`
  - 结束：`opacity: 1; letter-spacing: 4px; transform: scale(1)`

### 欢迎页副标题
- 字号：`14px; color: var(--muted); letter-spacing: 6px; margin-top: 20px`
- 初始 `opacity: 0`，动画 `fadeInSimple 1s ease 1.2s forwards`（延迟1.2秒后淡入）

---

## 四、登录注册面板

- 定位：`position: absolute; top: 0; left: 0; width: 100%; height: 100%`
- 底色：`var(--bg-canvas)`，层级：`z-index: 500`
- 布局：`display: flex; justify-content: center; align-items: center`
- 隐藏：由 Vue `v-if="phase==='auth'"` 控制渲染/销毁（CSS `.hidden` 类定义存在但未使用）

### 面板卡片 `auth-card`
- 宽度：`380px`
- 底：`var(--bg-surface)`
- 边框：`1px solid var(--border-weak)`，圆角 `10px`（手绘覆写为不对称圆角 `255px 15px 225px 15px / 15px 225px 15px 255px`）
- 内边距：`padding: 40px`

### 标签切换 `auth-tabs` / `auth-tab`
- tabs容器：`display: flex; gap: 20px; margin-bottom: 24px; border-bottom: 1px solid rgba(212,199,176,0.05); padding-bottom: 10px`
- 单个tab：`font-size: 16px; color: var(--muted); cursor: pointer; font-weight: 600`
- 激活态：`color: var(--text-light)`，底部 `2px solid var(--primary)` 下划线（通过 `::after` 伪元素实现，`bottom: -11px`）
- 悬浮：颜色变 `var(--text-light)`

### 表单输入框 `form-control`
- 底：`var(--bg-surface)`，边框：`1.5px solid var(--primary-dark)`，圆角：`8px`
- 内边距：`padding: 12px 16px`
- 文字：`font-size: 14px; color: var(--text-light)`
- 聚焦：边框变 `var(--primary)`

---

## 五、视图区域 `view-section`

- 默认：`display: none`
- 激活：`.active` → `display: block`，伴随 `fadeIn 0.3s ease-in-out` 动画（`from {opacity:0; transform:translateY(4px)} to {opacity:1; transform:translateY(0)}`）

---

## 六、仪表盘（Dashboard）

### 三栏数据卡 `dashboard-row`
- 布局：`display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 20px; margin-bottom: 30px`

### 数据卡片 `dash-card`
- 底：`var(--bg-surface)`，圆角：`16px`（手绘覆写为不对称圆角）
- 内边距：`padding: 24px`
- 边框：`1px solid rgba(212,199,176,0.05)`（手绘覆写为 `1.5px solid var(--primary-dark)`）
- 阴影：`3px 3px 0px rgba(34,34,34,0.1)`（硬纸叠阴影）
- 悬浮：`transform: translate(2px, 2px)`（按下位移），阴影收缩

### 卡片内数值 `.value`
- 字号：`32px; font-weight: 700; color: var(--text-light); margin: 8px 0 4px 0`

### 会员总览卡片
- 同 `dash-card` 样式，外加 `margin-bottom: 24px`
- 内部横向 flex 排列 5 个统计数字

### 今日物语容器 `timeline-container`
- 同 `dash-card` 样式，`padding: 24px`
- 标题 h3：`margin: 0 0 16px 0; font-size: 16px; color: var(--text-light)`

### 物语条目 `timeline-item`
- 布局：`display: flex; gap: 20px; margin-bottom: 18px`（HTML 中存在，由 `v-for` 遍历 `insights` 数组动态渲染）

### 物语圆点 `timeline-icon`
- 尺寸：`10px × 10px`，圆形
- 默认底：`var(--primary)`（奶茶色）
- `.warning`：底 `var(--danger)`
- `.success`：底 `var(--success)`

### 物语文字 `timeline-content`
- 字号：`14px; color: var(--text-light); line-height: 1.5`

### 仪表盘快速按钮 → 表单抽屉映射

仪表盘和物尽其用页面的标题栏右侧都有快速创建按钮。每个按钮点击后打开右侧新建抽屉，**并自动切换到对应的资产类型表单**：

| 按钮图标 | title | 打开的抽屉模式 | 对应资产类型 |
|---------|-------|--------------|-------------|
| `fa-box`（箱子） | 长期主义 | `longterm` | 细水长流 |
| `fa-boxes-stacked`（堆叠箱子） | 囤货储备 | `stockpile` | 储备幸福 |
| `fa-book`（书本） | 收藏状态 | `recordOnly` | 收藏状态 |
| `fa-credit-card`（卡片） | 储值次卡 | `storedCard` | 储值卡 |
| `fa-microchip`（芯片） | 数字订阅 | `digitalSub` | 数字化订阅 |

物尽其用页面的「添置新物」按钮固定打开 `longterm`（细水长流）模式。
会员中心页面的「绑定新权益」按钮固定打开 `digitalSub`（数字化订阅）模式。

所有按钮最终调用同一个 `toggleDrawer(true, mode)` 函数，只是传入的 `mode` 参数不同。

---

## 七、卡片通用规格

### 实体物品卡片 `asset-card`
- 布局：`display: flex; flex-direction: column; justify-content: space-between`
- 最小高度：`250px`
- 底：`var(--bg-surface)`
- 圆角：`16px`（手绘覆写为不对称圆角：`255px 15px 225px 15px / 15px 225px 15px 255px`）
- 边框：`1px solid rgba(212,199,176,0.05)`（手绘覆写为 `1.5px solid var(--primary-dark)`）
- 内边距：`padding: 24px`
- 阴影：`3px 3px 0px rgba(34,34,34,0.1)`
- 悬浮：`transform: translate(2px, 2px)`，阴影收缩
- 蜡笔底色：通过 `::before` 伪元素 + `data-category` 属性控制
  - `data-category="longterm"` → 底 `var(--crayon-green)` `#9BB8A7`，opacity 0.2
  - `data-category="stockpile"` → 底 `var(--crayon-yellow)` `#EAD2AC`，opacity 0.2
  - `data-category="record"` → 底 `var(--crayon-pink)` `#E6B1B1`，opacity 0.2
  - `::before` 定位：`position: absolute; top: -3px; left: 5px; right: -3px; bottom: 2px; z-index: -1`
  - 形状：`border-radius: 12px 200px 25px 150px / 150px 25px 200px 12px`

### 数字订阅卡片 `sub-card`
- 与 `asset-card` 相似，但专门有 `overflow: hidden` 用于裁切进度条
- 布局：`display: flex; flex-direction: column; justify-content: space-between; text-align: center`
- 最小高度：`200px`
- 内边距：`padding: 20px`
- 悬浮：`transform: translate(2px, 2px)`

### 卡片网格
- 实体物品 `items-grid`：`display: grid; grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); gap: 20px`
- 数字订阅 `sub-grid`：`display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 20px`
- 成就 `achievement-grid`：`display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 16px`

### 卡片头部 `card-meta`
- 布局：`display: flex; justify-content: space-between; align-items: flex-start`
- 标题 h4：`margin: 0; font-size: 18px; font-weight: 600; color: var(--text-light)`
- 副文字 p：`margin: 4px 0 0 0; font-size: 12px; color: var(--muted)`

### 卡片核心数值区 `card-core-value`
- 外间距：`margin: 10px 0`
- 核心数字 `.num`：`font-size: 32px; font-weight: 700; color: var(--text-light)`，`font-variant-numeric: tabular-nums`（数字等宽）
- 铜绿变色 `.num.status-perfect`：`color: var(--accent-green) !important`（`#55876F`）

### 标签系统 `tag`
- 字号：`11px`，内边距：`padding: 4px 10px`，圆角：`4px`
- 边框：`1.2px dashed currentColor`（虚线手绘签，颜色跟随文字）
- 底：`transparent`
- `.success`：颜色 `var(--accent-green)`
- `.warning`：颜色 `var(--accent-amber)`
- 文字标签 `#coatTag`：初始 `color: var(--primary)`，`.success` 时变 `var(--accent-green)`
- 收藏标签 `#braceletTag`：`cursor: pointer`，`color: var(--accent-rose)` 或 `var(--text-main)`

### 操作栏 `card-action-row`
- 布局：`display: flex; justify-content: space-between; align-items: center`
- 上分隔线：`border-top: 1px solid rgba(212,199,176,0.04)`（手绘覆写为 `1.5px solid var(--primary-dark)`）
- 上内边距：`padding-top: 12px; margin-top: 10px`
- 文字：`font-size: 12px; color: var(--muted)`

---

## 八、每种卡片的精确设计

### 类型1 — 细水长流·按次（`LONG_TERM_PER_USE`）

**外层容器**：`asset-card`，`data-category="longterm"`，边框色 `rgba(138,184,154,0.2)`

**蜡笔底色**：抹茶绿 `var(--crayon-green)`

**头部标签**（两枚）：
- 左标签（陪伴天数）：底 `rgba(85,135,111,0.08)`，文字 `var(--accent-green)`，图标 `fa-calendar-check`，文案 "已陪伴 X 天"
- 右标签（状态）：图标 `fa-star`，文字"细水长流"，`color: var(--primary)`

**核心数字区**：
- 标签文字"当前成本"，字号 11px，颜色 `var(--muted)`
- ¥ 符号：18px，`var(--accent-green)`，font-weight 600
- 数字：class `num`，32px，font-weight 700，初始颜色 `var(--text-light)`
- 单位 "/ 次"：14px，`var(--muted)`
- 铜绿变色条件：`ratio ≤ 0.10` 时，数字加 `.status-perfect`，颜色变为 `var(--accent-green)`

**人文文案** `coatMsg`（字号 13px，颜色 `var(--muted)`，最小高度 36px）：
- 使用次数 = 0："它在衣柜角落睡得有点久了，下一个雨天，带它一起去看看世界吧。"
- 使用次数 = 1："太棒了，开启了第一次重逢！它正披在肩上为你遮风挡雨。"
- 成本比例 ≤ 0.70 且 > 0.50："感知到它高频陪伴的温度了吗？单次相遇成本已经开始平稳下降，谢谢你没有让它在角落里孤单落灰。"
- 成本比例 ≤ 0.50 且 > 0.30："消费泡沫已被成功斩断一半！每一次使用，都是你对盲目消费主义的一次优雅胜诉。"
- 成本比例 ≤ 0.30 且 > 0.10："它默默地融入了你的日常，成为了不用言语的底色。你正在用克制和珍惜，践行生活的断舍离美学。"
- 成本比例 ≤ 0.10："你们已经是形影不离的老朋友了，每一次使用都在榨干它当初昂贵的消费泡沫，真正赋予了它不负相遇的圆满。"

**操作栏**（底部一行，从左到右）：
- 历史轨迹按钮 `btn-secondary-sm`，图标 `fa-clock-rotate-left`
- 归档按钮 `btn-secondary-sm`，颜色 `var(--warning)`，图标 `fa-box-archive`
- 删除按钮 `btn-secondary-sm`，颜色 `var(--danger)`，图标 `fa-trash-can`
- 打卡按钮 `btn-action-sm`（靠右），图标 `fa-feather-pointed`，旁边显示累计使用次数

**计算逻辑**：
```
ratio = (购入价格 ÷ 使用次数) ÷ 购入价格
当前成本 = 购入价格 ÷ 使用次数 （使用次数>0时）
         = 购入价格           （使用次数=0时）
```

**标签联动**：ratio ≤ 0.10 时，右标签从 `fa-star` 细水长流 → `fa-leaf` 物尽其用，颜色从奶茶 `var(--primary)` → 铜绿 `var(--accent-green)`，class 从 `tag` → `tag success`

---

### 类型2 — 细水长流·按天（`LONG_TERM_PER_DAY`）

**外层容器**：同按次型，`data-category="longterm"`，边框 `rgba(138,184,154,0.2)`

**蜡笔底色**：抹茶绿

**头部标签**（两枚）：
- 左标签（陪伴天数）：同按次型，显示"已陪伴 X 天"
- 右标签（状态）：图标 `fa-clock`，文字"按天陪伴"，底 `rgba(85,135,111,0.08)`，文字 `var(--accent-green)`

**核心数字区**：
- 标签文字"日均持有成本"，11px，`var(--muted)`
- ¥ 符号：18px，`var(--accent-green)`，font-weight 600
- 数字：class `num`，当前日均成本
- 单位 "/ 天"：14px，`var(--muted)`
- 铜绿变色条件：日均成本 < ¥10 时触发 `status-perfect`

**文案**（固定）："每一天的陪伴都在悄悄降低它的持有成本。"（精简版，去掉了第二句"不需要刻意记录，时间会帮你说清一切"）

**操作栏**：
- 历史轨迹、归档、删除（同按次型）
- 靠右显示绿色文字 `fa-infinity` 自然陪伴中（无需点击，纯展示）

**计算逻辑**：
```
日均成本 = 购入价格 ÷ max(已持有天数, 1)
已持有天数 = 今天日期 - 购入日期
```

**标签联动**：无手动打卡，天数随日期自动增长，日均成本自然下降。

---

### 类型3 — 收藏状态（`COLLECTIBLE`）

**外层容器**：`asset-card`，`data-category="record"`，边框 `rgba(207,168,122,0.15)`

**蜡笔底色**：肉粉 `var(--crayon-pink)`

**头部标签**（两枚）：
- 左标签（陪伴天数）：底 `rgba(166,82,82,0.08)`，文字 `var(--accent-rose)`，图标 `fa-calendar-check`，文案 "已陪伴 X 天"
- 右标签（收藏状态）：**可点击**（`cursor: pointer`），图标 `fa-check-circle`，文字为当前收藏状态

**收藏状态三态切换**（点击右标签循环）：
1. "日常使用中" → 标签底 `rgba(212,199,176,0.06)`，文字 `var(--text-main)`，标签旁边颜色 `var(--accent-rose)`
2. "完美珍藏中" → 标签底 `rgba(166,82,82,0.08)`，文字 `var(--accent-rose)`，图标 `fa-gem`
3. "计划转手中" → 标签底 `rgba(207,168,122,0.08)`，文字 `var(--accent-amber)`，图标 `fa-share`

每次切换状态后，后端写入一条操作流水，记录状态变更。

**核心数字区**：**无**（不计算成本分摊）

**文案**（固定）："每一次目光停留，都像是和自己的一次小小确认。"

**操作栏**：
- 历史轨迹、归档、删除（同其他类型）
- 无打卡按钮

---

### 类型4 — 储备幸福 / 囤货（`STOCKPILE`）

**外层容器**：`asset-card`，`data-category="stockpile"`，边框 `rgba(207,168,122,0.2)`

**蜡笔底色**：麦穗黄 `var(--crayon-yellow)`

**头部**：
- 标题行：图标 `fa-box`，颜色 `var(--accent-amber)`，物品名称
- 副文字："当前均价 ¥XX.XX / 箱"（综合均价 = 所有批次总金额 ÷ 所有批次总数量）

**右标签**（状态标签，随库存水位变化）：
- 库存 > 安全线："状态充盈"，`tag success`，图标 `fa-circle-check`
- 0 < 库存 ≤ 安全线："余量轻盈"，`tag warning`，图标 `fa-hourglass-half`
- 库存 = 0："库存耗尽"，`tag danger`

**SVG 手绘量杯**（`stock-cup-container`）：
- 量杯容器：`width: 40px; height: 60px`，flex-shrink: 0
- 使用 SVG 滤镜 `url(#hand-drawn)` 渲染手绘效果
- 液体填充：`fill="#F1C292"`，opacity 0.85
- 液面曲线：贝塞尔路径 `M 0,50 Q 25,45 50,50 T 100,45`
- 杯体轮廓：深色描边 `stroke="#1A1A1A" stroke-width="3"`
- 刻度线：三条横线，分别在 y=40, y=70, y=100 位置
- **液面高度联动**：`updateCupLevel(percentage)` 根据库存百分比（当前库存 ÷ 初始库存 × 100）平移液体路径的 Y 坐标，低于 30% 时改变液面波浪形状

**价格区间滑块**（`price-range-bar`）：
- 标签："历史价格监控区间"，11px，`var(--muted)`
- 轨道 `range-track`：`height: 4px`，底 `rgba(212,199,176,0.1)`，圆角 2px
- 当前圆点 `range-current-dot`：`8px × 8px`，圆形，底 `var(--text-light)`，`position: absolute; top: -2px`，水平位置根据最近一次补货单价在史低和史高之间的百分比定位
- 标签行 `range-labels`：flex 两端对齐，左"史低 ¥XX"，右"史高 ¥XX"，11px，`var(--muted)`

**比价反馈栏** `price-feedback-bar`：
- 默认 `display: none`，`.active` 时 `display: block`
- 底 `rgba(95,95,255,0.05)`，边框 `1px dashed rgba(95,95,255,0.15)`，圆角 8px
- 内边距 8px 12px，字号 11px
- 补货后显示：低单价→绿色边框；高单价→琥珀边框

**操作栏**：
- 左：文字显示"库存剩 X 箱 (安全线 Y 箱)"
- 右（5个按钮）：
  1. 历史轨迹（时钟图标）
  2. 归档（箱子图标，warning色）
  3. 删除（垃圾桶图标，danger色）
  4. 消耗按钮（打开箱子图标，`btn-action-sm`）
  5. 快捷补货按钮（购物车图标，warning边框，`btn-action-sm`）

**计算逻辑**：
```
当前库存 = 所有批次入库总量 - 所有消耗总量
综合均价 = 所有批次总金额 ÷ 所有批次总数量
本次单价 = 本次补货总金额 ÷ 本次补货数量
补货后 → 均价变化 → 圆点位置变化 → 量杯液面上升
消耗后 → 库存变化 → 标签状态可能变化 → 量杯液面下降
比价：本次单价 ≤ 史低 → 绿色反馈；本次单价 > 史高 → 琥珀反馈
```

---

### 类型5 — 周期续费（`SUBSCRIPTION_MONTHLY` / `QUARTERLY` / `YEARLY`）

**外层容器**：`sub-card`，边框 `rgba(107,138,158,0.2)`

**主题色**：`var(--accent-blue)` `#6B8A9E`（盐系蓝灰）

**卡片内容（从上到下）**：
1. 图标：24px，颜色 `var(--accent-blue)`，如 `fa-film`（Netflix）
2. 服务名称 h4：`font-size` 继承，`color: var(--text-light)`，可双击编辑（`cursor: pointer`）
3. 价格大字：20px，font-weight 700，如 "¥ 45.00 / 月"
4. 状态文字：11px
   - 剩余 > 3天："距离续费还有 X 天"，颜色 `var(--muted)`
   - 剩余 ≤ 3天："X 天后自动扣费"，颜色 `var(--warning)`，触发 `critical-alert` 呼吸动效
5. 按钮行（居中，`z-index: 2` 确保在进度条上方）：
   - 历史历程（时钟图标，`btn-secondary-sm`）
   - 暂停订阅 / 归档（箱子图标，warning色）
   - 删除（垃圾桶图标，danger色）
6. 液体进度条 `sub-progress-bar`：
   - 定位：`position: absolute; bottom: 0; left: 0; width: 100%; height: 100%`，`z-index: 0`
   - 填充条 `sub-progress-inner`：底 `var(--crayon-green)`，opacity 0.25，从底部向上
   - 高度计算：`剩余天数 ÷ 周期总天数 × 100%`
   - 过渡：`height 0.8s cubic-bezier(0.4, 0, 0.2, 1)`
   - critical 状态：底变 `var(--crayon-pink)`，opacity 0.22

**呼吸动效**（`sub-card.critical-alert`）：
- `animation: warningGlow 2s infinite alternate`
- 0%：`box-shadow: 0 0 5px rgba(245,158,11,0.05)`
- 100%：`box-shadow: 0 0 20px rgba(245,158,11,0.2)`

**计算逻辑**：
```
周期天数：MONTHLY=30, QUARTERLY=90, YEARLY=365
剩余天数 = nextBillingDate - 今天日期
进度条高度 = 剩余天数 ÷ 周期天数 × 100%（最小5%防止完全看不见）
```

---

### 类型6 — 按量计费（`SUBSCRIPTION_METERED`）

**外层容器**：`sub-card`，边框 `rgba(138,184,154,0.25)`

**主题色**：`var(--accent-rose)` `#A65252`（枯玫瑰）

**卡片内容（从上到下）**：
1. 图标：24px，颜色 `var(--accent-rose)`，如 `fa-microchip`
2. 服务名称 h4（可双击编辑）
3. 余额大字：18px，font-weight 700，"余量: ¥ XXX.XX"
4. 提示文字：11px，`var(--muted)`
5. 按钮行：历史历程 + 归档 + 删除 + 快捷充值 + 记录消耗

**进度条计算**：
```
进度条高度 = 当前余额 ÷ 累计充值总额 × 100%（最大100%）
```

---

### 类型7 — 永久有效（`SUBSCRIPTION_LIFETIME`）

**外层容器**：`sub-card`，边框 `rgba(166,82,82,0.18)`

**主题色**：`var(--accent-green)` `#55876F`（铜绿）

**卡片内容**：
1. 图标：`fa-infinity`，颜色 `var(--accent-green)`
2. 价格："¥ XXX.XX 买断"
3. 状态："永久有效 · 已陪伴 X 天"，颜色 `var(--accent-green)`
4. 按钮：历史历程 + 归档 + 删除（无充值/消耗）

**进度条**：固定 100%

---

### 类型8 — 储值次卡（`STORED_TIME_CARD`）

**外层容器**：`sub-card`，边框 `rgba(138,184,154,0.18)`

**主题色**：`var(--primary)` `#CBAF88`（奶茶/金色）

**卡片内容**：
1. 图标：24px
2. 服务名称 h4（可双击编辑）
3. 大字：20px，font-weight 700，"X 次剩余"
4. 小字："单次核算成本: ¥XX.XX"（累计充值总额 ÷ 累计总次数）
5. 按钮行：历史历程 + 归档 + 删除 + 充值次数 + 核销

**进度条计算**：
```
进度条高度 = 剩余次数 ÷ 累计总次数 × 100%
```

**核销按钮行为**：
- 剩余次数 > 0：次数减一，进度条下降
- 剩余次数 = 0：按钮禁用，Toast"卡券完全履约！"

---

### 类型9 — 储值量卡（`STORED_AMOUNT_CARD`）

**外层容器**：`sub-card`，边框 `rgba(207,168,122,0.18)`

**主题色**：`var(--warning)` `#D9A87C`（焦糖琥珀）

**卡片内容**：
1. 图标：24px，如 `fa-utensils`
2. 服务名称 h4（可双击编辑）
3. 大字："余额 ¥XXX"
4. 小字："累计充值 ¥XXX · 已消费 ¥XXX"
5. 按钮行：历史历程 + 归档 + 删除 + 充值 + 记录消费

**进度条计算**：
```
进度条高度 = 当前余额 ÷ 累计充值总额 × 100%
```

---

## 九、右侧抽屉（新建 / 编辑资产）

### 遮罩层 `modal-overlay`
- 定位：`position: absolute; top: 0; left: 0; width: 100%; height: 100%`（相对于 window-container）
- 底：`rgba(0,0,0,0.5)`
- 层级：`z-index: 100`
- 默认 `opacity: 0; pointer-events: none`
- 打开 `.open`：`opacity: 1; pointer-events: auto`

### 抽屉面板 `drawer-content`
- 宽度：`420px`，高度：`100%`
- 底：`var(--bg-surface)`，左边框：`1.5px solid var(--primary-dark)`
- 内边距：`padding: 28px 36px`
- 布局：`display: flex; flex-direction: column; gap: 14px`
- 初始：`transform: translateX(100%)`（藏在右侧屏幕外）
- 打开：`.modal-overlay.open .drawer-content` → `transform: translateX(0)`
- 过渡：`0.3s cubic-bezier(0.25, 0.8, 0.25, 1)`
- 阴影：`4px 4px 0px var(--primary-dark)`
- 可滚动（内容溢出时）

### 表单组 `form-group`
- 布局：`display: flex; flex-direction: column; gap: 6px`
- 标签：`font-size: 13px; color: var(--muted)`

### 表单行 `form-row`
- 布局：`display: flex; gap: 20px`，子元素 `flex: 1`

### 分段控制器 `segmented-control`
- 底：`var(--input-bg)`，边框：`1.5px solid var(--primary-dark)`，圆角 8px
- 内边距 5px，gap 3px

### 分段按钮 `segment-btn`
- `flex: 1`，`padding: 10px 0`，居中
- 字号：12px，font-weight 600，颜色 `var(--muted)`
- 激活：底 `var(--primary)`，文字 `var(--primary-dark)`，font-weight 700

### 图标选择器
- 网格：`display: grid; grid-template-columns: repeat(7, 1fr); gap: 8px; margin-top: 6px`
- 单个选项：`40px × 40px`，圆角 8px，底 `var(--input-bg)`
- 边框：`1.5px solid rgba(203,175,136,0.15)`（奶茶色边框）
- 文字：`font-size: 17px; color: var(--muted); line-height: 1`
- 悬浮：边框变 `var(--primary)`，文字变 `var(--primary-dark)`，底淡染 `rgba(203,175,136,0.15)`
- 选中：边框变 `var(--primary)`，底 `rgba(203,175,136,0.25)`

### 表单底部 `drawer-foot`
- 布局：`display: flex; justify-content: space-between`
- 上分隔线：`1.5px solid var(--primary-dark)`，padding-top 15px
- `margin-top: auto`（推到底部）

### 单选框
- 使用 `accent-color: var(--primary)` 着奶茶色
- `vertical-align: -2px !important`（与文字水平中线对齐）

---

## 十、历史时间轴抽屉

- 面板宽度：`380px`
- 时间轴 `history-timeline`：`display: flex; flex-direction: column; gap: 14px`
- 每节点 `history-node`：左边框 `2px solid var(--primary)`，左内边距 14px
- 节点圆点：`::before` 伪元素，`8px × 8px`，圆形，底 `var(--primary)`，`left: -5px; top: 4px`
- 时间文字：`font-size: 11px; color: var(--muted); margin-bottom: 2px`
- 正文：`font-size: 13px`

---

## 十一、弹窗系统

### 通用弹窗 `pop-modal-overlay`
- 定位：`position: absolute`（相对于 window-container），铺满
- 底：`rgba(0,0,0,0.5)`，层级 `z-index: 300`
- 默认隐藏，`.open` → `opacity: 1; pointer-events: auto`

### 弹窗内容 `pop-modal-content`
- 宽度：`330px`，内边距：`24px`，圆角：`10px`
- 底：`var(--bg-surface)`，边框：`1px solid var(--border-weak)`
- 布局：`display: flex; flex-direction: column; gap: 20px`

### Toast 通知容器
- 定位：`position: absolute; bottom: 24px; right: 24px; z-index: 600`
- 单条 Toast `win-toast`：宽度 `340px`，底 `var(--bg-surface)`，圆角 8px
- 左边框 4px 强调色：success 绿 / warning 琥珀 / danger 红
- 入场动画：`slideIn 0.3s`（从右平移入）

---

## 十二、成就卡片

- 网格：`display: grid; grid-template-columns: repeat(auto-fill, minmax(240px, 1fr)); gap: 16px`
- 单卡 `achievement-card`：底 `var(--bg-surface)`，边框 `1px solid rgba(212,199,176,0.05)`，圆角 14px，内边距 18px
- 锁定态 `.locked`：`opacity: 0.35; filter: grayscale(0.6)`
- 解锁态 `.unlocked`：边框 `rgba(180,134,80,0.2)`（焦糖金），无绿三角
- 图标 `ach-icon`：`font-size: 20px`，`40px × 40px`，圆角 8px，底 `rgba(34,34,34,0.04)`
  - 解锁态底 `rgba(234,210,172,0.25)`，颜色 `#B38650`
- 进度条 `ach-progress-bar`：`height: 3px`，底 `var(--progress-bg)`，圆角 2px
  - 填充 `ach-progress-inner`：底 `var(--primary)`，解锁态变 `#B38650`，过渡 `width 0.5s ease`
- 分类标题 `ach-category-title`：`grid-column: 1 / -1`（横跨整行），`font-size: 14px`，`color: var(--text-light)`，margin-top 8px，下划线分隔

---

## 十三、手绘风格覆写规则

这些规则在 CSS 最后统一覆写，优先级最高：

- 所有卡片、弹窗统一使用不对称圆角：`border-radius: 255px 15px 225px 15px / 15px 225px 15px 255px !important`
- 统一手绘炭笔框：`border: 1.5px solid var(--primary-dark) !important`
- 按钮统一：`border: 1.5px solid var(--primary-dark) !important; border-radius: 8px !important; box-shadow: 2px 2px 0px rgba(34,34,34,0.1) !important`
- 悬浮统一：`background: color-mix(in srgb, var(--primary) 30%, transparent) !important; transform: translate(2px, 2px)`
- 卡片悬浮：`transform: translate(2px, 2px); box-shadow: 1px 1px 0px rgba(34,34,34,0.08) !important`
- 分隔线：`border-top: 1.5px solid var(--primary-dark) !important`
- 表单输入框：`border: 1.5px solid var(--primary-dark) !important; border-radius: 8px !important`
- 标签：`border-radius: 4px !important; background: transparent !important; border-color: currentColor !important`
- 进度条：`position: absolute !important; bottom: 0 !important; left: 0 !important; width: 100% !important; height: 100% !important; background: transparent !important; z-index: 0`
- 全局去荧光：`* { box-shadow: none !important; text-shadow: none !important }`

---

## 十四、关键数值公式汇总

| 公式 | 变量来源 | 触发时机 |
|------|---------|---------|
| 单次成本 = 购入价格 ÷ 使用次数 | 价格：asset.purchasePrice；次数：asset.usageCount | 每次打卡 |
| 成本比例 ratio = 单次成本 ÷ 购入价格 | 同上 | 每次打卡 |
| 日均成本（按天资产）= 购入价格 ÷ 已持有天数 | 价格：asset.purchasePrice；天数：getDaysHeld() | 页面加载（自然递增） |
| 大盘日均（仪表盘） = 所有 LONT_TERM 资产（按次+按天）的 (购入价格 ÷ 持有天数) 求算术平均 | 所有 LONG_TERM_PER_USE + LONG_TERM_PER_DAY 资产 | 每次操作后 |
| 库存 = 入库总量 - 消耗总量 | 入库：ΣpurchaseBatches；消耗：ΣusageLogs(消耗) | 每次消耗/补货 |
| 综合均价 = Σ批次总金额 ÷ Σ批次数量 | 所有 purchaseBatches | 每次补货 |
| 续费剩余天数 = nextBillingDate - today | nextBillingDate | 页面加载 |
| 进度条% = 剩余天数 ÷ 周期天数 × 100 | 周期：MONTHLY=30, QUARTERLY=90, YEARLY=365 | 页面加载 |
| 次卡进度% = 剩余次数 ÷ 累计总次数 × 100 | remainingTimes, totalTimes | 每次核销/充值 |
| 量卡进度% = 当前余额 ÷ 累计充值 × 100 | cardBalance, totalTopup | 每次消费/充值 |
| 按量进度% = 当前余额 ÷ 累计充值 × 100 | apiBalance, totalCharged | 每次充值/消耗 |

---

## 十五、成就系统

### 成就数据结构（36条，7大类）

每个成就包含：ID（如a01）、分类、图标（FontAwesome class）、名称、描述、目标值。

**物品收集（4条）**：初识资产（1件）→ 小有所成（5件）→ 琳琅满目（10件）→ 博物收藏家（20件）。图标从 fa-trophy → fa-medal → fa-award → fa-crown 递进。

**细水长流（6条）**：初次相遇（打卡1次）→ 常伴左右（10次）→ 习惯成自然（30次）→ 百次相伴（100次）→ 物尽其用（单次成本≤原价10%）→ 日积月累（按天陪伴超365天）。

**储备幸福（8条）**：未雨绸缪（拥有1件囤货）→ 满载而归（完成1次补货）→ 库存告急（库存低于安全线）→ 弹尽粮绝（库存为0）→ 仓廪丰实（3件以上囤货）→ 温柔相遇（触发1次史低价）→ 砍价高手（触发3次史低价）→ 高价也从容（补货单价高于历史最高价）。

**收藏纪念（4条）**：珍视之物（1件收藏）→ 岁月珍藏（陪伴30天）→ 传家之宝（陪伴365天）→ 物语收藏家（3件以上收藏）。

**数字订阅（6条）**：初试订阅（1个订阅）→ 数字游民（3个以上）→ 精打细算（暂停/归档1个）→ 终身相伴（1个永久有效）→ 储值达人（2个以上储值卡）→ 订阅掌控者（同时有续费+按量+永久+储值4种）。

**资产总览（5条）**：万元户（总价值≥¥10,000）→ 资产丰盈（≥¥50,000）→ 多元配置（同时有实体+数字+收藏3种）→ 仓库管理员（仓库中有过1件归档物品）→ 全面掌控（解锁全部5种资产类型）。

**里程碑（3条）**：日省一文（1件物品单次成本<¥1）→ 日省百文（3件物品单次成本<¥10）→ 归藏大师（共解锁25个成就）。

### 成就卡片渲染逻辑

每次操作后调用成就检查。遍历所有成就定义，逐个计算 `current` 值（如当前物品总数、总打卡次数等）。若 `current ≥ goal`，该成就标记为已解锁。比较操作前和操作后的已解锁集合，差集即为新解锁成就，逐个弹Toast通知。**每个成就仅通知一次**（通过已通知ID集合去重）。

### 成就卡片DOM结构

- 分类标题：`ach-category-title`，字号14px，font-weight 600，颜色 `var(--text-light)`，横跨整行（`grid-column: 1 / -1`），margin-top 8px，下划线分隔
- 每卡内部布局：上方横向flex（图标40×40px圆角8px + 名称+描述），下方进度条+进度文字
- 已解锁状态：extra边框 `rgba(180,134,80,0.2)`（焦糖金），图标底 `rgba(234,210,172,0.25)` 颜色 `#B38650`，进度填充 `#B38650`
- 未解锁状态：`opacity: 0.35; filter: grayscale(0.6)`，图标底 `rgba(34,34,34,0.04)` 颜色 `var(--muted)`

---

## 十六、收藏状态三态切换

三种状态：日常使用中、完美珍藏中、计划转手中。

每种状态有对应的图标和颜色：
- 日常使用中：图标 `fa-check-circle`，标签底 `rgba(212,199,176,0.06)`，文字 `var(--text-main)`
- 完美珍藏中：图标 `fa-gem`，标签底 `rgba(166,82,82,0.08)`，文字 `var(--accent-rose)`（枯玫瑰 `#A65252`）
- 计划转手中：图标 `fa-share`，标签底 `rgba(207,168,122,0.08)`，文字 `var(--accent-amber)`（焦糖琥珀 `#B38650`）

切换逻辑：每张收藏卡独立维护一个状态索引（0/1/2），点击标签时索引+1取模3，更新标签的 innerHTML、背景色、文字色。

---

## 十七、Toast 通知系统

- 容器定位：`position: absolute; bottom: 24px; right: 24px; z-index: 600`
- 单条Toast：宽度340px，底 `var(--bg-surface)`，圆角8px，左边框4px强调色
- 类型与颜色：success 绿 `var(--success)`、warning 琥珀 `var(--warning)`、danger 红 `var(--danger)`、info 默认奶茶 `var(--primary)`
- 入场动画：从右平移入 `slideIn 0.3s cubic-bezier(0.1,0.9,0.2,1)`
- 自动消失：3.5秒后（`setTimeout 3500ms`）触发移除动画（opacity 变0 + translateX 右移），过渡0.4秒后 remove DOM
- 支持手动关闭按钮

---

## 十八、主题切换

- 触发：导航栏底部半透明图标
- 实现：`document.body.classList.toggle('light')`
- CSS变量体系自动切换所有颜色
- 蜡笔色（crayon系列）和主题强调色（accent系列）在深浅模式间不变
- 切换后偏好通过 `setConfig('theme', ...)` 持久化到后端
- 启动时 `loadTheme()` 从后端读取偏好并设置 `body.classList`

---

## 十九、头像选择面板

- 触发：点击用户抽屉内的圆形头像
- 类型：`pop-modal-overlay` + `pop-modal-content`（居中浮层，非侧边绝对定位）
- 底：`var(--bg-surface)`，边框 `1px solid var(--border-weak)`，圆角10px，内边距24px（由 `.pop-modal-content` 统一样式控制；CSS 中 `.avatar-picker-panel` 类定义存在但未被模板使用）
- 网格：`display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px`（4列）
- 每选项：48×48px 圆形，底 `var(--bg-surface)`，边框 2px transparent
- 选中态：边框 `#B38650`（焦糖金）
- 悬浮态：边框 `var(--primary)`，底 `rgba(95,95,255,0.08)`，`scale(1.08)`
- 可选图标8个：user-shield, user-astronaut, user-ninja, user-secret, cat, fish, otter, kiwi-bird
- 选择后：更新抽屉头像图标，Toast提示"头像已更新"，250ms后自动关闭面板

---

## 二十、编辑元数据弹窗

- 触发：双击任意卡片的标题区域 → 调用 `openEditAsset(a)` 打开右侧新建抽屉并切换到编辑模式
- 类型：复用 `modal-overlay` + `drawer-content`（右侧滑入抽屉，宽420px），`editAsset` 存在时标题显示"编辑资产信息"
- 表单字段：名称、价格、购入日期、图标、备注——全部回填当前值
- 资产类型：只读灰色锁定（不可修改），显示当前类型名称
- 子类型字段（分摊维度/储值方式/计费模式等）：只读锁定（不可修改）
- 提交后：调用 `api.updateAsset(id, payload)` 更新后端 → `loadAll()` 全量刷新前端显示

---

## 二十一、修改昵称弹窗

- 触发：点击用户抽屉昵称旁的铅笔图标（`event.stopPropagation()` 防止冒泡关抽屉）
- 类型：`pop-modal-overlay` + `pop-modal-content`
- 标题："修改个性昵称"，图标 `fa-pencil`
- 输入框：默认填当前昵称，maxlength 20
- 提示："昵称用于界面展示，不影响账户登录凭证"
- 提交：更新 `barUserName` 文本节点（保留铅笔图标）

---

## 二十二、修改密码弹窗

- 触发：点击用户抽屉"修改密码"按钮
- 字段：当前密码 + 新密码 + 确认新密码，全部 type="password"
- 校验：当前密码必须正确 → 新密码≥6位含字母和数字 → 两次输入一致
- 成功后Toast"密码已更新"

---

## 二十三、归档/封存系统

**归档流程**：
1. 点击归档按钮 → `confirmArchive(cardId, key, assetName, originType)`
2. 弹出确认框（`archiveConfirmModal`），border-color `rgba(245,158,11,0.3)`
3. 确认文案包含："归档后该资产将暂时退出日常流速分摊大盘""所有历史历程完整保留""可在仓库中一键唤醒恢复"
4. 点击确认 → `executeArchive()` → `archiveCard()`
5. `archiveCard` 执行：
   - 将卡片HTML缓存到 `archivedData[cardId]`（含原始key、name、type）
   - 从原页面网格（physicalGrid/digitalGrid）中移除DOM
   - 对应大盘物语条目隐藏（`display:none`）
   - 在仓库网格中插入"休眠卡片"——名称灰色带删除线、标签"仓库封存"、有"查看封存历程"和"重启唤醒"两个按钮
   - 检查仓库空态（无卡片时显示空态提示）

**唤醒流程**：
1. 点击仓库中卡片的"重启唤醒"按钮 → `restoreCard(cardId)`
2. 从 `archivedData` 取出缓存HTML
3. 根据type（physical/digital）插回对应网格
4. 大盘物语恢复显示
5. 删除缓存、检查仓库空态

---

## 二十四、所有弹窗清单

| 弹窗/组件 | 宽度 | 触发方式 | 内容 |
|--------|------|---------|------|
| `addDrawer` | 420px | 点"添置新物"或仪表盘快速按钮 | 新建资产表单（5种模式分段控制器切换） |
| `addDrawer`（编辑模式） | 420px | 双击卡片标题区域 | 编辑资产信息（类型只读锁定） |
| `historyDrawer` | 380px | 点卡片上的历史轨迹按钮 | 生命周期时间轴 |
| `confirmModal`（删除/归档） | 330px | 点卡片上的垃圾桶/归档按钮 | 二次确认，共享 `confirmOpen` 状态，由 `confirmType` 区分 |
| `formModal`（通用快捷操作） | 330px | 点卡片的补货/充值/核销/消费按钮 | 由 `formModalMode` 区分：`restock`（补货）、`topup_time`（次卡充值）、`topup_amount`（量卡充值）、`spend`（消费） |
| `avatarPicker` | 330px | 点用户抽屉头像 | 8个图标选择网格（4列） |
| `nicknameModal` | 330px | 点昵称旁的铅笔图标 | 修改个性昵称 |
| `passwordModal` | 330px | 点"修改密码"菜单按钮 | 旧密码+新密码+确认密码 |

所有 `pop-modal-overlay` 弹窗共享：`position: absolute`、铺满 window-container、`rgba(0,0,0,0.5)` 半透明黑遮罩、`z-index: 300`。点击遮罩层自身关闭（`@click.self="xxx=false"`）。

`addDrawer` 和 `historyDrawer` 是 `modal-overlay` 类型（右侧滑入抽屉，`z-index: 100`），点击遮罩层自身关闭。

---

## 二十五、盘点刷新与成就联动

每次操作后调用 `refreshInsightAndAchievements()`：
1. `updateAllDays()` — 重新计算所有物品的陪伴天数，更新卡片上的"已陪伴X天"标签和大盘物语中的天数
2. `checkAchievements()` — 遍历36条成就定义，根据当前数据计算current值，更新成就网格中的进度条和文字，统计已解锁数
3. 更新侧边栏统计数字（物品件数、成就数、总价值）

**各成就的 current 计算逻辑**：
- 物品收集a01-a03：`totalItems`（当前总物品数）
- 细水长流a04-a07：`totalCheckIns`（总打卡次数）
- 物尽其用a08：是否有物品 `ratio ≤ 0.10`
- 日积月累a09：是否有按天物品 `daysHeld ≥ 365`
- 储备幸福a10-a14：囤货物品数、补货次数、是否有低库存/零库存
- 收藏纪念a15-a18：收藏物品数、最大陪伴天数
- 数字订阅a19-a24：订阅数、归档数、永久拥有数、储值卡数、类型覆盖数
- 资产总览a25-a29：总价值、是否有多种类型、归档数
- 里程碑a30-a32：单次成本<¥1的物品数、单次成本<¥10的物品数、已解锁成就总数

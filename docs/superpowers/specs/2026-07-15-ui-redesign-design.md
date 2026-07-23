# CleanSuper AI — UI 商业级重设计

## 目标
将现有"demo 感"界面升级为商业级精致深色 UI，统一视觉语言，覆盖全部页面。

## 约束（用户确认）
- 范围：全部页面统一翻新
- 视觉：保持深色，但更精致
- 技术栈：继续用 XML View + ViewBinding（不引入 Compose）
- 图标：引入 Material Symbols 矢量图标，替换所有文字符号
- 配色：保留紫青双色调，精修

## 设计方向：层级玻璃感
深色基底 + 半透明玻璃卡片 + 微妙渐变光晕 + 大圆角，营造高级、克制、有呼吸感的层次。

## 设计令牌

### 配色
| 用途 | 色值 |
|------|------|
| bg_primary | #FF0A0B10 |
| bg_elevated | #FF12141C |
| surface_card | #FF1A1D28 |
| surface_glass | #1AFFFFFF (10% 白) |
| surface_glass_stroke | #33FFFFFF (20% 白) |
| accent_primary | #FF7C6BFF |
| accent_secondary | #FF2DD4E8 |
| accent_primary_soft | #267C6BFF |
| accent_secondary_soft | #262DD4E8 |
| text_primary | #FFF2F3F7 |
| text_secondary | #FF9CA3B0 |
| text_tertiary | #FF5C6373 |
| success | #FF3DDC97 |
| danger | #FFFF6B6B |
| gold_text | #FFFFD37A |
| stroke_light | #1FFFFFFF |

### 尺寸
- 圆角：卡片 24dp，按钮 16dp，图标容器 14dp，胶囊 999dp
- 间距：页面边距 20dp，卡片内边距 22dp，卡片间距 14dp
- 字号：标题 22sp/粗，副标题 16sp/中粗，正文 14sp，说明 12sp，大数字 34sp/粗
- 图标：Material Symbols 24dp，容器 44×44dp

### 层次
- 禁用 elevation 阴影，改用 accent_primary_soft 径向渐变光晕（卡片顶部内侧 80dp 高）

## 组件系统
1. 玻璃卡片：surface_card 底 + stroke_light 1dp + 24dp 圆角；强调卡片用 surface_glass + 顶部光晕
2. 按钮：主(紫青渐变 16dp 圆角 56dp 高)、次(玻璃+描边)、文字、危险
3. 图标容器：44×44dp 14dp 圆角 surface_glass 底，图标 accent_primary/secondary
4. 进度：线性 3dp 渐变；环形 120dp 6dp 描边旋转扫描
5. 列表项：图标容器 + 标题/副标题 + 右操作，分隔线 1dp stroke_light，整组玻璃卡片包裹
6. 底部导航：Material Symbols，选中 Filled+accent_primary+渐变小圆点指示，未选 Outlined+text_tertiary，背景 surface_glass
7. 顶部栏：标题左对齐 22sp 粗，右侧 42×42dp 玻璃图标容器
8. 空状态：64dp 图标容器 + 标题 + 说明 + 可选按钮

## 逐页方案
- SplashActivity：玻璃容器 Logo + auto_awesome + 径向光晕 + 淡入
- MainActivity/底部导航：重做导航栏与图标
- HomeFragment：扫描大卡片(环形进度+数字) + 清理库 4 宫格 + 截图全宽卡
- SwipeFragment：全屏图 + 底部半透明玻璃信息条
- Compress/VideoCompress：原图/压缩后对比双卡 + 压缩比大数字 + 滑块 + 主按钮
- ToolsFragment：分组玻璃卡片工具列表
- CleanCenterFragment：大环形可清理空间 + 分组列表
- SimilarPhotos/Contacts/各详情：统一列表项 + 网格缩略图选中态
- ProfileFragment：金色会员卡 + 设置分组列表
- Privacy/AppManager/CleanCalendar/ImageEnhancer/SpeedTest：套用统一组件

## 资源产出清单
- colors.xml 增补令牌
- themes.xml 更新主题与新增样式
- 新增 Material Symbols 矢量 drawable（约 30 个）
- 新增/重做 drawable：玻璃卡片、渐变光晕、按钮渐变、图标容器、底部导航选中点、环形扫描背景
- 新增 CircularScanView 自定义 View
- 重做 29 个布局 XML

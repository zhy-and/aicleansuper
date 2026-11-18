# 多语言资源覆盖检查（2026-11-18）

对比 `app/src/main/res/values` 中可翻译的 `string`、`string-array` 与各语言目录。缺失时 Android 会回退到默认英文资源。

## 补齐结果

- 29 个语言目录均覆盖默认资源中的 706 个可翻译 key，包括 665 个字符串和 41 个字符串数组。
- 补齐 1,785 个原有的「语言目录 × key」缺口，其中日语 578 个。另外为全部 29 个目录补上应用选择框的无障碍标签。
- 同步「删除照片」文案，并修复 27 处定时推送备用文案中的占位符名称。数组长度、资源类型和占位符已与默认资源核对。
- 小组件本体使用本地化资源，预览使用固定英文资源。应用进入前台时会按当前应用语言发布动态桌面快捷方式。前台服务按应用语言生成折叠和展开通知的标签，并同步更新通知渠道名称。

以下为补齐前的检查记录。

## 本地推送

截图涉及的 31 个候选文案数组，在 29 个语言目录中均存在，且条数与默认资源一致。`scheduled_push_1/2` 各 10 条，其余各 5 条。

## 其余资源缺口

| 语言目录 | 缺少的默认资源 key 数 |
| --- | ---: |
| `values-ar` | 47 |
| `values-b+en+GB` | 49 |
| `values-b+es+419` | 47 |
| `values-b+fil+PH` | 47 |
| `values-b+fr+CA` | 47 |
| `values-b+ms+MY` | 47 |
| `values-b+pt+BR` | 47 |
| `values-b+zh+Hans` | 28 |
| `values-b+zh+Hant` | 28 |
| `values-de` | 44 |
| `values-es` | 47 |
| `values-fr` | 47 |
| `values-in` | 47 |
| `values-it` | 47 |
| `values-iw` | 47 |
| `values-ja` | 578 |
| `values-ko` | 47 |
| `values-nl` | 47 |
| `values-pl` | 47 |
| `values-pt` | 47 |
| `values-ro` | 47 |
| `values-ru` | 47 |
| `values-sv` | 47 |
| `values-ta` | 47 |
| `values-th` | 47 |
| `values-tr` | 47 |
| `values-vi` | 47 |
| `values-zh` | 16 |
| `values-zh-rCN` | 8 |

多数非中文语言共同缺少以下 47 个 key：

- `app_manager_uninstall_confirm`
- `cache_access_required`
- `cache_clean_action`
- `cache_clean_result`
- `cache_confirm_clean`
- `cleanup_report_complete`
- `cleanup_report_explore`
- `cleanup_report_go`
- `home_pending_items_format`
- `info_report_title`
- `large_file_access_required`
- `large_file_delete`
- `large_file_delete_amount`
- `large_file_delete_confirm`
- `large_file_delete_selected`
- `large_file_deleted_summary`
- `large_file_deselect_all`
- `large_file_deselect_all_shown`
- `large_file_filter_description`
- `large_file_filter_size`
- `large_file_filter_time`
- `large_file_filter_type`
- `large_file_open`
- `large_file_scanning`
- `large_file_select_all`
- `large_file_select_all_shown`
- `large_file_summary`
- `large_file_time_options`
- `large_file_type_options`
- `new_feature_badge_description`
- `sys_android_version`
- `sys_discharging`
- `sys_level`
- `sys_status`
- `traffic_empty`
- `traffic_guide_message`
- `traffic_loading`
- `traffic_usage_required`
- `vlt_delete_confirm`
- `vlt_deselect_all`
- `vlt_import_media`
- `vlt_originals_remain`
- `vlt_restore_confirm`
- `vlt_restored_count`
- `vlt_select_all`
- `widget_action_files`
- `widget_action_photos`

补齐前日语目录缺少 578 个默认资源 key。上表的基线已计入此前补齐的日语本地推送数组。

这份清单反映资源覆盖情况，不等同于逐句译文质量审核。

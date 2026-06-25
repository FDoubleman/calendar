# Mobile DB Schema

适用于 `data/calendar.db` 的 Android/iOS 直接查询说明。

## 推荐查询方式

- 主查询表：`calendar_day`
- 主键：`solar_date`
- 推荐按天查询：`WHERE solar_date = '2026-03-18'`
- 推荐按月查询：`WHERE year = 2026 AND month = 3`
- 多值字段优先读子表：
  - 节日：`calendar_day_festival`
  - 宜：`calendar_day_yi`
  - 忌：`calendar_day_ji`
- 如果移动端只想做快速展示，可直接使用主表中的：
  - `festivals_text`
  - `yi_text`
  - `ji_text`

## 主表

表名：`calendar_day`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `solar_date` | `TEXT` | 公历日期，格式 `YYYY-MM-DD`，主键 |
| `year` | `INTEGER` | 公历年 |
| `month` | `INTEGER` | 公历月 |
| `day` | `INTEGER` | 公历日 |
| `weekday_cn` | `TEXT` | 星期中文，如 `一`、`日` |
| `weekday_num` | `INTEGER` | 星期数字，`1-7`，其中 `7` 表示周日 |
| `is_weekend` | `INTEGER` | 是否周末，`0` 否，`1` 是 |
| `lunar_date` | `TEXT` | 农历日期 |
| `ganzhi_year` | `TEXT` | 干支年 |
| `ganzhi_month` | `TEXT` | 干支月 |
| `ganzhi_day` | `TEXT` | 干支日 |
| `zodiac` | `TEXT` | 生肖 |
| `constellation` | `TEXT` | 星座 |
| `pengzu_baiji` | `TEXT` | 彭祖百忌 |
| `taishen_direction` | `TEXT` | 胎神占方 |
| `year_wuxing` | `TEXT` | 年五行 |
| `season` | `TEXT` | 季节 |
| `month_wuxing` | `TEXT` | 月五行 |
| `xingxiu` | `TEXT` | 星宿 |
| `day_wuxing` | `TEXT` | 日五行 |
| `jieqi` | `TEXT` | 节气信息 |
| `chong` | `TEXT` | 冲 |
| `sha` | `TEXT` | 煞 |
| `liuyao` | `TEXT` | 六曜 |
| `shier_shen` | `TEXT` | 十二神 |
| `festivals_text` | `TEXT` | 节日聚合文本，使用 `、` 分隔 |
| `yi_text` | `TEXT` | 宜聚合文本，使用 `、` 分隔 |
| `ji_text` | `TEXT` | 忌聚合文本，使用 `、` 分隔 |
| `source_url` | `TEXT` | 原始抓取来源页面 |
| `fetched_at` | `TEXT` | 抓取时间，ISO 8601 格式 |

## 子表

### `calendar_day_festival`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `solar_date` | `TEXT` | 对应主表日期 |
| `sort_order` | `INTEGER` | 排序号，从 `0` 开始 |
| `name` | `TEXT` | 节日名称 |

### `calendar_day_yi`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `solar_date` | `TEXT` | 对应主表日期 |
| `sort_order` | `INTEGER` | 排序号，从 `0` 开始 |
| `name` | `TEXT` | 宜事项 |

### `calendar_day_ji`

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `solar_date` | `TEXT` | 对应主表日期 |
| `sort_order` | `INTEGER` | 排序号，从 `0` 开始 |
| `name` | `TEXT` | 忌事项 |

## 索引

- `calendar_day(year, month)`
- `calendar_day_festival(solar_date)`
- `calendar_day_festival(name)`
- `calendar_day_yi(solar_date)`
- `calendar_day_yi(name)`
- `calendar_day_ji(solar_date)`
- `calendar_day_ji(name)`

## Android/iOS 常用 SQL

### 按天查询完整信息

```sql
SELECT *
FROM calendar_day
WHERE solar_date = ?;
```

### 查询某天节日

```sql
SELECT name
FROM calendar_day_festival
WHERE solar_date = ?
ORDER BY sort_order;
```

### 查询某天宜忌

```sql
SELECT name
FROM calendar_day_yi
WHERE solar_date = ?
ORDER BY sort_order;

SELECT name
FROM calendar_day_ji
WHERE solar_date = ?
ORDER BY sort_order;
```

### 查询某月日历列表

```sql
SELECT solar_date, day, weekday_cn, is_weekend, lunar_date, jieqi, festivals_text
FROM calendar_day
WHERE year = ? AND month = ?
ORDER BY solar_date;
```

### 查询某个节日出现的日期

```sql
SELECT solar_date
FROM calendar_day_festival
WHERE name = ?
ORDER BY solar_date;
```

## 移动端建模建议

- 日期主实体建议直接映射 `calendar_day`
- `festival`、`yi`、`ji` 建议作为列表字段，按 `solar_date` 懒加载
- 若追求单次查询性能，可以只读取主表聚合字段
- 若追求结构化展示和搜索，优先读取子表
